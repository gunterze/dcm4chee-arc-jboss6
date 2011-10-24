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

import org.dcm4che.data.Attributes;
import org.dcm4chee.archive.persistence.Availability;
import org.dcm4chee.archive.persistence.QPatient;
import org.dcm4chee.archive.persistence.QSeries;
import org.dcm4chee.archive.persistence.QStudy;
import org.dcm4chee.archive.persistence.StoreParam;
import org.dcm4chee.archive.persistence.Utils;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class SeriesQueryImpl extends CompositeQueryImpl {

    public SeriesQueryImpl(StatelessSession session, IDWithIssuer[] pids, Attributes keys,
            QueryParam queryParam, StoreParam storeParam) {
        super(query(session, pids, keys, queryParam, storeParam), false);
    }

    private static ScrollableResults query(StatelessSession session, IDWithIssuer[] pids,
            Attributes keys, QueryParam queryParam, StoreParam storeParam) {
        BooleanBuilder builder = new BooleanBuilder();
        Builder.addPatientLevelPredicates(builder, pids, keys, queryParam, storeParam);
        Builder.addStudyLevelPredicates(builder, keys, queryParam, storeParam);
        Builder.addSeriesLevelPredicates(builder, keys, queryParam, storeParam);
        return new HibernateQuery(session)
            .from(QSeries.series)
            .innerJoin(QSeries.series.study, QStudy.study)
            .innerJoin(QStudy.study.patient, QPatient.patient)
            .where(builder)
            .scroll(ScrollMode.FORWARD_ONLY,
                QStudy.study.numberOfStudyRelatedSeries,
                QStudy.study.numberOfStudyRelatedInstances,
                QSeries.series.numberOfSeriesRelatedInstances,
                QStudy.study.modalitiesInStudy,
                QStudy.study.sopClassesInStudy,
                QSeries.series.retrieveAETs,
                QSeries.series.externalRetrieveAET,
                QSeries.series.availability,
                QSeries.series.encodedAttributes,
                QStudy.study.encodedAttributes,
                QPatient.patient.encodedAttributes);
    }

    @Override
    protected Attributes toAttributes(ScrollableResults results) {
        int numberOfStudyRelatedSeries = results.getInteger(0);
        int numberOfStudyRelatedInstances = results.getInteger(1);
        int numberOfSeriesRelatedInstances = results.getInteger(2);
        String modalitiesInStudy = results.getString(3);
        String sopClassesInStudy = results.getString(4);
        String retrieveAETs = results.getString(5);
        String externalRetrieveAET = results.getString(6);
        Availability availability = (Availability) results.get(7);
        byte[] seriesAttributes = results.getBinary(8);
        byte[] studyAttributes = results.getBinary(9);
        byte[] patientAttributes = results.getBinary(10);
        Attributes attrs = new Attributes();
        Utils.decodeAttributes(attrs, patientAttributes);
        Utils.decodeAttributes(attrs, studyAttributes);
        Utils.decodeAttributes(attrs, seriesAttributes);
        Utils.setStudyQueryAttributes(attrs,
                numberOfStudyRelatedSeries,
                numberOfStudyRelatedInstances,
                modalitiesInStudy,
                sopClassesInStudy);
        Utils.setSeriesQueryAttributes(attrs, numberOfSeriesRelatedInstances);
        Utils.setRetrieveAET(attrs, retrieveAETs, externalRetrieveAET);
        Utils.setAvailability(attrs, availability);
        return attrs;
    }

}
