/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.archive.ejb.store;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Sequence;
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;
import org.dcm4che.util.StringUtils;
import org.dcm4chee.archive.persistence.Availability;
import org.dcm4chee.archive.persistence.ContentItem;
import org.dcm4chee.archive.persistence.FileRef;
import org.dcm4chee.archive.persistence.FileSystem;
import org.dcm4chee.archive.persistence.FileSystemStatus;
import org.dcm4chee.archive.persistence.Instance;
import org.dcm4chee.archive.persistence.Patient;
import org.dcm4chee.archive.persistence.ScheduledProcedureStep;
import org.dcm4chee.archive.persistence.Series;
import org.dcm4chee.archive.persistence.StoreParam;
import org.dcm4chee.archive.persistence.Study;
import org.dcm4chee.archive.persistence.VerifyingObserver;
import org.dcm4chee.archive.persistence.StoreParam.StoreDuplicate;

import com.mysema.query.NonUniqueResultException;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Stateful
public class InstanceStoreBean implements InstanceStore {

    @PersistenceContext(unitName = "dcm4chee-arc", type = PersistenceContextType.EXTENDED)
    private EntityManager em;
    private Series cachedSeries;

    @Override
    public boolean addFileRef(String sourceAET, Attributes data, Attributes modified,
            FileRef fileRef, StoreParam storeParam) {
        FileSystem fs = fileRef.getFileSystem();
        Instance inst;
        try {
            inst = findInstance(data.getString(Tag.SOPInstanceUID, null));
            StoreDuplicate storeDuplicate = storeParam.getStoreDuplicate();
            switch (storeDuplicate) {
            case IGNORE:
            case STORE:
                coerceAttributes(data, inst, modified);
                data.coerceAttributes(inst.getAttributes(), modified);
                modified.remove(Tag.OriginalAttributesSequence);
                if (storeDuplicate == StoreDuplicate.IGNORE)
                    return false;
                break;
            case REPLACE:
                inst.setReplaced(true);
                inst = newInstance(sourceAET, data, fs.getAvailability(), storeParam);
                coerceAttributes(data, inst, modified);
                storeOriginalAttributes(sourceAET, data, modified, inst, storeParam);
                break;
            }
        } catch (NoResultException e) {
            inst = newInstance(sourceAET, data, fs.getAvailability(), storeParam);
            coerceAttributes(data, inst, modified);
            storeOriginalAttributes(sourceAET, data, modified, inst, storeParam);
        }
        fileRef.setInstance(inst);
        em.persist(fileRef);
        return true;
    }

    private static void coerceAttributes(Attributes data, Instance inst, Attributes modified) {
        Series series = inst.getSeries();
        Study study = series.getStudy();
        Patient patient = study.getPatient();
        data.coerceAttributes(patient.getAttributes(), modified);
        data.coerceAttributes(study.getAttributes(), modified);
        data.coerceAttributes(series.getAttributes(), modified);
    }

    private static void storeOriginalAttributes(String sourceAET, Attributes data,
            Attributes modified, Instance inst, StoreParam storeParam) {
        if (!modified.isEmpty() && storeParam.isStoreOriginalAttributes()) {
            Attributes instAttrs = inst.getAttributes();
            Attributes item = new Attributes(4);
            instAttrs.ensureSequence(Tag.OriginalAttributesSequence, 1).add(item);
            item.setDate(Tag.AttributeModificationDateTime, VR.DT, new Date());
            item.setString(Tag.ModifyingSystem, VR.LO, storeParam.getModifyingSystem());
            item.setString(Tag.SourceOfPreviousValues, VR.LO, sourceAET);
            item.newSequence(Tag.ModifiedAttributesSequence, 1).add(modified);
            inst.setAttributes(instAttrs, storeParam);
        }
    }

    @Override
    public Instance newInstance(String sourceAET, Attributes data, Availability availability,
            StoreParam storeParam) {
        Instance inst = new Instance();
        Series series = getSeries(sourceAET, data, availability, storeParam);
        inst.setSeries(series);
        inst.setConceptNameCode(
                CodeFactory.getCode(em, data.getNestedDataset(Tag.ConceptNameCodeSequence)));
        inst.setVerifyingObservers(createVerifyingObservers(
                data.getSequence(Tag.VerifyingObserverSequence), storeParam));
        inst.setContentItems(createContentItems(
                data.getSequence(Tag.ContentSequence), storeParam));
        inst.setRetrieveAETs(storeParam.getRetrieveAETs());
        inst.setExternalRetrieveAET(storeParam.getExternalRetrieveAET());
        inst.setAvailability(availability);
        inst.setAttributes(data, storeParam);
        em.persist(inst);
        setDirty(series);
        em.flush();
        return inst;
    }

    @Override
    public FileSystem selectFileSystem(String groupID) {
        return em.createNamedQuery(FileSystem.FIND_BY_GROUP_ID_AND_STATUD, FileSystem.class)
                .setParameter(1, groupID)
                .setParameter(2, FileSystemStatus.RW)
                .getSingleResult();
    }

    @Override
    public boolean initFileSystem(String groupID) {
        if (em.createNamedQuery(FileSystem.COUNT_WITH_GROUP_ID, Long.class)
                .setParameter(1, groupID)
                .getSingleResult() > 0)
            return false;

        FileSystem fs = new FileSystem();
        fs.setGroupID(groupID);
        fs.setURI(new File(System.getProperty("jboss.server.data.dir")).toURI().toString());
        fs.setAvailability(Availability.ONLINE);
        fs.setStatus(FileSystemStatus.RW);
        em.persist(fs);
        return true;
    }

    private Instance findInstance(String sopIUID) {
        return em.createNamedQuery(
                    Instance.FIND_BY_SOP_INSTANCE_UID, Instance.class)
                 .setParameter(1, sopIUID).getSingleResult();
    }

    private Series findSeries(String seriesIUID) {
        return em.createNamedQuery(
                    Series.FIND_BY_SERIES_INSTANCE_UID, Series.class)
                 .setParameter(1, seriesIUID)
                 .getSingleResult();
    }

    private Study findStudy(String studyIUID) {
        return em.createNamedQuery(
                    Study.FIND_BY_STUDY_INSTANCE_UID, Study.class)
                 .setParameter(1, studyIUID)
                 .getSingleResult();
    }

    private void setDirty(Series series) {
        em.createNamedQuery(Series.SET_DIRTY)
          .setParameter(1, series)
          .executeUpdate();
    }

    private String[] sopClassesOf(Study study) {
        return em.createNamedQuery(Study.SOP_CLASSES_IN_STUDY, String.class)
            .setParameter(1, study)
            .getResultList().toArray(StringUtils.EMPTY_STRING);
    }

    private String[] modalitiesOf(Study study) {
        List<String> resultList = em.createNamedQuery(Study.MODALITIES_IN_STUDY, String.class)
            .setParameter(1, study)
            .getResultList();
        resultList.remove(null);
        return resultList.toArray(StringUtils.EMPTY_STRING);
    }

    private int countRelatedSeriesOf(Study study) {
        return em.createNamedQuery(Study.COUNT_RELATED_SERIES, Long.class)
          .setParameter(1, study)
          .getSingleResult().intValue();
    }

    private int countRelatedInstancesOf(Study study) {
        return em.createNamedQuery(Study.COUNT_RELATED_INSTANCES, Long.class)
          .setParameter(1, study)
          .getSingleResult().intValue();
    }

    private int countRelatedInstancesOf(Series series) {
        return em.createNamedQuery(Series.COUNT_RELATED_INSTANCES, Long.class)
          .setParameter(1, series)
          .getSingleResult().intValue();
    }

    private Availability availabilityOf(Study study) {
        return em.createNamedQuery(Study.AVAILABILITY, Availability.class)
          .setParameter(1, study)
          .getSingleResult();
    }

    private Availability availabilityOf(Series series) {
        return em.createNamedQuery(Series.AVAILABILITY, Availability.class)
          .setParameter(1, series)
          .getSingleResult();
    }

    private String[] retrieveAETsOf(Study study) {
        return commonAETs(em.createNamedQuery(Study.RETRIEVE_AETS, String.class)
                .setParameter(1, study)
                .getResultList());
    }

    private String[] retrieveAETsOf(Series series) {
        return commonAETs(em.createNamedQuery(Series.RETRIEVE_AETS, String.class)
                .setParameter(1, series)
                .getResultList());
    }

    private String[] commonAETs(List<String> resultList) {
        LinkedHashSet<String> set = null;
        for (String aets : resultList) {
            if (aets == null)
                return StringUtils.EMPTY_STRING;
            List<String> aetList = Arrays.asList(StringUtils.split(aets, '\\'));
            if (set == null) {
                set = new LinkedHashSet<String>(aetList);
            } else {
                set.retainAll(aetList);
            }
            if (set.isEmpty())
                return StringUtils.EMPTY_STRING;
        }
        return set.toArray(StringUtils.EMPTY_STRING);
    }

    private String externalRetrieveAETOf(Study study) {
        return commonAET(em.createNamedQuery(Study.EXTERNAL_RETRIEVE_AET, String.class)
                .setParameter(1, study)
                .getResultList());
    }

    private String externalRetrieveAETOf(Series series) {
        return commonAET(em.createNamedQuery(Series.EXTERNAL_RETRIEVE_AET, String.class)
                .setParameter(1, series)
                .getResultList());
    }

    private String commonAET(List<String> resultList) {
        String common = null;
        for (String aet : resultList) {
            if (aet == null)
                return null;
            if (common == null) {
                common = aet;
            } else {
                if (!common.equals(aet));
                    return null;
            }
        }
        return common;
    }

    private List<VerifyingObserver> createVerifyingObservers(Sequence seq, StoreParam storeParam) {
        if (seq == null || seq.isEmpty())
            return null;

        ArrayList<VerifyingObserver> list =
                new ArrayList<VerifyingObserver>(seq.size());
        for (Attributes item : seq)
            list.add(new VerifyingObserver(item, storeParam));
        return list;
    }

    private Collection<ContentItem> createContentItems(Sequence seq, StoreParam storeParam) {
        if (seq == null || seq.isEmpty())
            return null;

        Collection<ContentItem> list = new ArrayList<ContentItem>(seq.size());
        for (Attributes item : seq) {
            String type = item.getString(Tag.ValueType);
            if ("CODE".equals(type)) {
                list.add(new ContentItem(
                        item.getString(Tag.RelationshipType).toUpperCase(),
                        CodeFactory.getCode(em, item.getNestedDataset(
                                Tag.ConceptNameCodeSequence)),
                        CodeFactory.getCode(em, item.getNestedDataset(
                                Tag.ConceptCodeSequence))
                        ));
            } else if ("TEXT".equals(type)) {
                list.add(new ContentItem(
                        item.getString(Tag.RelationshipType).toUpperCase(),
                        CodeFactory.getCode(em, item.getNestedDataset(
                                Tag.ConceptNameCodeSequence)),
                                item.getString(Tag.TextValue, "*")
                        ));
            }
        }
        return list;
    }


    private Series getSeries(String sourceAET, Attributes data, Availability availability,
            StoreParam storeParam) {
        String seriesIUID = data.getString(Tag.SeriesInstanceUID, null);
        Series series = cachedSeries;
        if (series == null || !series.getSeriesInstanceUID().equals(seriesIUID)) {
            updateCachedSeries();
            try {
                cachedSeries = series = findSeries(seriesIUID);
            } catch (NoResultException e) {
                cachedSeries = series = new Series();
                Study study = getStudy(data, availability, storeParam);
                series.setStudy(study);
                series.setInstitutionCode(
                        CodeFactory.getCode(em, data.getNestedDataset(Tag.InstitutionCodeSequence)));
                series.setScheduledProcedureSteps(
                        getScheduledProcedureSteps(
                                study.getPatient(),
                                data.getSequence(Tag.RequestAttributesSequence), storeParam));
                series.setSourceAET(sourceAET);
                series.setRetrieveAETs(storeParam.getRetrieveAETs());
                series.setExternalRetrieveAET(storeParam.getExternalRetrieveAET());
                series.setAvailability(availability);
                series.setAttributes(data, storeParam);
                em.persist(series);
                return series;
            }
        }
        Attributes seriesAttrs = series.getAttributes();
        if (seriesAttrs.mergeSelected(data, storeParam.getSeriesAttributes())) {
            series.setAttributes(seriesAttrs, storeParam);
        }
        return series;
    }

    private Collection<ScheduledProcedureStep> getScheduledProcedureSteps(
            Patient patient, Sequence requestAttrsSeq, StoreParam storeParam) {
        if (requestAttrsSeq == null)
            return null;
        ArrayList<ScheduledProcedureStep> list =
                new ArrayList<ScheduledProcedureStep>(requestAttrsSeq.size());
        for (Attributes requestAttrs : requestAttrsSeq) {
            ScheduledProcedureStep sps =
                    RequestFactory.getScheduledProcedureStep(em, requestAttrs,
                            requestAttrsSeq.getParent(), patient, storeParam);
            if (sps != null)
                list.add(sps);
        }
        return list;
    }

    @Override
    @Remove
    public void close() {
        updateCachedSeries();
        cachedSeries = null;
    }

    private void updateCachedSeries() {
        Series series = cachedSeries;
        if (series == null)
            return;

        series.setNumberOfSeriesRelatedInstances(countRelatedInstancesOf(series));
        series.setRetrieveAETs(retrieveAETsOf(series));
        series.setExternalRetrieveAET(externalRetrieveAETOf(series));
        series.setAvailability(availabilityOf(series));
        series.setDirty(false);

        Study study = series.getStudy();
        study.setModalitiesInStudy(modalitiesOf(study));
        study.setSOPClassesInStudy(sopClassesOf(study));
        study.setNumberOfStudyRelatedSeries(countRelatedSeriesOf(study));
        study.setNumberOfStudyRelatedInstances(countRelatedInstancesOf(study));
        study.setRetrieveAETs(retrieveAETsOf(study));
        study.setExternalRetrieveAET(externalRetrieveAETOf(study));
        study.setAvailability(availabilityOf(study));

        em.flush();
    }

    private Study getStudy(Attributes data, Availability availability, StoreParam storeParam) {
        Study study;
        try {
            study = findStudy(data.getString(Tag.StudyInstanceUID, null));
            Attributes studyAttrs = study.getAttributes();
            if (studyAttrs.mergeSelected(data, storeParam.getStudyAttributes())) {
                study.setAttributes(studyAttrs, storeParam);
            }
        } catch (NoResultException e) {
            study = new Study();
            Patient patient = getPatient(data, storeParam);
            study.setPatient(patient);
            study.setProcedureCodes(CodeFactory.createCodes(em,
                    data.getSequence(Tag.ProcedureCodeSequence)));
            study.setIssuerOfAccessionNumber(
                    IssuerFactory.getIssuer(em, data.getNestedDataset(
                            Tag.IssuerOfAccessionNumberSequence)));
            study.setModalitiesInStudy(data.getString(Tag.Modality, null));
            study.setSOPClassesInStudy(data.getString(Tag.SOPClassUID, null));
            study.setRetrieveAETs(storeParam.getRetrieveAETs());
            study.setExternalRetrieveAET(storeParam.getExternalRetrieveAET());
            study.setAvailability(availability);
            study.setAttributes(data, storeParam);
            em.persist(study);
        }
        return study;
    }

    private Patient getPatient(Attributes data, StoreParam storeParam) {
        Patient patient;
        try {
            patient = PatientFactory.followMergedWith(
                    PatientFactory.findPatient(em, data, storeParam));
            Attributes patientAttrs = patient.getAttributes();
            if (patientAttrs.mergeSelected(data, storeParam.getPatientAttributes()))
                patient.setAttributes(patientAttrs, storeParam);
        } catch (NonUniqueResultException e) {
            patient = PatientFactory.createNewPatient(em, data, storeParam);
        } catch (NoResultException e) {
            patient = PatientFactory.createNewPatient(em, data, storeParam);
        }
        return patient;
    }

}
