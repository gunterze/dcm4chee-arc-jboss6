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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4chee.archive.domain.Instance;
import org.dcm4chee.archive.domain.Instance_;
import org.dcm4chee.archive.domain.Patient;
import org.dcm4chee.archive.domain.Patient_;
import org.dcm4chee.archive.domain.Series;
import org.dcm4chee.archive.domain.Series_;
import org.dcm4chee.archive.domain.Study;
import org.dcm4chee.archive.domain.Study_;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Stateful
public class InstanceQuery {

    @PersistenceUnit(unitName="dcm4chee-arc")
    private EntityManagerFactory emf;
    private EntityManager em;
    private TypedQuery<SeriesOfInstanceQueryResult> seriesQuery;
    private long seriesPk = -1L;
    private Attributes seriesAttrs;
    private Iterator<InstanceQueryResult> results;

    public void find(String[] pids, Attributes keys, boolean matchUnknown) {
        em = emf.createEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        seriesQuery = buildSeriesQuery(cb);
        CriteriaQuery<InstanceQueryResult> cq =
                cb.createQuery(InstanceQueryResult.class);
        
        Root<Instance> inst = cq.from(Instance.class);
        Join<Instance, Series> series = inst.join(Instance_.series);
        Join<Series, Study> study = series.join(Series_.study);
        Join<Study, Patient> pat = study.join(Study_.patient);
        cq.select(cb.construct(InstanceQueryResult.class,
                series.get(Series_.pk),
                inst.get(Instance_.retrieveAETs),
                inst.get(Instance_.externalRetrieveAET),
                inst.get(Instance_.availability),
                inst.get(Instance_.encodedAttributes)));
        cq.orderBy(cb.asc(series.get(Series_.pk)));
        List<Predicate> predicates = new ArrayList<Predicate>();
        List<Object> params = new ArrayList<Object>();
        fillPredicates(cb, pat, study, series, inst, pids, keys, matchUnknown,
                predicates, params);
        cq.where(predicates.toArray(new Predicate[predicates.size()]));
        TypedQuery<InstanceQueryResult> q = em.createQuery(cq);
        int i = 0;
        for (Object param : params)
            q.setParameter(Matching.paramName(i++), param);
        results = q.getResultList().iterator();
    }

    private TypedQuery<SeriesOfInstanceQueryResult> buildSeriesQuery(
            CriteriaBuilder cb) {
        CriteriaQuery<SeriesOfInstanceQueryResult> cq = cb
                .createQuery(SeriesOfInstanceQueryResult.class);
        Root<Series> series = cq.from(Series.class);
        Join<Series, Study> study = series.join(Series_.study);
        Join<Study, Patient> pat = study.join(Study_.patient);
        cq.select(cb.construct(SeriesOfInstanceQueryResult.class, study
                .get(Study_.numberOfStudyRelatedSeries), study
                .get(Study_.numberOfStudyRelatedInstances), series
                .get(Series_.numberOfSeriesRelatedInstances), study
                .get(Study_.modalitiesInStudy), study
                .get(Study_.sopClassesInStudy), series
                .get(Series_.encodedAttributes), study
                .get(Study_.encodedAttributes), pat
                .get(Patient_.encodedAttributes)));
        cq.where(cb.equal(series.get(Series_.pk),
                cb.parameter(Long.class, Matching.paramName(0))));
        return em.createQuery(cq);
    }

    private void fillPredicates(CriteriaBuilder cb, Path<Patient> pat,
            Join<Series, Study> study, Path<Series> series, Root<Instance> inst,
            String[] pids, Attributes keys, boolean matchUnknown,
            List<Predicate> predicates, List<Object> params) {
        SeriesQuery.fillPredicates(cb, pat, study, series, pids, keys,
                matchUnknown, predicates, params);
        Matching.add(predicates, Matching.listOfUID(cb,
                series.get(Series_.seriesInstanceUID),
                keys.getStrings(Tag.SeriesInstanceUID), params));
    }

    public boolean hasNext() {
        checkResults();
        return results.hasNext();
    }

    public Attributes next() throws IOException {
        checkResults();
        InstanceQueryResult result = results.next();
        return result.mergeAttributes(seriesAttrs(result.getSeriesPk()));
    }

    private Attributes seriesAttrs(long seriesPk) throws IOException {
        if (this.seriesPk != seriesPk) {
            this.seriesAttrs = seriesQuery
                    .setParameter(Matching.paramName(0), seriesPk)
                    .getSingleResult().mergeAttributes();
            this.seriesPk = seriesPk;
        }
        return seriesAttrs;
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
