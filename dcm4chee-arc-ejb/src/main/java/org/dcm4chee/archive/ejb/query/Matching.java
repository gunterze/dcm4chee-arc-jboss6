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
import java.util.EnumSet;
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
import org.dcm4che.data.Sequence;
import org.dcm4che.data.Tag;
import org.dcm4che.net.pdu.QueryOption;
import org.dcm4chee.archive.persistence.Action;
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
import org.dcm4chee.archive.persistence.StudyPermission;
import org.dcm4chee.archive.persistence.StudyPermission_;
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

    static String paramName(int paramIndex) {
        return paramIndex < NAMES.length ? NAMES[paramIndex]
                : ("p" + paramIndex);
    }

    private static String toLikePattern(String s) {
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

    private static boolean containsWildcard(String s) {
        return s.indexOf('*') >= 0 || s.indexOf('?') >= 0;
    }

    @SuppressWarnings("unchecked")
    static void listOfUID(CriteriaBuilder cb, Path<String> field, String[] values,
            List<Predicate> predicates, List<Object> params) {
        if (values == null || values.length == 0)
            return;

        if (values.length == 1)
            singleValue(cb, field, values[0], false, predicates, params);
        
        else {
            ParameterExpression<String>[] pes =
                    new ParameterExpression[values.length];
            for (int i = 0; i < values.length; i++) {
                pes[i] = cb.parameter(String.class, paramName(params.size()));
                params.add(values[i]);
            }
            predicates.add(field.in(pes));
        }
    }

    static void wildCard(CriteriaBuilder cb, 
            Path<String> field, String value, 
            boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        if (value.equals("*"))
            return;
        
        if (Matching.containsWildcard(value)) {
            String pattern = Matching.toLikePattern(value);
            if (!pattern.equals("%")){
                ParameterExpression<String> param = setParam(cb, pattern, params);
                predicates.add(matchUnknown(cb, field, matchUnknown, cb.like(field, param)));
            }
        }
        else
            singleValue(cb, field, value, matchUnknown, predicates, params);
    }

    static void singleValue(CriteriaBuilder cb,
            Path<String> field, String value, 
            boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        ParameterExpression<String> param = setParam(cb, value, params);
        predicates.add(matchUnknown(cb, field, matchUnknown, cb.equal(field, param)));
    }

    private static void modalitiesInStudy(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq, Path<Study> study, String modality,
            boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        if (modality.equals("*"))
            return;

        Subquery<Series> sq = cq.subquery(Series.class);
        Root<Series> series = sq.from(Series.class);
        ArrayList<Predicate> modPredicates = new ArrayList<Predicate>(1);
        wildCard(cb, series.get(Series_.modality), modality, matchUnknown, modPredicates, params);
        if (modPredicates.isEmpty())
            return;
        
        sq.select(series);
        sq.where(cb.equal(study, series.get(Series_.study)), modPredicates.get(0));
        predicates.add(cb.exists(sq));
    }

    private static void withCode(CriteriaBuilder cb, CriteriaQuery<Tuple> cq, 
            Expression<Collection<Code>> collection, Attributes item, AttributeFilter filter, 
            boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        if (item == null || item.isEmpty())
            return;

        Subquery<Code> sq = cq.subquery(Code.class);
        Root<Code> root = sq.from(Code.class);
        ArrayList<Predicate> codePredicates = addCodePredicates(cb, item, root, filter, 
                matchUnknown, params);
        if (codePredicates.isEmpty())
            return;
        
        codePredicates.add(cb.isMember(root, collection));
        sq.select(root);
        sq.where(codePredicates.toArray(new Predicate[codePredicates.size()]));
        predicates.add(matchUnknownCollection(cb, collection, matchUnknown, sq));
    }

    static void withCode(CriteriaBuilder cb, CriteriaQuery<Tuple> cq, 
            Path<Code> path, Attributes item, AttributeFilter filter,
            boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        if (item == null || item.isEmpty())
            return;

        Subquery<Code> sq = cq.subquery(Code.class);
        Root<Code> root = sq.from(Code.class);
        ArrayList<Predicate> codePredicates = addCodePredicates(cb, item, root, filter, 
                matchUnknown, params);
        if (codePredicates.isEmpty())
            return;
        
        codePredicates.add(cb.equal(root, path));
        sq.select(root);
        sq.where(codePredicates.toArray(new Predicate[codePredicates.size()]));
        predicates.add(matchUnknownPath(cb, path, matchUnknown, sq));
    }

    private static ArrayList<Predicate> addCodePredicates(CriteriaBuilder cb, 
            Attributes item,
            Root<Code> root, 
            AttributeFilter filter, 
            boolean matchUnknown,
            List<Object> params) {
        ArrayList<Predicate> predicates = new ArrayList<Predicate>(4);
        wildCard(cb, root.get(Code_.codeValue), 
                filter.getString(item, Tag.CodeValue), 
                false, predicates, params);
        wildCard(cb, root.get(Code_.codingSchemeDesignator), 
                filter.getString(item, Tag.CodingSchemeDesignator), 
                false, predicates, params);
        wildCard(cb, root.get(Code_.codingSchemeVersion), 
                filter.getString(item, Tag.CodingSchemeVersion), 
                false, predicates, params);
        return predicates;
    }

    private static void withIssuer(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq, Path<Issuer> issuer,
            Attributes item, AttributeFilter filter, 
            boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        if (item == null || item.isEmpty())
            return;

        Subquery<Issuer> sq = cq.subquery(Issuer.class);
        Root<Issuer> root = sq.from(Issuer.class);
        ArrayList<Predicate> issuerPredicates = addIssuerPredicates(cb, root, item, filter, params);
        if (issuerPredicates.isEmpty())
            return;
        
        issuerPredicates.add(cb.equal(root, issuer));
        sq.select(root);
        sq.where(issuerPredicates.toArray(new Predicate[issuerPredicates.size()]));
        predicates.add(matchUnknownPath(cb, issuer, matchUnknown, sq));
    }

    private static ArrayList<Predicate> addIssuerPredicates(CriteriaBuilder cb,
            Root<Issuer> root, Attributes item, AttributeFilter filter, List<Object> params) {
        ArrayList<Predicate> issuerPredicates = new ArrayList<Predicate>(4);
        wildCard(cb, root.get(Issuer_.entityID),
                filter.getString(item, Tag.LocalNamespaceEntityID),
                false, issuerPredicates, params);
        wildCard(cb, root.get(Issuer_.entityUID),
                filter.getString(item, Tag.UniversalEntityID),
                false, issuerPredicates, params);
        wildCard(cb, root.get(Issuer_.entityUIDType),
                filter.getString(item, Tag.UniversalEntityIDType),
                false, issuerPredicates, params);
        return issuerPredicates;
    }

    private static void withObserver(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq, Expression<Collection<VerifyingObserver>> collection,
            Attributes item, AttributeFilter filter, EnumSet<QueryOption> queryOpts,
            boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        if (item == null || item.isEmpty())
            return;

        Subquery<VerifyingObserver> sq = cq.subquery(VerifyingObserver.class);
        Root<VerifyingObserver> root = sq.from(VerifyingObserver.class);
        ArrayList<Predicate> observerPredicates = addObserverPredicates(cb, root, item, filter, 
                queryOpts, params);
        if (observerPredicates.isEmpty())
            return;

        observerPredicates.add(cb.isMember(root, collection));
        sq.select(root);
        sq.where(observerPredicates.toArray(new Predicate[observerPredicates.size()]));
        predicates.add(matchUnknownCollection(cb, collection, matchUnknown, sq));
    }

    private static ArrayList<Predicate> addObserverPredicates(CriteriaBuilder cb,
            Root<VerifyingObserver> root, Attributes item, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, List<Object> params) {
        ArrayList<Predicate> observerPredicates = new ArrayList<Predicate>(3);
        RangeMatching.rangeMatch(cb, 
                root.get(VerifyingObserver_.verificationDateTime), 
                Tag.VerificationDateTime, RangeMatching.FormatDate.DT, item, 
                false, observerPredicates, params);
        PersonNameMatching.personName(cb, 
                root.get(VerifyingObserver_.verifyingObserverName), 
                root.get(VerifyingObserver_.verifyingObserverIdeographicName), 
                root.get(VerifyingObserver_.verifyingObserverPhoneticName),
                root.get(VerifyingObserver_.verifyingObserverFamilyNameSoundex),
                root.get(VerifyingObserver_.verifyingObserverGivenNameSoundex),
                filter.getString(item, Tag.VerifyingObserverName),
                filter, queryOpts, 
                false, observerPredicates, params);
        return observerPredicates;
    }

    private static void checkPermission(CriteriaBuilder cb, CriteriaQuery<Tuple> cq, 
            Path<Study> study, String[] roles, Action action, 
            List<Predicate> predicates, List<Object> params) {
        if (roles == null || roles.length == 0)
            return;
        
        Subquery<StudyPermission> sq = cq.subquery(StudyPermission.class);
        Root<StudyPermission> root = sq.from(StudyPermission.class);
        sq.select(root);
        ArrayList<Predicate> permissionPredicates = new ArrayList<Predicate>();
        permissionPredicates.add(cb.equal(study.get(Study_.studyInstanceUID), 
                root.get(StudyPermission_.studyInstanceUID)));
        permissionPredicates.add(cb.equal(root.get(StudyPermission_.action), action));
        ArrayList<Predicate> rolesPredicate = new ArrayList<Predicate>();
        for (String role : roles)
            rolesPredicate.add(cb.equal(root.get(StudyPermission_.role), role));
        permissionPredicates.add(cb.or(rolesPredicate.toArray(new Predicate[rolesPredicate.size()])));
        sq.where(permissionPredicates.toArray(new Predicate[permissionPredicates.size()]));
        predicates.add(cb.exists(sq));
    }

    private static void requestAttributesSequence(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq, Attributes keys, 
            Expression<Collection<RequestAttributes>> collection,
            Attributes item, AttributeFilter filter, EnumSet<QueryOption> queryOpts,
            boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        if (item == null || item.isEmpty())
            return;

        Subquery<RequestAttributes> sq = cq.subquery(RequestAttributes.class);
        Root<RequestAttributes> root = sq.from(RequestAttributes.class);
        sq.select(root);
        ArrayList<Predicate> sequencePredicates = addSequencePredicates(cb, cq, root, collection, 
                item, filter, queryOpts, matchUnknown, params);
        if (sequencePredicates.isEmpty())
            return;
        
        sequencePredicates.add(cb.isMember(root, collection));
        sq.where(sequencePredicates.toArray(new Predicate[sequencePredicates.size()]));
        predicates.add(matchUnknownCollection(cb, collection, matchUnknown, sq));
    }

    private static ArrayList<Predicate> addSequencePredicates(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq, Root<RequestAttributes> root,
            Expression<Collection<RequestAttributes>> collection,
            Attributes item, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, boolean matchUnknown,
            List<Object> params) {
        ArrayList<Predicate> sequencePredicates = new ArrayList<Predicate>(7);
        wildCard(cb, root.get(RequestAttributes_.requestedProcedureID),
                filter.getString(item, Tag.RequestedProcedureID),
                matchUnknown, sequencePredicates, params);
        wildCard(cb, root.get(RequestAttributes_.scheduledProcedureStepID),
                filter.getString(item, Tag.ScheduledProcedureStepID),
                matchUnknown, sequencePredicates, params);
        wildCard(cb, root.get(RequestAttributes_.requestingService),
                filter.getString(item, Tag.RequestingService),
                matchUnknown, sequencePredicates, params);
        PersonNameMatching.personName(cb,
                root.get(RequestAttributes_.requestingPhysician),
                root.get(RequestAttributes_.requestingPhysicianIdeographicName),
                root.get(RequestAttributes_.requestingPhysicianPhoneticName),
                root.get(RequestAttributes_.requestingPhysicianFamilyNameSoundex),
                root.get(RequestAttributes_.requestingPhysicianGivenNameSoundex),
                filter.getString(item, Tag.ReferringPhysicianName), filter,
                queryOpts, matchUnknown, sequencePredicates, params);
        listOfUID(cb, root.get(RequestAttributes_.studyInstanceUID),
                item.getStrings(Tag.StudyInstanceUID), sequencePredicates, params);
        String accNo = filter.getString(item, Tag.AccessionNumber);
        wildCard(cb, root.get(RequestAttributes_.accessionNumber),
                accNo, matchUnknown, sequencePredicates, params);
        if (!accNo.equals("*"))
            withIssuer(cb, cq,
                    root.get(RequestAttributes_.issuerOfAccessionNumber),
                    item.getNestedDataset(Tag.IssuerOfAccessionNumberSequence),
                    filter, matchUnknown, sequencePredicates, params);
        return sequencePredicates;
    }

    private static <T> Predicate matchUnknownCollection(CriteriaBuilder cb,
            Expression<Collection<T>> collection,
            boolean matchUnknown, Subquery<T> sq) {
        return matchUnknown ? cb.or(cb.exists(sq), cb.isEmpty(collection)) : cb.exists(sq);
    }

    private static <T> Predicate matchUnknownPath(CriteriaBuilder cb,
            Path<T> path, boolean matchUnknown, Subquery<T> sq) {
        return matchUnknown ? cb.or(cb.exists(sq), cb.isNull(path)) : cb.exists(sq);
    }

    private static Predicate matchUnknown(CriteriaBuilder cb, Path<String> field,
            boolean matchUnknown, Predicate predicate) {
        return matchUnknown
            ? cb.or(predicate, cb.equal(field, "*"))
            : predicate;
    }

    static void patientID(CriteriaBuilder cb, Path<Patient> pat, String[] pids,
            boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        patientID(cb, pat.get(Patient_.patientID), pat.get(Patient_.issuerOfPatientID), 
                pids, matchUnknown, predicates, params);
    }


    private static void patientID(CriteriaBuilder cb, Path<String> idField, Path<String> issuerField, String[] pids, 
            boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        if (pids == null || pids.length == 0)
            return;

        if (pids.length == 1)
            singlePatientID(cb, idField, issuerField, pids[0], pids[1], matchUnknown, predicates, params);
        else
            listOfPatientIDs(cb, idField, issuerField, pids, matchUnknown, predicates, params);
    }

    private static void listOfPatientIDs(CriteriaBuilder cb,
            Path<String> idField, Path<String> issuerField, String[] pids,
            boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        ArrayList<Predicate> pidPredicates = new ArrayList<Predicate>(pids.length);
        for (int i = 0, j = 0; i < pids.length-1; i++, j++, j++)
            singlePatientID(cb, idField, issuerField, pids[j], pids[j + 1], matchUnknown, pidPredicates, params);
        predicates.add(cb.or(pidPredicates.toArray(new Predicate[pidPredicates.size()])));
    }

    private static void singlePatientID(CriteriaBuilder cb,
            Path<String> idField, Path<String> issuerField, String id, String issuer, 
            boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        if (issuer == null)
            wildCard(cb, idField, id, matchUnknown, predicates, params);
        else {
            ArrayList<Predicate> pidPredicates = new ArrayList<Predicate>(2);
            wildCard(cb, idField, id, matchUnknown, pidPredicates, params);
            wildCard(cb, issuerField, issuer, matchUnknown, pidPredicates, params);
            predicates.add(cb.and(pidPredicates.toArray(new Predicate[pidPredicates.size()])));
        }
    }

    public static void patient(CriteriaBuilder cb, Path<Patient> pat,
            String[] pids, Attributes keys, AttributeFilter filter, EnumSet<QueryOption> queryOpts,
            List<Predicate> predicates, List<Object> params) {
        patientID(cb, pat, pids, filter.isMatchUnknown(), predicates, params);
        if (keys == null)
            return;

        PersonNameMatching.personName(cb,
                pat.get(Patient_.patientName),
                pat.get(Patient_.patientIdeographicName),
                pat.get(Patient_.patientPhoneticName),
                pat.get(Patient_.patientFamilyNameSoundex),
                pat.get(Patient_.patientGivenNameSoundex),
                filter.getString(keys, Tag.PatientName), filter,
                queryOpts, filter.isMatchUnknown(), predicates, params);
        wildCard(cb, pat.get(Patient_.patientSex),
                filter.getString(keys, Tag.PatientSex), filter.isMatchUnknown(),
                predicates, params);
        RangeMatching.rangeMatch(cb, pat.get(Patient_.patientBirthDate),
                Tag.PatientBirthDate, RangeMatching.FormatDate.DA, keys, 
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, pat.get(Patient_.patientCustomAttribute1),
                filter.selectPatientCustomAttribute1(keys),
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, pat.get(Patient_.patientCustomAttribute2),
                filter.selectPatientCustomAttribute2(keys),
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, pat.get(Patient_.patientCustomAttribute3),
                filter.selectPatientCustomAttribute3(keys),
                filter.isMatchUnknown(), predicates, params);
    }

    public static void study(CriteriaBuilder cb, CriteriaQuery<Tuple> cq,
            Path<Patient> pat, Path<Study> study, String[] pids, Attributes keys,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts,
            String[] roles, List<Predicate> predicates, List<Object> params) {
        patient(cb, pat, pids, keys, filter, queryOpts, predicates, params);
        if (keys == null)
            return;

        listOfUID(cb, study.get(Study_.studyInstanceUID), keys
                .getStrings(Tag.StudyInstanceUID), predicates, params);
        wildCard(cb, study.get(Study_.studyID),
                filter.getString(keys, Tag.StudyID), filter.isMatchUnknown(), predicates, params);
        PersonNameMatching.personName(cb,
                study.get(Study_.referringPhysicianName),
                study.get(Study_.referringPhysicianIdeographicName),
                study.get(Study_.referringPhysicianPhoneticName),
                study.get(Study_.referringPhysicianFamilyNameSoundex),
                study.get(Study_.referringPhysicianGivenNameSoundex),
                filter.getString(keys, Tag.ReferringPhysicianName), filter,
                queryOpts, filter.isMatchUnknown(), predicates, params);
        RangeMatching.rangeMatch(cb,
                study.get(Study_.studyDate), study.get(Study_.studyTime),
                Tag.StudyDate, Tag.StudyTime, Tag.StudyDateAndTime,
                keys, queryOpts, filter.isMatchUnknown(), predicates, params);
        wildCard(cb, study.get(Study_.studyDescription), 
                filter.getString(keys, Tag.StudyDescription),
                filter.isMatchUnknown(), predicates, params);
        String accNo = filter.getString(keys, Tag.AccessionNumber);
        wildCard(cb, study.get(Study_.accessionNumber), 
                accNo, filter.isMatchUnknown(), predicates, params);
        if(!accNo.equals("*"))
            withIssuer(cb, cq,
                    study.get(Study_.issuerOfAccessionNumber),
                    keys.getNestedDataset(Tag.IssuerOfAccessionNumberSequence),
                    filter, filter.isMatchUnknown(), predicates, params);
        modalitiesInStudy(cb, cq, study,
                filter.getString(keys, Tag.ModalitiesInStudy),
                filter.isMatchUnknown(), predicates, params);
        withCode(cb, cq, study.get(Study_.procedureCodes),
                keys.getNestedDataset(Tag.ProcedureCodeSequence),
                filter, filter.isMatchUnknown(), predicates, params);
        wildCard(cb, study.get(Study_.studyCustomAttribute1),
                filter.selectStudyCustomAttribute1(keys),
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, study.get(Study_.studyCustomAttribute2),
                filter.selectStudyCustomAttribute2(keys),
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, study.get(Study_.studyCustomAttribute3),
                filter.selectStudyCustomAttribute3(keys),
                filter.isMatchUnknown(), predicates, params);
        checkPermission(cb, cq, study, roles, Action.QUERY, predicates, params);
    }

    public static void series(CriteriaBuilder cb, CriteriaQuery<Tuple> cq,
            Path<Patient> pat, Join<Series, Study> study, Path<Series> series,
            String[] pids, Attributes keys, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, String[] roles, 
            List<Predicate> predicates, List<Object> params) {
        study(cb, cq, pat, study, pids, keys, filter, queryOpts, roles,
                predicates, params);
        if (keys == null)
            return;

        listOfUID(cb, series.get(Series_.seriesInstanceUID),
                keys.getStrings(Tag.SeriesInstanceUID), predicates, params);
        wildCard(cb, series.get(Series_.modality), filter.getString(keys, Tag.Modality), 
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, series
                .get(Series_.performedProcedureStepInstanceUID),
                filter.getString(keys, Tag.PerformedProcedureStepID),
                filter.isMatchUnknown(), predicates, params);
        RangeMatching.rangeMatch(cb, series
                .get(Series_.performedProcedureStepStartDate), series
                .get(Series_.performedProcedureStepStartTime),
                Tag.PerformedProcedureStepStartDate,
                Tag.PerformedProcedureStepStartTime,
                Tag.PerformedProcedureStepStartDateAndTime, keys, queryOpts,
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, series.get(Series_.seriesNumber),
                filter.getString(keys, Tag.SeriesNumber),
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, series.get(Series_.seriesDescription),
                filter.getString(keys, Tag.SeriesDescription),
                filter.isMatchUnknown(), predicates, params);
        requestAttributesSequence(cb, cq, keys,
                series.get(Series_.requestAttributes),
                keys.getNestedDataset(Tag.RequestAttributesSequence), filter, queryOpts, 
                filter.isMatchUnknown(), predicates, params);
        withCode(cb, cq, 
                series.get(Series_.institutionCode),
                keys.getNestedDataset(Tag.InstitutionCodeSequence), filter,
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, series.get(Series_.seriesCustomAttribute1),
                filter.selectSeriesCustomAttribute1(keys),
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, series.get(Series_.seriesCustomAttribute2),
                filter.selectSeriesCustomAttribute2(keys),
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, series.get(Series_.seriesCustomAttribute3),
                filter.selectSeriesCustomAttribute3(keys),
                filter.isMatchUnknown(), predicates, params);
    }

    public static void instance(CriteriaBuilder cb, CriteriaQuery<Tuple> cq,
            Path<Patient> pat, Join<Series, Study> study, Path<Series> series,
            Root<Instance> inst, String[] pids, Attributes keys, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, String[] roles,
            List<Predicate> predicates, List<Object> params) {
        series(cb, cq, pat, study, series, pids, keys, filter, queryOpts, roles,
                predicates, params);
        if (keys == null)
            return;

        listOfUID(cb, inst.get(Instance_.sopInstanceUID), 
                keys.getStrings(Tag.SOPInstanceUID), 
                predicates, params);
        wildCard(cb, inst.get(Instance_.instanceNumber),
                filter.getString(keys, Tag.InstanceNumber),
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, inst.get(Instance_.verificationFlag),
                filter.getString(keys, Tag.VerificationFlag),
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, inst.get(Instance_.sopClassUID),
                filter.getString(keys, Tag.SOPClassUID), 
                filter.isMatchUnknown(), predicates, params);
        withCode(cb, cq, 
                inst.get(Instance_.conceptNameCode),
                keys.getNestedDataset(Tag.ConceptNameCodeSequence), filter,
                filter.isMatchUnknown(), predicates, params);
        withObserver(cb, cq, 
                inst.get(Instance_.verifyingObservers),
                keys.getNestedDataset(Tag.VerifyingObserverSequence),
                filter, queryOpts, filter.isMatchUnknown(), predicates, params);
        Sequence contentSeq = keys.getSequence(Tag.ContentSequence);
        if (contentSeq != null)
            for (Attributes item : contentSeq)
                ContentItemMatching.withContentItem(cb, cq, 
                        inst.get(Instance_.contentItems), item, filter,
                        item.getString(Tag.ValueType, null), predicates, params);
        wildCard(cb, inst.get(Instance_.instanceCustomAttribute1),
                filter.selectInstanceCustomAttribute1(keys),
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, inst.get(Instance_.instanceCustomAttribute2),
                filter.selectInstanceCustomAttribute2(keys),
                filter.isMatchUnknown(), predicates, params);
        wildCard(cb, inst.get(Instance_.instanceCustomAttribute3),
                filter.selectInstanceCustomAttribute3(keys),
                filter.isMatchUnknown(), predicates, params);
        RangeMatching.rangeMatch(cb, inst.get(Instance_.contentDate), 
                inst.get(Instance_.contentTime), Tag.ContentDate, Tag.ContentTime, 
                Tag.ContentDateAndTime, keys, queryOpts, 
                filter.isMatchUnknown(), predicates, params);
    }

    static ParameterExpression<String> setParam(CriteriaBuilder cb,
            String value, List<Object> params) {
        ParameterExpression<String> paramExp =
                cb.parameter(String.class, paramName(params.size()));
        params.add(value);
        return paramExp;
    }
}