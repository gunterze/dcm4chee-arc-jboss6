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

import java.util.Date;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.DateRange;
import org.dcm4che.data.PersonName;
import org.dcm4che.data.Tag;
import org.dcm4che.util.DateUtils;
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

    private static Predicate rangeDT(CriteriaBuilder cb, Path<String> date,
            Path<String> time, DateRange range, boolean matchUnknown,
            List<Object> params) {
        Date startDateRange = range.getStartDate();
        Date endDateRange = range.getEndDate();
        if (startDateRange == null)
            return endDTRange(cb, date, time, endDateRange, matchUnknown, params);
        if (endDateRange == null)
            return startDTRange(cb, date, time, startDateRange, matchUnknown, params);
        else
            return startEndDTRange(cb, date, time, 
                    startDateRange, endDateRange, 
                    matchUnknown, params);
    }
    private static Predicate startEndDTRange(CriteriaBuilder cb,
                Path<String> date, Path<String> time, 
                Date startDateRange, Date endDateRange,
                boolean matchUnknown, List<Object> params) {
        String startTime = DateUtils.formatTM(null, startDateRange);
        String endTime = DateUtils.formatTM(null, endDateRange);
        if ( endTime == null )
            return cb.and(
                    endDate(cb, date, startDateRange, matchUnknown, FormatDate.DA, params),
                    startDTRange(cb, date, time, endDateRange, matchUnknown, params));
        if ( startTime == null )
            return cb.and(
                    startDate(cb, date, endDateRange, matchUnknown, FormatDate.DA, params),
                    endDTRange(cb, date, time, startDateRange, matchUnknown, params));
        else
            return cb.and(
                    startDTRange(cb, date, time, startDateRange, matchUnknown, params),
                    endDTRange(cb, date, time, endDateRange, matchUnknown, params));
        }

    private static Predicate startDTRange(CriteriaBuilder cb,
                Path<String> date, Path<String> time, Date startDateRange,
                boolean matchUnknown, List<Object> params) {
        String startDate = DateUtils.formatDA(null, startDateRange);
        String startTime = DateUtils.formatTM(null, startDateRange);
        ParameterExpression<String> paramStartDate = setParam(cb, params, startDate);
        ParameterExpression<String> paramStartTime = setParam(cb, params, startTime);
        Predicate startDayTime = cb.and(cb.equal(date, paramStartDate), 
                cb.greaterThanOrEqualTo(time, paramStartTime));
        Predicate startDayTimeUnknown = cb.and(cb.equal(date, paramStartDate),
                cb.equal(time, "*"));
        Predicate startDayFollowing = cb.greaterThan(date, paramStartDate);
        Predicate unknown = cb.or(cb.equal(date, "*"),
                cb.and(cb.greaterThanOrEqualTo(date, paramStartDate),
                        cb.equal(time, "*")));
        if(matchUnknown)
            return cb.or(startDayTime, cb.or(startDayFollowing, unknown));
        else
            return cb.or(cb.or(startDayTime,startDayTimeUnknown), startDayFollowing);
    }

    private static Predicate endDTRange(CriteriaBuilder cb, Path<String> date,
                Path<String> time, Date endDateRange, boolean matchUnknown,
                List<Object> params) {
        String endDate = DateUtils.formatDA(null, endDateRange);
        String endTime = DateUtils.formatTM(null, endDateRange);
        ParameterExpression<String> paramEndDate = setParam(cb, params, endDate);
        ParameterExpression<String> paramEndTime = setParam(cb, params, endTime);
        Predicate endDayTime = cb.and(cb.equal(date, paramEndDate), 
                cb.lessThanOrEqualTo(time, paramEndTime));
        Predicate endDayTimeUnknown = cb.and(cb.equal(date, paramEndDate),
                cb.equal(time, "*"));
        Predicate endDayPrevious = cb.lessThan(date, paramEndDate);
        Predicate unknown = cb.or(cb.equal(date, "*"),
                cb.and(cb.lessThanOrEqualTo(date, paramEndDate),
                        cb.equal(time, "*")));
        if(matchUnknown)
            return cb.or(endDayTime, cb.or(endDayPrevious, unknown));
        else
            return cb.or(cb.or(endDayTime,endDayTimeUnknown), endDayPrevious);
        }

    private static Predicate range(CriteriaBuilder cb, Path<String> field,
            DateRange range, boolean matchUnknown, FormatDate dt,
            List<Object> params) {
        Date startDate = range.getStartDate();
        Date endDate = range.getEndDate();
        if (startDate == null)
            return endDate(cb, field, endDate, matchUnknown, dt, params);
        if (endDate == null)
            return startDate(cb, field, startDate, matchUnknown, dt, params);
        else
            return dateRange(cb, field, startDate, endDate, matchUnknown, dt, params);
    }
    
    private static Predicate startDate(CriteriaBuilder cb, Path<String> field,
            Date startDate, boolean matchUnknown, FormatDate dt,
            List<Object> params) {
        String start = dt.format(startDate);
        ParameterExpression<String> paramStart = setParam(cb, params, start);
        Predicate predicate = cb.greaterThanOrEqualTo(field, paramStart);
        return matchUnknown0(cb, field, matchUnknown, predicate);
    }
    
    private static Predicate endDate(CriteriaBuilder cb, Path<String> field,
            Date endDate, boolean matchUnknown, FormatDate dt,
            List<Object> params) {
        String end = dt.format(endDate);
        ParameterExpression<String> paramEnd = setParam(cb, params, end);
        Predicate predicate = cb.lessThanOrEqualTo(field, paramEnd);
        return matchUnknown0(cb, field, matchUnknown, predicate);
    }
    
    private static Predicate dateRange(CriteriaBuilder cb, Path<String> field,
            Date startDate, Date endDate, boolean matchUnknown, FormatDate dt,
            List<Object> params) {
        Predicate predicate;
        String start = dt.format(startDate);
        String end = dt.format(endDate);
        ParameterExpression<String> paramStart = setParam(cb, params, start);
        if (end.equals(start)){
            predicate = cb.equal(field, paramStart);
        } else {
            ParameterExpression<String> paramEnd = setParam(cb, params, end);
            predicate = cb.between(field, paramStart, paramEnd);
        }
        return matchUnknown0(cb, field, matchUnknown, predicate);
    }
    
    private static ParameterExpression<String> setParam(CriteriaBuilder cb,
            List<Object> params, String value) {
        ParameterExpression<String> paramExp =
            cb.parameter(String.class, paramName(params.size()));
        params.add(value);
        return paramExp;
    }
    
    private static Predicate matchUnknown0(CriteriaBuilder cb,
            Path<String> field, boolean matchUnknown, Predicate predicate) {
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
        if (combinedDateTime && keys.containsValue(Tag.StudyDate)
                && keys.containsValue(Tag.StudyTime))
            add(predicates, rangeDT(cb, study.get(Study_.studyDate), study
                    .get(Study_.studyTime), keys.getDateRange(
                    Tag.StudyDateAndTime, null), matchUnknown, params));
        else {
            if (keys.containsValue(Tag.StudyDate))
                add(predicates, range(cb, study.get(Study_.studyDate), keys
                        .getDateRange(Tag.StudyDate, null), matchUnknown,
                        FormatDate.DA, params));
            if (keys.containsValue(Tag.StudyTime))
                add(predicates, range(cb, study.get(Study_.studyTime), keys
                        .getDateRange(Tag.StudyTime, null), matchUnknown,
                        FormatDate.TM, params));
        }
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
        if (keys.containsValue(Tag.PerformedProcedureStepStartTime))
            add(predicates, range(cb, series
                    .get(Series_.performedProcedureStepStartDate), keys
                    .getDateRange(Tag.PerformedProcedureStepStartTime, null),
                    matchUnknown, FormatDate.TM, params));
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
}
