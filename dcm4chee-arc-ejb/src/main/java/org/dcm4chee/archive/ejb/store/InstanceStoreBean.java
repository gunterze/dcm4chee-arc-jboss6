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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
import org.dcm4che.util.StringUtils;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.dcm4chee.archive.persistence.Availability;
import org.dcm4chee.archive.persistence.ContentItem;
import org.dcm4chee.archive.persistence.FileRef;
import org.dcm4chee.archive.persistence.FileSystem;
import org.dcm4chee.archive.persistence.FileSystemStatus;
import org.dcm4chee.archive.persistence.Instance;
import org.dcm4chee.archive.persistence.RequestAttributes;
import org.dcm4chee.archive.persistence.Series;
import org.dcm4chee.archive.persistence.Study;
import org.dcm4chee.archive.persistence.VerifyingObserver;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Stateful
public class InstanceStoreBean implements InstanceStore {

    @PersistenceContext(unitName = "dcm4chee-arc", type = PersistenceContextType.EXTENDED)
    private EntityManager em;
    private final HashMap<String,Series> cachedSeries = new HashMap<String,Series>();

    @Override
    public Instance store(Attributes attrs, AttributeFilter filter, String sourceAET,
             String retrieveAETs, String externalRetrieveAET, Availability availability) {
        try {
            return findInstance(attrs.getString(Tag.SOPInstanceUID, null));
        } catch (NoResultException e) {
            Instance inst = new Instance();
            Series series = getSeries(attrs, filter, sourceAET, retrieveAETs,
                    externalRetrieveAET, availability);
            inst.setSeries(series);
            inst.setConceptNameCode(
                    CodeFactory.getCode(em, attrs.getNestedDataset(Tag.ConceptNameCodeSequence)));
            inst.setVerifyingObservers(createVerifyingObservers(
                    attrs.getSequence(Tag.VerifyingObserverSequence), filter));
            inst.setContentItems(createContentItems(
                    attrs.getSequence(Tag.ContentSequence), filter));
            inst.setRetrieveAETs(retrieveAETs);
            inst.setExternalRetrieveAET(externalRetrieveAET);
            inst.setAvailability(availability);
            inst.setAttributes(attrs, filter);
            em.persist(inst);
            Study study = series.getStudy();
            incNumberOfSeriesRelatedInstances(series);
            incNumberOfStudyRelatedInstances(study);
            if (!contains(study.getSOPClassesInStudy(), inst.getSopClassUID()))
                study.setSOPClassesInStudy(join(
                        em.createNamedQuery(Study.SOP_CLASSES_IN_STUDY, String.class)
                        .setParameter(1, study)
                        .getResultList()));
            if (!contains(study.getModalitiesInStudy(), series.getModality()))
                study.setModalitiesInStudy(join(
                        em.createNamedQuery(Study.MODALITIES_IN_STUDY, String.class)
                          .setParameter(1, study)
                          .getResultList()));
            if (!isNullOrEquals(series.getRetrieveAETs(), retrieveAETs))
                series.setRetrieveAETs(
                        common(series.getRetrieveAETs(), retrieveAETs));
            if (!isNullOrEquals(series.getExternalRetrieveAET(),
                    externalRetrieveAET))
                series.setExternalRetrieveAET(null);
            if (series.getAvailability().compareTo(availability) < 0)
                series.setAvailability(availability);
            if (!isNullOrEquals(study.getRetrieveAETs(), retrieveAETs))
                study.setRetrieveAETs(
                        common(study.getRetrieveAETs(), retrieveAETs));
            if (!isNullOrEquals(study.getExternalRetrieveAET(),
                    externalRetrieveAET))
                study.setExternalRetrieveAET(null);
            if (study.getAvailability().compareTo(availability) < 0)
                study.setAvailability(availability);
            em.flush();
            return inst;
        }
    }

    @Override
    public FileSystem selectFileSystem(String groupID) {
        return em.createNamedQuery(FileSystem.FIND_BY_GROUP_ID_AND_STATUD, FileSystem.class)
                .setParameter(1, groupID)
                .setParameter(2, FileSystemStatus.RW)
                .getSingleResult();
    }

    @Override
    public void store(FileRef fileRef) {
        em.persist(fileRef);
    }

    private static String common(String aets1, String aets2) {
        if (aets1 == null || aets2 == null)
            return null;
        String[] ss1 = StringUtils.split(aets1, '\\');
        String[] ss2 = StringUtils.split(aets2, '\\');
        int len = 0;
        for (int i = 0; i < ss1.length; i++) {
            if (contains(ss2, ss1[i]))
                ss1[len++] = ss1[i];
        }
        return len == 0 ? null : StringUtils.join(Arrays.copyOf(ss1, len), '\\');
    }

    private static boolean isNullOrEquals(String aet1, String aet2) {
        return aet1 == null || aet1.equals(aet2);
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

        return contains(StringUtils.split(vals, '\\'), val);
    }

    private static boolean contains(String[] vals, String val) {
        for (String s : vals)
            if (s.equals(val))
                return true;
        return false;
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

    private void incNumberOfStudyRelatedInstances(Study study) {
        em.createNamedQuery(Study.INC_NUMBER_OF_STUDY_RELATED_INSTANCES)
          .setParameter(1, study)
          .executeUpdate();
        study.incNumberOfStudyRelatedInstances();
    }

    private void incNumberOfStudyRelatedSeries(Study study) {
        em.createNamedQuery(Study.INC_NUMBER_OF_STUDY_RELATED_SERIES)
          .setParameter(1, study)
          .executeUpdate();
        study.incNumberOfStudyRelatedSeries();
    }

    private void incNumberOfSeriesRelatedInstances(Series series) {
        em.createNamedQuery(Series.INC_NUMBER_OF_SERIES_RELATED_INSTANCES)
          .setParameter(1, series)
          .executeUpdate();
        series.incNumberOfSeriesRelatedInstances();
    }

    private List<VerifyingObserver> createVerifyingObservers(Sequence seq, AttributeFilter filter) {
        if (seq == null || seq.isEmpty())
            return null;

        ArrayList<VerifyingObserver> list =
                new ArrayList<VerifyingObserver>(seq.size());
        for (Attributes item : seq)
            list.add(new VerifyingObserver(item, filter));
        return list;
    }

    private Collection<ContentItem> createContentItems(Sequence seq, AttributeFilter filter) {
        if (seq == null || seq.isEmpty())
            return null;

        Collection<ContentItem> list = new ArrayList<ContentItem>(seq.size());
        for (Attributes item : seq) {
            String type = item.getString(Tag.ValueType, null);
            if ("CODE".equals(type)) {
                list.add(new ContentItem(
                        item.getString(Tag.RelationshipType, null),
                        CodeFactory.getCode(em, item.getNestedDataset(
                                Tag.ConceptNameCodeSequence)),
                        CodeFactory.getCode(em, item.getNestedDataset(
                                Tag.ConceptCodeSequence))
                        ));
            } else if ("TEXT".equals(type)) {
                list.add(new ContentItem(
                        item.getString(Tag.RelationshipType, null),
                        CodeFactory.getCode(em, item.getNestedDataset(
                                Tag.ConceptNameCodeSequence)),
                                filter.getString(item, Tag.TextValue)
                        ));
            }
        }
        return list;
    }


    private Series getSeries(Attributes attrs, AttributeFilter filter, String sourceAET,
            String retrieveAETs, String externalRetrieveAET, Availability availability) {
        String seriesIUID = attrs.getString(Tag.SeriesInstanceUID, null);
        Series series = cachedSeries.get(seriesIUID);
        if (series != null)
            return series;
        try {
            series = findSeries(seriesIUID);
        } catch (NoResultException e) {
            series = new Series();
            Study study = getStudy(attrs, filter, retrieveAETs, externalRetrieveAET, availability);
            series.setStudy(study);
            series.setInstitutionCode(
                    CodeFactory.getCode(em, attrs.getNestedDataset(Tag.InstitutionCodeSequence)));
            series.setRequestAttributes(createRequestAttributes(
                    attrs.getSequence(Tag.RequestAttributesSequence), filter));
            series.setSourceAET(sourceAET);
            series.setRetrieveAETs(retrieveAETs);
            series.setExternalRetrieveAET(externalRetrieveAET);
            series.setAvailability(availability);
            series.setAttributes(attrs, filter);
            em.persist(series);
            incNumberOfStudyRelatedSeries(study);
        }
        cachedSeries.put(seriesIUID, series);
        return series;
    }

    @Override
    @Remove
    public void close() {
        cachedSeries.clear();
    }

    private List<RequestAttributes> createRequestAttributes(Sequence seq, AttributeFilter filter) {
        if (seq == null || seq.isEmpty())
            return null;

        ArrayList<RequestAttributes> list =
                new ArrayList<RequestAttributes>(seq.size());
        for (Attributes item : seq) {
            RequestAttributes rqAttrs = new RequestAttributes(item, filter);
            rqAttrs.setIssuerOfAccessionNumber(
                    IssuerFactory.getIssuer(em, item.getNestedDataset(
                            Tag.IssuerOfAccessionNumberSequence)));
            list.add(rqAttrs);
        }
        return list;
    }

    private Study getStudy(Attributes attrs, AttributeFilter filter, String retrieveAETs,
            String externalRetrieveAET, Availability availability) {
        try {
            return findStudy(attrs.getString(Tag.StudyInstanceUID, null));
        } catch (NoResultException e) {
            Study study = new Study();
            study.setPatient(
                    PatientFactory.followMergedWith(PatientFactory.getPatient(em, attrs, filter)));
            study.setProcedureCodes(CodeFactory.createCodes(em,
                    attrs.getSequence(Tag.ProcedureCodeSequence)));
            study.setIssuerOfAccessionNumber(
                    IssuerFactory.getIssuer(em, attrs.getNestedDataset(
                            Tag.IssuerOfAccessionNumberSequence)));
            study.setModalitiesInStudy(attrs.getString(Tag.Modality, null));
            study.setSOPClassesInStudy(attrs.getString(Tag.SOPClassUID, null));
            study.setRetrieveAETs(retrieveAETs);
            study.setExternalRetrieveAET(externalRetrieveAET);
            study.setAvailability(availability);
            study.setAttributes(attrs, filter);
            em.persist(study);
            return study;
        }
    }

}
