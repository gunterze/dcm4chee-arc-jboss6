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
import org.dcm4che.data.Tag;
import org.dcm4che.util.DateUtils;
import org.dcm4chee.archive.persistence.Study;
import org.dcm4chee.archive.persistence.Study_;
import org.w3c.dom.ranges.Range;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Michael Backhaus <michael.backhaus@agfa.com>
 */
public class RangeMatching extends Matching {

    private static Predicate rangeDT(CriteriaBuilder cb, Path<String> date,
            Path<String> time, DateRange range, boolean matchUnknown,
            List<Object> params) {
        Date startDateRange = range.getStartDate();
        Date endDateRange = range.getEndDate();
        if (startDateRange == null)
            return RangeMatching.endDTRange(cb, date, time, endDateRange,
                    matchUnknown, params);
        if (endDateRange == null)
            return RangeMatching.startDTRange(cb, date, time, startDateRange,
                    matchUnknown, params);
        else
            return RangeMatching.startEndDTRange(cb, date, time,
                    startDateRange, endDateRange, matchUnknown, params);
    }

    private static Predicate startEndDTRange(CriteriaBuilder cb,
            Path<String> date, Path<String> time, Date startDateRange,
            Date endDateRange, boolean matchUnknown, List<Object> params) {
        String startTime = DateUtils.formatTM(null, startDateRange);
        String endTime = DateUtils.formatTM(null, endDateRange);
        String startDate = DateUtils.formatDA(null, startDateRange);
        String endDate = DateUtils.formatDA(null, endDateRange);
        if (endDate.equals(startDate))
            return cb.and(
                    cb.and(RangeMatching.startDateInterval0(cb, time,
                            startTime, matchUnknown, params), RangeMatching
                            .endDateInterval0(cb, time, endTime, matchUnknown,
                                    params)), matchUnknown0(cb, date,
                            matchUnknown, cb.equal(date, startDate)));
        else
            return cb.and(RangeMatching.startDTRange(cb, date, time,
                    startDateRange, matchUnknown, params), RangeMatching
                    .endDTRange(cb, date, time, endDateRange, matchUnknown,
                            params));
    }

    private static Predicate startDTRange(CriteriaBuilder cb,
            Path<String> date, Path<String> time, Date startDateRange,
            boolean matchUnknown, List<Object> params) {
        String startDate = DateUtils.formatDA(null, startDateRange);
        String startTime = DateUtils.formatTM(null, startDateRange);
        ParameterExpression<String> paramStartDate =
                setParam(cb, params, startDate);
        ParameterExpression<String> paramStartTime =
                setParam(cb, params, startTime);
        Predicate startDayTime =
                cb.and(cb.equal(date, paramStartDate), cb.greaterThanOrEqualTo(
                        time, paramStartTime));
        Predicate startDayTimeUnknown =
                cb.and(cb.equal(date, paramStartDate), cb.equal(time, "*"));
        Predicate startDayFollowing = cb.greaterThan(date, paramStartDate);
        if (matchUnknown) {
            Predicate unknown =
                    cb.or(cb.equal(date, "*"), cb.and(cb.greaterThanOrEqualTo(
                            date, paramStartDate), cb.equal(time, "*")));
            return cb.or(cb.or(startDayTime, startDayTimeUnknown), cb.or(
                    startDayFollowing, unknown));
        } else
            return cb.or(cb.or(startDayTime, startDayTimeUnknown),
                    startDayFollowing);
    }

    private static Predicate endDTRange(CriteriaBuilder cb, Path<String> date,
            Path<String> time, Date endDateRange, boolean matchUnknown,
            List<Object> params) {
        String endDate = DateUtils.formatDA(null, endDateRange);
        String endTime = DateUtils.formatTM(null, endDateRange);
        ParameterExpression<String> paramEndDate =
                setParam(cb, params, endDate);
        ParameterExpression<String> paramEndTime =
                setParam(cb, params, endTime);
        Predicate endDayTime =
                cb.and(cb.equal(date, paramEndDate), cb.lessThanOrEqualTo(time,
                        paramEndTime));
        Predicate endDayTimeUnknown =
                cb.and(cb.equal(date, paramEndDate), cb.equal(time, "*"));
        Predicate endDayPrevious = cb.lessThan(date, paramEndDate);
        if (matchUnknown) {
            Predicate unknown =
                    cb.or(cb.equal(date, "*"), cb.and(cb.lessThanOrEqualTo(
                            date, paramEndDate), cb.equal(time, "*")));
            return cb.or(cb.or(endDayTime, endDayTimeUnknown), cb.or(
                    endDayPrevious, unknown));
        } else
            return cb.or(cb.or(endDayTime, endDayTimeUnknown), endDayPrevious);
    }

    private static Predicate range(CriteriaBuilder cb, Path<String> field,
            DateRange range, boolean matchUnknown, FormatDate dt,
            List<Object> params) {
        Date startDate = range.getStartDate();
        Date endDate = range.getEndDate();
        if (startDate == null)
            return endDateInterval(cb, field, endDate, matchUnknown, dt, params);
        if (endDate == null)
            return startDateInterval(cb, field, startDate, matchUnknown, dt,
                    params);
        else
            return dateRange(cb, field, startDate, endDate, matchUnknown, dt,
                    params);
    }

    private static Predicate startDateInterval(CriteriaBuilder cb,
            Path<String> field, Date startDate, boolean matchUnknown,
            FormatDate dt, List<Object> params) {
        String start = dt.format(startDate);
        return startDateInterval0(cb, field, start, matchUnknown, params);
    }

    private static Predicate startDateInterval0(CriteriaBuilder cb,
            Path<String> field, String start, boolean matchUnknown,
            List<Object> params) {
        ParameterExpression<String> paramStart = setParam(cb, params, start);
        Predicate predicate = cb.greaterThanOrEqualTo(field, paramStart);
        return matchUnknown0(cb, field, matchUnknown, predicate);
    }

    private static Predicate endDateInterval(CriteriaBuilder cb,
            Path<String> field, Date endDate, boolean matchUnknown,
            FormatDate dt, List<Object> params) {
        String end = dt.format(endDate);
        return endDateInterval0(cb, field, end, matchUnknown, params);
    }

    private static Predicate endDateInterval0(CriteriaBuilder cb,
            Path<String> field, String end, boolean matchUnknown,
            List<Object> params) {
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
        if (end.equals(start)) {
            predicate = cb.equal(field, paramStart);
        } else {
            ParameterExpression<String> paramEnd = setParam(cb, params, end);
            predicate = cb.between(field, paramStart, paramEnd);
        }
        return matchUnknown0(cb, field, matchUnknown, predicate);
    }

    private enum FormatDate {
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
        };
        abstract String format(Date date);
    }

    public static void rangeMatch(CriteriaBuilder cb, Path<String> dateField,
            Path<String> timeField, boolean date, boolean time,
            DateRange dateRange, DateRange timeRange, DateRange dateTimeRange,
            boolean matchUnknown, boolean combinedDateTime,
            List<Predicate> predicates, List<Object> params) {
        if (combinedDateTime && date && time)
            add(predicates, rangeDT(cb, dateField, timeField, dateTimeRange,
                    matchUnknown, params));
        else {
            if (date)
                add(predicates, range(cb, dateField, dateRange, matchUnknown,
                        FormatDate.DA, params));
            if (time)
                add(predicates, range(cb, timeField, timeRange, matchUnknown,
                        FormatDate.TM, params));
        }
    }

}
