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

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4chee.archive.persistence.Patient;
import org.dcm4chee.archive.persistence.RequestedProcedure;
import org.dcm4chee.archive.persistence.ScheduledProcedureStep;
import org.dcm4chee.archive.persistence.ServiceRequest;
import org.dcm4chee.archive.persistence.StoreParam;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public abstract class RequestFactory {

    public static ScheduledProcedureStep getScheduledProcedureStep(EntityManager em,
            Attributes attrs, Patient patient, StoreParam storeParam) {
        String spsid = attrs.getString(Tag.ScheduledProcedureStepID);
        if (spsid == null)
            return null;

        try {
            TypedQuery<ScheduledProcedureStep> query = em.createNamedQuery(
                            ScheduledProcedureStep.FIND_BY_SPS_ID,
                            ScheduledProcedureStep.class)
                    .setParameter(1, spsid);
            return query.getSingleResult();
        } catch (NoResultException e) {
            RequestedProcedure rp = getRequestedProcedure(em, attrs, patient, storeParam);
            if (rp == null)
                return null;

            ScheduledProcedureStep sps = new ScheduledProcedureStep();
            sps.setRequestedProcedure(rp);
            sps.setAttributes(attrs, storeParam);
            em.persist(sps);
            return sps;
        }

    }

    private static RequestedProcedure getRequestedProcedure(EntityManager em,
            Attributes attrs, Patient patient, StoreParam storeParam) {
        String rpid = attrs.getString(Tag.RequestedProcedureID);
        if (rpid == null)
            return null;

        try {
            TypedQuery<RequestedProcedure> query = em.createNamedQuery(
                            RequestedProcedure.FIND_BY_REQUESTED_PROCEDURE_ID,
                            RequestedProcedure.class)
                    .setParameter(1, rpid);
            return query.getSingleResult();
        } catch (NoResultException e) {
            ServiceRequest rq = getServiceRequest(em, attrs, patient, storeParam);
            if (rq == null)
                return null;

            RequestedProcedure rp = new RequestedProcedure();
            rp.setServiceRequest(rq);
            rp.setAttributes(attrs, storeParam);
            em.persist(rp);
            return rp;
        }
    }

    private static ServiceRequest getServiceRequest(EntityManager em,
            Attributes attrs, Patient patient, StoreParam storeParam) {
        String accno = attrs.getString(Tag.AccessionNumber);
        if (accno == null)
            return null;

        try {
            TypedQuery<ServiceRequest> query = em.createNamedQuery(
                            ServiceRequest.FIND_BY_ACCESSION_NUMBER,
                            ServiceRequest.class)
                    .setParameter(1, accno);
            return query.getSingleResult();
        } catch (NoResultException e) {
            ServiceRequest request = new ServiceRequest();
            request.setPatient(patient);
            request.setIssuerOfAccessionNumber(
                    IssuerFactory.getIssuer(em,
                            attrs.getNestedDataset(
                                    Tag.IssuerOfAccessionNumberSequence)));
            request.setAttributes(attrs, storeParam);
            em.persist(request);
            return request;
        }
    }

}
