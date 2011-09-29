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

import org.dcm4che.data.ValueSelector;
import org.dcm4che.soundex.FuzzyStr;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class QueryParam {

    public enum Level { PATIENT, STUDY, SERIES, IMAGE };

    private boolean combinedDatetimeMatching;
    private FuzzyStr fuzzyStr;
    private boolean matchUnknown;
    private final ValueSelector[] customAttributes = new ValueSelector[12];
    private String[] roles;

    public final boolean isCombinedDatetimeMatching() {
        return combinedDatetimeMatching;
    }

    public final QueryParam setCombinedDatetimeMatching(boolean combinedDatetimeMatching) {
        this.combinedDatetimeMatching = combinedDatetimeMatching;
        return this;
    }

    public final FuzzyStr getFuzzyStr() {
        return fuzzyStr;
    }

    public final QueryParam setFuzzyStr(FuzzyStr fuzzyStr) {
        this.fuzzyStr = fuzzyStr;
        return this;
    }

    public boolean isFuzzySemanticMatching() {
        return fuzzyStr != null;
    }

    public final boolean isMatchUnknown() {
        return matchUnknown;
    }

    public final QueryParam setMatchUnknown(boolean matchUnknown) {
        this.matchUnknown = matchUnknown;
        return this;
    }

    public ValueSelector getCustomAttributeValueSelector(Level level, int index) {
        checkIndex(index);
        return customAttributes[level.ordinal() * 3 + index];
    }

    public QueryParam setCustomAttributeValueSelector(Level level, int index, ValueSelector selector) {
        checkIndex(index);
        customAttributes[level.ordinal() * 3 + index] = selector;
        return this;
    }

    private void checkIndex(int index) {
        if (index < 0 || index > 2)
            throw new IndexOutOfBoundsException("index: " + index);
    }

    public final String[] getRoles() {
        return roles != null ? roles.clone() : null;
    }

    public final QueryParam setRoles(String... roles) {
        this.roles = roles != null ? roles.clone() : null;
        return this;
    }

}
