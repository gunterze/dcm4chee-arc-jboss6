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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.dcm4che.data.Attributes;
import org.dcm4che.net.Status;
import org.dcm4che.net.pdu.QueryOption;
import org.dcm4che.net.service.DicomServiceException;
import org.dcm4chee.archive.persistence.Availability;
import org.dcm4chee.archive.persistence.Instance;
import org.dcm4chee.archive.persistence.Instance_;
import org.dcm4chee.archive.persistence.Patient;
import org.dcm4chee.archive.persistence.Series;
import org.dcm4chee.archive.persistence.Series_;
import org.dcm4chee.archive.persistence.Study;
import org.dcm4chee.archive.persistence.Study_;
import org.dcm4chee.archive.persistence.Utils;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Stateful
public class InstanceQueryBean implements InstanceQuery {

    @PersistenceContext(unitName = "dcm4chee-arc",
                        type = PersistenceContextType.EXTENDED)
    private EntityManager em;
    private Query seriesQuery;

    private Attributes rq;
    private Iterator<Tuple> results;

    private long seriesPk = -1L;
    private Attributes seriesAttrs;
    private boolean optionalKeyNotSupported;

    @Override
    public void find(Attributes rq, String[] pids, Attributes keys,
            EnumSet<QueryOption> queryOpts, boolean matchUnknown) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        TypedQuery<Tuple> instQuery =
                buildInstanceQuery(cb, pids, keys, queryOpts,
                        matchUnknown);
        results = instQuery.getResultList().iterator();
        seriesQuery = em.createNamedQuery(Series.FIND_ATTRIBUTES_BY_SERIES_PK);
    }

    @Override
    public boolean optionalKeyNotSupported() {
        return optionalKeyNotSupported;
    }

    private Attributes querySeriesAttrs(long seriesPk) throws IOException {
        Object[] tuple = (Object[])
                seriesQuery.setParameter(1, seriesPk).getSingleResult();
        int numberOfStudyRelatedSeries = (Integer) tuple[0];
        int numberOfStudyRelatedInstances = (Integer) tuple[1];
        int numberOfSeriesRelatedInstances = (Integer) tuple[2];
        String modalitiesInStudy = (String) tuple[3];
        String sopClassesInStudy = (String) tuple[4];
        byte[] seriesAttributes = (byte[]) tuple[5];
        byte[] studyAttributes = (byte[]) tuple[6];
        byte[] patientAttributes = (byte[]) tuple[7];
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
        return attrs;
    }

    private TypedQuery<Tuple> buildInstanceQuery(CriteriaBuilder cb,
            String[] pids, Attributes keys, EnumSet<QueryOption> queryOpts,
            boolean matchUnknown) {
        CriteriaQuery<Tuple> cq =  cb.createTupleQuery();
        Root<Instance> inst = cq.from(Instance.class);
        Join<Instance, Series> series = inst.join(Instance_.series);
        Join<Series, Study> study = series.join(Series_.study);
        Join<Study, Patient> pat = study.join(Study_.patient);
        cq.multiselect(
                series.get(Series_.pk),
                inst.get(Instance_.retrieveAETs),
                inst.get(Instance_.externalRetrieveAET),
                inst.get(Instance_.availability),
                inst.get(Instance_.encodedAttributes));
        cq.orderBy(cb.asc(series.get(Series_.pk)));
        List<Predicate> predicates = new ArrayList<Predicate>();
        List<Object> params = new ArrayList<Object>();
        Matching.instance(cb, cq, pat, study, series, inst, pids, keys,
                queryOpts, matchUnknown, predicates, params);
        cq.where(predicates.toArray(new Predicate[predicates.size()]));
        TypedQuery<Tuple> instQuery = em.createQuery(cq);
        int i = 0;
        for (Object param : params)
            instQuery.setParameter(Matching.paramName(i++), param);
        return instQuery;
    }

    @Override
    public boolean hasMoreMatches() throws DicomServiceException {
        checkResults();
        try {
            return results.hasNext();
        } catch (Exception e) {
            throw unableToProcess(e);
        }
    }

    @Override
    public Attributes nextMatch() throws DicomServiceException {
        checkResults();
        try {
            Tuple tuple = results.next();
            long seriesPk = tuple.get(0, Long.class);
            String retrieveAETs = tuple.get(1, String.class);
            String externalRetrieveAET = tuple.get(2, String.class);
            Availability availability = tuple.get(3, Availability.class);
            byte[] instanceAttributes = tuple.get(4, byte[].class);
            if (this.seriesPk != seriesPk) {
                this.seriesAttrs = querySeriesAttrs(seriesPk);
                this.seriesPk = seriesPk;
            }
    
            Attributes attrs = new Attributes();
            attrs.addAll(seriesAttrs);
            Utils.decodeAttributes(attrs, instanceAttributes);
            Utils.setRetrieveAET(attrs, retrieveAETs, externalRetrieveAET);
            Utils.setAvailability(attrs, availability);
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
