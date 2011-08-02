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

package org.dcm4chee.archive.persistence;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.ValueSelector;
import org.dcm4che.soundex.FuzzyStr;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class AttributeFilter {

    final Attributes patientFilter;
    final Attributes studyFilter;
    final Attributes seriesFilter;
    final Attributes instanceFilter;
    private final Attributes caseInsensitive;
    private final FuzzyStr fuzzyStr;
    private boolean matchUnknown;
    ValueSelector patientCustomAttribute1;
    ValueSelector patientCustomAttribute2;
    ValueSelector patientCustomAttribute3;
    ValueSelector studyCustomAttribute1;
    ValueSelector studyCustomAttribute2;
    ValueSelector studyCustomAttribute3;
    ValueSelector seriesCustomAttribute1;
    ValueSelector seriesCustomAttribute2;
    ValueSelector seriesCustomAttribute3;
    ValueSelector instanceCustomAttribute1;
    ValueSelector instanceCustomAttribute2;
    ValueSelector instanceCustomAttribute3;

    public AttributeFilter(Attributes patientFilter, Attributes studyFilter,
            Attributes seriesFilter, Attributes instanceFilter,
            Attributes caseInsensitive, FuzzyStr fuzzyStr) {
        if (patientFilter == null)
            throw new NullPointerException("patientFilter");
        if (studyFilter == null)
            throw new NullPointerException("studyFilter");
        if (seriesFilter == null)
            throw new NullPointerException("seriesFilter");
        if (instanceFilter == null)
            throw new NullPointerException("instanceFilter");
        if (caseInsensitive == null)
            throw new NullPointerException("caseInsensitive");
        if (fuzzyStr == null)
            throw new NullPointerException("fuzzyStr");
        this.patientFilter = patientFilter;
        this.studyFilter = studyFilter;
        this.seriesFilter = seriesFilter;
        this.instanceFilter = instanceFilter;
        this.caseInsensitive = caseInsensitive;
        this.fuzzyStr = fuzzyStr;
    }

    public final boolean isMatchUnknown() {
        return matchUnknown;
    }

    public final void setMatchUnknown(boolean matchUnknown) {
        this.matchUnknown = matchUnknown;
    }

    public final void setPatientCustomAttribute1(ValueSelector selector) {
        this.patientCustomAttribute1 = selector;
    }

    public final void setPatientCustomAttribute2(ValueSelector selector) {
        this.patientCustomAttribute2 = selector;
    }

    public final void setPatientCustomAttribute3(ValueSelector selector) {
        this.patientCustomAttribute3 = selector;
    }

    public final void setStudyCustomAttribute1(ValueSelector selector) {
        this.studyCustomAttribute1 = selector;
    }

    public final void setStudyCustomAttribute2(ValueSelector selector) {
        this.studyCustomAttribute2 = selector;
    }

    public final void setStudyCustomAttribute3(ValueSelector selector) {
        this.studyCustomAttribute3 = selector;
    }

    public final void setSeriesCustomAttribute1(ValueSelector selector) {
        this.seriesCustomAttribute1 = selector;
    }

    public final void setSeriesCustomAttribute2(ValueSelector selector) {
        this.seriesCustomAttribute2 = selector;
    }

    public final void setSeriesCustomAttribute3(ValueSelector selector) {
        this.seriesCustomAttribute3 = selector;
    }

    public final void setInstanceCustomAttribute1(ValueSelector selector) {
        this.instanceCustomAttribute1 = selector;
    }

    public final void setInstanceCustomAttribute2(ValueSelector selector) {
        this.instanceCustomAttribute2 = selector;
    }

    public final void setInstanceCustomAttribute3(ValueSelector selector) {
        this.instanceCustomAttribute3 = selector;
    }

    public String getString(Attributes attrs, int tag) {
        String val = attrs.getString(tag, null);
        return val != null
                ? (caseInsensitive.contains(tag) ? val.toUpperCase() : val)
                : "*";
    }

    public String toFuzzy(String val) {
        return val != null
                ? fuzzyStr.toFuzzy(val)
                : "*";
    }

    public String selectPatientCustomAttribute1(Attributes attrs) {
        return selectStringValue(patientCustomAttribute1, attrs);
    }

    public String selectPatientCustomAttribute2(Attributes attrs) {
        return selectStringValue(patientCustomAttribute2, attrs);
    }

    public String selectPatientCustomAttribute3(Attributes attrs) {
        return selectStringValue(patientCustomAttribute3, attrs);
    }

    public String selectStudyCustomAttribute1(Attributes attrs) {
        return selectStringValue(studyCustomAttribute1, attrs);
    }

    public String selectStudyCustomAttribute2(Attributes attrs) {
        return selectStringValue(studyCustomAttribute2, attrs);
    }

    public String selectStudyCustomAttribute3(Attributes attrs) {
        return selectStringValue(studyCustomAttribute3, attrs);
    }

    public String selectSeriesCustomAttribute1(Attributes attrs) {
        return selectStringValue(seriesCustomAttribute1, attrs);
    }

    public String selectSeriesCustomAttribute2(Attributes attrs) {
        return selectStringValue(seriesCustomAttribute2, attrs);
    }

    public String selectSeriesCustomAttribute3(Attributes attrs) {
        return selectStringValue(instanceCustomAttribute3, attrs);
    }

    public String selectInstanceCustomAttribute1(Attributes attrs) {
        return selectStringValue(instanceCustomAttribute1, attrs);
    }

    public String selectInstanceCustomAttribute2(Attributes attrs) {
        return selectStringValue(instanceCustomAttribute2, attrs);
    }

    public String selectInstanceCustomAttribute3(Attributes attrs) {
        return selectStringValue(instanceCustomAttribute3, attrs);
    }

    private String selectStringValue(ValueSelector selector, Attributes attrs) {
        if (selector != null) {
            String val = selector.selectStringValue(attrs, null);
            if (val != null)
                return caseInsensitive.contains(selector.tag(), selector.privateCreator())
                        ? val.toUpperCase()
                        : val;
        }
        return "*";
    }
}
