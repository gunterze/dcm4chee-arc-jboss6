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

package org.dcm4chee.archive.dao;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

/**
@author Gunter Zeilinger <gunterze@gmail.com>
 */
class Matching {

    private static final String[] NAMES = {
        "p0", "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9"
    };

    public static String paramName(int paramIndex) {
        return paramIndex < NAMES.length
                ? NAMES[paramIndex]
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

        ParameterExpression<String>[] pes = new ParameterExpression[values.length];
        for (int i = 0; i < values.length; i++) {
            pes[i] = cb.parameter(String.class, paramName(params.size()));
            params.add(values[i]);
        }
        return field.in(pes);
    }

    public static Predicate wildCard(CriteriaBuilder cb, Path<String> field,
            String value, boolean toUpper, boolean matchUnknown,
            List<Object> params) {
        if (value == null || value.isEmpty())
            return null;

        return matchUnknown0(cb, field, matchUnknown,
                wildCard0(cb, field, 
                        toUpper ? value.toUpperCase() : value, params));
    }

    private static Predicate wildCard0(CriteriaBuilder cb,
            Path<String> field, String value, List<Object> params) {
        if (!Matching.containsWildcard(value))
            return singleValue0(cb, field, value, params);

        String pattern = Matching.toLikePattern(value);
        if (pattern.equals("%")) 
            return null;

        ParameterExpression<String> param = 
                cb.parameter(String.class, paramName(params.size()));
        params.add(pattern);
        return cb.like(field, param);
    }

    private static Predicate singleValue0(CriteriaBuilder cb,
            Path<String> field, String value, List<Object> params) {
        ParameterExpression<String> param =
                cb.parameter(String.class, paramName(params.size()));
        params.add(value);
        return cb.equal(field, param);
    }

    private static Predicate matchUnknown0(CriteriaBuilder cb,
            Path<String> field, boolean matchUnknown, Predicate predicate) {
        return matchUnknown ? cb.or(predicate, cb.isNull(field)) : predicate;
    }

    public static Predicate patientID(CriteriaBuilder cb, Path<String> idField,
            Path<String> issuerField, String[] pids, boolean matchUnknown,
            List<Object> params) {
        if (pids == null || pids.length == 0)
            return null;

        Predicate predicate = pids.length == 1
                ? patientID0(cb, idField, issuerField, pids[0], pids[1], params)
                : patientID0(cb, idField, issuerField, pids, params);
        return matchUnknown ? cb.or(predicate, cb.isNull(idField)) : predicate;
    }

    private static Predicate patientID0(CriteriaBuilder cb,
            Path<String> idField, Path<String> issuerField,
            String[] pids, List<Object> params) {
        Predicate[] predicates = new Predicate[pids.length >> 1];
        for (int i = 0, j = 0; i < predicates.length; i++, j++, j++)
            predicates[i] = patientID0(cb, idField, issuerField,
                    pids[j], pids[j+1], params);
        return cb.or(predicates);
    }

    private static Predicate patientID0(CriteriaBuilder cb,
            Path<String> idField, Path<String> issuerField,
            String id, String issuer, List<Object> params) {
        Predicate predicate = wildCard0(cb, idField, id, params);
        return issuer == null
                ? predicate
                : cb.and(predicate, wildCard0(cb, issuerField, issuer, params));
    }
}
