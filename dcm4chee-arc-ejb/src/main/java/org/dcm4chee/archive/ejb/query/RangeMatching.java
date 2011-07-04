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
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.DateRange;
import org.dcm4che.util.DateUtils;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Michael Backhaus <michael.backhaus@agfa.com>
 */
public class RangeMatching {

    private static Predicate combinedRange(CriteriaBuilder cb,
            Path<String> date, Path<String> time, DateRange range,
            List<Object> params) {
        if (range.getStartDate() == null)
            return combinedRangeOpenStart(cb, date, time, 
                    DateUtils.formatDA(null, range.getEndDate()), 
                    DateUtils.formatTM(null, range.getEndDate()), 
                    params);
        if (range.getEndDate() == null)
            return combinedRangeOpenEnd(cb, date, time, 
                    DateUtils.formatDA(null, range.getStartDate()), 
                    DateUtils.formatTM(null, range.getStartDate()), 
                    params);
        else
            return combinedRangeInterval(cb, date, time, 
                    range.getStartDate(), range.getEndDate(), params);
    }

    private static Predicate combinedRangeInterval(CriteriaBuilder cb,
            Path<String> date, Path<String> time, Date startDateRange,
            Date endDateRange, List<Object> params) {
        String startTime = DateUtils.formatTM(null, startDateRange);
        String endTime = DateUtils.formatTM(null, endDateRange);
        String startDate = DateUtils.formatDA(null, startDateRange);
        String endDate = DateUtils.formatDA(null, endDateRange);
        if (endDate.equals(startDate))
            return cb.and(cb.equal(date, startDate), 
                    cb.and(
                            rangeOpenEnd(cb,time, startTime, params), 
                            rangeOpenStart(cb, time, endTime, params)));
        else
            return cb.and(
                    combinedRangeOpenEnd(cb, date, time, startDate, startTime, params), 
                    combinedRangeOpenStart(cb, date, time, endDate, endTime, params));
    }

    private static Predicate combinedRangeOpenEnd(CriteriaBuilder cb,
            Path<String> date, Path<String> time, String startDate,
            String startTime, List<Object> params) {
        ParameterExpression<String> paramStartDate =
                Matching.setParam(cb, params, startDate);
        ParameterExpression<String> paramStartTime =
                Matching.setParam(cb, params, startTime);
        Predicate startDayTime =
                cb.and(
                        cb.equal(date, paramStartDate), 
                        cb.greaterThanOrEqualTo(time, paramStartTime));
        Predicate startDayTimeUnknown =
                cb.and(
                        cb.equal(date, paramStartDate), 
                        cb.equal(time, "*"));
        Predicate startDayFollowing = cb.greaterThan(date, paramStartDate);
        return cb.or(startDayTime, startDayTimeUnknown, startDayFollowing);
    }

    private static Predicate combinedRangeOpenStart(CriteriaBuilder cb,
            Path<String> date, Path<String> time, String endDate,
            String endTime, List<Object> params) {
        ParameterExpression<String> paramEndDate =
                Matching.setParam(cb, params, endDate);
        ParameterExpression<String> paramEndTime =
                Matching.setParam(cb, params, endTime);
        Predicate endDayTime =
                cb.and(
                        cb.equal(date, paramEndDate), 
                        cb.lessThanOrEqualTo(time, paramEndTime));
        Predicate endDayTimeUnknown =
                cb.and(cb.equal(date, paramEndDate), cb.equal(time, "*"));
        Predicate endDayPrevious = cb.lessThan(date, paramEndDate);
        return cb.or(endDayTime, endDayTimeUnknown, endDayPrevious);
    }

    private static Predicate range(CriteriaBuilder cb, Path<String> field,
            DateRange range, FormatDate dt, List<Object> params) {
        if (range.getStartDate() == null)
            return rangeOpenStart(cb, field, dt.format(range.getEndDate()), params);
        if (range.getEndDate() == null)
            return rangeOpenEnd(cb, field, dt.format(range.getStartDate()), params);
        else
            return rangeInterval(cb, field, range.getStartDate(), 
                    range.getEndDate(), dt, params);
    }

    private static Predicate rangeOpenEnd(CriteriaBuilder cb,
            Path<String> field, String start, List<Object> params) {
        ParameterExpression<String> paramStart =
                Matching.setParam(cb, params, start);
        return cb.greaterThanOrEqualTo(field, paramStart);
    }

    private static Predicate rangeOpenStart(CriteriaBuilder cb,
            Path<String> field, String end, List<Object> params) {
        ParameterExpression<String> paramEnd =
                Matching.setParam(cb, params, end);
        return cb.lessThanOrEqualTo(field, paramEnd);
    }

    private static Predicate rangeInterval(CriteriaBuilder cb,
            Path<String> field, Date startDate, Date endDate, FormatDate dt,
            List<Object> params) {
        Predicate predicate;
        String start = dt.format(startDate);
        String end = dt.format(endDate);
        ParameterExpression<String> paramStart =
                Matching.setParam(cb, params, start);
        if (end.equals(start)) {
            predicate = cb.equal(field, paramStart);
        } else {
            ParameterExpression<String> paramEnd =
                    Matching.setParam(cb, params, end);
            predicate = cb.between(field, paramStart, paramEnd);
        }
        return predicate;
    }

    static Predicate matchUnknown(CriteriaBuilder cb, Path<String> field,
            boolean matchUnknown, Predicate predicate) {
        return matchUnknown ? 
                cb.or(predicate, cb.equal(field, "*")) 
                : cb.and(predicate, cb.notEqual(field, "*"));
    }

    static enum FormatDate {
        DA {
            @Override
            String format(Date date) {
                return DateUtils.formatDA(null, date);
            }
        },
        TM {
            @Override
            String format(Date date) {
                return DateUtils.formatTM(null, date);
            }
        },
        DT {
            @Override
            String format(Date date) {
                return DateUtils.formatDT(null, date);
            }
        };
        abstract String format(Date date);
    }

    public static void rangeMatch(CriteriaBuilder cb, Path<String> dateField,
            Path<String> timeField, int dateTag, int timeTag,
            long dateAndTimeTag, Attributes keys, boolean matchUnknown,
            boolean combinedDateTime, List<Predicate> predicates,
            List<Object> params) {
        if (combinedDateTime 
                && keys.containsValue(dateTag)
                && keys.containsValue(timeTag))
            Matching.add(predicates, matchUnknown(cb, dateField, matchUnknown,
                    combinedRange(cb, dateField, timeField, keys.getDateRange(
                            dateAndTimeTag, null), params)));
        else {
            if (keys.containsValue(dateTag))
                Matching.add(predicates, matchUnknown(cb, dateField,
                        matchUnknown, range(cb, dateField, keys.getDateRange(
                                dateTag, null), FormatDate.DA, params)));
            if (keys.containsValue(timeTag))
                Matching.add(predicates, matchUnknown(cb, timeField,
                        matchUnknown, range(cb, timeField, keys.getDateRange(
                                timeTag, null), FormatDate.TM, params)));
        }
    }

    public static Predicate rangeMatch(CriteriaBuilder cb, Path<String> path,
            int tag, FormatDate format, Attributes keys, boolean matchUnknown, 
            List<Object> params) {
        if (!keys.containsValue(tag))
            return null;
        
        else
            return matchUnknown(cb, path, matchUnknown,
                    range(cb, path, keys.getDateRange(tag, null),
                            format, params));
    }
}
