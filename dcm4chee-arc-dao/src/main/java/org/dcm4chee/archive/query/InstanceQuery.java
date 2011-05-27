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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.dcm4che.data.Attributes;
import org.dcm4chee.archive.domain.Availability;
import org.dcm4chee.archive.domain.Instance;
import org.dcm4chee.archive.domain.Instance_;
import org.dcm4chee.archive.domain.Patient;
import org.dcm4chee.archive.domain.Patient_;
import org.dcm4chee.archive.domain.Series;
import org.dcm4chee.archive.domain.Series_;
import org.dcm4chee.archive.domain.Study;
import org.dcm4chee.archive.domain.Study_;
import org.dcm4chee.archive.domain.Utils;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Stateful
public class InstanceQuery {

    @PersistenceUnit(unitName="dcm4chee-arc")
    private EntityManagerFactory emf;

    private EntityManager em;

    private Iterator<Tuple> results;

    private TypedQuery<Tuple> seriesQuery;

    private long seriesPk = -1L;
    private Attributes seriesAttrs;

    @PostConstruct
    public void init() {
        em = emf.createEntityManager();
    }

    public void find(String[] pids, Attributes keys, boolean matchUnknown) {
        em = emf.createEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        seriesQuery = buildSeriesQuery(cb);
        TypedQuery<Tuple> instQuery =
                buildInstanceQuery(cb, pids, keys, matchUnknown);
        results = instQuery.getResultList().iterator();
    }

    private TypedQuery<Tuple> buildSeriesQuery(CriteriaBuilder cb) {
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<Series> series = cq.from(Series.class);
        Join<Series, Study> study = series.join(Series_.study);
        Join<Study, Patient> pat = study.join(Study_.patient);
        cq.multiselect(
                study.get(Study_.numberOfStudyRelatedSeries),
                study.get(Study_.numberOfStudyRelatedInstances),
                series.get(Series_.numberOfSeriesRelatedInstances),
                study.get(Study_.modalitiesInStudy),
                study.get(Study_.sopClassesInStudy),
                series.get(Series_.encodedAttributes),
                study.get(Study_.encodedAttributes),
                pat.get(Patient_.encodedAttributes));
        cq.where(cb.equal(series.get(Series_.pk),
                cb.parameter(Long.class, Matching.paramName(0))));
        return em.createQuery(cq);
    }

    private Attributes querySeriesAttrs(long seriesPk) throws IOException {
        seriesQuery.setParameter(Matching.paramName(0), seriesPk);
        Tuple tuple = seriesQuery.getSingleResult();
        int numberOfStudyRelatedSeries = tuple.get(0, Integer.class);
        int numberOfStudyRelatedInstances = tuple.get(1, Integer.class);
        int numberOfSeriesRelatedInstances = tuple.get(2, Integer.class);
        String modalitiesInStudy = tuple.get(3, String.class);
        String sopClassesInStudy = tuple.get(4, String.class);
        byte[] seriesAttributes = tuple.get(5, byte[].class);
        byte[] studyAttributes = tuple.get(6, byte[].class);
        byte[] patientAttributes = tuple.get(7, byte[].class);
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
            String[] pids, Attributes keys, boolean matchUnknown) {
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
        Matching.instance(cb, pat, study, series, inst, pids, keys, matchUnknown,
                predicates, params);
        cq.where(predicates.toArray(new Predicate[predicates.size()]));
        TypedQuery<Tuple> instQuery = em.createQuery(cq);
        int i = 0;
        for (Object param : params)
            instQuery.setParameter(Matching.paramName(i++), param);
        return instQuery;
    }

    public boolean hasNext() {
        checkResults();
        return results.hasNext();
    }

    public Attributes next() throws IOException {
        checkResults();
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
    }

    private void checkResults() {
        if (results == null)
            throw new IllegalStateException("results not initalized");
    }

    @Remove
    public void close() {
        em.close();
    }

}
