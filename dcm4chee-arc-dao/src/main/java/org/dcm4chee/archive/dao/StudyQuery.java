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

package org.dcm4chee.archive.dao;

import java.util.Iterator;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.dcm4che.data.Attributes;
import org.dcm4chee.archive.domain.Study;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Stateful
public class StudyQuery {

    @PersistenceContext(unitName="dcm4chee-arc")
    private EntityManager em;
    private Iterator<Study> results;

    public void find(Attributes keys) {
        results = em.createQuery(createQuery(keys)).getResultList().iterator();
    }

    public Study next() {
        if (results == null)
            throw new IllegalStateException("results not initalized");
        return results.hasNext() ? results.next() : null;
    }

    @Remove
    public void close() {
    }

    private CriteriaQuery<Study> createQuery(Attributes keys) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Study> cq = cb.createQuery(Study.class);
        cq.from(Study.class);
         // TODO
        return cq ;
    }

}
