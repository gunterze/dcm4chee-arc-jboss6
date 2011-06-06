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

package org.dcm4chee.archive.store;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.ItemPointer;
import org.dcm4che.data.Sequence;
import org.dcm4che.data.Tag;
import org.dcm4che.util.StringUtils;
import org.dcm4chee.archive.domain.Availability;
import org.dcm4chee.archive.domain.Instance;
import org.dcm4chee.archive.domain.RequestAttributes;
import org.dcm4chee.archive.domain.Series;
import org.dcm4chee.archive.domain.Study;
import org.dcm4chee.archive.domain.VerifyingObserver;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Stateful
public class InstanceStore {


    @PersistenceContext(unitName = "dcm4chee-arc",
            type = PersistenceContextType.EXTENDED)
    private EntityManager em;
    private TypedQuery<Study> findStudy;
    private TypedQuery<Series> findSeries;
    private TypedQuery<Instance> findInstance;
    private TypedQuery<String> getModalitiesInStudy;
    private TypedQuery<String> getSOPClassesInStudy;
    private Query incNumberOfSeriesRelatedInstances;
    private Query incNumberOfStudyRelatedSeries;
    private Query incNumberOfStudyRelatedInstances;
    private Query updateModalitiesInStudy;
    private Query updateSOPClassesInStudy;

    public Instance store(Attributes attrs, String sourceAET,
             String retrieveAETs, String externalRetrieveAET,
             Availability availability) {
        try {
            return findInstance(attrs.getString(Tag.SOPInstanceUID, null));
        } catch (NoResultException e) {
            Instance inst = new Instance();
            Series series = getSeries(attrs, sourceAET, retrieveAETs,
                    externalRetrieveAET, availability);
            inst.setSeries(series);
            inst.setConceptNameCode(
                    CodeFactory.getCode(em, attrs.getNestedDataset(
                            new ItemPointer(Tag.ConceptNameCodeSequence))));
            inst.setVerifyingObservers(createVerifyingObservers(
                    attrs.getSequence(Tag.VerifyingObserverSequence)));
            inst.setRetrieveAETs(retrieveAETs);
            inst.setExternalRetrieveAET(externalRetrieveAET);
            inst.setAvailability(availability);
            inst.setAttributes(attrs);
            em.persist(inst);
            incNumberOfSeriesRelatedInstances(series);
            Study study = series.getStudy();
            incNumberOfStudyRelatedInstances(study);
            if (updateSOPClassesInStudy(study,
                    attrs.getString(Tag.SOPClassUID, null)))
                em.refresh(study);
            return inst;
        }
    }

    private Instance findInstance(String sopIUID) {
        if (findInstance == null)
            findInstance = em.createNamedQuery(
                    Instance.FIND_BY_SOP_INSTANCE_UID, Instance.class);
        return findInstance.setParameter(1, sopIUID).getSingleResult();
    }

    private Series findSeries(String seriesIUID) {
        if (findSeries == null)
            findSeries = em.createNamedQuery(
                    Series.FIND_BY_SERIES_INSTANCE_UID, Series.class);
        return findSeries.setParameter(1, seriesIUID).getSingleResult();
    }

    private Study findStudy(String studyIUID) {
        if (findStudy == null)
            findStudy = em.createNamedQuery(
                    Study.FIND_BY_STUDY_INSTANCE_UID, Study.class);
        return findStudy.setParameter(1, studyIUID).getSingleResult();
    }

    private void incNumberOfStudyRelatedInstances(Study study) {
        if (incNumberOfStudyRelatedInstances == null)
            incNumberOfStudyRelatedInstances = em.createNamedQuery(
                    Study.INC_NUMBER_OF_STUDY_RELATED_INSTANCES);
        incNumberOfStudyRelatedInstances.setParameter(1, study);
        incNumberOfStudyRelatedInstances.executeUpdate();
    }

    private void incNumberOfStudyRelatedSeries(Study study) {
        if (incNumberOfStudyRelatedSeries == null)
            incNumberOfStudyRelatedSeries = em.createNamedQuery(
                    Study.INC_NUMBER_OF_STUDY_RELATED_SERIES);
        incNumberOfStudyRelatedSeries.setParameter(1, study);
        incNumberOfStudyRelatedSeries.executeUpdate();
    }

    private void incNumberOfSeriesRelatedInstances(Series series) {
        if (incNumberOfSeriesRelatedInstances == null)
            incNumberOfSeriesRelatedInstances = em.createNamedQuery(
                    Series.INC_NUMBER_OF_SERIES_RELATED_INSTANCES);
        incNumberOfSeriesRelatedInstances.setParameter(1, series);
        incNumberOfSeriesRelatedInstances.executeUpdate();
    }

    public boolean updateModalitiesInStudy(Study study, String modality) {
        if (contains(study.getModalitiesInStudy(), modality))
            return false;

        if (getModalitiesInStudy == null)
            getModalitiesInStudy = em.createNamedQuery(
                    Study.MODALITIES_IN_STUDY, String.class);

        String value = join(
                getModalitiesInStudy.setParameter(1, study).getResultList());

        if (updateModalitiesInStudy == null)
            updateModalitiesInStudy = em.createNamedQuery(
                    Study.UPDATE_MODALITIES_IN_STUDY);
        updateModalitiesInStudy
                .setParameter(1, study)
                .setParameter(2, value)
                .executeUpdate();

        return true;
    }

    public boolean updateSOPClassesInStudy(Study study, String cuid) {
        if (contains(study.getSOPClassesInStudy(), cuid))
            return false;

        if (getSOPClassesInStudy == null)
            getSOPClassesInStudy = em.createNamedQuery(
                    Study.SOP_CLASSES_IN_STUDY, String.class);

        String value = join(
                getSOPClassesInStudy.setParameter(1, study).getResultList());

        if (updateSOPClassesInStudy == null)
            updateSOPClassesInStudy = em.createNamedQuery(
                    Study.UPDATE_SOP_CLASSES_IN_STUDY);
        updateSOPClassesInStudy
                .setParameter(1, study)
                .setParameter(2, value)
                .executeUpdate();

        return true;
    }
    private static String join(List<String> list) {
        return StringUtils.join(list.toArray(new String[list.size()]), '\\');
    }

    private static boolean contains(String vals, String val) {
        if (val == null)
            return true;

        if (vals == null)
            return false;

        if (vals.equals(val))
            return true;

        for (String s : StringUtils.split(vals, '\\'))
            if (s.equals(val))
                return true;

        return false;
    }

    private List<VerifyingObserver> createVerifyingObservers(Sequence seq) {
        if (seq == null || seq.isEmpty())
            return null;

        ArrayList<VerifyingObserver> list =
                new ArrayList<VerifyingObserver>(seq.size());
        for (Attributes item : seq)
            list.add(new VerifyingObserver(item));
        return list;
    }

    private Series getSeries(Attributes attrs, String sourceAET,
            String retrieveAETs, String externalRetrieveAET,
            Availability availability) {
        try {
            Series series = findSeries(attrs.getString(Tag.SeriesInstanceUID, null));
            return series;
        } catch (NoResultException e) {
            Series series = new Series();
            Study study = getStudy(attrs, retrieveAETs, externalRetrieveAET,
                    availability);
            series.setStudy(study);
            series.setInstitutionCode(
                    CodeFactory.getCode(em, attrs.getNestedDataset(
                            new ItemPointer(Tag.InstitutionCodeSequence))));
            series.setRequestAttributes(createRequestAttributes(
                    attrs.getSequence(Tag.RequestAttributesSequence)));
            series.setSourceAET(sourceAET);
            series.setRetrieveAETs(retrieveAETs);
            series.setExternalRetrieveAET(externalRetrieveAET);
            series.setAvailability(availability);
            series.setAttributes(attrs);
            em.persist(series);
            incNumberOfStudyRelatedSeries(study);
            updateModalitiesInStudy(study, attrs.getString(Tag.Modality, null));
            return series;
        }
    }

    private List<RequestAttributes> createRequestAttributes(Sequence seq) {
        if (seq == null || seq.isEmpty())
            return null;

        ArrayList<RequestAttributes> list =
                new ArrayList<RequestAttributes>(seq.size());
        for (Attributes item : seq) {
            RequestAttributes rqAttrs = new RequestAttributes(item);
            rqAttrs.setIssuerOfAccessionNumber(
                    IssuerFactory.getIssuer(em, item.getNestedDataset(
                            new ItemPointer(Tag.IssuerOfAccessionNumberSequence))));
            list.add(rqAttrs);
        }
        return list;
    }

    private Study getStudy(Attributes attrs, String retrieveAETs,
            String externalRetrieveAET, Availability availability) {
        String studyIUID = attrs.getString(Tag.StudyInstanceUID, null);
        try {
            return findStudy(studyIUID);
        } catch (NoResultException e) {
            Study study = new Study();
            study.setPatient(
                    PatientFactory.followMergedWith(
                            PatientFactory.getPatient(em, attrs)));
            study.setProcedureCodes(CodeFactory.createCodes(em,
                    attrs.getSequence(Tag.ProcedureCodeSequence)));
            study.setIssuerOfAccessionNumber(
                    IssuerFactory.getIssuer(em, attrs.getNestedDataset(
                            new ItemPointer(Tag.IssuerOfAccessionNumberSequence))));
            study.setModalitiesInStudy(attrs.getString(Tag.Modality, null));
            study.setSOPClassesInStudy(attrs.getString(Tag.SOPClassUID, null));
            study.setRetrieveAETs(retrieveAETs);
            study.setExternalRetrieveAET(externalRetrieveAET);
            study.setAvailability(availability);
            study.setAttributes(attrs);
            em.persist(study);
            return study;
        }
    }

   @Remove
   public void close() {
   }

}
