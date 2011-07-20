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
import org.dcm4che.io.SAXReader;
import org.dcm4che.soundex.FuzzyStr;
import org.dcm4che.soundex.KPhonetik;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class AttributeFilter {

    private static final String PATIENT_ATTRIBUTE_FILTER =
            "resource:patient-attribute-filter.xml";
    private static final String STUDY_ATTRIBUTE_FILTER =
            "resource:study-attribute-filter.xml";
    private static final String SERIES_ATTRIBUTE_FILTER =
            "resource:series-attribute-filter.xml";
    private static final String INSTANCE_ATTRIBUTE_FILTER =
            "resource:instance-attribute-filter.xml";
    private static final String CASE_INSENSITIVE_ATTRIBUTES =
            "resource:case-insensitive-attributes.xml";

    public final static Attributes patientFilter;
    public final static Attributes studyFilter;
    public final static Attributes seriesFilter;
    public final static Attributes instanceFilter;
    public final static Attributes caseInsensitive;
    public final static FuzzyStr fuzzyStr = new KPhonetik();

    static {
        try {
            patientFilter = SAXReader.parse(PATIENT_ATTRIBUTE_FILTER, null);
            studyFilter = SAXReader.parse(STUDY_ATTRIBUTE_FILTER, null);
            seriesFilter = SAXReader.parse(SERIES_ATTRIBUTE_FILTER, null);
            instanceFilter = SAXReader.parse(INSTANCE_ATTRIBUTE_FILTER, null);
            caseInsensitive = SAXReader.parse(CASE_INSENSITIVE_ATTRIBUTES, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getString(Attributes attrs, int tag) {
        String val = attrs.getString(tag, null);
        return val != null
                ? (caseInsensitive.contains(tag) ? val.toUpperCase() : val)
                : "*";
    }

    public static String toFuzzy(String val) {
        return val != null
                ? fuzzyStr.toFuzzy(val)
                : "*";
    }
}
