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

import org.dcm4che.data.Attributes;
import org.dcm4che.net.pdu.QueryOption;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.dcm4chee.archive.persistence.Availability;
import org.dcm4chee.archive.persistence.QInstance;
import org.dcm4chee.archive.persistence.QPatient;
import org.dcm4chee.archive.persistence.QSeries;
import org.dcm4chee.archive.persistence.QStudy;
import org.dcm4chee.archive.persistence.Utils;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
class InstanceQueryImpl extends CompositeQueryImpl {

    private static final String QUERY_SERIES_ATTRS = "select "
            + "s.study.numberOfStudyRelatedSeries, "
            + "s.study.numberOfStudyRelatedInstances, "
            + "s.numberOfSeriesRelatedInstances, "
            + "s.study.modalitiesInStudy, "
            + "s.study.sopClassesInStudy, "
            + "s.encodedAttributes, "
            + "s.study.encodedAttributes, "
            + "s.study.patient.encodedAttributes "
            + "from Series s "
            + "where s.pk = ?";
    private long seriesPk = -1L;
    private Attributes seriesAttrs;
    private Query seriesQuery;

    public InstanceQueryImpl(StatelessSession session, String[] pids, Attributes keys,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts, String[] roles) {
        super(query(session, pids, keys, filter, queryOpts, roles), false);
        seriesQuery = session.createQuery(QUERY_SERIES_ATTRS);
    }

    private static ScrollableResults query(StatelessSession session, String[] pids,
            Attributes keys, AttributeFilter filter, EnumSet<QueryOption> queryOpts,
            String[] roles) {
        BooleanBuilder builder = new BooleanBuilder();
        Builder.addPatientLevelPredicates(builder, pids, keys, filter, queryOpts);
        Builder.addStudyLevelPredicates(builder, keys, filter, queryOpts, roles);
        Builder.addSeriesLevelPredicates(builder, keys, filter, queryOpts);
        Builder.addInstanceLevelPredicates(builder, keys, filter, queryOpts);
        return new HibernateQuery(session)
            .from(QInstance.instance)
            .innerJoin(QInstance.instance.series, QSeries.series)
            .innerJoin(QSeries.series.study, QStudy.study)
            .innerJoin(QStudy.study.patient, QPatient.patient)
            .where(builder)
            .scroll(ScrollMode.FORWARD_ONLY,
                QSeries.series.pk,
                QInstance.instance.retrieveAETs,
                QInstance.instance.externalRetrieveAET,
                QInstance.instance.availability,
                QInstance.instance.encodedAttributes);
    }

    @Override
    protected Attributes toAttributes(ScrollableResults results) {
        long seriesPk = results.getLong(0);
        String retrieveAETs = results.getString(1);
        String externalRetrieveAET = results.getString(2);
        Availability availability = (Availability) results.get(3);
        byte[] instAttributes = results.getBinary(4);
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
