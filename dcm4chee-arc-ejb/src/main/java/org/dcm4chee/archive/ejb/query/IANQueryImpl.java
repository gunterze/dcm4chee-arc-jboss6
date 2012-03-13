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
 * Portions created by the Initial Developer are Copyright (C) 2012
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

package org.dcm4chee.archive.ejb.query;

import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Sequence;
import org.dcm4che.data.Tag;
import org.dcm4che.data.UID;
import org.dcm4che.data.VR;
import org.dcm4che.net.Status;
import org.dcm4che.net.service.DicomServiceException;
import org.dcm4chee.archive.ejb.exception.DicomServiceRuntimeException;
import org.dcm4chee.archive.persistence.Availability;
import org.dcm4chee.archive.persistence.PerformedProcedureStep;
import org.dcm4chee.archive.persistence.QInstance;
import org.dcm4chee.archive.persistence.QSeries;
import org.dcm4chee.archive.persistence.QStudy;
import org.dcm4chee.archive.persistence.Utils;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.ejb.HibernateEntityManagerFactory;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class IANQueryImpl implements IANQuery {

    @PersistenceUnit(unitName = "dcm4chee-arc")
    private EntityManagerFactory emf;
    private StatelessSession session;

    @PostConstruct
    public void init() {
        SessionFactory sessionFactory =
                ((HibernateEntityManagerFactory) emf).getSessionFactory();
        session = sessionFactory.openStatelessSession();
    }

    @PreDestroy
    public void destroy() {
        session.close();
    }

    @Override
    public Attributes createIANforMPPS(PerformedProcedureStep mpps) {
        Sequence perfSeriesSeq = mpps.getAttributes()
                .getSequence(Tag.PerformedSeriesSequence);
        if (perfSeriesSeq == null)
            return null;

        String[] seriesIUIDs = new String[perfSeriesSeq.size()];
        int remaining = 0;
        for (int i = 0; i < seriesIUIDs.length; i++) {
            Attributes perfSeries = perfSeriesSeq.get(i);
            seriesIUIDs[i] = perfSeries.getString(Tag.SeriesInstanceUID);
            Sequence refImgs =
                    perfSeries.getSequence(Tag.ReferencedImageSequence);
            Sequence refNonImgs =
                    perfSeries.getSequence(Tag.ReferencedNonImageCompositeSOPInstanceSequence);
            if (refImgs != null)
                remaining += refImgs.size();
            if (refNonImgs != null)
                remaining += refNonImgs.size();
        }
        if (remaining == 0)
            return null;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(Builder.uids(QSeries.series.seriesInstanceUID, seriesIUIDs, false));
        builder.and(QInstance.instance.replaced.isFalse());
        List<Object[]> list = new HibernateQuery(session)
            .from(QInstance.instance)
            .innerJoin(QInstance.instance.series, QSeries.series)
            .innerJoin(QSeries.series.study, QStudy.study)
            .where(builder)
            .list(
                QStudy.study.studyInstanceUID,
                QSeries.series.seriesInstanceUID,
                QInstance.instance.sopClassUID,
                QInstance.instance.sopInstanceUID,
                QInstance.instance.retrieveAETs,
                QInstance.instance.externalRetrieveAET,
                QInstance.instance.availability);

        if (list.isEmpty())
            return null;

        HashMap<String,Object[]> map = new HashMap<String,Object[]>(list.size() * 4 / 3);
        for (Object[] a : list)
            map.put((String) a[3], a);

        Attributes ian = new Attributes(4);
        Attributes refPPS = new Attributes(3);
        ian.newSequence(Tag.ReferencedPerformedProcedureStepSequence, 1).add(refPPS);
        refPPS.setString(Tag.ReferencedSOPClassUID, VR.UI,
                UID.ModalityPerformedProcedureStepSOPClass);
        refPPS.setString(Tag.ReferencedSOPInstanceUID, VR.UI,
                mpps.getSopInstanceUID());
        refPPS.setNull(Tag.PerformedWorkitemCodeSequence, VR.SQ);
        String studyIUID = (String) list.get(0)[0];
        ian.setString(Tag.StudyInstanceUID, VR.UI, studyIUID);
        Sequence refSeriesSeq = ian.newSequence(Tag.ReferencedSeriesSequence, seriesIUIDs.length);
        for (Attributes perfSeries : perfSeriesSeq) {
            Sequence refImgs =
                    perfSeries.getSequence(Tag.ReferencedImageSequence);
            Sequence refNonImgs =
                    perfSeries.getSequence(Tag.ReferencedNonImageCompositeSOPInstanceSequence);
            int seriesSize = 0;
            if (refImgs != null)
                seriesSize += refImgs.size();
            if (refNonImgs != null)
                seriesSize += refNonImgs.size();
            if (seriesSize == 0)
                continue;
            String seriesIUID = perfSeries.getString(Tag.SeriesInstanceUID);
            Attributes refSeries = new Attributes(3);
            refSeriesSeq.add(refSeries);
            Sequence refSOPs = refSeries.newSequence(Tag.ReferencedSOPSequence, seriesSize);
            refSeries.setString(Tag.SeriesInstanceUID, VR.UI, seriesIUID);
            if (refImgs != null)
                remaining -= addAllTo(studyIUID, seriesIUID, refImgs, map, refSOPs);
            if (refNonImgs != null)
                remaining -= addAllTo(studyIUID, seriesIUID, refNonImgs, map, refSOPs);
        }
        return remaining == 0 ? ian : null;
    }

    private static int addAllTo(String studyIUD, String seriesIUID,
            Sequence ppsRefs, HashMap<String, Object[]> map, Sequence refSOPs) {
        int count = 0;
        for (Attributes ppsRef : ppsRefs) {
            Object[] a = map.get(ppsRef.getString(Tag.ReferencedSOPInstanceUID));
            if (a != null) {
                if (!studyIUD.equals(a[0]))
                    throw new DicomServiceRuntimeException(
                            new DicomServiceException(Status.ProcessingFailure,
                                    "Referenced SOP Instances belongs to multiple Studies"));
                if (!seriesIUID.equals(a[1]))
                    throw new DicomServiceRuntimeException(
                            new DicomServiceException(Status.ProcessingFailure,
                                    "Mismatch of Series Instance UID of referenced Instance"));
                if (!ppsRef.getString(Tag.ReferencedSOPClassUID).equals(a[2]))
                    throw new DicomServiceRuntimeException(
                            new DicomServiceException(Status.ProcessingFailure,
                                    "Mismatch of SOP Class UID of referenced Instance"));
                refSOPs.add(refSOP(ppsRef, (String) a[4], (String) a[5], (Availability) a[6]));
                count++;
            }
        }
        return count;
    }

    private static Attributes refSOP(Attributes ppsRef, String retrieveAETs,
            String externalRetrieveAET, Availability availability) {
        Attributes attrs = new Attributes(4);
        Utils.setRetrieveAET(attrs, retrieveAETs, externalRetrieveAET);
        attrs.setString(Tag.InstanceAvailability, VR.CS, availability.toString());
        attrs.setString(Tag.ReferencedSOPClassUID, VR.UI,
                ppsRef.getString(Tag.ReferencedSOPClassUID));
        attrs.setString(Tag.ReferencedSOPInstanceUID, VR.UI,
                ppsRef.getString(Tag.ReferencedSOPInstanceUID));
        return attrs ;
    }
}
