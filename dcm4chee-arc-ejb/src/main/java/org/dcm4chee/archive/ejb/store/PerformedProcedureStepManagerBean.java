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

package org.dcm4chee.archive.ejb.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Sequence;
import org.dcm4che.data.Tag;
import org.dcm4che.net.Status;
import org.dcm4che.net.service.BasicMppsSCP;
import org.dcm4che.net.service.DicomServiceException;
import org.dcm4chee.archive.ejb.exception.DicomServiceRuntimeException;
import org.dcm4chee.archive.ejb.query.IANQuery;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.dcm4chee.archive.persistence.Code;
import org.dcm4chee.archive.persistence.Instance;
import org.dcm4chee.archive.persistence.Patient;
import org.dcm4chee.archive.persistence.PerformedProcedureStep;
import org.dcm4chee.archive.persistence.ScheduledProcedureStep;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Stateless
public class PerformedProcedureStepManagerBean implements PerformedProcedureStepManager {

    @PersistenceContext(unitName = "dcm4chee-arc")
    private EntityManager em;

    @EJB
    private IANQuery ianQuery;

    @Override
    public PerformedProcedureStep createPerformedProcedureStep(
            String sopInstanceUID, Attributes attrs, StoreParam storeParam) {
        try {
            PerformedProcedureStep pps = find(sopInstanceUID);
            throw new EntityAlreadyExistsException(pps.toString());
        } catch (NoResultException e) {}
        AttributeFilter filter = storeParam.getAttributeFilter(Entity.PerformedProcedureStep);
        Patient patient = PatientFactory.findUniqueOrCreatePatient(em, attrs, storeParam);
        PerformedProcedureStep mpps = new PerformedProcedureStep();
        mpps.setSopInstanceUID(sopInstanceUID);
        mpps.setAttributes(attrs, filter);
        mpps.setScheduledProcedureSteps(
                getScheduledProcedureSteps(
                        attrs.getSequence(Tag.ScheduledStepAttributesSequence),
                        patient,
                        storeParam));
        mpps.setPatient(patient);
        em.persist(mpps);
        return mpps;
    }

    @Override
    public PPSWithIAN updatePerformedProcedureStep(String sopInstanceUID,
            Attributes modified, StoreParam storeParam) {
        PerformedProcedureStep pps;
        try {
            pps = find(sopInstanceUID);
        } catch (NoResultException e) {
            throw new EntityNotExistsException(sopInstanceUID);
        }
        if (!pps.isInProgress())
            throw new DicomServiceRuntimeException(
                    new DicomServiceException(Status.ProcessingFailure,
                            BasicMppsSCP.MAY_NO_LONGER_BE_UPDATED)
                        .setErrorID(BasicMppsSCP.MAY_NO_LONGER_BE_UPDATED_ERROR_ID));

        AttributeFilter filter = storeParam.getAttributeFilter(Entity.PerformedProcedureStep);
        Attributes attrs = pps.getAttributes();
        attrs.addAll(modified);
        pps.setAttributes(attrs, filter);
        Attributes ian = null;
        if (!pps.isInProgress()) {
            if (!attrs.containsValue(Tag.PerformedSeriesSequence))
                throw new DicomServiceRuntimeException(
                        new DicomServiceException(Status.MissingAttributeValue)
                        .setAttributeIdentifierList(Tag.PerformedSeriesSequence));
            ian = ianQuery.createIANforMPPS(pps);
            if (pps.isDiscontinued()) {
                RejectionNote rn = storeParam.getRejectionNote(
                        attrs.getNestedDataset(
                                Tag.PerformedProcedureStepDiscontinuationReasonCodeSequence));
                if (rn != null) {
                    setRejectionCodeInRefSOPInstances(attrs, CodeFactory.getCode(em, rn));
                    ian = null;
                }
            }
        }
        em.merge(pps);
        return new PPSWithIAN(pps, ian);
    }

    private void setRejectionCodeInRefSOPInstances(Attributes attrs, Code rejectionCode) {
        Sequence perfSeriesSeq = attrs.getSequence(Tag.PerformedSeriesSequence);
        HashSet<String> iuids = new HashSet<String>();
        for (Attributes perfSeries : perfSeriesSeq) {
            addRefSOPInstanceUIDs(iuids,
                    perfSeries.getSequence(Tag.ReferencedImageSequence));
            addRefSOPInstanceUIDs(iuids,
                    perfSeries.getSequence(Tag.ReferencedNonImageCompositeSOPInstanceSequence));
            List<Instance> insts =
                em.createNamedQuery(Instance.FIND_BY_SERIES_INSTANCE_UID, Instance.class)
                  .setParameter(1, perfSeries.getString(Tag.SeriesInstanceUID))
                  .getResultList();
            for (Instance inst : insts)
                if (iuids.contains(inst.getSopInstanceUID()))
                    inst.setRejectionCode(rejectionCode);
            iuids.clear();
        }
    }

    private void addRefSOPInstanceUIDs(HashSet<String> iuids, Sequence refImgs) {
        if (refImgs != null)
            for (Attributes ref : refImgs)
                iuids.add(ref.getString(Tag.ReferencedSOPInstanceUID));
    }

    private PerformedProcedureStep find(String sopInstanceUID) {
        return em.createNamedQuery(
                PerformedProcedureStep.FIND_BY_SOP_INSTANCE_UID,
                PerformedProcedureStep.class)
             .setParameter(1, sopInstanceUID)
             .getSingleResult();
    }

    private Collection<ScheduledProcedureStep> getScheduledProcedureSteps(
            Sequence ssaSeq, Patient patient, StoreParam storeParam) {
        ArrayList<ScheduledProcedureStep> list =
                new ArrayList<ScheduledProcedureStep>(ssaSeq.size());
        for (Attributes ssa : ssaSeq) {
            if (ssa.containsValue(Tag.ScheduledProcedureStepID)
                    && ssa.containsValue(Tag.RequestedProcedureID)
                    && ssa.containsValue(Tag.AccessionNumber)) {
                ScheduledProcedureStep sps =
                        RequestFactory.findOrCreateScheduledProcedureStep(em,
                                ssa, patient, storeParam);
                list.add(sps);
            }
        }
        return list;
    }

}
