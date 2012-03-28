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

import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.dcm4chee.archive.persistence.Issuer;
import org.dcm4chee.archive.persistence.Patient;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public abstract class PatientFactory {

    public static Patient findPatient(EntityManager em, String pid, Issuer issuer,
            StoreParam storeParam) {
        if (pid == null)
            throw new NonUniqueResultException();
        TypedQuery<Patient> query = em.createNamedQuery(
                    Patient.FIND_BY_PATIENT_ID, Patient.class)
                .setParameter(1, pid);
        List<Patient> list = query.getResultList();
        if (issuer != null) {
            for (Iterator<Patient> it = list.iterator(); it.hasNext();) {
                Patient pat = (Patient) it.next();
                Issuer issuer2 = pat.getIssuerOfPatientID();
                if (issuer2 != null) {
                    if (equals(issuer, issuer2))
                        return pat;
                    else
                        it.remove();
                }
            }
        }
        if (list.isEmpty())
            throw new NoResultException();
        if (list.size() > 1)
            throw new NonUniqueResultException();
        return list.get(0);
    }

    public static Patient followMergedWith(Patient patient) {
        while (patient.getMergedWith() != null)
            patient = patient.getMergedWith();
        return patient;
    }

    public static Patient createNewPatient(EntityManager em, Attributes attrs, Issuer issuer,
            StoreParam storeParam) {
        Patient patient = new Patient();
        patient.setIssuerOfPatientID(issuer);
        patient.setAttributes(attrs, storeParam.getAttributeFilter(Entity.Patient),
                storeParam.getFuzzyStr());
        em.persist(patient);
        return patient;
    }

    public static Patient findUniqueOrCreatePatient(EntityManager em, Attributes data,
            StoreParam storeParam) {
        AttributeFilter filter = storeParam.getAttributeFilter(Entity.Patient);
        String pid = data.getString(Tag.PatientID);
        Issuer issuer = IssuerFactory.getIssuerOfPatientID(em, data);
        Patient patient;
        try {
            patient = followMergedWith(findPatient(em, pid, issuer, storeParam));
            Attributes patientAttrs = patient.getAttributes();
            if (patientAttrs.mergeSelected(data, filter.getSelection()))
                patient.setAttributes(patientAttrs, filter, storeParam.getFuzzyStr());
        } catch (NonUniqueResultException e) {
            patient = createNewPatient(em, data, issuer, storeParam);
        } catch (NoResultException e) {
            patient = createNewPatient(em, data, issuer, storeParam);
            try {
                findPatient(em, pid, issuer, storeParam);
            } catch (NonUniqueResultException e2) {
                em.remove(patient);
                return findUniqueOrCreatePatient(em, data, storeParam);
            }
        }
        return patient;
    }

    public static Patient updateOrCreatePatient(EntityManager em, Attributes data,
            StoreParam storeParam) {
        AttributeFilter filter = storeParam.getAttributeFilter(Entity.Patient);
        String pid = data.getString(Tag.PatientID);
        Issuer issuer = IssuerFactory.getIssuerOfPatientID(em, data);
        Patient patient;
        try {
            patient = PatientFactory.findPatient(em, pid, issuer, storeParam);
            Patient mergedWith = patient.getMergedWith();
            if (mergedWith != null)
                throw new PatientMergedException("" + patient + " merged with " + mergedWith);
            if (issuer != null && patient.getIssuerOfPatientID() == null)
                patient.setIssuerOfPatientID(issuer);
            Attributes patientAttrs = patient.getAttributes();
            Attributes modified = new Attributes();
            if (patientAttrs.updateSelected(data, modified, filter.getSelection())) {
                patient.setAttributes(patientAttrs, filter, storeParam.getFuzzyStr());
            }
        } catch (NonUniqueResultException e) {
            throw new NonUniquePatientException(pid, issuer);
        } catch (NoResultException e) {
            patient = createNewPatient(em, data, issuer, storeParam);
            try {
                findPatient(em, pid, issuer, storeParam);
            } catch (NonUniqueResultException e2) {
                em.remove(patient);
                return updateOrCreatePatient(em, data, storeParam);
            }
        }
        return patient;
    }

    public static boolean equals(Issuer issuer1, Issuer issuer2) {
        if (issuer1 == issuer2)
            return true;

        String entityID1 = issuer1.getLocalNamespaceEntityID();
        if (entityID1 != null && entityID1.equals(issuer2.getLocalNamespaceEntityID()))
            return true;

        String entityUID1 = issuer1.getUniversalEntityID();
        String entityType1 = issuer1.getUniversalEntityIDType();
        if (entityUID1 != null && entityType1 != null
                && entityUID1.equals(issuer2.getUniversalEntityID())
                && entityType1.equals(issuer2.getUniversalEntityIDType()))
            return true;

        return false;
    }
}
