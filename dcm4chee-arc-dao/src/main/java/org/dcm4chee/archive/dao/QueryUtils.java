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
class QueryUtils {

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

    public static Predicate matchWC(CriteriaBuilder cb, Path<String> pnfield,
            String value, boolean toUpper, boolean matchUnknown,
            List<Object> params) {
        if (value == null) 
            return null;

        if (toUpper)
            value = value.toUpperCase();

        Predicate predicate;
        if (QueryUtils.containsWildcard(value)) {
            String pattern = QueryUtils.toLikePattern(value);
            if (pattern.equals("%")) 
                return null;

            ParameterExpression<String> param = 
                    cb.parameter(String.class, "param" + params.size());
            params.add(pattern);
            predicate = cb.like(pnfield, param);
        } else {
            ParameterExpression<String> param =
                    cb.parameter(String.class, "param" + params.size());
            params.add(value);
            predicate = cb.equal(pnfield, param);
        }
        return matchUnknown ? cb.or(predicate, cb.isNull(pnfield)) : predicate;
    }

    public static void addNotNull(List<Predicate> predicates, Predicate e) {
        if (e != null)
            predicates.add(e);
    }

}
