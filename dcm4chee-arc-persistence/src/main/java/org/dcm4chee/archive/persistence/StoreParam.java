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
public class StoreParam {

    private int[] patientAttributes;
    private int[] studyAttributes;
    private int[] seriesAttributes;
    private int[] instanceAttributes;
    private ValueSelector patientCustomAttribute1;
    private ValueSelector patientCustomAttribute2;
    private ValueSelector patientCustomAttribute3;
    private ValueSelector studyCustomAttribute1;
    private ValueSelector studyCustomAttribute2;
    private ValueSelector studyCustomAttribute3;
    private ValueSelector seriesCustomAttribute1;
    private ValueSelector seriesCustomAttribute2;
    private ValueSelector seriesCustomAttribute3;
    private ValueSelector instanceCustomAttribute1;
    private ValueSelector instanceCustomAttribute2;
    private ValueSelector instanceCustomAttribute3;
    private FuzzyStr fuzzyStr;

    public final int[] getPatientAttributes() {
        return patientAttributes;
    }

    public final void setPatientAttributes(int... patientAttributes) {
        this.patientAttributes = patientAttributes;
    }

    public final int[] getStudyAttributes() {
        return studyAttributes;
    }

    public final void setStudyAttributes(int... studyAttributes) {
        this.studyAttributes = studyAttributes;
    }

    public final int[] getSeriesAttributes() {
        return seriesAttributes;
    }

    public final void setSeriesAttributes(int... seriesAttributes) {
        this.seriesAttributes = seriesAttributes;
    }

    public final int[] getInstanceAttributes() {
        return instanceAttributes;
    }

    public final void setInstanceAttributes(int... instanceAttributes) {
        this.instanceAttributes = instanceAttributes;
    }

    public final ValueSelector getPatientCustomAttribute1() {
        return patientCustomAttribute1;
    }

    public final void setPatientCustomAttribute1(
            ValueSelector patientCustomAttribute1) {
        this.patientCustomAttribute1 = patientCustomAttribute1;
    }

    public final ValueSelector getPatientCustomAttribute2() {
        return patientCustomAttribute2;
    }

    public final void setPatientCustomAttribute2(
            ValueSelector patientCustomAttribute2) {
        this.patientCustomAttribute2 = patientCustomAttribute2;
    }

    public final ValueSelector getPatientCustomAttribute3() {
        return patientCustomAttribute3;
    }

    public final void setPatientCustomAttribute3(
            ValueSelector patientCustomAttribute3) {
        this.patientCustomAttribute3 = patientCustomAttribute3;
    }

    public final ValueSelector getStudyCustomAttribute1() {
        return studyCustomAttribute1;
    }

    public final void setStudyCustomAttribute1(ValueSelector studyCustomAttribute1) {
        this.studyCustomAttribute1 = studyCustomAttribute1;
    }

    public final ValueSelector getStudyCustomAttribute2() {
        return studyCustomAttribute2;
    }

    public final void setStudyCustomAttribute2(ValueSelector studyCustomAttribute2) {
        this.studyCustomAttribute2 = studyCustomAttribute2;
    }

    public final ValueSelector getStudyCustomAttribute3() {
        return studyCustomAttribute3;
    }

    public final void setStudyCustomAttribute3(ValueSelector studyCustomAttribute3) {
        this.studyCustomAttribute3 = studyCustomAttribute3;
    }

    public final ValueSelector getSeriesCustomAttribute1() {
        return seriesCustomAttribute1;
    }

    public final void setSeriesCustomAttribute1(ValueSelector seriesCustomAttribute1) {
        this.seriesCustomAttribute1 = seriesCustomAttribute1;
    }

    public final ValueSelector getSeriesCustomAttribute2() {
        return seriesCustomAttribute2;
    }

    public final void setSeriesCustomAttribute2(ValueSelector seriesCustomAttribute2) {
        this.seriesCustomAttribute2 = seriesCustomAttribute2;
    }

    public final ValueSelector getSeriesCustomAttribute3() {
        return seriesCustomAttribute3;
    }

    public final void setSeriesCustomAttribute3(ValueSelector seriesCustomAttribute3) {
        this.seriesCustomAttribute3 = seriesCustomAttribute3;
    }

    public final ValueSelector getInstanceCustomAttribute1() {
        return instanceCustomAttribute1;
    }

    public final void setInstanceCustomAttribute1(
            ValueSelector instanceCustomAttribute1) {
        this.instanceCustomAttribute1 = instanceCustomAttribute1;
    }

    public final ValueSelector getInstanceCustomAttribute2() {
        return instanceCustomAttribute2;
    }

    public final void setInstanceCustomAttribute2(
            ValueSelector instanceCustomAttribute2) {
        this.instanceCustomAttribute2 = instanceCustomAttribute2;
    }

    public final ValueSelector getInstanceCustomAttribute3() {
        return instanceCustomAttribute3;
    }

    public final void setInstanceCustomAttribute3(
            ValueSelector instanceCustomAttribute3) {
        this.instanceCustomAttribute3 = instanceCustomAttribute3;
    }

    public static String selectStringValue(Attributes attrs, ValueSelector selector, String defVal) {
        return selector != null ? selector.selectStringValue(attrs, defVal) : defVal;
    }

    public final FuzzyStr getFuzzyStr() {
        return fuzzyStr;
    }

    public final void setFuzzyStr(FuzzyStr fuzzyStr) {
        this.fuzzyStr = fuzzyStr;
    }

    public String toFuzzy(String s, String defVal) {
        return s != null ? fuzzyStr.toFuzzy(s) : defVal;
    }

}