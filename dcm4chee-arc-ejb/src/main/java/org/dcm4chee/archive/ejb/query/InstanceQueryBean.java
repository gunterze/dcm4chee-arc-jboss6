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
import org.dcm4chee.archive.ejb.query.metadata.Instance_;
import org.dcm4chee.archive.ejb.query.metadata.Series_;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.dcm4chee.archive.persistence.Availability;
import org.dcm4chee.archive.persistence.Instance;
import org.dcm4chee.archive.persistence.Utils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class InstanceQueryBean extends AbstractQueryBean implements InstanceQuery {

    private static final String QUERY_SERIES_ATTRS = "select " +
            "s.study.numberOfStudyRelatedSeries, " +
            "s.study.numberOfStudyRelatedInstances, " +
            "s.numberOfSeriesRelatedInstances, " +
            "s.study.modalitiesInStudy, " +
            "s.study.sopClassesInStudy, " +
            "s.encodedAttributes, " +
            "s.study.encodedAttributes, " +
            "s.study.patient.encodedAttributes " +
            "from Series s " +
            "where s.pk = ?";
    private long seriesPk = -1L;
    private Attributes seriesAttrs;
    private Query seriesQuery;

    @Override
    protected Criteria createCriteria(String[] pids, Attributes keys, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, String[] roles) {
        seriesQuery = session().createQuery(QUERY_SERIES_ATTRS);
        Criteria criteria = session().createCriteria(Instance.class, "instance")
            .createAlias("instance.series", "series")
            .createAlias("series.study", "study")
            .createAlias("study.patient", "patient")
            .setProjection(projection())
            .addOrder(Order.asc(Series_.pk));
        Criterions.addPatientLevelCriteriaTo(criteria, pids, keys, filter, queryOpts);
        Criterions.addStudyLevelCriteriaTo(criteria, keys, filter, queryOpts, roles);
        Criterions.addSeriesLevelCriteriaTo(criteria, keys, filter, queryOpts);
        Criterions.addInstanceLevelCriteriaTo(criteria, keys, filter, queryOpts);
        return criteria;
    }

    private Projection projection() {
        ProjectionList select = Projections.projectionList();
        select.add(Projections.property(Series_.pk));
        select.add(Projections.property(Instance_.retrieveAETs));
        select.add(Projections.property(Instance_.externalRetrieveAET));
        select.add(Projections.property(Instance_.availability));
        select.add(Projections.property(Instance_.encodedAttributes));
        return select;
    }

    @Override
    protected Attributes toAttributes(ScrollableResults results) {
        long seriesPk = results.getLong(0);
        String retrieveAETs = results.getString(1);
        String externalRetrieveAET = results.getString(2);
        Availability availability = (Availability) results.get(3);
        byte[] instAttributes = (byte[]) results.get(4);
        if (this.seriesPk != seriesPk) {
            this.seriesAttrs = querySeriesAttrs(seriesPk);
            this.seriesPk = seriesPk;
        }
        Attributes attrs = new Attributes(seriesAttrs);
        try {
            Utils.decodeAttributes(attrs, instAttributes);
        } catch (IOException e) {
            throw new EJBException(e);
        }
        Utils.setRetrieveAET(attrs, retrieveAETs, externalRetrieveAET);
        Utils.setAvailability(attrs, availability);
        return attrs;
    }

    private Attributes querySeriesAttrs(long seriesPk) {
        Object[] tuple = (Object[]) seriesQuery.setParameter(0, seriesPk).uniqueResult();
        int numberOfStudyRelatedSeries = (Integer) tuple[0];
        int numberOfStudyRelatedInstances = (Integer) tuple[1];
        int numberOfSeriesRelatedInstances = (Integer) tuple[2];
        String modalitiesInStudy = (String) tuple[3];
        String sopClassesInStudy = (String) tuple[4];
        byte[] seriesAttributes = (byte[]) tuple[5];
        byte[] studyAttributes = (byte[]) tuple[6];
        byte[] patientAttributes = (byte[]) tuple[7];
        Attributes attrs = new Attributes();
        try {
            Utils.decodeAttributes(attrs, patientAttributes);
            Utils.decodeAttributes(attrs, studyAttributes);
            Utils.decodeAttributes(attrs, seriesAttributes);
        } catch (IOException e) {
            throw new EJBException(e);
        }
        Utils.setStudyQueryAttributes(attrs,
                numberOfStudyRelatedSeries,
                numberOfStudyRelatedInstances,
                modalitiesInStudy,
                sopClassesInStudy);
        Utils.setSeriesQueryAttributes(attrs, numberOfSeriesRelatedInstances);
        return attrs;
    }
}
