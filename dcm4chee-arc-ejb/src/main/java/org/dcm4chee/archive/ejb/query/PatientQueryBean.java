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

package org.dcm4chee.archive.ejb.query;

import java.io.IOException;
import java.util.EnumSet;

import javax.ejb.EJBException;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.dcm4che.data.Attributes;
import org.dcm4che.net.pdu.QueryOption;
import org.dcm4chee.archive.ejb.query.metadata.Patient_;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.dcm4chee.archive.persistence.Patient;
import org.dcm4chee.archive.persistence.Utils;
import org.hibernate.Criteria;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PatientQueryBean extends AbstractQueryBean implements PatientQuery {

    @Override
    protected Criteria createCriteria(String[] pids, Attributes keys,AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, String[] roles) {
        return session().createCriteria(Patient.class, "patient")
            .setProjection(projection())
            .add(Criterions.matchPatient(pids, keys, filter, queryOpts));
    }

    private Projection projection() {
        ProjectionList list = Projections.projectionList();
        list.add(Patient_.pk);
        list.add(Patient_.encodedAttributes);
        // just criteria.setProjection(Patient_.encodedAttributes) does not work
        // because Hibernate tries to cast byte[] to Object[]
        return list;
    }

    @Override
    protected Attributes toAttributes(ScrollableResults results) {
        byte[] result = (byte[]) results.get(1);
        Attributes attrs = new Attributes();
        try {
            Utils.decodeAttributes(attrs, result);
        } catch (IOException e) {
            throw new EJBException(e);
        }
        return attrs;
    }

}
