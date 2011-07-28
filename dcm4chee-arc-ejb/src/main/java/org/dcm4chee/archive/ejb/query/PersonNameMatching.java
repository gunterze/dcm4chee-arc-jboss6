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
import org.dcm4che.net.pdu.QueryOption;
import org.dcm4chee.archive.persistence.AttributeFilter;

/**
 * @author Michael Backhaus <michael.backhaus@agfa.com>
 */
class PersonNameMatching {

    static void personName(CriteriaBuilder cb, 
            Path<String> alphabethic,
            Path<String> ideographic,
            Path<String> phonetic,
            Path<String> familyNameSoundex,
            Path<String> givenNameSoundex,
            String value, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, boolean matchUnknown,
            List<Object> params, List<Predicate> predicates) {
        if (value.equals("*"))
            return;
    
        if (queryOpts.contains(QueryOption.FUZZY))
            predicates.add(PersonNameMatching.fuzzyPersonName(cb, 
                    familyNameSoundex, givenNameSoundex, value, filter, matchUnknown, params));
        else
            predicates.add(PersonNameMatching.literalPersonName(cb, 
                    alphabethic, ideographic, phonetic, value, matchUnknown, params));
    }

    static private Predicate fuzzyPersonName(CriteriaBuilder cb,
            Path<String> familyNameSoundex, Path<String> givenNameSoundex,
            String value, AttributeFilter filter, boolean matchUnknown, List<Object> params) {
        PersonName pn = new PersonName(value);
        boolean containsFamilyName = pn.containst(PersonName.Component.FamilyName);
        boolean containsGivenName = pn.containst(PersonName.Component.GivenName);
        if (containsFamilyName && containsGivenName)
            return PersonNameMatching.fuzzyNames(cb, filter, familyNameSoundex, givenNameSoundex, 
                    pn.get(PersonName.Component.FamilyName), pn.get(PersonName.Component.GivenName), 
                    matchUnknown, params);
        else if (containsGivenName)
            return PersonNameMatching.fuzzyName(cb, filter, familyNameSoundex, givenNameSoundex, 
                    pn.get(PersonName.Component.GivenName), matchUnknown, params);
        else if (containsFamilyName)
            return PersonNameMatching.fuzzyName(cb, filter, familyNameSoundex, givenNameSoundex, 
                    pn.get(PersonName.Component.FamilyName), matchUnknown, params);
        return null;
    }

    static private Predicate fuzzyNames(CriteriaBuilder cb, AttributeFilter filter, 
            Path<String> familyNameSoundex, Path<String> givenNameSoundex,
            String givenName, String familyName, 
            boolean matchUnknown, List<Object> params) {
        String fuzzyFamilyName = filter.toFuzzy(familyName);
        String fuzzyGivenName = filter.toFuzzy(givenName);
        Predicate names = 
            cb.and(PersonNameMatching.fuzzyPersonNameWildCard(cb, givenNameSoundex, givenName, fuzzyGivenName, params),
                   PersonNameMatching.fuzzyPersonNameWildCard(cb, familyNameSoundex, familyName, fuzzyFamilyName, params));
        Predicate namesSwap = 
            cb.and(PersonNameMatching.fuzzyPersonNameWildCard(cb, givenNameSoundex, familyName, fuzzyFamilyName, params),
                   PersonNameMatching.fuzzyPersonNameWildCard(cb, familyNameSoundex, givenName, fuzzyGivenName, params));
        return matchUnknown
                    ? PersonNameMatching.unknownFuzzyNames(cb, familyNameSoundex, givenNameSoundex, familyName, 
                            givenName, fuzzyFamilyName, fuzzyGivenName, names, namesSwap, params)
                    : cb.or(names, namesSwap);
    }

    static private Predicate unknownFuzzyNames(CriteriaBuilder cb, 
            Path<String> familyNameSoundex, Path<String> givenNameSoundex, 
            String familyName, String givenName, 
            String fuzzyFamilyName, String fuzzyGivenName,
            Predicate names, Predicate namesSwap, 
            List<Object> params){
        ArrayList<Predicate> predicates = new ArrayList<Predicate>(7);
        predicates.add(names);
        predicates.add(namesSwap);
        predicates.add(cb.and(PersonNameMatching.fuzzyPersonNameWildCard(cb, givenNameSoundex, givenName, 
                fuzzyGivenName, params), cb.equal(familyNameSoundex, "*")));
        predicates.add(cb.and(PersonNameMatching.fuzzyPersonNameWildCard(cb, familyNameSoundex, familyName, 
                fuzzyFamilyName, params), cb.equal(givenNameSoundex, "*")));
        predicates.add(cb.and(PersonNameMatching.fuzzyPersonNameWildCard(cb, givenNameSoundex, familyName, 
                fuzzyFamilyName, params), cb.equal(familyNameSoundex, "*")));
        predicates.add(cb.and(PersonNameMatching.fuzzyPersonNameWildCard(cb, familyNameSoundex, givenName, 
                fuzzyGivenName, params), cb.equal(givenNameSoundex, "*")));
        predicates.add(cb.and(cb.equal(givenNameSoundex, "*"), 
                cb.equal(familyNameSoundex, "*")));
        return cb.or(predicates.toArray(new Predicate[predicates.size()]));
    }

    static private Predicate fuzzyName(CriteriaBuilder cb, AttributeFilter filter, 
            Path<String> familyNameSoundex, Path<String> givenNameSoundex,
            String name, boolean matchUnknown, List<Object> params) {
        String fuzzyName = filter.toFuzzy(name);
        Predicate predicate = cb.or(
                PersonNameMatching.fuzzyPersonNameWildCard(cb, familyNameSoundex, name, fuzzyName, params),
                PersonNameMatching.fuzzyPersonNameWildCard(cb, givenNameSoundex, name, fuzzyName, params));
        return matchUnknown 
                    ? cb.or(predicate, cb.and(cb.equal(givenNameSoundex, "*"), 
                            cb.equal(familyNameSoundex, "*")))
                    : predicate;
    }

    static private Predicate fuzzyPersonNameWildCard(CriteriaBuilder cb, 
            Path<String> field, String name, String fuzzy, List<Object> params) {
        if (name.endsWith("*"))
            return PersonNameMatching.likeValue(cb, field, fuzzy, params);
        return Matching.singleValue0(cb, field, fuzzy, params);
    }

    static private Predicate likeValue(CriteriaBuilder cb, Path<String> field,
            String value, List<Object> params) {
        String pattern = value.concat("%");
        ParameterExpression<String> param = Matching.setParam(cb, params, pattern);
        return cb.like(field, param);
    }

    static private Predicate literalPersonName(CriteriaBuilder cb,
            Path<String> alphabethic, Path<String> ideographic,
            Path<String> phonetic, String value, boolean matchUnknown,
            List<Object> params) {
        PersonName pn = new PersonName(value);
        Predicate predicate;
        String queryString =
                pn.getNormalizedQueryString(PersonName.Group.Alphabetic);
        if (value.indexOf('=') == -1) {
            predicate = cb.or(
                    Matching.wildCard0(cb, alphabethic, queryString, params),
                    Matching.wildCard0(cb, ideographic, queryString, params),
                    Matching.wildCard0(cb, phonetic, queryString, params));
        } else {
            predicate = Matching.and(cb,
                    Matching.wildCard0(cb, alphabethic, queryString, params),
                    Matching.wildCard0(cb, ideographic,
                            pn.getNormalizedQueryString(PersonName.Group.Ideographic),
                            params),
                    Matching.wildCard0(cb, phonetic,
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

}
