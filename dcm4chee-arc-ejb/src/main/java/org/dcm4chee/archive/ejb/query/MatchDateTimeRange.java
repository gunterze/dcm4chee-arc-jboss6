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

import org.dcm4che.data.Attributes;
import org.dcm4che.data.DateRange;
import org.dcm4che.util.DateUtils;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.ExpressionUtils;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.StringPath;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Michael Backhaus <michael.backhaus@agfa.com>
 */
class MatchDateTimeRange {
    
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

    static Predicate rangeMatch(StringPath path,
            Attributes keys, int tag, FormatDate dt,
            boolean matchUnknown) {
        if (!keys.containsValue(tag))
            return null;
        
        return matchUnknown(path, matchUnknown, range(path, keys.getDateRange(tag, null), dt));
    }

    static Predicate rangeMatch(StringPath dateField, StringPath timeField, 
            int dateTag, int timeTag, long dateAndTimeTag, 
            Attributes keys, boolean combined, boolean matchUnknown) {
        final boolean containsDateTag = keys.containsValue(dateTag);
        final boolean containsTimeTag = keys.containsValue(timeTag);
        if (!containsDateTag && !containsTimeTag)
            return null;
        
        BooleanBuilder predicates = new BooleanBuilder();
        if (containsDateTag && containsTimeTag && combined)
                predicates.and(matchUnknown(dateField, matchUnknown,
                        combinedRange(dateField, timeField, keys.getDateRange(dateAndTimeTag, null))));
        else { 
            if (containsDateTag)
                predicates.and(matchUnknown(dateField, matchUnknown, 
                        range(dateField, keys.getDateRange(dateTag, null), FormatDate.DA)));
            if (containsTimeTag)
                predicates.and(matchUnknown(timeField, matchUnknown, 
                        range(timeField, keys.getDateRange(timeTag, null), FormatDate.TM)));
        }
        return Builder.nullIfNoValue(predicates);
    }

    private static Predicate matchUnknown(StringPath field, boolean matchUnknown, 
            Predicate predicate) {
        return matchUnknown ? 
                ExpressionUtils.or(predicate, field.eq("*")): 
                ExpressionUtils.and(predicate, field.ne("*"));
    }

    private static Predicate range(StringPath field, DateRange range, FormatDate dt) {
        Date startDate = range.getStartDate();
        Date endDate = range.getEndDate();
        if (startDate == null)
            return rangeEnd(field, dt.format(endDate));
        if (endDate == null)
            return rangeStart(field, dt.format(startDate));
        return rangeInterval(field, startDate, endDate, dt);
    }

    private static Predicate rangeInterval(StringPath field, Date startDate,
            Date endDate, FormatDate dt) {
        String start = dt.format(startDate);
        String end = dt.format(endDate);
        if (end.equals(start))
            return field.eq(start);
        else
            return field.between(start, end);
    }

    private static Predicate rangeEnd(StringPath field, String value) {
        return field.loe(value);
    }

    private static Predicate rangeStart(StringPath field, String value) {
        return field.goe(value);
    }

    private static Predicate combinedRange(StringPath dateField, StringPath timeField, DateRange dateRange) {
        if (dateRange.getStartDate() == null)
            return combinedRangeEnd(dateField, timeField, 
                    DateUtils.formatDA(null, dateRange.getEndDate()), 
                    DateUtils.formatTM(null, dateRange.getEndDate()));
        if (dateRange.getEndDate() == null)
            return combinedRangeStart(dateField, timeField, 
                    DateUtils.formatDA(null, dateRange.getStartDate()), 
                    DateUtils.formatTM(null, dateRange.getStartDate()));
        return combinedRangeInterval(dateField, timeField, 
                    dateRange.getStartDate(), dateRange.getEndDate());
    }

    private static Predicate combinedRangeInterval(StringPath dateField,
            StringPath timeField, Date startDateRange, Date endDateRange) {
        String startTime = DateUtils.formatTM(null, startDateRange);
        String endTime = DateUtils.formatTM(null, endDateRange);
        String startDate = DateUtils.formatDA(null, startDateRange);
        String endDate = DateUtils.formatDA(null, endDateRange);
        if (endDate.equals(startDate))
            return ExpressionUtils.allOf(
                    dateField.eq(startDate), 
                    rangeStart(timeField, startTime), 
                    rangeEnd(timeField, endTime));
        return ExpressionUtils.and(
                combinedRangeStart(dateField, timeField, startDate, startTime), 
                combinedRangeEnd(dateField, timeField, endDate, endTime));
    }

    private static Predicate combinedRangeEnd(StringPath dateField,
            StringPath timeField, String endDate, String endTime) {
        Predicate endDayTime =
            ExpressionUtils.and(dateField.eq(endDate), timeField.loe(endTime));
        Predicate endDayTimeUnknown =
            ExpressionUtils.and(dateField.eq(endDate), timeField.eq("*"));
        Predicate endDayPrevious = dateField.lt(endDate);
        return ExpressionUtils.anyOf(endDayTime, endDayTimeUnknown, endDayPrevious);
    }

    private static Predicate combinedRangeStart(StringPath dateField,
            StringPath timeField, String startDate, String startTime) {
        Predicate startDayTime = 
            ExpressionUtils.and(dateField.eq(startDate), timeField.goe(startTime));
        Predicate startDayTimeUnknown = 
            ExpressionUtils.and(dateField.eq(startDate), timeField.eq("*"));
        Predicate startDayFollowing = dateField.gt(startDate);
        return ExpressionUtils.anyOf(startDayTime, startDayTimeUnknown, startDayFollowing);
    }
}
