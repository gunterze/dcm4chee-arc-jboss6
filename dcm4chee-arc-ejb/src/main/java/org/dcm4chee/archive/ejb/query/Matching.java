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
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.PersonName;
import org.dcm4che.data.Tag;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.dcm4chee.archive.persistence.Instance;
import org.dcm4chee.archive.persistence.Instance_;
import org.dcm4chee.archive.persistence.Patient;
import org.dcm4chee.archive.persistence.Patient_;
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
        // TODO: range matching birthdate
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
        // add(predicates, wildCard(cb, study.get(Study_.modalitiesInStudy),
        // AttributeFilter.getString(keys, Tag.ModalitiesInStudy),
        // matchUnknown, params));
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
        // if (keys.containsValue(Tag.PerformedProcedureStepStartTime))
        // add(predicates, RangeMatching.range(cb, series
        // .get(Series_.performedProcedureStepStartDate), keys
        // .getDateRange(Tag.PerformedProcedureStepStartTime, null),
        // matchUnknown, FormatDate.TM, params));
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
