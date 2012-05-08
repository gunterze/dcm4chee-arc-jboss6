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

import java.util.HashSet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.dcm4che.data.Tag;
import org.dcm4chee.archive.persistence.QPatient;
import org.dcm4chee.archive.persistence.Utils;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
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
public class PatientNameQueryBean implements PatientNameQuery {

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
    public String[] query(IDWithIssuer[] pids) {
        HashSet<String> c = new HashSet<String>(pids.length * 4 / 3 + 1);
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(Builder.pids(pids, false));
        builder.and(QPatient.patient.mergedWith.isNull());
        ScrollableResults results = new HibernateQuery(session)
            .from(QPatient.patient)
            .where(builder)
            .scroll(ScrollMode.FORWARD_ONLY, QPatient.patient.pk, QPatient.patient.encodedAttributes);
        try {
            while (results.next())
                c.add(Utils.decodeAttributes(results.getBinary(1))
                        .getString(Tag.PatientName));
        } finally {
            results.close();
        }
        c.remove(null);
        return c.isEmpty() ? null : c.toArray(new String[c.size()]);
    }
}
