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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.PersonName;
import org.dcm4che.data.Sequence;
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
import org.dcm4chee.archive.persistence.VerifyingObserver;
import org.dcm4chee.archive.persistence.VerifyingObserver_;

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

    public static boolean add(List<Predicate> predicates, Predicate e) {
        if (e == null)
            return false;

        predicates.add(e);
        return true;
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
            predicate = cb.or(
                    wildCard0(cb, alphabethic, queryString, params),
                    wildCard0(cb, ideographic, queryString, params),
                    wildCard0(cb, phonetic, queryString, params));
        } else {
            predicate = and(cb,
                    wildCard0(cb, alphabethic, queryString, params),
                    wildCard0(cb, ideographic,
                            pn.getNormalizedQueryString(PersonName.Group.Ideographic),
                            params),
                    wildCard0(cb, phonetic,
                            pn.getNormalizedQueryString(PersonName.Group.Phonetic),
                            params));
            if (predicate == null)
                return null;
        }
        return matchUnknown 
                ? cb.or(predicate, cb.and(
                        cb.equal(alphabethic, "*"),
                        cb.equal(ideographic, "*"),
                        cb.equal(phonetic, "*")))
                : predicate;
    }

    private static Predicate and(CriteriaBuilder cb, Predicate p1,
            Predicate p2, Predicate p3) {
        return p1 != null 
                ? p2 != null 
                    ? p3 != null
                        ? cb.and(p1, p2, p3)
                        : cb.and(p1, p2)
                    : p3 != null 
                        ? cb.and(p1, p3)
                        : p1
                : p2 != null
                    ? p3 != null 
                        ? cb.and(p2, p3) 
                        : p2
                    : null;
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

    private static Predicate modalitiesInStudy(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq, Path<Study> study, String modality,
            boolean matchUnknown, List<Object> params) {
        if (modality.equals("*"))
            return null;

        Subquery<Series> sq = cq.subquery(Series.class);
        Root<Series> series = sq.from(Series.class);
        sq.select(series);
        sq.where(cb.equal(study, series.get(Series_.study)),
                wildCard(cb, series.get(Series_.modality), modality,
                matchUnknown, params));
        return cb.exists(sq);
    }

    private static Predicate withCode(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq, Expression<Collection<Code>> collection,
            Attributes item, boolean matchUnknown, List<Object> params) {
        if (item == null || item.isEmpty())
            return null;

        Subquery<Code> sq = cq.subquery(Code.class);
        Root<Code> root = sq.from(Code.class);
        sq.select(root);
        ArrayList<Predicate> predicates = new ArrayList<Predicate>(4);
        predicates.add(cb.isMember(root, collection));
        if (!addCodePredicates(cb, item, params, root, predicates))
            return null;

        sq.where(predicates.toArray(new Predicate[predicates.size()]));
        return matchUnknown ? cb.or(cb.exists(sq), cb.isEmpty(collection)) : cb.exists(sq);
    }

    static Predicate withCode(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq, Path<Code> path, Attributes item,
            boolean matchUnknown, List<Object> params) {
        if (item == null || item.isEmpty())
            return null;

        Subquery<Code> sq = cq.subquery(Code.class);
        Root<Code> root = sq.from(Code.class);
        sq.select(root);
        ArrayList<Predicate> predicates = new ArrayList<Predicate>(4);
        predicates.add(cb.equal(root, path));
        if (!addCodePredicates(cb, item, params, root, predicates))
            return null;

        sq.where(predicates.toArray(new Predicate[predicates.size()]));
        return matchUnknown 
                ? cb.or(cb.exists(sq), cb.isNull(path))
                : cb.exists(sq);
    }

    private static boolean addCodePredicates(CriteriaBuilder cb, Attributes item,
            List<Object> params, Root<Code> root,
            ArrayList<Predicate> predicates) {
        boolean restrict = add(predicates,
                wildCard(cb, root.get(Code_.codeValue),
                    AttributeFilter.getString(item, Tag.CodeValue),
                    false, params));
        restrict = add(predicates,
                wildCard(cb, root.get(Code_.codingSchemeDesignator),
                    AttributeFilter.getString(item, Tag.CodingSchemeDesignator),
                    false, params))
                || restrict;
        restrict = add(predicates,
                wildCard(cb, root.get(Code_.codingSchemeVersion),
                    AttributeFilter.getString(item, Tag.CodingSchemeVersion),
                    false, params))
                || restrict;
        return restrict;
    }

    private static Predicate withIssuer(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq, Path<Issuer> issuer,
            Attributes item, boolean matchUnknown, List<Object> params) {
        if (item == null || item.isEmpty())
            return null;

        Subquery<Issuer> sq = cq.subquery(Issuer.class);
        Root<Issuer> root = sq.from(Issuer.class);
        sq.select(root);
        ArrayList<Predicate> predicates = new ArrayList<Predicate>(4);
        predicates.add(cb.equal(root, issuer));
        boolean restrict = add(predicates,
                wildCard(cb, root.get(Issuer_.entityID),
                    AttributeFilter.getString(item, Tag.LocalNamespaceEntityID),
                    false, params));
        restrict = add(predicates,
                wildCard(cb, root.get(Issuer_.entityUID),
                    AttributeFilter.getString(item, Tag.UniversalEntityID),
                    false, params))
                || restrict;
        restrict = add(predicates,
                wildCard(cb, root.get(Issuer_.entityUIDType),
                    AttributeFilter.getString(item, Tag.UniversalEntityIDType),
                    false, params))
                || restrict;
        if (!restrict)
            return null;

        sq.where(predicates.toArray(new Predicate[predicates.size()]));
        return matchUnknown 
                ? cb.or(cb.exists(sq), cb.isNull(issuer))
                : cb.exists(sq);
    }
    
    private static Predicate withObserver(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq, Expression<Collection<VerifyingObserver>> collection,
            Attributes item, boolean matchUnknown, List<Object> params) {
        if (item == null || item.isEmpty())
            return null;

        Subquery<VerifyingObserver> sq = cq.subquery(VerifyingObserver.class);
        Root<VerifyingObserver> root = sq.from(VerifyingObserver.class);
        sq.select(root);
        ArrayList<Predicate> predicates = new ArrayList<Predicate>(2);
        predicates.add(cb.isMember(root, collection));
        boolean restrict = add(predicates, 
                RangeMatching.rangeMatch(cb, 
                    root.get(VerifyingObserver_.verificationDateTime), 
                    Tag.VerificationDateTime, 
                    RangeMatching.FormatDate.DT, item, false, params));
        restrict = add(predicates, 
                personName(cb, 
                    root.get(VerifyingObserver_.verifyingObserverName), 
                    root.get(VerifyingObserver_.verifyingObserverIdeographicName), 
                    root.get(VerifyingObserver_.verifyingObserverPhoneticName), 
                    AttributeFilter.getString(item, Tag.VerifyingObserverName), 
                    false, params))
                || restrict;
        if (!restrict)
            return null;

        sq.where(predicates.toArray(new Predicate[predicates.size()]));
        return matchUnknown 
                ? cb.or(cb.exists(sq), cb.isEmpty(collection))
                : cb.exists(sq);
    }

    private static Predicate requestAttributesSequence(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq,
            Expression<Collection<RequestAttributes>> collection,
            Attributes item, boolean matchUnknown, List<Object> params) {
        if (item == null || item.isEmpty())
            return null;

        Subquery<RequestAttributes> sq = cq.subquery(RequestAttributes.class);
        Root<RequestAttributes> root = sq.from(RequestAttributes.class);
        sq.select(root);
        ArrayList<Predicate> predicates = new ArrayList<Predicate>(4);
        predicates.add(cb.isMember(root, collection));
        boolean restrict = add(predicates, 
                wildCard(cb, root.get(RequestAttributes_.requestedProcedureID),
                    AttributeFilter.getString(item, Tag.RequestedProcedureID),
                    matchUnknown, params));
        restrict = add(predicates,
                wildCard(cb, root.get(RequestAttributes_.scheduledProcedureStepID),
                    AttributeFilter.getString(item, Tag.ScheduledProcedureStepID),
                    matchUnknown, params))
                || restrict;
        restrict = add(predicates,
                wildCard(cb, root.get(RequestAttributes_.requestingService),
                    AttributeFilter.getString(item, Tag.RequestingService),
                    matchUnknown, params))
                || restrict;
        restrict = add(predicates,
                personName(cb,
                    root.get(RequestAttributes_.requestingPhysician),
                    root.get(RequestAttributes_.requestingPhysicianIdeographicName),
                    root.get(RequestAttributes_.requestingPhysicianPhoneticName),
                    AttributeFilter.getString(item, Tag.ReferringPhysicianName),
                    matchUnknown, params))
                || restrict;
        restrict = add(predicates,
                listOfUID(cb, root.get(RequestAttributes_.studyInstanceUID),
                        item.getStrings(Tag.StudyInstanceUID), params))
                || restrict;
        if (add(predicates, wildCard(cb, root.get(RequestAttributes_.accessionNumber),
                AttributeFilter.getString(item, Tag.AccessionNumber),
                matchUnknown, params))) {
            add(predicates, withIssuer(cb, cq,
                    root.get(RequestAttributes_.issuerOfAccessionNumber),
                    item.getNestedDataset(Tag.IssuerOfAccessionNumberSequence),
                    matchUnknown, params));
            restrict = true;
        }

        if (!restrict)
            return null;

        sq.where(predicates.toArray(new Predicate[predicates.size()]));
        return matchUnknown ? cb.or(cb.exists(sq), cb.isEmpty(collection)) : cb.exists(sq);
    }

    private static Predicate matchUnknown0(CriteriaBuilder cb, Path<String> field,
            boolean matchUnknown, Predicate predicate) {
        return matchUnknown ? cb.or(predicate, cb.equal(field, "*")) : predicate;
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

        add(predicates, personName(cb, 
                pat.get(Patient_.patientName),
                pat.get(Patient_.patientIdeographicName),
                pat.get(Patient_.patientPhoneticName),
                AttributeFilter.getString(keys, Tag.PatientName),
                matchUnknown, params));
        add(predicates, wildCard(cb, pat.get(Patient_.patientSex),
                AttributeFilter.getString(keys, Tag.PatientSex), matchUnknown,
                params));
        add(predicates, RangeMatching.rangeMatch(cb, pat.get(Patient_.patientBirthDate),
                Tag.PatientBirthDate, RangeMatching.FormatDate.DA, keys, matchUnknown, params));
    }

    public static void study(CriteriaBuilder cb, CriteriaQuery<Tuple> cq,
            Path<Patient> pat, Path<Study> study, String[] pids,
            Attributes keys, boolean matchUnknown, boolean combinedDateTime,
            List<Predicate> predicates, List<Object> params) {
        patient(cb, pat, pids, keys, matchUnknown, predicates, params);
        if (keys == null)
            return;

        add(predicates, listOfUID(cb, study.get(Study_.studyInstanceUID), keys
                .getStrings(Tag.StudyInstanceUID), params));
        add(predicates, wildCard(cb, study.get(Study_.studyID), AttributeFilter
                .getString(keys, Tag.StudyID), matchUnknown, params));
        add(predicates, personName(cb,
                study.get(Study_.referringPhysicianName),
                study.get(Study_.referringPhysicianIdeographicName),
                study.get(Study_.referringPhysicianPhoneticName),
                AttributeFilter.getString(keys, Tag.ReferringPhysicianName),
                matchUnknown, params));
        RangeMatching.rangeMatch(cb, study.get(Study_.studyDate), study
                .get(Study_.studyTime), Tag.StudyDate, Tag.StudyTime,
                Tag.StudyDateAndTime, keys, matchUnknown, combinedDateTime,
                predicates, params);
        add(predicates, wildCard(cb, study.get(Study_.studyDescription),
                AttributeFilter.getString(keys, Tag.StudyDescription),
                matchUnknown, params));
        if (add(predicates, wildCard(cb, study.get(Study_.accessionNumber),
                AttributeFilter.getString(keys, Tag.AccessionNumber),
                matchUnknown, params))) {
            add(predicates, withIssuer(cb, cq,
                    study.get(Study_.issuerOfAccessionNumber),
                    keys.getNestedDataset(Tag.IssuerOfAccessionNumberSequence),
                    matchUnknown, params));
        }
        add(predicates, modalitiesInStudy(cb, cq, study,
                AttributeFilter.getString(keys, Tag.ModalitiesInStudy),
                matchUnknown, params));
        add(predicates, withCode(cb, cq, study.get(Study_.procedureCodes),
                keys.getNestedDataset(Tag.ProcedureCodeSequence),
                matchUnknown, params));
    }

    public static void series(CriteriaBuilder cb, CriteriaQuery<Tuple> cq,
            Path<Patient> pat, Join<Series, Study> study, Path<Series> series,
            String[] pids, Attributes keys, boolean matchUnknown,
            boolean combinedDateTime, List<Predicate> predicates,
            List<Object> params) {
        study(cb, cq, pat, study, pids, keys, combinedDateTime, matchUnknown,
                predicates, params);
        if (keys == null)
            return;

        add(predicates, listOfUID(cb, series.get(Series_.seriesInstanceUID),
                keys.getStrings(Tag.SeriesInstanceUID), params));
        add(predicates, wildCard(cb, series.get(Series_.modality),
                AttributeFilter.getString(keys, Tag.Modality), matchUnknown,
                params));
        add(predicates, wildCard(cb, series
                .get(Series_.performedProcedureStepInstanceUID),
                AttributeFilter.getString(keys, Tag.PerformedProcedureStepID),
                matchUnknown, params));
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
        add(predicates, requestAttributesSequence(cb, cq,
                series.get(Series_.requestAttributes),
                keys.getNestedDataset(Tag.RequestAttributesSequence),
                matchUnknown, params));
        add(predicates, withCode(cb, cq, 
                series.get(Series_.institutionCode),
                keys.getNestedDataset(Tag.InstitutionCodeSequence), 
                matchUnknown, params));
    }

    public static void instance(CriteriaBuilder cb, CriteriaQuery<Tuple> cq,
            Path<Patient> pat, Join<Series, Study> study, Path<Series> series,
            Root<Instance> inst, String[] pids, Attributes keys,
            boolean matchUnknown, boolean combinedDateTime,
            List<Predicate> predicates, List<Object> params) {
        series(cb, cq, pat, study, series, pids, keys, matchUnknown,
                combinedDateTime, predicates, params);
        if (keys == null)
            return;

        add(predicates, listOfUID(cb, inst.get(Instance_.sopInstanceUID), 
                keys.getStrings(Tag.SOPInstanceUID), params));
        add(predicates, wildCard(cb, inst.get(Instance_.instanceNumber),
                AttributeFilter.getString(keys, Tag.InstanceNumber),
                matchUnknown, params));
        add(predicates, wildCard(cb, inst.get(Instance_.verificationFlag),
                AttributeFilter.getString(keys, Tag.VerificationFlag),
                matchUnknown, params));
        add(predicates, wildCard(cb, inst.get(Instance_.sopClassUID),
                AttributeFilter.getString(keys, Tag.SOPClassUID), matchUnknown,
                params));
        add(predicates, withCode(cb, cq, inst.get(Instance_.conceptNameCode),
                keys.getNestedDataset(Tag.ConceptNameCodeSequence),
                matchUnknown, params));
        add(predicates, withObserver(cb, cq, 
                inst.get(Instance_.verifyingObservers),
                keys.getNestedDataset(Tag.VerifyingObserverSequence), 
                matchUnknown, params));
        Sequence contentSeq = keys.getSequence(Tag.ContentSequence);
        if (contentSeq != null)
            for (Attributes item : contentSeq) {
                add(predicates, ContentItemMatching.withContentItem(cb, cq, 
                        inst.get(Instance_.contentItems), item, 
                        item.getString(Tag.ValueType, null), params));
            }
    }

    static ParameterExpression<String> setParam(CriteriaBuilder cb,
            List<Object> params, String value) {
        ParameterExpression<String> paramExp =
                cb.parameter(String.class, paramName(params.size()));
        params.add(value);
        return paramExp;
    }
}