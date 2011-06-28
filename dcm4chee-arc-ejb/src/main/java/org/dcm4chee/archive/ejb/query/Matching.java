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

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.SingularAttribute;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.ItemPointer;
import org.dcm4che.data.PersonName;
import org.dcm4che.data.Tag;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.dcm4chee.archive.persistence.Code;
import org.dcm4chee.archive.persistence.Code_;
import org.dcm4chee.archive.persistence.Instance;
import org.dcm4chee.archive.persistence.Instance_;
import org.dcm4chee.archive.persistence.Issuer;
import org.dcm4chee.archive.persistence.Issuer_;
import org.dcm4chee.archive.persistence.Patient;
import org.dcm4chee.archive.persistence.Patient_;
import org.dcm4chee.archive.persistence.RequestAttributes;
import org.dcm4chee.archive.persistence.RequestAttributes_;
import org.dcm4chee.archive.persistence.Series;
import org.dcm4chee.archive.persistence.Series_;
import org.dcm4chee.archive.persistence.Study;
import org.dcm4chee.archive.persistence.Study_;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Michael Backhaus <michael.backhaus@agfa.com>
 */
class Matching {

    private static final String[] NAMES =
            { "p0", "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9" };

    public static String paramName(int paramIndex) {
        return paramIndex < NAMES.length ? NAMES[paramIndex]
                : ("p" + paramIndex);
    }

    public static void add(List<Predicate> predicates, Predicate e) {
        if (e != null)
            predicates.add(e);
    }

    public static String toLikePattern(String s) {
        StringBuilder like = new StringBuilder(s.length());
        char[] cs = s.toCharArray();
        char p = 0;
        for (char c : cs) {
            switch (c) {
            case '*':
                if (c != p)
                    like.append('%');
                break;
            case '?':
                like.append('_');
                break;
            case '_':
            case '%':
                like.append('\\');
                // fall through
            default:
                like.append(c);
            }
            p = c;
        }
        return like.toString();
    }

    public static boolean containsWildcard(String s) {
        return s.indexOf('*') >= 0 || s.indexOf('?') >= 0;
    }

    @SuppressWarnings("unchecked")
    public static Predicate listOfUID(CriteriaBuilder cb, Path<String> field,
            String[] values, List<Object> params) {
        if (values == null || values.length == 0)
            return null;

        if (values.length == 1)
            return singleValue0(cb, field, values[0], params);

        ParameterExpression<String>[] pes =
                new ParameterExpression[values.length];
        for (int i = 0; i < values.length; i++) {
            pes[i] = cb.parameter(String.class, paramName(params.size()));
            params.add(values[i]);
        }
        return field.in(pes);
    }

    private static Predicate personName(CriteriaBuilder cb,
            Path<String> alphabethic, Path<String> ideographic,
            Path<String> phonetic, String value, boolean matchUnknown,
            List<Object> params) {
        if (value.equals("*"))
            return null;

        PersonName pn = new PersonName(value);
        Predicate predicate;
        String queryString =
                pn.getNormalizedQueryString(PersonName.Group.Alphabetic);
        if (value.indexOf('=') == -1) {
            predicate =
                    cb.or(wildCard0(cb, alphabethic, queryString, params),
                            wildCard0(cb, ideographic, queryString, params),
                            wildCard0(cb, phonetic, queryString, params));
        } else {
            predicate =
                    and(
                            cb,
                            wildCard0(cb, alphabethic, queryString, params),
                            wildCard0(
                                    cb,
                                    ideographic,
                                    pn
                                            .getNormalizedQueryString(PersonName.Group.Ideographic),
                                    params),
                            wildCard0(
                                    cb,
                                    phonetic,
                                    pn
                                            .getNormalizedQueryString(PersonName.Group.Phonetic),
                                    params));
            if (predicate == null)
                return null;
        }
        return matchUnknown ? cb.or(predicate, cb.and(cb
                .equal(alphabethic, "*"), cb.equal(ideographic, "*"), cb.equal(
                phonetic, "*"))) : predicate;
    }

    private static Predicate and(CriteriaBuilder cb, Predicate p1,
            Predicate p2, Predicate p3) {
        return p1 != null ? p2 != null ? p3 != null ? cb.and(p1, p2, p3) : cb
                .and(p1, p2) : p3 != null ? cb.and(p1, p3) : p1
                : p2 != null ? p3 != null ? cb.and(p2, p3) : p2 : null;
    }

    public static Predicate wildCard(CriteriaBuilder cb, Path<String> field,
            String value, boolean matchUnknown, List<Object> params) {
        if (value.equals("*"))
            return null;

        return matchUnknown0(cb, field, matchUnknown, wildCard0(cb, field,
                value, params));
    }

    private static Predicate wildCard0(CriteriaBuilder cb, Path<String> field,
            String value, List<Object> params) {
        if (!Matching.containsWildcard(value))
            return singleValue0(cb, field, value, params);

        String pattern = Matching.toLikePattern(value);
        if (pattern.equals("%"))
            return null;

        ParameterExpression<String> param = setParam(cb, params, pattern);
        return cb.like(field, param);
    }

    private static Predicate singleValue0(CriteriaBuilder cb,
            Path<String> field, String value, List<Object> params) {
        ParameterExpression<String> param = setParam(cb, params, value);
        return cb.equal(field, param);
    }

    private static Predicate studySeriesSubQuery(CriteriaBuilder cb,
            SingularAttribute<Series, String> attr, String value,
            SingularAttribute<Study, String> nullField, Path<Study> study,
            boolean matchUnknown, List<Object> params) {
        if (value.equals("*"))
            return null;

        CriteriaQuery<String> q = cb.createQuery(String.class);
        Subquery<Series> sq = q.subquery(Series.class);
        Root<Study> studySub = sq.correlate((Root<Study>) study);
        Join<Study, Series> series = studySub.join(Study_.series);
        sq.select(series);
        ParameterExpression<String> param = setParam(cb, params, value);
        sq.where(cb.equal(series.get(attr), param));
        return matchUnknown ? cb.or(cb.exists(sq), cb.isNull(study
                .get(nullField))) : cb.exists(sq);
    }

    private static Predicate studyInstanceSubQuery(CriteriaBuilder cb,
            SingularAttribute<Instance, String> attr, String value,
            SingularAttribute<Study, String> nullField, Path<Study> study,
            boolean matchUnknown, List<Object> params) {
        if (value.equals("*"))
            return null;

        CriteriaQuery<String> q = cb.createQuery(String.class);
        Subquery<Instance> sq = q.subquery(Instance.class);
        Root<Study> studySub = sq.correlate((Root<Study>) study);
        Join<Study, Series> series = studySub.join(Study_.series);
        Join<Series, Instance> instance = series.join(Series_.instances);
        sq.select(instance);
        ParameterExpression<String> param = setParam(cb, params, value);
        sq.where(cb.equal(instance.get(attr), param));
        return matchUnknown ? cb.or(cb.exists(sq), cb.isNull(study
                .get(nullField))) : cb.exists(sq);
    }

    private static Predicate issuerOfAccessionNumber(CriteriaBuilder cb,
            Path<Study> study, Attributes keys, boolean matchUnknown,
            List<Object> params) {
        if (!keys.containsValue(Tag.AccessionNumber))
            return null;
        Attributes attrs =
                keys.getNestedDataset(new ItemPointer(
                        Tag.IssuerOfAccessionNumberSequence));
        if (attrs.isEmpty() || attrs == null)
            return null;
        else
            return studyIssuerSubQuery(cb, attrs, study, params, matchUnknown);
    }

    private static Predicate studyIssuerSubQuery(CriteriaBuilder cb,
            Attributes attrs, Path<Study> study, List<Object> params,
            boolean matchUnknown) {
        CriteriaQuery<String> q = cb.createQuery(String.class);
        Subquery<Issuer> sq = q.subquery(Issuer.class);
        Root<Study> studySub = sq.correlate((Root<Study>) study);
        Join<Study, Issuer> issuer =
                studySub.join(Study_.issuerOfAccessionNumber);
        Predicate predicate = queryIssuer(cb, attrs, params, issuer);
        if (predicate == null)
            return null;

        sq.select(issuer);
        sq.where(predicate);
        return matchUnknown ? cb.or(cb.exists(sq), cb.isNull(study
                .get(Study_.issuerOfAccessionNumber))) : cb.exists(sq);
    }

    private static<T> Predicate queryIssuer(CriteriaBuilder cb, Attributes attrs,
            List<Object> params, Join<T, Issuer> issuer) {
        Predicate predicate = null;
        if (attrs.containsValue(Tag.LocalNamespaceEntityID)) {
            ParameterExpression<String> param =
                    setParam(cb, params, attrs.getString(
                            Tag.LocalNamespaceEntityID, null));
            predicate = cb.equal(issuer.get(Issuer_.entityID), param);
        }
        if (attrs.containsValue(Tag.UniversalEntityID)) {
            ParameterExpression<String> param =
                    setParam(cb, params, attrs.getString(Tag.UniversalEntityID,
                            null));
            Predicate UID = cb.equal(issuer.get(Issuer_.entityUID), param);
            if (attrs.containsValue(Tag.UniversalEntityIDType)) {
                param =
                        setParam(cb, params, attrs.getString(
                                Tag.UniversalEntityIDType, null));
                UID =
                        cb.and(UID, cb.equal(issuer.get(Issuer_.entityUIDType),
                                param));
            }
            if (predicate == null)
                predicate = UID;
            else
                predicate = cb.and(predicate, UID);
        }
        return predicate;
    }

    private static Predicate procedureCodes(CriteriaBuilder cb,
            Path<Study> study, Attributes keys, boolean matchUnknown,
            List<Object> params) {
        if (!keys.containsValue(Tag.ProcedureCodeSequence))
            return null;
        Attributes attrs =
                keys
                        .getNestedDataset(new ItemPointer(
                                Tag.ProcedureCodeSequence));
        if (attrs.isEmpty() || attrs == null)
            return null;
        else
            return studyCodesSubQuery(cb, attrs, study, params, matchUnknown);
    }

    private static Predicate studyCodesSubQuery(CriteriaBuilder cb,
            Attributes attrs, Path<Study> study, List<Object> params,
            boolean matchUnknown) {
        if (!attrs.containsValue(Tag.CodeValue)
                || !attrs.containsValue(Tag.CodingSchemeDesignator))
            return null;
        CriteriaQuery<String> q = cb.createQuery(String.class);
        Subquery<Code> sq = q.subquery(Code.class);
        Root<Study> studySub = sq.correlate((Root<Study>) study);
        Join<Study, Code> codes = studySub.join(Study_.procedureCodes);
        ParameterExpression<String> param =
                setParam(cb, params, attrs.getString(Tag.CodeValue, null));
        Predicate predicate = cb.equal(codes.get(Code_.codeValue), param);
        param =
                setParam(cb, params, attrs.getString(
                        Tag.CodingSchemeDesignator, null));
        predicate =
                cb.and(predicate, cb.equal(codes
                        .get(Code_.codingSchemeDesignator), param));
        if (attrs.containsValue(Tag.CodingSchemeVersion)) {
            param =
                    setParam(cb, params, attrs.getString(
                            Tag.CodingSchemeVersion, null));
            predicate =
                    cb.and(predicate, cb.equal(codes
                            .get(Code_.codingSchemeVersion), param));
        }
        sq.select(codes);
        sq.where(predicate);
        return matchUnknown ? cb.or(cb.exists(sq), cb.isEmpty(study
                .get(Study_.procedureCodes))) : cb.exists(sq);
    }

    private static Predicate requestedAttributesSequence(CriteriaBuilder cb,
            Path<Series> series, Attributes keys, boolean matchUnknown,
            List<Object> params) {
        if (!keys.containsValue(Tag.RequestAttributesSequence))
            return null;
        Attributes attrs =
                keys.getNestedDataset(new ItemPointer(
                        Tag.RequestAttributesSequence));
        if (attrs.isEmpty() || attrs == null)
            return null;
        else
            return seriesRequestedAttributesSubQuery(cb, attrs, series, params,
                    matchUnknown);
    }

    private static Predicate seriesRequestedAttributesSubQuery(
            CriteriaBuilder cb, Attributes attrs, Path<Series> series,
            List<Object> params, boolean matchUnknown) {
        CriteriaQuery<String> q = cb.createQuery(String.class);
        Subquery<RequestAttributes> sq = q.subquery(RequestAttributes.class);
        Root<Series> seriesSub = sq.correlate((Root<Series>) series);
        Join<Series, RequestAttributes> requestedAttributes =
                seriesSub.join(Series_.requestAttributes);
        Predicate predicate = null;
        predicate =
                addRequestedAttributesSearch(cb, attrs, params,
                        requestedAttributes, predicate,
                        Tag.RequestedProcedureID,
                        RequestAttributes_.requestedProcedureID, matchUnknown);
        predicate =
                addRequestedAttributesSearch(cb, attrs, params,
                        requestedAttributes, predicate,
                        Tag.ScheduledProcedureStepID,
                        RequestAttributes_.scheduledProcedureStepID,
                        matchUnknown);
        predicate =
                personName(
                        cb,
                        requestedAttributes
                                .get(RequestAttributes_.requestingPhysician),
                        requestedAttributes
                                .get(RequestAttributes_.requestingPhysicianIdeographicName),
                        requestedAttributes
                                .get(RequestAttributes_.requestingPhysicianPhoneticName),
                        AttributeFilter.getString(attrs,
                                Tag.ReferringPhysicianName), matchUnknown,
                        params);
        predicate =
                addRequestedAttributesSearch(cb, attrs, params,
                        requestedAttributes, predicate, Tag.StudyInstanceUID,
                        RequestAttributes_.studyInstanceUID, matchUnknown);
        predicate =
            addRequestedAttributesSearch(cb, attrs, params,
                    requestedAttributes, predicate, Tag.AccessionNumber,
                    RequestAttributes_.accessionNumber, matchUnknown);
        if (attrs.contains(Tag.AccessionNumber))
            predicate = requestedAttributesIssuerSubQuery(cb, attrs, 
                    requestedAttributes, params, matchUnknown);
        predicate =
            addRequestedAttributesSearch(cb, attrs, params,
                    requestedAttributes, predicate, Tag.ScheduledProcedureStepID,
                    RequestAttributes_.scheduledProcedureStepID, matchUnknown);
        predicate =
            addRequestedAttributesSearch(cb, attrs, params,
                    requestedAttributes, predicate, Tag.RequestingService,
                    RequestAttributes_.requestingService, matchUnknown);
        if (predicate == null)
            return null;

        sq.select(requestedAttributes);
        sq.where(predicate);
        return matchUnknown ? cb.or(cb.exists(sq), cb.isEmpty(series
                .get(Series_.requestAttributes))) : cb.exists(sq);
    }

    private static Predicate addRequestedAttributesSearch(CriteriaBuilder cb,
            Attributes attrs, List<Object> params,
            Join<Series, RequestAttributes> requestedAttributes,
            Predicate predicate, int tag,
            SingularAttribute<RequestAttributes, String> field,
            boolean matchUnknown) {
        if (attrs.containsValue(tag)) {
            ParameterExpression<String> param =
                    setParam(cb, params, attrs.getString(tag, null));
            if (predicate == null)
                predicate = cb.equal(requestedAttributes.get(field), param);
            else
                predicate =
                        cb.and(predicate, cb.equal(requestedAttributes
                                .get(field), param));
        }
        return matchUnknown0(cb, requestedAttributes.get(field), matchUnknown,
                predicate);
    }
    
    private static Predicate requestedAttributesIssuerSubQuery(CriteriaBuilder cb,
            Attributes attrs, Path<RequestAttributes> rePath, List<Object> params,
            boolean matchUnknown) {
        CriteriaQuery<String> q = cb.createQuery(String.class);
        Subquery<Issuer> sq = q.subquery(Issuer.class);
        Root<RequestAttributes> root = sq.correlate((Root<RequestAttributes>) rePath);
        Join<RequestAttributes, Issuer> issuer =
                root.join(RequestAttributes_.issuerOfAccessionNumber);
        Predicate predicate = queryIssuer(cb, attrs, params, issuer);
        if (predicate == null)
            return null;

        sq.select(issuer);
        sq.where(predicate);
        return matchUnknown ? cb.or(cb.exists(sq), cb.isNull(rePath
                .get(RequestAttributes_.issuerOfAccessionNumber))) : cb.exists(sq);
    }

    static Predicate matchUnknown0(CriteriaBuilder cb, Path<String> field,
            boolean matchUnknown, Predicate predicate) {
        return matchUnknown ? cb.or(predicate, cb.equal(field, "*"))
                : predicate;
    }

    public static Predicate patientID(CriteriaBuilder cb, Path<String> idField,
            Path<String> issuerField, String[] pids, boolean matchUnknown,
            List<Object> params) {
        if (pids == null || pids.length == 0)
            return null;

        Predicate predicate =
                pids.length == 1 ? patientID0(cb, idField, issuerField,
                        pids[0], pids[1], params) : patientID0(cb, idField,
                        issuerField, pids, params);
        return matchUnknown ? cb.or(predicate, cb.isNull(idField)) : predicate;
    }

    private static Predicate patientID0(CriteriaBuilder cb,
            Path<String> idField, Path<String> issuerField, String[] pids,
            List<Object> params) {
        Predicate[] predicates = new Predicate[pids.length >> 1];
        for (int i = 0, j = 0; i < predicates.length; i++, j++, j++)
            predicates[i] =
                    patientID0(cb, idField, issuerField, pids[j], pids[j + 1],
                            params);
        return cb.or(predicates);
    }

    private static Predicate patientID0(CriteriaBuilder cb,
            Path<String> idField, Path<String> issuerField, String id,
            String issuer, List<Object> params) {
        Predicate predicate = wildCard0(cb, idField, id, params);
        return issuer == null ? predicate : cb.and(predicate, wildCard0(cb,
                issuerField, issuer, params));
    }

    public static void patient(CriteriaBuilder cb, Path<Patient> pat,
            String[] pids, Attributes keys, boolean matchUnknown,
            List<Predicate> predicates, List<Object> params) {
        add(predicates, patientID(cb, pat.get(Patient_.patientID), pat
                .get(Patient_.issuerOfPatientID), pids, matchUnknown, params));
        if (keys == null)
            return;

        add(predicates, personName(cb, pat.get(Patient_.patientName), pat
                .get(Patient_.patientIdeographicName), pat
                .get(Patient_.patientPhoneticName), AttributeFilter.getString(
                keys, Tag.PatientName), matchUnknown, params));
        add(predicates, wildCard(cb, pat.get(Patient_.patientSex),
                AttributeFilter.getString(keys, Tag.PatientSex), matchUnknown,
                params));
        RangeMatching.rangeMatch(cb, pat.get(Patient_.patientBirthDate),
                Tag.PatientBirthDate, keys, matchUnknown, predicates, params);
    }

    public static void study(CriteriaBuilder cb, Path<Patient> pat,
            Path<Study> study, String[] pids, Attributes keys,
            boolean matchUnknown, boolean combinedDateTime,
            List<Predicate> predicates, List<Object> params) {
        patient(cb, pat, pids, keys, matchUnknown, predicates, params);
        if (keys == null)
            return;

        add(predicates, listOfUID(cb, study.get(Study_.studyInstanceUID), keys
                .getStrings(Tag.StudyInstanceUID), params));
        add(predicates, wildCard(cb, study.get(Study_.studyID), AttributeFilter
                .getString(keys, Tag.StudyID), matchUnknown, params));
        add(predicates, personName(cb,
                study.get(Study_.referringPhysicianName), study
                        .get(Study_.referringPhysicianIdeographicName), study
                        .get(Study_.referringPhysicianPhoneticName),
                AttributeFilter.getString(keys, Tag.ReferringPhysicianName),
                matchUnknown, params));
        RangeMatching.rangeMatch(cb, study.get(Study_.studyDate), study
                .get(Study_.studyTime), Tag.StudyDate, Tag.StudyTime,
                Tag.StudyDateAndTime, keys, matchUnknown, combinedDateTime,
                predicates, params);
        add(predicates, wildCard(cb, study.get(Study_.studyDescription),
                AttributeFilter.getString(keys, Tag.StudyDescription),
                matchUnknown, params));
        add(predicates, wildCard(cb, study.get(Study_.accessionNumber),
                AttributeFilter.getString(keys, Tag.AccessionNumber),
                matchUnknown, params));
        add(predicates, studySeriesSubQuery(cb, Series_.modality,
                AttributeFilter.getString(keys, Tag.ModalitiesInStudy),
                Study_.modalitiesInStudy, study, matchUnknown, params));
        add(predicates, studyInstanceSubQuery(cb, Instance_.sopClassUID,
                AttributeFilter.getString(keys, Tag.SOPClassesInStudy),
                Study_.sopClassesInStudy, study, matchUnknown, params));
        add(predicates, issuerOfAccessionNumber(cb, study, keys, matchUnknown,
                params));
        add(predicates, procedureCodes(cb, study, keys, matchUnknown, params));
    }

    public static void series(CriteriaBuilder cb, Path<Patient> pat,
            Join<Series, Study> study, Path<Series> series, String[] pids,
            Attributes keys, boolean matchUnknown, boolean combinedDateTime,
            List<Predicate> predicates, List<Object> params) {
        study(cb, pat, study, pids, keys, combinedDateTime, matchUnknown,
                predicates, params);
        if (keys == null)
            return;

        add(predicates, listOfUID(cb, series.get(Series_.seriesInstanceUID),
                keys.getStrings(Tag.SeriesInstanceUID), params));
        add(predicates, wildCard(cb, series.get(Series_.modality),
                AttributeFilter.getString(keys, Tag.Modality), matchUnknown,
                params));
        RangeMatching.rangeMatch(cb, series
                .get(Series_.performedProcedureStepStartDate), series
                .get(Series_.performedProcedureStepStartTime),
                Tag.PerformedProcedureStepStartDate,
                Tag.PerformedProcedureStepStartTime,
                Tag.PerformedProcedureStepStartDateAndTime, keys, matchUnknown,
                combinedDateTime, predicates, params);
        add(predicates, wildCard(cb, series.get(Series_.seriesNumber),
                AttributeFilter.getString(keys, Tag.SeriesNumber),
                matchUnknown, params));
        add(predicates, wildCard(cb, series.get(Series_.seriesDescription),
                AttributeFilter.getString(keys, Tag.SeriesDescription),
                matchUnknown, params));
        add(predicates, requestedAttributesSequence(cb, series, keys,
                matchUnknown, params));
        // TODO
    }

    public static void instance(CriteriaBuilder cb, Path<Patient> pat,
            Join<Series, Study> study, Path<Series> series,
            Root<Instance> inst, String[] pids, Attributes keys,
            boolean matchUnknown, boolean combinedDateTime,
            List<Predicate> predicates, List<Object> params) {
        series(cb, pat, study, series, pids, keys, matchUnknown,
                combinedDateTime, predicates, params);
        if (keys == null)
            return;

        add(predicates, listOfUID(cb, inst.get(Instance_.sopInstanceUID), keys
                .getStrings(Tag.SOPInstanceUID), params));
        add(predicates, wildCard(cb, inst.get(Instance_.instanceNumber),
                AttributeFilter.getString(keys, Tag.InstanceNumber),
                matchUnknown, params));
        add(predicates, wildCard(cb, inst.get(Instance_.verificationFlag),
                AttributeFilter.getString(keys, Tag.VerificationFlag),
                matchUnknown, params));
        // TODO
    }

    static ParameterExpression<String> setParam(CriteriaBuilder cb,
            List<Object> params, String value) {
        ParameterExpression<String> paramExp =
                cb.parameter(String.class, paramName(params.size()));
        params.add(value);
        return paramExp;
    }
}