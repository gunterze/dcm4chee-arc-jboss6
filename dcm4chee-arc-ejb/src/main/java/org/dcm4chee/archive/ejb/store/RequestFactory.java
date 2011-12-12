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
import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.dcm4chee.archive.persistence.Issuer;
import org.dcm4chee.archive.persistence.Patient;
import org.dcm4chee.archive.persistence.RequestedProcedure;
import org.dcm4chee.archive.persistence.ScheduledProcedureStep;
import org.dcm4chee.archive.persistence.ScheduledStationAETitle;
import org.dcm4chee.archive.persistence.ServiceRequest;
import org.dcm4chee.archive.persistence.Visit;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public abstract class RequestFactory {

    public static ScheduledProcedureStep findOrCreateScheduledProcedureStep(
            EntityManager em, Attributes attrs, Patient patient, StoreParam storeParam) {
        String spsid = attrs.getString(Tag.ScheduledProcedureStepID);
        String rpid = attrs.getString(Tag.RequestedProcedureID);
        String accno = attrs.getString(Tag.AccessionNumber);
        return findOrCreateScheduleProcedureStep(em, attrs, patient,
                storeParam, spsid, rpid, accno);
    }

    public static ScheduledProcedureStep createScheduledProcedureStep(EntityManager em,
            Attributes attrs, Patient patient, StoreParam storeParam) {
        String spsid = attrs.getString(Tag.ScheduledProcedureStepID);
        String rpid = attrs.getString(Tag.RequestedProcedureID);
        String accno = attrs.getString(Tag.AccessionNumber);
        Issuer isserOfAccNo = IssuerFactory.getIssuer(em,
                attrs.getNestedDataset(Tag.IssuerOfAccessionNumberSequence));
        ScheduledProcedureStep sps = createScheduledProcedureStep(em, attrs, patient,
                storeParam, spsid, rpid, accno, isserOfAccNo);
        try {
            findScheduledProcedureStep(em, spsid, rpid, accno, isserOfAccNo);
        } catch (NonUniqueResultException e2) {
            em.remove(sps);
            throw new EntityAlreadyExistsException(sps.toString());
        }
        return sps;
    }

    private static ScheduledProcedureStep findOrCreateScheduleProcedureStep(
            EntityManager em, Attributes attrs, Patient patient,
            StoreParam storeParam, String spsid, String rpid, String accno) {
        Issuer isserOfAccNo = IssuerFactory.getIssuer(em,
                attrs.getNestedDataset(Tag.IssuerOfAccessionNumberSequence));
        try {
            ScheduledProcedureStep sps = findScheduledProcedureStep(
                    em, spsid, rpid, accno, isserOfAccNo);
            PatientMismatchException.check(sps, patient,
                    sps.getRequestedProcedure().getServiceRequest().getVisit().getPatient());
            return sps;
        } catch (NoResultException e) {
            ScheduledProcedureStep sps = createScheduledProcedureStep(em, attrs, patient, 
                    storeParam, spsid, rpid, accno, isserOfAccNo);
            try {
                findScheduledProcedureStep(em, spsid, rpid, accno, isserOfAccNo);
            } catch (NonUniqueResultException e2) {
                em.remove(sps);
                return findOrCreateScheduleProcedureStep(em, attrs, patient,
                        storeParam, spsid, rpid, accno);
            }
            return sps;
        }
    }

    private static ScheduledProcedureStep createScheduledProcedureStep(
            EntityManager em, Attributes attrs, Patient patient,
            StoreParam storeParam, String spsid, String rpid, String accno,
            Issuer isserOfAccNo) {
        ScheduledProcedureStep sps = new ScheduledProcedureStep();
        RequestedProcedure rp =
            findOrCreateRequestedProcedure(em, attrs, patient, storeParam,
                    rpid, accno, isserOfAccNo);
        sps.setRequestedProcedure(rp);
        sps.setScheduledStationAETs(
                createScheduledStationAETs(attrs.getStrings(Tag.ScheduledStationAETitle)));
        sps.setAttributes(attrs, storeParam.getAttributeFilter(Entity.ScheduledProcedureStep),
                storeParam.getFuzzyStr());
        em.persist(sps);
        return sps;
    }

    private static Collection<ScheduledStationAETitle> createScheduledStationAETs(
            String[] aets) {
        if (aets == null || aets.length == 0)
            return null;

        ArrayList<ScheduledStationAETitle> list =
                new ArrayList<ScheduledStationAETitle>(aets.length);
        for (String aet : aets)
            list.add(new ScheduledStationAETitle(aet));
        return list ;
    }

    private static ScheduledProcedureStep findScheduledProcedureStep(EntityManager em,
            String spsid, String rpid, String accno, Issuer issuerOfAccNo) {
        TypedQuery<ScheduledProcedureStep> query = em.createNamedQuery(
                 issuerOfAccNo != null
                     ? ScheduledProcedureStep.FIND_BY_SPS_ID_WITH_ISSUER
                     : ScheduledProcedureStep.FIND_BY_SPS_ID_WITHOUT_ISSUER,
                 ScheduledProcedureStep.class)
                 .setParameter(1, spsid)
                 .setParameter(2, rpid)
                 .setParameter(3, accno);
        if (issuerOfAccNo != null)
            query.setParameter(4, issuerOfAccNo);
        return query.getSingleResult();
    }

    private static Visit getVisit(EntityManager em, Attributes attrs,
            Patient patient, StoreParam storeParam) {
        String admissionID = attrs.getString(Tag.AdmissionID);
        Issuer issuerOfAdmissionID = IssuerFactory.getIssuer(em,
                attrs.getNestedDataset(Tag.IssuerOfAdmissionIDSequence));
        AttributeFilter filter = storeParam.getAttributeFilter(Entity.Visit);

        if (admissionID == null) {
            return newVisit(em, attrs, patient, issuerOfAdmissionID, filter);
        }

        try {
            Visit visit = findVisit(em, admissionID, issuerOfAdmissionID);
            PatientMismatchException.check(visit, patient, visit.getPatient());
            return visit;
        } catch (NoResultException e) {
            return newVisit(em, attrs, patient, issuerOfAdmissionID, filter);
        }
    }

    private static Visit newVisit(EntityManager em, Attributes attrs,
            Patient patient, Issuer issuerOfAdmissionID, AttributeFilter filter) {
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setIssuerOfAdmissionID(issuerOfAdmissionID);
        visit.setAttributes(attrs, filter);
        em.persist(visit);
        return visit;
    }

    private static Visit findVisit(EntityManager em, String admissionID, Issuer issuer) {
        TypedQuery<Visit> query = em.createNamedQuery(
                issuer != null
                    ? Visit.FIND_BY_ADMISSION_ID_WITH_ISSUER
                    : Visit.FIND_BY_ADMISSION_ID_WITHOUT_ISSUER,
                Visit.class)
                .setParameter(1, admissionID);
        if (issuer != null)
            query.setParameter(2, issuer);
        return query.getSingleResult();
    }

    private static RequestedProcedure findOrCreateRequestedProcedure(EntityManager em,
            Attributes attrs, Patient patient, StoreParam storeParam,
            String rpid, String accno, Issuer issuerOfAccNo) {
        try {
            RequestedProcedure rp = findRequestedProcedure(em, rpid, accno, issuerOfAccNo);
            PatientMismatchException.check(rp, patient,
                    rp.getServiceRequest().getVisit().getPatient());
            return rp;
        } catch (NoResultException e) {
            ServiceRequest rq = getServiceRequest(em, attrs, patient, storeParam,
                    accno, issuerOfAccNo);
            RequestedProcedure rp = new RequestedProcedure();
            rp.setServiceRequest(rq);
            rp.setAttributes(attrs, storeParam.getAttributeFilter(Entity.RequestedProcedure));
            em.persist(rp);
            return rp;
        }
    }

    private static RequestedProcedure findRequestedProcedure(EntityManager em,
            String rpid, String accno, Issuer issuerOfAccNo) {
        TypedQuery<RequestedProcedure> query = em.createNamedQuery(
                issuerOfAccNo != null
                    ? RequestedProcedure.FIND_BY_REQUESTED_PROCEDURE_ID_WITH_ISSUER
                    : RequestedProcedure.FIND_BY_REQUESTED_PROCEDURE_ID_WITHOUT_ISSUER,
                    RequestedProcedure.class)
                .setParameter(1, rpid)
                .setParameter(2, accno);
       if (issuerOfAccNo != null)
           query.setParameter(3, issuerOfAccNo);
       return query.getSingleResult();
    }

    private static ServiceRequest getServiceRequest(EntityManager em,
            Attributes attrs, Patient patient, StoreParam storeParam,
            String accno, Issuer issuerOfAccNo) {
        try {
            ServiceRequest request = findServiceRequest(em, accno, issuerOfAccNo);
            PatientMismatchException.check(request, patient,
                    request.getVisit().getPatient());
            return request;
        } catch (NoResultException e) {
            ServiceRequest request = new ServiceRequest();
            Visit visit = getVisit(em, attrs, patient, storeParam);
            request.setVisit(visit);
            request.setIssuerOfAccessionNumber(issuerOfAccNo);
            request.setAttributes(attrs, storeParam.getAttributeFilter(Entity.ServiceRequest),
                    storeParam.getFuzzyStr());
            em.persist(request);
            return request;
        }
    }

    private static ServiceRequest findServiceRequest(EntityManager em,
            String accno, Issuer issuerOfAccNo) {
        TypedQuery<ServiceRequest> query = em.createNamedQuery(
                issuerOfAccNo != null
                    ? ServiceRequest.FIND_BY_ACCESSION_NUMBER_WITH_ISSUER
                    : ServiceRequest.FIND_BY_ACCESSION_NUMBER_WITHOUT_ISSUER,
                    ServiceRequest.class)
                .setParameter(1, accno);
        if (issuerOfAccNo != null)
            query.setParameter(2, issuerOfAccNo);
        return query.getSingleResult();
    }

}
