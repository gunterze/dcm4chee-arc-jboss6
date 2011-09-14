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
import java.util.EnumSet;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.dcm4che.data.PersonName;
import org.dcm4che.data.PersonName.Group;
import org.dcm4che.net.pdu.QueryOption;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Property;

/**
 * @author Michael Backhaus <michael.backhaus@agfa.com>
 */
abstract class PersonNameMatching {

    static void addMatch(Property alpha, Property ideographic, Property phonetic,
            Property fnsoundex, Property gnsoundex, String value,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts,
            boolean matchUnknown, Conjunction predicates) {
        // TODO Auto-generated method stub
        
    }

    static void personName(CriteriaBuilder cb, 
            Path<String> alphabethic,
            Path<String> ideographic,
            Path<String> phonetic,
            Path<String> familyNameSoundex,
            Path<String> givenNameSoundex,
            String value, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, boolean matchUnknown,
            List<Predicate> predicates, List<Object> params) {
        if (value.equals("*"))
            return;
    
        if (queryOpts.contains(QueryOption.FUZZY))
            PersonNameMatching.fuzzyPersonName(cb, 
                    familyNameSoundex, givenNameSoundex, value, filter, 
                    matchUnknown, predicates, params);
        else
            PersonNameMatching.literalPersonName(cb, 
                    alphabethic, ideographic, phonetic, value, 
                    matchUnknown, predicates, params);
    }

    static private void fuzzyPersonName(CriteriaBuilder cb,
            Path<String> familyNameSoundex, Path<String> givenNameSoundex,
            String value, AttributeFilter filter, 
            boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        PersonName pn = new PersonName(value);
        boolean containsFamilyName = pn.containst(PersonName.Component.FamilyName);
        boolean containsGivenName = pn.containst(PersonName.Component.GivenName);
        if (containsFamilyName && containsGivenName)
            PersonNameMatching.fuzzyNames(cb, filter, familyNameSoundex, givenNameSoundex, 
                    pn.get(PersonName.Component.FamilyName), pn.get(PersonName.Component.GivenName), 
                    matchUnknown, predicates, params);
        else if (containsGivenName)
            PersonNameMatching.fuzzyName(cb, filter, familyNameSoundex, givenNameSoundex, 
                    pn.get(PersonName.Component.GivenName), matchUnknown, predicates, params);
        else if (containsFamilyName)
            PersonNameMatching.fuzzyName(cb, filter, familyNameSoundex, givenNameSoundex, 
                    pn.get(PersonName.Component.FamilyName), matchUnknown, predicates, params);
    }

    static private void fuzzyNames(CriteriaBuilder cb, AttributeFilter filter, 
            Path<String> familyNameSoundex, Path<String> givenNameSoundex,
            String givenName, String familyName, 
            boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        String fuzzyFamilyName = filter.toFuzzy(familyName);
        String fuzzyGivenName = filter.toFuzzy(givenName);
        Predicate names = 
            cb.and(PersonNameMatching.fuzzyNameWildCard(cb, givenNameSoundex, givenName, fuzzyGivenName, params),
                   PersonNameMatching.fuzzyNameWildCard(cb, familyNameSoundex, familyName, fuzzyFamilyName, params));
        Predicate namesSwap = 
            cb.and(PersonNameMatching.fuzzyNameWildCard(cb, givenNameSoundex, familyName, fuzzyFamilyName, params),
                   PersonNameMatching.fuzzyNameWildCard(cb, familyNameSoundex, givenName, fuzzyGivenName, params));
        if (matchUnknown)
            PersonNameMatching.unknownFuzzyNames(cb, familyNameSoundex, givenNameSoundex, familyName, 
                    givenName, fuzzyFamilyName, fuzzyGivenName, names, namesSwap, predicates, params);
        else
            predicates.add(cb.or(names, namesSwap));
    }

    static private void unknownFuzzyNames(CriteriaBuilder cb, 
            Path<String> familyNameSoundex, Path<String> givenNameSoundex, 
            String familyName, String givenName, 
            String fuzzyFamilyName, String fuzzyGivenName,
            Predicate names, Predicate namesSwap, 
            List<Predicate> predicates, List<Object> params){
        ArrayList<Predicate> fuzzyPredicates = new ArrayList<Predicate>(7);
        fuzzyPredicates.add(names);
        fuzzyPredicates.add(namesSwap);
        fuzzyPredicates.add(cb.and(PersonNameMatching.fuzzyNameWildCard(cb, givenNameSoundex, givenName, 
                fuzzyGivenName, params), cb.equal(familyNameSoundex, "*")));
        fuzzyPredicates.add(cb.and(PersonNameMatching.fuzzyNameWildCard(cb, familyNameSoundex, familyName, 
                fuzzyFamilyName, params), cb.equal(givenNameSoundex, "*")));
        fuzzyPredicates.add(cb.and(PersonNameMatching.fuzzyNameWildCard(cb, givenNameSoundex, familyName, 
                fuzzyFamilyName, params), cb.equal(familyNameSoundex, "*")));
        fuzzyPredicates.add(cb.and(PersonNameMatching.fuzzyNameWildCard(cb, familyNameSoundex, givenName, 
                fuzzyGivenName, params), cb.equal(givenNameSoundex, "*")));
        fuzzyPredicates.add(cb.and(cb.equal(givenNameSoundex, "*"), 
                cb.equal(familyNameSoundex, "*")));
        predicates.add(cb.or(fuzzyPredicates.toArray(new Predicate[fuzzyPredicates.size()])));
    }

    static private void fuzzyName(CriteriaBuilder cb, AttributeFilter filter, 
            Path<String> familyNameSoundex, Path<String> givenNameSoundex,
            String name, boolean matchUnknown, List<Predicate> predicates, List<Object> params) {
        String fuzzyName = filter.toFuzzy(name);
        Predicate predicate = cb.or(
                PersonNameMatching.fuzzyNameWildCard(cb, familyNameSoundex, name, fuzzyName, params),
                PersonNameMatching.fuzzyNameWildCard(cb, givenNameSoundex, name, fuzzyName, params));
        if (matchUnknown)
            predicates.add(cb.or(predicate, cb.and(cb.equal(givenNameSoundex, "*"), 
                            cb.equal(familyNameSoundex, "*"))));
        else
            predicates.add(predicate);
    }

    static private Predicate fuzzyNameWildCard(CriteriaBuilder cb, 
            Path<String> field, String name, String fuzzy, List<Object> params) {
        if (name.endsWith("*"))
            return PersonNameMatching.likeValue(cb, field, fuzzy, params);
        return PersonNameMatching.singleValue(cb, field, fuzzy, params);
    }

    private static Predicate singleValue(CriteriaBuilder cb, Path<String> field, String value,
            List<Object> params) {
        ParameterExpression<String> param = Matching.setParam(cb, value, params);
        return cb.equal(field, param);
    }

    static private Predicate likeValue(CriteriaBuilder cb, Path<String> field,
            String value, List<Object> params) {
        String pattern = value.concat("%");
        ParameterExpression<String> param = Matching.setParam(cb, pattern, params);
        return cb.like(field, param);
    }

    static private void literalPersonName(CriteriaBuilder cb,
            Path<String> alphabethic, Path<String> ideographic,
            Path<String> phonetic, String value, boolean matchUnknown,
            List<Predicate> predicates, List<Object> params) {
        PersonName pn = new PersonName(value);
        if (value.indexOf('=') == -1) {
            String queryString = toQueryString(pn, PersonName.Group.Alphabetic);
            ArrayList<Predicate> namePredicates = new ArrayList<Predicate>(4);
            Matching.wildCard(cb, alphabethic, queryString, false, namePredicates, params);
            Matching.wildCard(cb, ideographic, queryString, false, namePredicates, params);
            Matching.wildCard(cb, phonetic, queryString, false, namePredicates, params);
            if (matchUnknown)
                namePredicates.add(cb.and(
                        cb.equal(alphabethic, "*"),
                        cb.equal(ideographic, "*"),
                        cb.equal(phonetic, "*")));
            predicates.add(cb.or(namePredicates.toArray(new Predicate[namePredicates.size()])));
        } else {
            wildCard(cb, alphabethic, pn, PersonName.Group.Alphabetic, matchUnknown,
                    predicates, params);
            wildCard(cb, ideographic, pn, PersonName.Group.Ideographic, matchUnknown,
                    predicates, params);
            wildCard(cb, phonetic, pn, PersonName.Group.Phonetic, matchUnknown,
                    predicates, params);
        }
    }

    private static void wildCard(CriteriaBuilder cb, Path<String> path,
            PersonName pn, Group group, boolean matchUnknown,
            List<Predicate> predicates, List<Object> params) {
        if (pn.contains(group))
            Matching.wildCard(cb, path, toQueryString(pn, group), matchUnknown, predicates, params);
    }

    private static String toQueryString(PersonName pn, PersonName.Group g) {
        String s = pn.toString(g, true);
        return (s.endsWith("*") || pn.contains(g, PersonName.Component.NameSuffix)) ? s : s + "^*";
    }
}
