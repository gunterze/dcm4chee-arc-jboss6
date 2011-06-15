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

package org.dcm4chee.archive.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.dcm4che.data.Attributes;
import org.dcm4che.net.Status;
import org.dcm4che.net.service.DicomServiceException;
import org.dcm4chee.archive.domain.Patient;
import org.dcm4chee.archive.domain.Patient_;
import org.dcm4chee.archive.domain.Utils;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Stateful
public class PatientQueryBean implements PatientQuery {

    @PersistenceContext(unitName = "dcm4chee-arc",
                        type = PersistenceContextType.EXTENDED)
    private EntityManager em;

    private Attributes rq;
    private Iterator<byte[]> results;

    private boolean optionalKeyNotSupported;

    @Override
    public void find(Attributes rq, String[] pids, Attributes keys,
            boolean matchUnknown) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<byte[]> cq = cb.createQuery(byte[].class);
        Root<Patient> pat = cq.from(Patient.class);
        cq.select(pat.get(Patient_.encodedAttributes));
        List<Predicate> predicates = new ArrayList<Predicate>();
        List<Object> params = new ArrayList<Object>();
        predicates.add(cb.isNull(pat.get(Patient_.mergedWith)));
        Matching.patient(cb, pat, pids, keys, matchUnknown, predicates, params);
        cq.where(predicates.toArray(new Predicate[predicates.size()]));
        TypedQuery<byte[]> q = em.createQuery(cq);
        int i = 0;
        for (Object param : params)
            q.setParameter(Matching.paramName(i++), param);

        results = q.getResultList().iterator();
    }

    @Override
    public boolean optionalKeyNotSupported() {
        return optionalKeyNotSupported;
    }

    @Override
    public boolean hasMoreMatches() throws DicomServiceException {
        checkResults();
        return results.hasNext();
    }

    @Override
    public Attributes nextMatch() throws DicomServiceException {
        checkResults();
        try {
            byte[] result = results.next();
            Attributes attrs = new Attributes();
            Utils.decodeAttributes(attrs, result);
            return attrs;
        } catch (Exception e) {
            throw unableToProcess(e);
        }
    }

    @Override
    @Remove
    public void close() {}

    private void checkResults() {
        if (results == null)
            throw new IllegalStateException("results not initalized");
    }

    private DicomServiceException unableToProcess(Exception e)
            throws DicomServiceException {
        return new DicomServiceException(rq, Status.UnableToProcess, e);
    }

}
