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
import org.dcm4che.data.PersonName;
import org.dcm4che.data.Sequence;
import org.dcm4che.data.Tag;
import org.dcm4che.net.pdu.QueryOption;
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
            Path<String> alphabethic,
            Path<String> ideographic,
            Path<String> phonetic,
            Path<String> familyNameSoundex,
            Path<String> givenNameSoundex,
            String value, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, boolean matchUnknown,
            List<Object> params) {
        if (value.equals("*"))
            return null;

        return queryOpts.contains(QueryOption.FUZZY)
                ? fuzzyPersonName(cb, familyNameSoundex, givenNameSoundex,
                        value, filter, matchUnknown, params)
                : literalPersonName(cb, alphabethic, ideographic, phonetic,
                        value, matchUnknown, params);
    }

    private static Predicate fuzzyPersonName(CriteriaBuilder cb,
            Path<String> familyNameSoundex, Path<String> givenNameSoundex,
            String value, AttributeFilter filter, boolean matchUnknown, List<Object> params) {
        PersonName pn = new PersonName(value);
        boolean containsFamilyName = !pn.isEmpty(PersonName.Component.FamilyName);
        boolean containsGivenName = !pn.isEmpty(PersonName.Component.GivenName);
        if (containsFamilyName && containsGivenName)
            return fuzzyNames(cb, filter, familyNameSoundex, givenNameSoundex, 
                    pn.get(PersonName.Component.FamilyName), pn.get(PersonName.Component.GivenName), 
                    matchUnknown, params);
        else if (containsGivenName)
            return fuzzyName(cb, filter, familyNameSoundex, givenNameSoundex, 
                    pn.get(PersonName.Component.GivenName), matchUnknown, params);
        else if (containsFamilyName)
            return fuzzyName(cb, filter, familyNameSoundex, givenNameSoundex, 
                    pn.get(PersonName.Component.FamilyName), matchUnknown, params);
        return null;
    }

    private static Predicate fuzzyNames(CriteriaBuilder cb, AttributeFilter filter, 
            Path<String> familyNameSoundex, Path<String> givenNameSoundex,
            String givenName, String familyName, 
            boolean matchUnknown, List<Object> params) {
        String fuzzyFamilyName = filter.toFuzzy(familyName);
        String fuzzyGivenName = filter.toFuzzy(givenName);
        Predicate names = 
            cb.and(fuzzyPersonNameWildCard(cb, givenNameSoundex, givenName, fuzzyGivenName, params),
                   fuzzyPersonNameWildCard(cb, familyNameSoundex, familyName, fuzzyFamilyName, params));
        Predicate namesSwap = 
            cb.and(fuzzyPersonNameWildCard(cb, givenNameSoundex, familyName, fuzzyFamilyName, params),
                   fuzzyPersonNameWildCard(cb, familyNameSoundex, givenName, fuzzyGivenName, params));
        return matchUnknown
                    ? unknownFuzzyNames(cb, familyNameSoundex, givenNameSoundex, familyName, 
                            givenName, fuzzyFamilyName, fuzzyGivenName, names, namesSwap, params)
                    : cb.or(names, namesSwap);
    }
    
    private static Predicate unknownFuzzyNames(CriteriaBuilder cb, 
            Path<String> familyNameSoundex, Path<String> givenNameSoundex, 
            String familyName, String givenName, 
            String fuzzyFamilyName, String fuzzyGivenName,
            Predicate names, Predicate namesSwap, 
            List<Object> params){
        ArrayList<Predicate> predicates = new ArrayList<Predicate>(7);
        predicates.add(names);
        predicates.add(namesSwap);
        predicates.add(cb.and(fuzzyPersonNameWildCard(cb, givenNameSoundex, givenName, 
                fuzzyGivenName, params), cb.equal(familyNameSoundex, "*")));
        predicates.add(cb.and(fuzzyPersonNameWildCard(cb, familyNameSoundex, familyName, 
                fuzzyFamilyName, params), cb.equal(givenNameSoundex, "*")));
        predicates.add(cb.and(fuzzyPersonNameWildCard(cb, givenNameSoundex, familyName, 
                fuzzyFamilyName, params), cb.equal(familyNameSoundex, "*")));
        predicates.add(cb.and(fuzzyPersonNameWildCard(cb, familyNameSoundex, givenName, 
                fuzzyGivenName, params), cb.equal(givenNameSoundex, "*")));
        predicates.add(cb.and(cb.equal(givenNameSoundex, "*"), 
                cb.equal(familyNameSoundex, "*")));
        return cb.or(predicates.toArray(new Predicate[predicates.size()]));
    }

    private static Predicate fuzzyName(CriteriaBuilder cb, AttributeFilter filter, 
            Path<String> familyNameSoundex, Path<String> givenNameSoundex,
            String name, boolean matchUnknown, List<Object> params) {
        String fuzzyName = filter.toFuzzy(name);
        Predicate predicate = cb.or(
                fuzzyPersonNameWildCard(cb, familyNameSoundex, name, fuzzyName, params),
                fuzzyPersonNameWildCard(cb, givenNameSoundex, name, fuzzyName, params));
        return matchUnknown 
                    ? cb.or(predicate, cb.and(cb.equal(givenNameSoundex, "*"), 
                            cb.equal(familyNameSoundex, "*")))
                    : predicate;
    }

    private static Predicate fuzzyPersonNameWildCard(CriteriaBuilder cb, 
            Path<String> field, String name, String fuzzy, List<Object> params) {
        if (name.endsWith("*"))
            return likeValue(cb, field, fuzzy, params);
        return singleValue0(cb, field, fuzzy, params);
    }

    private static Predicate likeValue(CriteriaBuilder cb, Path<String> field,
            String value, List<Object> params) {
        String pattern = value.concat("%");
        ParameterExpression<String> param = setParam(cb, params, pattern);
        return cb.like(field, param);
    }

    private static Predicate literalPersonName(CriteriaBuilder cb,
            Path<String> alphabethic, Path<String> ideographic,
            Path<String> phonetic, String value, boolean matchUnknown,
            List<Object> params) {
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

        return matchUnknown0(cb, field, matchUnknown, wildCard0(cb, field, value, params));
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
            Attributes item, AttributeFilter filter, boolean matchUnknown, List<Object> params) {
        if (item == null || item.isEmpty())
            return null;

        Subquery<Code> sq = cq.subquery(Code.class);
        Root<Code> root = sq.from(Code.class);
        sq.select(root);
        ArrayList<Predicate> predicates = new ArrayList<Predicate>(4);
        predicates.add(cb.isMember(root, collection));
        if (!addCodePredicates(cb, item, filter, params, root, predicates))
            return null;

        sq.where(predicates.toArray(new Predicate[predicates.size()]));
        return matchUnknownCollection(cb, collection, matchUnknown, sq);
    }

    static Predicate withCode(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq, Path<Code> path, Attributes item, AttributeFilter filter,
            boolean matchUnknown, List<Object> params) {
        if (item == null || item.isEmpty())
            return null;

        Subquery<Code> sq = cq.subquery(Code.class);
        Root<Code> root = sq.from(Code.class);
        sq.select(root);
        ArrayList<Predicate> predicates = new ArrayList<Predicate>(4);
        predicates.add(cb.equal(root, path));
        if (!addCodePredicates(cb, item, filter, params, root, predicates))
            return null;

        sq.where(predicates.toArray(new Predicate[predicates.size()]));
        return matchUnknownPath(cb, path, matchUnknown, sq);
    }

    private static boolean addCodePredicates(CriteriaBuilder cb, Attributes item,
            AttributeFilter filter, List<Object> params, Root<Code> root,
            ArrayList<Predicate> predicates) {
        boolean restrict = add(predicates,
                wildCard(cb, root.get(Code_.codeValue),
                        filter.getString(item, Tag.CodeValue),
                        false, params));
        restrict = add(predicates,
                wildCard(cb, root.get(Code_.codingSchemeDesignator),
                        filter.getString(item, Tag.CodingSchemeDesignator),
                        false, params))
                || restrict;
        restrict = add(predicates,
                wildCard(cb, root.get(Code_.codingSchemeVersion),
                        filter.getString(item, Tag.CodingSchemeVersion),
                        false, params))
                || restrict;
        return restrict;
    }

    private static Predicate withIssuer(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq, Path<Issuer> issuer,
            Attributes item, AttributeFilter filter, boolean matchUnknown, List<Object> params) {
        if (item == null || item.isEmpty())
            return null;

        Subquery<Issuer> sq = cq.subquery(Issuer.class);
        Root<Issuer> root = sq.from(Issuer.class);
        sq.select(root);
        ArrayList<Predicate> predicates = new ArrayList<Predicate>(4);
        predicates.add(cb.equal(root, issuer));
        boolean restrict = add(predicates,
                wildCard(cb, root.get(Issuer_.entityID),
                        filter.getString(item, Tag.LocalNamespaceEntityID),
                        false, params));
        restrict = add(predicates,
                wildCard(cb, root.get(Issuer_.entityUID),
                        filter.getString(item, Tag.UniversalEntityID),
                        false, params))
                || restrict;
        restrict = add(predicates,
                wildCard(cb, root.get(Issuer_.entityUIDType),
                        filter.getString(item, Tag.UniversalEntityIDType),
                        false, params))
                || restrict;
        if (!restrict)
            return null;

        sq.where(predicates.toArray(new Predicate[predicates.size()]));
        return matchUnknownPath(cb, issuer, matchUnknown, sq);
    }

    private static Predicate withObserver(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq, Expression<Collection<VerifyingObserver>> collection,
            Attributes item, AttributeFilter filter, EnumSet<QueryOption> queryOpts,
            boolean matchUnknown, List<Object> params) {
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
                    root.get(VerifyingObserver_.verifyingObserverFamilyNameSoundex),
                    root.get(VerifyingObserver_.verifyingObserverGivenNameSoundex),
                    filter.getString(item, Tag.VerifyingObserverName),
                    filter,queryOpts, false, params))
                || restrict;
        if (!restrict)
            return null;

        sq.where(predicates.toArray(new Predicate[predicates.size()]));
        return matchUnknownCollection(cb, collection, matchUnknown, sq);
    }

    private static Predicate requestAttributesSequence(CriteriaBuilder cb,
            CriteriaQuery<Tuple> cq,
            Expression<Collection<RequestAttributes>> collection,
            Attributes item, AttributeFilter filter, EnumSet<QueryOption> queryOpts,
            boolean matchUnknown, List<Object> params) {
        if (item == null || item.isEmpty())
            return null;

        Subquery<RequestAttributes> sq = cq.subquery(RequestAttributes.class);
        Root<RequestAttributes> root = sq.from(RequestAttributes.class);
        sq.select(root);
        ArrayList<Predicate> predicates = new ArrayList<Predicate>(4);
        predicates.add(cb.isMember(root, collection));
        boolean restrict = add(predicates, 
                wildCard(cb, root.get(RequestAttributes_.requestedProcedureID),
                        filter.getString(item, Tag.RequestedProcedureID),
                        matchUnknown, params));
        restrict = add(predicates,
                wildCard(cb, root.get(RequestAttributes_.scheduledProcedureStepID),
                        filter.getString(item, Tag.ScheduledProcedureStepID),
                        matchUnknown, params))
                || restrict;
        restrict = add(predicates,
                wildCard(cb, root.get(RequestAttributes_.requestingService),
                        filter.getString(item, Tag.RequestingService),
                        matchUnknown, params))
                || restrict;
        restrict = add(predicates,
                personName(cb,
                    root.get(RequestAttributes_.requestingPhysician),
                    root.get(RequestAttributes_.requestingPhysicianIdeographicName),
                    root.get(RequestAttributes_.requestingPhysicianPhoneticName),
                    root.get(RequestAttributes_.requestingPhysicianFamilyNameSoundex),
                    root.get(RequestAttributes_.requestingPhysicianGivenNameSoundex),
                    filter.getString(item, Tag.ReferringPhysicianName), filter,
                    queryOpts, matchUnknown, params))
                || restrict;
        restrict = add(predicates,
                listOfUID(cb, root.get(RequestAttributes_.studyInstanceUID),
                        item.getStrings(Tag.StudyInstanceUID), params))
                || restrict;
        if (add(predicates, wildCard(cb, root.get(RequestAttributes_.accessionNumber),
                filter.getString(item, Tag.AccessionNumber),
                matchUnknown, params))) {
            add(predicates, withIssuer(cb, cq,
                    root.get(RequestAttributes_.issuerOfAccessionNumber),
                    item.getNestedDataset(Tag.IssuerOfAccessionNumberSequence),
                    filter, matchUnknown, params));
            restrict = true;
        }

        if (!restrict)
            return null;

        sq.where(predicates.toArray(new Predicate[predicates.size()]));
        return matchUnknownCollection(cb, collection, matchUnknown, sq);
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
        return issuer == null ? 
                predicate: 
                cb.and(predicate, wildCard0(cb, issuerField, issuer, params));
    }

    public static void patient(CriteriaBuilder cb, Path<Patient> pat,
            String[] pids, Attributes keys, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, boolean matchUnknown,
            List<Predicate> predicates, List<Object> params) {
        add(predicates, patientID(cb, pat.get(Patient_.patientID), pat
                .get(Patient_.issuerOfPatientID), pids, matchUnknown, params));
        if (keys == null)
            return;

        add(predicates, personName(cb, 
                pat.get(Patient_.patientName),
                pat.get(Patient_.patientIdeographicName),
                pat.get(Patient_.patientPhoneticName),
                pat.get(Patient_.patientFamilyNameSoundex),
                pat.get(Patient_.patientGivenNameSoundex),
                filter.getString(keys, Tag.PatientName), filter,
                queryOpts, matchUnknown, params));
        add(predicates, wildCard(cb, pat.get(Patient_.patientSex),
                filter.getString(keys, Tag.PatientSex), matchUnknown,
                params));
        add(predicates, RangeMatching.rangeMatch(cb, pat.get(Patient_.patientBirthDate),
                Tag.PatientBirthDate, RangeMatching.FormatDate.DA, keys, matchUnknown, params));
        add(predicates, wildCard(cb, pat.get(Patient_.patientCustomAttribute1),
                filter.selectPatientCustomAttribute1(keys),
                matchUnknown, params));
        add(predicates, wildCard(cb, pat.get(Patient_.patientCustomAttribute2),
                filter.selectPatientCustomAttribute2(keys),
                matchUnknown, params));
        add(predicates, wildCard(cb, pat.get(Patient_.patientCustomAttribute3),
                filter.selectPatientCustomAttribute3(keys),
                matchUnknown, params));
    }

    public static void study(CriteriaBuilder cb, CriteriaQuery<Tuple> cq,
            Path<Patient> pat, Path<Study> study, String[] pids, Attributes keys,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts, boolean matchUnknown,
            List<Predicate> predicates, List<Object> params) {
        patient(cb, pat, pids, keys, filter, queryOpts, matchUnknown, predicates, params);
        if (keys == null)
            return;

        add(predicates, listOfUID(cb, study.get(Study_.studyInstanceUID), keys
                .getStrings(Tag.StudyInstanceUID), params));
        add(predicates, wildCard(cb, study.get(Study_.studyID),
                filter.getString(keys, Tag.StudyID), matchUnknown, params));
        add(predicates, personName(cb,
                study.get(Study_.referringPhysicianName),
                study.get(Study_.referringPhysicianIdeographicName),
                study.get(Study_.referringPhysicianPhoneticName),
                study.get(Study_.referringPhysicianFamilyNameSoundex),
                study.get(Study_.referringPhysicianGivenNameSoundex),
                filter.getString(keys, Tag.ReferringPhysicianName), filter,
                queryOpts, matchUnknown, params));
        RangeMatching.rangeMatch(cb,
                study.get(Study_.studyDate),
                study.get(Study_.studyTime),
                Tag.StudyDate, Tag.StudyTime, Tag.StudyDateAndTime,
                keys, queryOpts, matchUnknown,
                predicates, params);
        add(predicates, wildCard(cb, study.get(Study_.studyDescription),
                filter.getString(keys, Tag.StudyDescription),
                matchUnknown, params));
        if (add(predicates, wildCard(cb, study.get(Study_.accessionNumber),
                filter.getString(keys, Tag.AccessionNumber),
                matchUnknown, params))) {
            add(predicates, withIssuer(cb, cq,
                    study.get(Study_.issuerOfAccessionNumber),
                    keys.getNestedDataset(Tag.IssuerOfAccessionNumberSequence),
                    filter, matchUnknown, params));
        }
        add(predicates, modalitiesInStudy(cb, cq, study,
                filter.getString(keys, Tag.ModalitiesInStudy),
                matchUnknown, params));
        add(predicates, withCode(cb, cq, study.get(Study_.procedureCodes),
                keys.getNestedDataset(Tag.ProcedureCodeSequence),
                filter, matchUnknown, params));
        add(predicates, wildCard(cb, study.get(Study_.studyCustomAttribute1),
                filter.selectStudyCustomAttribute1(keys),
                matchUnknown, params));
        add(predicates, wildCard(cb, study.get(Study_.studyCustomAttribute2),
                filter.selectStudyCustomAttribute2(keys),
                matchUnknown, params));
        add(predicates, wildCard(cb, study.get(Study_.studyCustomAttribute3),
                filter.selectStudyCustomAttribute3(keys),
                matchUnknown, params));
    }

    public static void series(CriteriaBuilder cb, CriteriaQuery<Tuple> cq,
            Path<Patient> pat, Join<Series, Study> study, Path<Series> series,
            String[] pids, Attributes keys, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, boolean matchUnknown,
            List<Predicate> predicates, List<Object> params) {
        study(cb, cq, pat, study, pids, keys, filter, queryOpts, matchUnknown,
                predicates, params);
        if (keys == null)
            return;

        add(predicates, listOfUID(cb, series.get(Series_.seriesInstanceUID),
                keys.getStrings(Tag.SeriesInstanceUID), params));
        add(predicates, wildCard(cb, series.get(Series_.modality),
                filter.getString(keys, Tag.Modality), matchUnknown,
                params));
        add(predicates, wildCard(cb, series
                .get(Series_.performedProcedureStepInstanceUID),
                filter.getString(keys, Tag.PerformedProcedureStepID),
                matchUnknown, params));
        RangeMatching.rangeMatch(cb, series
                .get(Series_.performedProcedureStepStartDate), series
                .get(Series_.performedProcedureStepStartTime),
                Tag.PerformedProcedureStepStartDate,
                Tag.PerformedProcedureStepStartTime,
                Tag.PerformedProcedureStepStartDateAndTime, keys, queryOpts,
                matchUnknown, predicates, params);
        add(predicates, wildCard(cb, series.get(Series_.seriesNumber),
                filter.getString(keys, Tag.SeriesNumber),
                matchUnknown, params));
        add(predicates, wildCard(cb, series.get(Series_.seriesDescription),
                filter.getString(keys, Tag.SeriesDescription),
                matchUnknown, params));
        add(predicates, requestAttributesSequence(cb, cq,
                series.get(Series_.requestAttributes),
                keys.getNestedDataset(Tag.RequestAttributesSequence), filter,
                queryOpts, matchUnknown, params));
        add(predicates, withCode(cb, cq, 
                series.get(Series_.institutionCode),
                keys.getNestedDataset(Tag.InstitutionCodeSequence), filter,
                matchUnknown, params));
        add(predicates, wildCard(cb, series.get(Series_.seriesCustomAttribute1),
                filter.selectSeriesCustomAttribute1(keys),
                matchUnknown, params));
        add(predicates, wildCard(cb, series.get(Series_.seriesCustomAttribute2),
                filter.selectSeriesCustomAttribute2(keys),
                matchUnknown, params));
        add(predicates, wildCard(cb, series.get(Series_.seriesCustomAttribute3),
                filter.selectSeriesCustomAttribute3(keys),
                matchUnknown, params));
    }

    public static void instance(CriteriaBuilder cb, CriteriaQuery<Tuple> cq,
            Path<Patient> pat, Join<Series, Study> study, Path<Series> series,
            Root<Instance> inst, String[] pids, Attributes keys, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, boolean matchUnknown,
            List<Predicate> predicates, List<Object> params) {
        series(cb, cq, pat, study, series, pids, keys, filter, queryOpts,
                matchUnknown, predicates, params);
        if (keys == null)
            return;

        add(predicates, listOfUID(cb, inst.get(Instance_.sopInstanceUID), 
                keys.getStrings(Tag.SOPInstanceUID), params));
        add(predicates, wildCard(cb, inst.get(Instance_.instanceNumber),
                filter.getString(keys, Tag.InstanceNumber),
                matchUnknown, params));
        add(predicates, wildCard(cb, inst.get(Instance_.verificationFlag),
                filter.getString(keys, Tag.VerificationFlag),
                matchUnknown, params));
        add(predicates, wildCard(cb, inst.get(Instance_.sopClassUID),
                filter.getString(keys, Tag.SOPClassUID), matchUnknown,
                params));
        add(predicates, withCode(cb, cq, inst.get(Instance_.conceptNameCode),
                keys.getNestedDataset(Tag.ConceptNameCodeSequence), filter,
                matchUnknown, params));
        add(predicates, withObserver(cb, cq, 
                inst.get(Instance_.verifyingObservers),
                keys.getNestedDataset(Tag.VerifyingObserverSequence),
                filter, queryOpts, matchUnknown, params));
        Sequence contentSeq = keys.getSequence(Tag.ContentSequence);
        if (contentSeq != null)
            for (Attributes item : contentSeq) {
                add(predicates, ContentItemMatching.withContentItem(cb, cq, 
                        inst.get(Instance_.contentItems), item, filter,
                        item.getString(Tag.ValueType, null), params));
            }
        add(predicates, wildCard(cb, inst.get(Instance_.instanceCustomAttribute1),
                filter.selectInstanceCustomAttribute1(keys),
                matchUnknown, params));
        add(predicates, wildCard(cb, inst.get(Instance_.instanceCustomAttribute2),
                filter.selectInstanceCustomAttribute2(keys),
                matchUnknown, params));
        add(predicates, wildCard(cb, inst.get(Instance_.instanceCustomAttribute3),
                filter.selectInstanceCustomAttribute3(keys),
                matchUnknown, params));
    }

    static ParameterExpression<String> setParam(CriteriaBuilder cb,
            List<Object> params, String value) {
        ParameterExpression<String> paramExp =
                cb.parameter(String.class, paramName(params.size()));
        params.add(value);
        return paramExp;
    }
}