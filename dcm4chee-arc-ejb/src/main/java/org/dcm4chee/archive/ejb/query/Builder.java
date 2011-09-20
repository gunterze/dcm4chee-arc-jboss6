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

import java.util.EnumSet;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Sequence;
import org.dcm4che.data.Tag;
import org.dcm4che.net.pdu.QueryOption;
import org.dcm4chee.archive.persistence.Action;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.dcm4chee.archive.persistence.Code;
import org.dcm4chee.archive.persistence.QCode;
import org.dcm4chee.archive.persistence.QInstance;
import org.dcm4chee.archive.persistence.QIssuer;
import org.dcm4chee.archive.persistence.QPatient;
import org.dcm4chee.archive.persistence.QRequestAttributes;
import org.dcm4chee.archive.persistence.QSeries;
import org.dcm4chee.archive.persistence.QStudy;
import org.dcm4chee.archive.persistence.QVerifyingObserver;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateSubQuery;
import com.mysema.query.types.ExpressionUtils;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.CollectionPath;
import com.mysema.query.types.path.StringPath;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
abstract class Builder {

    static void addPatientLevelPredicates(BooleanBuilder builder, String[] pids,
            Attributes keys, AttributeFilter filter, EnumSet<QueryOption> queryOpts) {

        boolean matchUnknown = filter.isMatchUnknown();

        builder.and(pids(pids, matchUnknown));

        if (keys == null)
            return;

        builder.and(pn(QPatient.patient.patientName,
                QPatient.patient.patientIdeographicName,
                QPatient.patient.patientPhoneticName,
                QPatient.patient.patientFamilyNameSoundex,
                QPatient.patient.patientGivenNameSoundex,
                filter.getString(keys, Tag.PatientName), filter,
                queryOpts.contains(QueryOption.FUZZY), matchUnknown));
        builder.and( wc(QPatient.patient.patientSex,
                filter.getString(keys, Tag.PatientSex), matchUnknown));
        builder.and(
                da(QPatient.patient.patientBirthDate, keys, Tag.PatientBirthDate, matchUnknown));
        builder.and(wc(QPatient.patient.patientCustomAttribute1,
                filter.selectPatientCustomAttribute1(keys), matchUnknown));
        builder.and(wc(QPatient.patient.patientCustomAttribute2,
                filter.selectPatientCustomAttribute2(keys), matchUnknown));
        builder.and(wc(QPatient.patient.patientCustomAttribute3,
                filter.selectPatientCustomAttribute3(keys), matchUnknown));
    }

    static void addStudyLevelPredicates(BooleanBuilder builder, Attributes keys,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts, String[] roles) {
        if (keys == null)
            return;

        boolean matchUnknown = filter.isMatchUnknown();
        builder.and(uids(QStudy.study.studyInstanceUID, keys.getStrings(Tag.StudyInstanceUID)));
        builder.and(wc(QStudy.study.studyID, filter.getString(keys, Tag.StudyID), matchUnknown));
        builder.and(datm(
                QStudy.study.studyDate, 
                QStudy.study.studyTime,
                Tag.StudyDate,
                Tag.StudyTime,
                Tag.StudyDateAndTime,
                keys, queryOpts.contains(QueryOption.DATETIME), matchUnknown));
        builder.and(pn(
                QStudy.study.referringPhysicianName,
                QStudy.study.referringPhysicianIdeographicName,
                QStudy.study.referringPhysicianPhoneticName,
                QStudy.study.referringPhysicianFamilyNameSoundex,
                QStudy.study.referringPhysicianGivenNameSoundex,
                filter.getString(keys, Tag.ReferringPhysicianName),
                filter, queryOpts.contains(QueryOption.FUZZY), matchUnknown));
        builder.and(wc(QStudy.study.studyDescription,
                filter.getString(keys, Tag.StudyDescription), matchUnknown));
        String accNo = filter.getString(keys, Tag.AccessionNumber);
        builder.and(wc(QStudy.study.accessionNumber, accNo, matchUnknown));
        if(!accNo.equals("*"))
            builder.and(issuer(QStudy.study.issuerOfAccessionNumber,
                    keys.getNestedDataset(Tag.IssuerOfAccessionNumberSequence),
                    filter, matchUnknown));
        builder.and(modalitiesInStudy(filter.getString(keys, Tag.ModalitiesInStudy), matchUnknown));
        builder.and(code(QStudy.study.procedureCodes,
                keys.getNestedDataset(Tag.ProcedureCodeSequence), filter, matchUnknown));
        builder.and(wc(QStudy.study.studyCustomAttribute1,
                filter.selectStudyCustomAttribute1(keys), matchUnknown));
        builder.and(wc(QStudy.study.studyCustomAttribute2,
                filter.selectStudyCustomAttribute2(keys), matchUnknown));
        builder.and(wc(QStudy.study.studyCustomAttribute3,
                filter.selectStudyCustomAttribute3(keys), matchUnknown));
        builder.and(permission(roles, Action.QUERY));
    }

    static void addSeriesLevelPredicates(BooleanBuilder builder, Attributes keys,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts) {
        if (keys == null)
            return;

        boolean matchUnknown = filter.isMatchUnknown();
        builder.and(uids(QSeries.series.seriesInstanceUID,
                keys.getStrings(Tag.SeriesInstanceUID)));
        builder.and(wc(QSeries.series.seriesNumber,
                filter.getString(keys, Tag.SeriesNumber), matchUnknown));
        builder.and(wc(QSeries.series.modality,
                filter.getString(keys, Tag.Modality), matchUnknown));
        builder.and(datm(
                QSeries.series.performedProcedureStepStartDate,
                QSeries.series.performedProcedureStepStartTime,
                Tag.PerformedProcedureStepStartDate,
                Tag.PerformedProcedureStepStartTime,
                Tag.PerformedProcedureStepStartDateAndTime,
                keys, queryOpts.contains(QueryOption.DATETIME), matchUnknown));
        builder.and(wc(QSeries.series.seriesDescription,
                filter.getString(keys, Tag.SeriesDescription), matchUnknown));
        builder.and(requestAttributes(keys.getNestedDataset(Tag.RequestAttributesSequence),
                filter, queryOpts.contains(QueryOption.FUZZY), matchUnknown));
        builder.and(code(QSeries.series.institutionCode,
                keys.getNestedDataset(Tag.InstitutionCodeSequence), filter, matchUnknown));
        builder.and(wc(QSeries.series.seriesCustomAttribute1,
                filter.selectSeriesCustomAttribute1(keys), matchUnknown));
        builder.and(wc(QSeries.series.seriesCustomAttribute2,
                filter.selectSeriesCustomAttribute2(keys), matchUnknown));
        builder.and(wc(QSeries.series.seriesCustomAttribute3,
                filter.selectSeriesCustomAttribute3(keys), matchUnknown));
    }

    static void addInstanceLevelPredicates(BooleanBuilder builder, Attributes keys,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts) {
        if (keys == null)
            return;

        boolean matchUnknown = filter.isMatchUnknown();
        builder.and(uids(QInstance.instance.sopInstanceUID, keys.getStrings(Tag.SOPInstanceUID)));
        builder.and(uids(QInstance.instance.sopClassUID, keys.getStrings(Tag.SOPClassUID)));
        builder.and(wc(QInstance.instance.instanceNumber,
                filter.getString(keys, Tag.InstanceNumber), matchUnknown));
        builder.and(wc(QInstance.instance.verificationFlag,
                filter.getString(keys, Tag.VerificationFlag), matchUnknown));
        builder.and(datm(
                QInstance.instance.contentDate,
                QInstance.instance.contentTime,
                Tag.ContentDate,
                Tag.ContentTime, 
                Tag.ContentDateAndTime,
                keys, queryOpts.contains(QueryOption.DATETIME), matchUnknown));
        builder.and(code(QInstance.instance.conceptNameCode,
                keys.getNestedDataset(Tag.ConceptNameCodeSequence), filter, matchUnknown));
        builder.and(verifyingObserver(keys.getNestedDataset(Tag.VerifyingObserverSequence),
                filter, queryOpts.contains(QueryOption.FUZZY), matchUnknown));
        Sequence contentSeq = keys.getSequence(Tag.ContentSequence);
        if (contentSeq != null)
            for (Attributes item : contentSeq)
                builder.and(contentItem(item, filter));
        builder.and(wc(QInstance.instance.instanceCustomAttribute1,
                filter.selectInstanceCustomAttribute1(keys), matchUnknown));
        builder.and(wc(QInstance.instance.instanceCustomAttribute2,
                filter.selectInstanceCustomAttribute2(keys), matchUnknown));
        builder.and(wc(QInstance.instance.instanceCustomAttribute3,
                filter.selectInstanceCustomAttribute3(keys), matchUnknown));
    }

    static Predicate or(Predicate left, Predicate right) {
        return left == null ? right : right == null ? left : ExpressionUtils.or(left, right);
    }

    static Predicate and(Predicate left, Predicate right) {
        return left == null ? right : right == null ? left : ExpressionUtils.and(left, right);
    }

    static Predicate pids(String[] pids, boolean matchUnknown) {
        if (pids == null || pids.length == 0)
            return null;

        Predicate result = null;
        for (int i = 0; i < pids.length-1; i++, i++)
            result = or(result, pid(pids[i], pids[i+1], matchUnknown));

        return result ;
    }

    static Predicate pid(String id, String issuer, boolean matchUnknown) {
        Predicate p1 = wc(QPatient.patient.patientID, id, matchUnknown);
        Predicate p2 = wc(QPatient.patient.issuerOfPatientID, issuer, matchUnknown);
        return and(p1, p2);
    }

    static Predicate wc(StringPath path, String value,  boolean matchUnknown) {
        if (value.equals("*"))
            return null;

        return containsWildcard(value)
            ? like(path, toLikePattern(value), matchUnknown)
            : eq(path, value, matchUnknown);
    }

    static boolean containsWildcard(String s) {
        return s.indexOf('*') >= 0 || s.indexOf('?') >= 0;
    }


    static Predicate like(StringPath path, String value, boolean matchUnknown) {
        if (value.equals("%"))
            return null;

        Predicate predicate = path.like(value);
        if (matchUnknown)
            predicate = ExpressionUtils.or(predicate, path.eq("*"));
        return predicate ;
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

    static Predicate eq(StringPath path, String value, boolean matchUnknown) {
        if (value.equals("*"))
            return null;

        Predicate predicate = path.eq(value);
        if (matchUnknown)
            predicate = ExpressionUtils.or(predicate, path.eq("*"));
        return predicate ;
    }

    static Predicate uids(StringPath path, String[] values) {
        if (values == null || values.length == 0 || values[0].equals("*"))
            return null;

        return values.length == 1
                ? path.eq(values[0])
                : path.in(values);
    }

    static Predicate da(StringPath datePath, Attributes keys, int daTag, boolean matchUnknown) {
        // TODO Auto-generated method stub
        return null;
    }

    static Predicate dt(StringPath dtPath, Attributes keys, int dtTag, boolean matchUnknown) {
        // TODO Auto-generated method stub
        return null;
    }

    static Predicate datm(StringPath datePath, StringPath timePath,
            int daTag, int dtTag, long datmTag,
            Attributes keys, boolean combined, boolean matchUnknown) {
        // TODO Auto-generated method stub
        return null;
    }

    static Predicate pn(StringPath alphabethic, StringPath ideographic, StringPath phonetic,
            StringPath familyNameSoundex, StringPath givenNameSoundex, String value,
            AttributeFilter filter, boolean fuzzy, boolean matchUnknown) {
        // TODO Auto-generated method stub
        return null;
    }

    static Predicate modalitiesInStudy(String modality, boolean matchUnknown) {
        if (modality.equals("*"))
            return null;

        return new HibernateSubQuery()
            .from(QSeries.series)
            .where(QSeries.series.study.eq(QStudy.study),
                    wc(QSeries.series.modality, modality, matchUnknown))
            .exists();
    }

    static Predicate code(Attributes item, AttributeFilter filter) {
        return and(wc(QCode.code.codeValue, filter.getString(item, Tag.CodeValue), false),
                and(wc(QCode.code.codingSchemeDesignator,
                        filter.getString(item, Tag.CodingSchemeDesignator), false),
                    wc(QCode.code.codingSchemeVersion,
                                filter.getString(item, Tag.CodingSchemeVersion), false)));
    }

    static Predicate code(QCode code, Attributes item, AttributeFilter filter,
            boolean matchUnknown) {
        Predicate predicate = code(item, filter);
        if (predicate == null)
            return null;

        predicate = new HibernateSubQuery()
            .from(QCode.code)
            .where(QCode.code.eq(code), predicate)
            .exists();

        if (matchUnknown)
            predicate = or(predicate, code.isNull());

        return predicate ;
    }

    static Predicate code(CollectionPath<Code, QCode> codes, Attributes item,
            AttributeFilter filter, boolean matchUnknown) {
        Predicate predicate = code(item, filter);
        if (predicate == null)
            return null;

        predicate = new HibernateSubQuery()
            .from(QCode.code)
            .where(codes.contains(QCode.code), predicate)
            .exists();

        if (matchUnknown)
            predicate = or(predicate, codes.isEmpty());

        return predicate;
    }

    static Predicate issuer(QIssuer path, Attributes item, AttributeFilter filter,
            boolean matchUnknown) {
        if (item == null || item.isEmpty())
            return null;

        Predicate predicate = and(wc(QIssuer.issuer.entityID,
                filter.getString(item, Tag.LocalNamespaceEntityID), false),
           and(wc(QIssuer.issuer.entityUID,
                filter.getString(item, Tag.UniversalEntityID), false),
               wc(QIssuer.issuer.entityUIDType,
                filter.getString(item, Tag.UniversalEntityIDType), false)));

        if (predicate == null)
            return null;

        predicate = new HibernateSubQuery()
            .from(QIssuer.issuer)
            .where(QIssuer.issuer.eq(path), predicate)
            .exists();

        if (matchUnknown)
            predicate = or(predicate, path.isNull());

        return predicate ;
    }

    static Predicate requestAttributes(Attributes item, AttributeFilter filter, boolean fuzzy,
            boolean matchUnknown) {
        if (item == null || item.isEmpty())
            return null;

        BooleanBuilder bb = new BooleanBuilder();
        bb.and(wc(QRequestAttributes.requestAttributes.requestedProcedureID,
                filter.getString(item, Tag.RequestedProcedureID),
                matchUnknown));
        bb.and(wc(QRequestAttributes.requestAttributes.scheduledProcedureStepID,
                filter.getString(item, Tag.ScheduledProcedureStepID),
                matchUnknown));
        bb.and(wc(QRequestAttributes.requestAttributes.requestingService,
                filter.getString(item, Tag.RequestingService),
                matchUnknown));
        bb.and(pn(
                QRequestAttributes.requestAttributes.requestingPhysician,
                QRequestAttributes.requestAttributes.requestingPhysicianIdeographicName,
                QRequestAttributes.requestAttributes.requestingPhysicianPhoneticName,
                QRequestAttributes.requestAttributes.requestingPhysicianFamilyNameSoundex,
                QRequestAttributes.requestAttributes.requestingPhysicianGivenNameSoundex,
                filter.getString(item, Tag.ReferringPhysicianName), filter, fuzzy, matchUnknown));
        bb.and(uids(QRequestAttributes.requestAttributes.studyInstanceUID,
                item.getStrings(Tag.StudyInstanceUID)));
        String accNo = filter.getString(item, Tag.AccessionNumber);
        bb.and(wc(QRequestAttributes.requestAttributes.accessionNumber, accNo, matchUnknown));
        if (!accNo.equals("*"))
            bb.and(issuer(QRequestAttributes.requestAttributes.issuerOfAccessionNumber,
                    item.getNestedDataset(Tag.IssuerOfAccessionNumberSequence),
                    filter, matchUnknown));

        if (!bb.hasValue())
            return null;

        Predicate predicate = new HibernateSubQuery()
            .from(QRequestAttributes.requestAttributes)
            .where(QSeries.series.requestAttributes.contains(QRequestAttributes.requestAttributes),
                    bb)
            .exists();

        if (matchUnknown)
            predicate = or(predicate, QSeries.series.requestAttributes.isEmpty());

        return predicate ;
    }

    static Predicate verifyingObserver(Attributes item, AttributeFilter filter,
            boolean fuzzy, boolean matchUnknown) {
        if (item == null || item.isEmpty())
            return null;

        Predicate predicate = and(
                dt(QVerifyingObserver.verifyingObserver.verificationDateTime,
                        item, Tag.VerificationDateTime, matchUnknown),
                pn(QVerifyingObserver.verifyingObserver.verifyingObserverName,
                   QVerifyingObserver.verifyingObserver.verifyingObserverIdeographicName,
                   QVerifyingObserver.verifyingObserver.verifyingObserverPhoneticName,
                   QVerifyingObserver.verifyingObserver.verifyingObserverFamilyNameSoundex,
                   QVerifyingObserver.verifyingObserver.verifyingObserverGivenNameSoundex,
                   filter.getString(item, Tag.VerifyingObserverName),
                   filter, fuzzy, matchUnknown));

        if (predicate == null)
            return null;

        predicate = new HibernateSubQuery()
            .from(QVerifyingObserver.verifyingObserver)
            .where(QInstance.instance.verifyingObservers
                        .contains(QVerifyingObserver.verifyingObserver),
                    predicate)
            .exists();

        if (matchUnknown)
            predicate = or(predicate, QSeries.series.requestAttributes.isEmpty());

        return predicate ;
    }

    static Predicate contentItem(Attributes item, AttributeFilter filter) {
        // TODO Auto-generated method stub
        return null;
    }

    static Predicate permission(String[] roles, Action query) {
        // TODO Auto-generated method stub
        return null;
    }

}
