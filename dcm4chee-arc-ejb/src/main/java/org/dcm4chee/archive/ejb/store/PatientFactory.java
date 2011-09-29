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

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4chee.archive.persistence.Patient;
import org.dcm4chee.archive.persistence.StoreParam;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class PatientFactory {

    public static Patient getPatient(EntityManager em, Attributes attrs, StoreParam storeParam) {
        try {
            return findPatient(em, attrs, storeParam);
        } catch (NoResultException e) {
            return createNewPatient(em, attrs, storeParam);
        } catch (NonUniqueResultException e) {
            return createNewPatient(em, attrs, storeParam);
        }
    }

    private static Patient findPatient(EntityManager em, Attributes attrs, StoreParam storeParam) {
        String pid = attrs.getString(Tag.PatientID);
        if (pid == null)
            throw new NonUniqueResultException();
        List<Patient> list =
            em.createNamedQuery(Patient.FIND_BY_PATIENT_ID, Patient.class)
                .setParameter(1, pid)
                .getResultList();
        String issuer1 = attrs.getString(Tag.IssuerOfPatientID);
        if (issuer1 != null)
            for (Iterator<Patient> it = list.iterator(); it.hasNext();) {
                Patient pat = it.next();
                String issuer2 = pat.getIssuerOfPatientID();
                if (!issuer2.equals("*") && !issuer2.equals(issuer1))
                    it.remove();
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

    private static Patient createNewPatient(EntityManager em, Attributes attrs,
            StoreParam storeParam) {
        Patient patient = new Patient();
        patient.setAttributes(attrs, storeParam);
        em.persist(patient);
        return patient;
    }

}
