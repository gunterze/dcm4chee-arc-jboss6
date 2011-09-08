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
import java.util.List;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.net.service.InstanceLocator;
import org.dcm4chee.archive.persistence.FileRef;
import org.dcm4chee.archive.persistence.FileRef_;
import org.dcm4chee.archive.persistence.FileSystem;
import org.dcm4chee.archive.persistence.FileSystem_;
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
@Stateless
public class CalculateMatchesBean implements CalculateMatches {

    @PersistenceContext(unitName = "dcm4chee-arc")
    private EntityManager em;

    @Override
    public List<InstanceLocator> calculateMatches(Attributes keys) {
        return calculateMatches(pids(keys), keys);
    }

    @Override
    public List<InstanceLocator> calculateMatches(String[] pids, Attributes keys) {
        TypedQuery<Tuple> query = buildQuery(pids, keys);
        return locate(query.getResultList());
    }

    private String[] pids(Attributes keys) {
        String pid = keys.getString(Tag.PatientID, null);
        return pid == null
                ? null 
                : new String[] { 
                        pid,
                        keys.getString(Tag.IssuerOfPatientID, null)};
    }

    private TypedQuery<Tuple> buildQuery(String[] pids, Attributes keys) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq =  cb.createTupleQuery();
        Root<Instance> inst = cq.from(Instance.class);
        Join<Instance, FileRef> fileRef = inst.join(Instance_.fileRefs, JoinType.LEFT);
        Join<FileRef, FileSystem> fs = fileRef.join(FileRef_.fileSystem, JoinType.LEFT);
        Join<Instance, Series> series = inst.join(Instance_.series);
        Join<Series, Study> study = series.join(Series_.study);
        Join<Study, Patient> pat = study.join(Study_.patient);
        cq.multiselect(
                fileRef.get(FileRef_.transferSyntaxUID),
                fileRef.get(FileRef_.filePath),
                fs.get(FileSystem_.uri),
                series.get(Series_.pk),
                inst.get(Instance_.pk),
                inst.get(Instance_.sopClassUID),
                inst.get(Instance_.sopInstanceUID),
                inst.get(Instance_.retrieveAETs),
                inst.get(Instance_.externalRetrieveAET),
                inst.get(Instance_.encodedAttributes));
        List<Predicate> predicates = new ArrayList<Predicate>();
        List<Object> params = new ArrayList<Object>();
        Matching.patientID(cb, pat, pids, false, predicates, params);
        Matching.listOfUID(cb, study.get(Study_.studyInstanceUID),
                keys.getStrings(Tag.StudyInstanceUID), predicates, params);
        Matching.listOfUID(cb, series.get(Series_.seriesInstanceUID),
                keys.getStrings(Tag.SeriesInstanceUID), predicates, params);
        Matching.listOfUID(cb, inst.get(Instance_.sopInstanceUID),
                keys.getStrings(Tag.SOPInstanceUID), predicates, params);
        cq.where(predicates.toArray(new Predicate[predicates.size()]));
        cq.orderBy(cb.asc(series.get(Series_.pk)),
                   cb.asc(inst.get(Instance_.pk)),
                   cb.asc(fs.get(FileSystem_.availability)));
        TypedQuery<Tuple> instQuery = em.createQuery(cq);
        int i = 0;
        for (Object param : params)
            instQuery.setParameter(Matching.paramName(i++), param);
        return instQuery;
    }

    private List<InstanceLocator> locate(List<Tuple> tuples) {
        List<InstanceLocator> locators = new ArrayList<InstanceLocator>(tuples.size());
        Query seriesQuery = em.createNamedQuery(Series.FIND_ATTRIBUTES_BY_SERIES_PK);
        long instPk = -1;
        long seriesPk = -1;
        Attributes seriesAttrs = null;
        for (Tuple tuple : tuples) {
            String tsuid = tuple.get(0, String.class);
            String filePath = tuple.get(1, String.class);
            String fsuri = tuple.get(2, String.class);
            long nextSeriesPk = tuple.get(3, Long.class);
            long nextInstPk = tuple.get(4, Long.class);
            if (seriesPk != nextSeriesPk) {
                seriesAttrs = fetchSeriesAttrs(seriesQuery, nextSeriesPk);
                seriesPk = nextSeriesPk;
            }
            if (instPk != nextInstPk) {
                String cuid = tuple.get(5, String.class);
                String iuid = tuple.get(6, String.class);
                String retrieveAETs = tuple.get(7, String.class);
                String externalRetrieveAET = tuple.get(8, String.class);
                String uri;
                Attributes attrs;
                if (fsuri != null) {
                    uri = fsuri + filePath;
                    byte[] instAttrs = tuple.get(9, byte[].class);
                    attrs = new Attributes(seriesAttrs);
                    try {
                        Utils.decodeAttributes(attrs, instAttrs);
                    } catch (IOException e) {
                        throw new EJBException(e);
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("aet:");
                    if (retrieveAETs != null) {
                        sb.append(retrieveAETs);
                    }
                    if (externalRetrieveAET != null) {
                        if (retrieveAETs != null)
                            sb.append('\\');
                        sb.append(externalRetrieveAET);
                    }
                    uri = sb.toString();
                    attrs = null;
                }
                locators.add(new InstanceLocator(cuid, iuid, tsuid, uri).setObject(attrs));
                instPk = nextInstPk;
            }
        }
        return locators ;
    }

    private Attributes fetchSeriesAttrs(Query seriesQuery, long seriesPk) {
        Object[] tuple = (Object[])
                seriesQuery.setParameter(1, seriesPk).getSingleResult();
        byte[] seriesAttributes = (byte[]) tuple[0];
        byte[] studyAttributes = (byte[]) tuple[1];
        byte[] patientAttributes = (byte[]) tuple[2];
        Attributes attrs = new Attributes();
        try {
            Utils.decodeAttributes(attrs, patientAttributes);
            Utils.decodeAttributes(attrs, studyAttributes);
            Utils.decodeAttributes(attrs, seriesAttributes);
        } catch (IOException e) {
            throw new EJBException(e);
        }
        return attrs;
    }
}
