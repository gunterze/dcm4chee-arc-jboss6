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

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Sequence;
import org.dcm4che.data.Tag;
import org.dcm4chee.archive.persistence.Action;
import org.dcm4chee.archive.persistence.Code;
import org.dcm4chee.archive.persistence.QCode;
import org.dcm4chee.archive.persistence.QContentItem;
import org.dcm4chee.archive.persistence.QInstance;
import org.dcm4chee.archive.persistence.QIssuer;
import org.dcm4chee.archive.persistence.QPatient;
import org.dcm4chee.archive.persistence.QRequestAttributes;
import org.dcm4chee.archive.persistence.QSeries;
import org.dcm4chee.archive.persistence.QStudy;
import org.dcm4chee.archive.persistence.QStudyPermission;
import org.dcm4chee.archive.persistence.QVerifyingObserver;
import org.dcm4chee.archive.persistence.StoreParam;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateSubQuery;
import com.mysema.query.types.ExpressionUtils;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.SimpleExpression;
import com.mysema.query.types.expr.StringExpression;
import com.mysema.query.types.path.BeanPath;
import com.mysema.query.types.path.CollectionPath;
import com.mysema.query.types.path.StringPath;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Michael Backhaus <michael.backhaus@gmail.com>
 */
abstract class Builder {

    static void addPatientLevelPredicates(BooleanBuilder builder, String[] pids,
            Attributes keys, QueryParam queryParam, StoreParam storeParam) {

        boolean matchUnknown = queryParam.isMatchUnknown();

        builder.and(pids(pids, matchUnknown));

        if (keys == null)
            return;

        builder.and(MatchPersonName.match(QPatient.patient.patientName,
                QPatient.patient.patientIdeographicName,
                QPatient.patient.patientPhoneticName,
                QPatient.patient.patientFamilyNameSoundex,
                QPatient.patient.patientGivenNameSoundex,
                keys.getString(Tag.PatientName, "*"),
                queryParam, storeParam.getFuzzyStr()));
        builder.and(wildCard(QPatient.patient.patientSex,
                keys.getString(Tag.PatientSex, "*").toUpperCase(), matchUnknown, false));
        builder.and(MatchDateTimeRange.rangeMatch(QPatient.patient.patientBirthDate, 
                keys, Tag.PatientBirthDate, MatchDateTimeRange.FormatDate.DA, matchUnknown));
        builder.and(wildCard(QPatient.patient.patientCustomAttribute1,
                StoreParam.selectStringValue(keys, storeParam.getPatientCustomAttribute1(), "*"),
                matchUnknown, true));
        builder.and(wildCard(QPatient.patient.patientCustomAttribute2,
                StoreParam.selectStringValue(keys, storeParam.getPatientCustomAttribute2(), "*"),
                matchUnknown, true));
        builder.and(wildCard(QPatient.patient.patientCustomAttribute3,
                StoreParam.selectStringValue(keys, storeParam.getPatientCustomAttribute2(), "*"),
                matchUnknown, true));
    }

    static void addStudyLevelPredicates(BooleanBuilder builder, Attributes keys,
            QueryParam queryParam, StoreParam storeParam) {
        if (keys == null)
            return;

        boolean matchUnknown = queryParam.isMatchUnknown();
        builder.and(uids(QStudy.study.studyInstanceUID, keys.getStrings(Tag.StudyInstanceUID)));
        builder.and(wildCard(QStudy.study.studyID, keys.getString(Tag.StudyID, "*"), matchUnknown, false));
        builder.and(MatchDateTimeRange.rangeMatch(QStudy.study.studyDate, QStudy.study.studyTime, 
                Tag.StudyDate, Tag.StudyTime, Tag.StudyDateAndTime, 
                keys, queryParam.isCombinedDatetimeMatching(), matchUnknown));
        builder.and(MatchPersonName.match(QStudy.study.referringPhysicianName,
                QStudy.study.referringPhysicianIdeographicName,
                QStudy.study.referringPhysicianPhoneticName,
                QStudy.study.referringPhysicianFamilyNameSoundex,
                QStudy.study.referringPhysicianGivenNameSoundex,
                keys.getString(Tag.ReferringPhysicianName, "*"),
                queryParam, storeParam.getFuzzyStr()));
        builder.and(wildCard(QStudy.study.studyDescription,
                keys.getString(Tag.StudyDescription, "*"), matchUnknown, true));
        String accNo = keys.getString(Tag.AccessionNumber, "*");
        builder.and(wildCard(QStudy.study.accessionNumber, accNo, matchUnknown, false));
        if(!accNo.equals("*"))
            builder.and(issuer(QStudy.study.issuerOfAccessionNumber,
                    keys.getNestedDataset(Tag.IssuerOfAccessionNumberSequence),
                    matchUnknown));
        builder.and(modalitiesInStudy(
                keys.getString(Tag.ModalitiesInStudy, "*").toUpperCase(), matchUnknown));
        builder.and(code(QStudy.study.procedureCodes,
                keys.getNestedDataset(Tag.ProcedureCodeSequence), matchUnknown));
        builder.and(wildCard(QStudy.study.studyCustomAttribute1,
                StoreParam.selectStringValue(keys, storeParam.getStudyCustomAttribute1(), "*"),
                matchUnknown, true));
        builder.and(wildCard(QStudy.study.studyCustomAttribute2,
                StoreParam.selectStringValue(keys, storeParam.getStudyCustomAttribute2(), "*"),
                matchUnknown, true));
        builder.and(wildCard(QStudy.study.studyCustomAttribute3,
                StoreParam.selectStringValue(keys, storeParam.getStudyCustomAttribute3(), "*"),
                matchUnknown, true));
        builder.and(permission(queryParam.getRoles(), Action.QUERY));
    }

    static void addSeriesLevelPredicates(BooleanBuilder builder, Attributes keys,
            QueryParam queryParam, StoreParam storeParam) {
        if (keys == null)
            return;

        boolean matchUnknown = queryParam.isMatchUnknown();
        builder.and(uids(QSeries.series.seriesInstanceUID,
                keys.getStrings(Tag.SeriesInstanceUID)));
        builder.and(wildCard(QSeries.series.seriesNumber,
                keys.getString(Tag.SeriesNumber, "*"), matchUnknown, false));
        builder.and(wildCard(QSeries.series.modality,
                keys.getString(Tag.Modality, "*").toUpperCase(), matchUnknown, false));
        builder.and(MatchDateTimeRange.rangeMatch(
                QSeries.series.performedProcedureStepStartDate,
                QSeries.series.performedProcedureStepStartTime,
                Tag.PerformedProcedureStepStartDate,
                Tag.PerformedProcedureStepStartTime,
                Tag.PerformedProcedureStepStartDateAndTime,
                keys, queryParam.isCombinedDatetimeMatching(), matchUnknown));
        builder.and(wildCard(QSeries.series.seriesDescription,
                keys.getString(Tag.SeriesDescription, "*"), matchUnknown, true));
        builder.and(requestAttributes(keys.getNestedDataset(Tag.RequestAttributesSequence),
                queryParam, storeParam));
        builder.and(code(QSeries.series.institutionCode,
                keys.getNestedDataset(Tag.InstitutionCodeSequence), matchUnknown));
        builder.and(wildCard(QSeries.series.seriesCustomAttribute1,
                StoreParam.selectStringValue(keys, storeParam.getSeriesCustomAttribute1(), "*"),
                matchUnknown, true));
        builder.and(wildCard(QSeries.series.seriesCustomAttribute2,
                StoreParam.selectStringValue(keys, storeParam.getSeriesCustomAttribute2(), "*"),
                matchUnknown, true));
        builder.and(wildCard(QSeries.series.seriesCustomAttribute3,
                StoreParam.selectStringValue(keys, storeParam.getSeriesCustomAttribute3(), "*"),
                matchUnknown, true));
    }

    static void addInstanceLevelPredicates(BooleanBuilder builder, Attributes keys,
            QueryParam queryParam, StoreParam storeParam) {
        if (keys == null)
            return;

        boolean matchUnknown = queryParam.isMatchUnknown();
        builder.and(uids(QInstance.instance.sopInstanceUID, keys.getStrings(Tag.SOPInstanceUID)));
        builder.and(uids(QInstance.instance.sopClassUID, keys.getStrings(Tag.SOPClassUID)));
        builder.and(wildCard(QInstance.instance.instanceNumber,
                keys.getString(Tag.InstanceNumber, "*"), matchUnknown, false));
        builder.and(wildCard(QInstance.instance.verificationFlag,
                keys.getString(Tag.VerificationFlag, "*").toUpperCase(), matchUnknown, false));
        builder.and(MatchDateTimeRange.rangeMatch(
                QInstance.instance.contentDate,
                QInstance.instance.contentTime,
                Tag.ContentDate, Tag.ContentTime, Tag.ContentDateAndTime,
                keys, queryParam.isCombinedDatetimeMatching(), matchUnknown));
        builder.and(code(QInstance.instance.conceptNameCode,
                keys.getNestedDataset(Tag.ConceptNameCodeSequence), matchUnknown));
        builder.and(verifyingObserver(keys.getNestedDataset(Tag.VerifyingObserverSequence),
                queryParam, storeParam));
        Sequence contentSeq = keys.getSequence(Tag.ContentSequence);
        if (contentSeq != null)
            for (Attributes item : contentSeq)
                builder.and(contentItem(item));
        builder.and(wildCard(QInstance.instance.instanceCustomAttribute1,
                StoreParam.selectStringValue(keys, storeParam.getInstanceCustomAttribute1(), "*"),
                matchUnknown, true));
        builder.and(wildCard(QInstance.instance.instanceCustomAttribute2,
                StoreParam.selectStringValue(keys, storeParam.getInstanceCustomAttribute2(), "*"),
                matchUnknown, true));
        builder.and(wildCard(QInstance.instance.instanceCustomAttribute3,
                StoreParam.selectStringValue(keys, storeParam.getInstanceCustomAttribute3(), "*"),
                matchUnknown, true));
    }

    static Predicate pids(String[] pids, boolean matchUnknown) {
        if (pids == null || pids.length == 0)
            return null;

        BooleanBuilder result = new BooleanBuilder();
        for (int i = 0; i < pids.length-1; i++, i++)
            result.or(pid(pids[i], pids[i+1], matchUnknown));

        return result;
    }

    static Predicate pid(String id, String issuer, boolean matchUnknown) {
        return ExpressionUtils.allOf(
                wildCard(QPatient.patient.patientID, id, matchUnknown, false),
                wildCard(QPatient.patient.issuerOfPatientID, issuer, matchUnknown, false));
    }

    static Predicate wildCard(StringPath path, String value, boolean matchUnknown, boolean ignoreCase) {
        if (value.equals("*"))
            return null;

        Predicate predicate;
        StringExpression expr = ignoreCase && isUpperCase(value) ? path.toUpperCase() : path;
        if (containsWildcard(value)) {
            String pattern = toLikePattern(value);
            if (pattern.equals("%"))
                return null;

            predicate = expr.like(pattern);
        } else
            predicate = expr.eq(value);

        return matchUnknown ? ExpressionUtils.or(predicate, path.eq("*")) : predicate;
    }

    static boolean isUpperCase(String s) {
        return s.equals(s.toUpperCase());
    }

    static boolean containsWildcard(String s) {
        return s.indexOf('*') >= 0 || s.indexOf('?') >= 0;
    }


    static <T> Predicate matchUnknown(Predicate predicate, BeanPath<T> path, boolean matchUnknown) {
        return matchUnknown ? ExpressionUtils.or(predicate, path.isNull()) : predicate;
    }

    static <E,Q extends SimpleExpression<? super E>> Predicate matchUnknown(
            Predicate predicate, CollectionPath<E, Q> path, boolean matchUnknown) {
        return matchUnknown ? ExpressionUtils.or(predicate, path.isEmpty()) : predicate;
    }

    static String toLikePattern(String s) {
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

    static Predicate uids(StringPath path, String[] values) {
        if (values == null || values.length == 0 || values[0].equals("*"))
            return null;

        return values.length == 1
                ? path.eq(values[0])
                : path.in(values);
    }

    static Predicate modalitiesInStudy(String modality, boolean matchUnknown) {
        if (modality.equals("*"))
            return null;

        return new HibernateSubQuery()
            .from(QSeries.series)
            .where(QSeries.series.study.eq(QStudy.study),
                    wildCard(QSeries.series.modality, modality, matchUnknown, false))
            .exists();
    }

    static Predicate code(Attributes item) {
        if (item == null || item.isEmpty())
            return null;

        return ExpressionUtils.allOf(
                wildCard(QCode.code.codeValue, 
                        item.getString(Tag.CodeValue, "*"), false, false),
                wildCard(QCode.code.codingSchemeDesignator,
                        item.getString(Tag.CodingSchemeDesignator, "*"), false, false),
                wildCard(QCode.code.codingSchemeVersion,
                        item.getString(Tag.CodingSchemeVersion, "*"), false, false));
    }

    static Predicate code(QCode code, Attributes item, boolean matchUnknown) {
        Predicate predicate = code(item);
        if (predicate == null)
            return null;

        return matchUnknown(
                new HibernateSubQuery()
                    .from(QCode.code)
                    .where(QCode.code.eq(code), predicate)
                    .exists(),
                code,
                matchUnknown);
    }

    static Predicate code(CollectionPath<Code, QCode> codes, Attributes item,
            boolean matchUnknown) {
        Predicate predicate = code(item);
        if (predicate == null)
            return null;

        return matchUnknown(new HibernateSubQuery()
            .from(QCode.code)
            .where(codes.contains(QCode.code), predicate)
            .exists(),
                codes,
               matchUnknown);
    }

    static Predicate issuer(QIssuer path, Attributes item,  boolean matchUnknown) {
        if (item == null || item.isEmpty())
            return null;

        Predicate predicate = ExpressionUtils.allOf(
               wildCard(QIssuer.issuer.entityID,
                       item.getString(Tag.LocalNamespaceEntityID, "*"), false, false),
               wildCard(QIssuer.issuer.entityUID,
                       item.getString(Tag.UniversalEntityID, "*"), false, false),
               wildCard(QIssuer.issuer.entityUIDType,
                       item.getString(Tag.UniversalEntityIDType, "*"), false, false));

        if (predicate == null)
            return null;

        return matchUnknown(
                new HibernateSubQuery()
                    .from(QIssuer.issuer)
                    .where(QIssuer.issuer.eq(path), predicate)
                    .exists(),
                path,
                matchUnknown);
    }

    static Predicate requestAttributes(Attributes item, QueryParam queryParam,
            StoreParam storeParam) {
        if (item == null || item.isEmpty())
            return null;

        boolean matchUnknown = queryParam.isMatchUnknown();
        String accNo = item.getString(Tag.AccessionNumber, "*");
        BooleanBuilder builder = new BooleanBuilder()
            .and(wildCard(QRequestAttributes.requestAttributes.requestedProcedureID,
                    item.getString(Tag.RequestedProcedureID, "*"),
                    matchUnknown, false))
            .and(wildCard(QRequestAttributes.requestAttributes.scheduledProcedureStepID,
                    item.getString(Tag.ScheduledProcedureStepID, "*"),
                    matchUnknown, false))
            .and(wildCard(QRequestAttributes.requestAttributes.requestingService,
                    item.getString(Tag.RequestingService, "*"),
                    matchUnknown, true))
            .and(MatchPersonName.match(
                QRequestAttributes.requestAttributes.requestingPhysician,
                QRequestAttributes.requestAttributes.requestingPhysicianIdeographicName,
                QRequestAttributes.requestAttributes.requestingPhysicianPhoneticName,
                QRequestAttributes.requestAttributes.requestingPhysicianFamilyNameSoundex,
                QRequestAttributes.requestAttributes.requestingPhysicianGivenNameSoundex,
                item.getString(Tag.ReferringPhysicianName, "*"),
                queryParam, storeParam.getFuzzyStr()))
            .and(uids(QRequestAttributes.requestAttributes.studyInstanceUID,
                    item.getStrings(Tag.StudyInstanceUID)))
            .and(wildCard(QRequestAttributes.requestAttributes.accessionNumber, accNo, matchUnknown, false));

        if (!accNo.equals("*"))
            builder.and(
                    issuer(QRequestAttributes.requestAttributes.issuerOfAccessionNumber,
                        item.getNestedDataset(Tag.IssuerOfAccessionNumberSequence),
                        matchUnknown));

        if (!builder.hasValue())
            return null;

        return matchUnknown(
                new HibernateSubQuery()
                    .from(QRequestAttributes.requestAttributes)
                    .where(QSeries.series.requestAttributes.contains(QRequestAttributes.requestAttributes),
                            builder)
                    .exists(),
                QSeries.series.requestAttributes,
                matchUnknown);
    }

    static Predicate verifyingObserver(Attributes item, QueryParam queryParam,
            StoreParam storeParam) {
        if (item == null || item.isEmpty())
            return null;

        boolean matchUnknown = queryParam.isMatchUnknown();
        Predicate predicate = ExpressionUtils.allOf(
                    MatchDateTimeRange.rangeMatch(
                        QVerifyingObserver.verifyingObserver.verificationDateTime, item, 
                        Tag.VerificationDateTime, MatchDateTimeRange.FormatDate.DT, matchUnknown),
                    MatchPersonName.match(
                        QVerifyingObserver.verifyingObserver.verifyingObserverName,
                        QVerifyingObserver.verifyingObserver.verifyingObserverIdeographicName,
                        QVerifyingObserver.verifyingObserver.verifyingObserverPhoneticName,
                        QVerifyingObserver.verifyingObserver.verifyingObserverFamilyNameSoundex,
                        QVerifyingObserver.verifyingObserver.verifyingObserverGivenNameSoundex,
                        item.getString(Tag.VerifyingObserverName, "*"),
                        queryParam, storeParam.getFuzzyStr()));
        
        if (predicate == null)
            return null;

        return matchUnknown(
                new HibernateSubQuery()
                    .from(QVerifyingObserver.verifyingObserver)
                    .where(QInstance.instance.verifyingObservers
                            .contains(QVerifyingObserver.verifyingObserver),
                            predicate)
                    .exists(),
                QSeries.series.requestAttributes,
                matchUnknown);
    }

    static Predicate contentItem(Attributes item) {
        String valueType = item.getString(Tag.ValueType);
        if (!("CODE".equals(valueType) || "TEXT".equals(valueType)))
            return null;

        Predicate predicate = ExpressionUtils.allOf(
                    code(QContentItem.contentItem.conceptName,
                        item.getNestedDataset(Tag.ConceptNameCodeSequence), false),
                    wildCard(QContentItem.contentItem.relationshipType,
                            item.getString(Tag.RelationshipType, "*").toUpperCase(), false, false),
                    code(QContentItem.contentItem.conceptCode,
                        item.getNestedDataset(Tag.ConceptCodeSequence), false),
                    wildCard(QContentItem.contentItem.textValue,
                            item.getString(Tag.TextValue, "*"), false, true));
        if (predicate == null)
            return null;

        return new HibernateSubQuery()
            .from(QContentItem.contentItem)
            .where(QInstance.instance.contentItems.contains(QContentItem.contentItem), predicate)
            .exists();
    }

    static Predicate permission(String[] roles, Action action) {
        if (roles == null || roles.length == 0)
            return null;
        
        return new HibernateSubQuery()
            .from(QStudyPermission.studyPermission)
            .where(QStudyPermission.studyPermission.studyInstanceUID.eq(QStudy.study.studyInstanceUID),
                   QStudyPermission.studyPermission.action.eq(action),
                   QStudyPermission.studyPermission.role.in(roles))
            .exists();
    }

}
