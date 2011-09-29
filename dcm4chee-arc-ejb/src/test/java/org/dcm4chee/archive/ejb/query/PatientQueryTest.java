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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.ejb.EJB;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;
import org.dcm4che.soundex.ESoundex;
import org.dcm4chee.archive.persistence.StoreParam;
import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Michael Backhaus <michael.backhaus@agfa.com>
 */
@RunWith(Arquillian.class)
public class PatientQueryTest {

    private static String[] dobPatientData = { "DOB*", "DCM4CHEE_TESTDATA" };
    
    private static String[] fuzzyPatientData = { "FUZZY*", "DCM4CHEE_TESTDATA" };

    private static final QueryParam QUERY_PARAM = new QueryParam();
    private static final QueryParam MATCH_UNKNOWN = new QueryParam().setMatchUnknown(true);
    private static final QueryParam FUZZY_MATCH = new QueryParam().setFuzzySemanticMatching(true);
    private static final QueryParam FUZZY_MATCH_UNKNOWN =
            new QueryParam().setFuzzySemanticMatching(true).setMatchUnknown(true);
    private static final StoreParam STORE_PARAM = new StoreParam();
    static { STORE_PARAM.setFuzzyStr(new ESoundex()); }

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(
                        QueryParam.class,
                        CompositeQuery.class,
                        CompositeQueryBean.class,
                        CompositeQueryImpl.class,
                        PatientQueryImpl.class,
                        StudyQueryImpl.class,
                        SeriesQueryImpl.class,
                        InstanceQueryImpl.class,
                        Builder.class,
                        MatchDateTimeRange.class,
                        MatchPersonName.class)
                .addAsWebInfResource("META-INF/ejb-jar.xml", "ejb-jar.xml");
    }

    @EJB
    private CompositeQuery query;

    @Test
    public void testByPatientID() throws Exception {
        query.findPatients(dobPatientData, null, QUERY_PARAM, STORE_PARAM);
        ArrayList<String> result = patientIDResultList(query);
        String patIDs[] = { "DOB_20010101", "DOB_20020202", "DOB_NONE" };
        Collection<String> col = Arrays.asList(patIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByPatientName() throws Exception {
        query.findPatients(null, patientName("大宮^省吾", true), QUERY_PARAM, STORE_PARAM);
        assertTrue(query.hasMoreMatches());
        query.nextMatch();
        assertFalse(query.hasMoreMatches());
        query.findPatients(fuzzyPatientData, patientName("Lucas^George=", false),
                QUERY_PARAM, STORE_PARAM);
        ArrayList<String> result = patientIDResultList(query);
        String patIDs[] = { "FUZZY_GEORGE" };
        Collection<String> col = Arrays.asList(patIDs);
        assertTrue(equals(result, col));
        query.findPatients(null, 
                patientName("oomiya^shougo=大宮^省吾=オオミヤ^ショウゴ",  true),
                QUERY_PARAM, STORE_PARAM);
        result = patientIDResultList(query);
        patIDs = new String[]{ "PERSON_NAME" };
        col = Arrays.asList(patIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByPatientBirthDate() throws Exception {
        query.findPatients(dobPatientData, patientBirthDate("20010101"), QUERY_PARAM, STORE_PARAM);
        ArrayList<String> result = patientIDResultList(query);
        String patIDs[] = { "DOB_20010101" };
        Collection<String> col = Arrays.asList(patIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByPatientBirthDateRange() throws Exception {
        query.findPatients(dobPatientData, patientBirthDate("20010101-20020202"),
                QUERY_PARAM, STORE_PARAM);
        ArrayList<String> result = patientIDResultList(query);
        String patIDs[] = { "DOB_20010101", "DOB_20020202" };
        Collection<String> col = Arrays.asList(patIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByPatientBirthDateMatchUnknown() throws Exception {
        query.findPatients(dobPatientData, patientBirthDate("20010101"),
                MATCH_UNKNOWN, STORE_PARAM);
        ArrayList<String> result = patientIDResultList(query);
        String patIDs[] = { "DOB_20010101", "DOB_NONE" };
        Collection<String> col = Arrays.asList(patIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByPatientBirthDateRangeMatchUnknown() throws Exception {
        query.findPatients(dobPatientData, patientBirthDate("20010101-20020202"),
                MATCH_UNKNOWN, STORE_PARAM);
        ArrayList<String> result = patientIDResultList(query);
        String patIDs[] = { "DOB_20010101", "DOB_20020202", "DOB_NONE" };
        Collection<String> col = Arrays.asList(patIDs);
        assertTrue(equals(result, col));
        query.close();
    }
    
    @Test
    public void testByPatientNameSoundex1() throws Exception {
        query.findPatients(fuzzyPatientData, patientName("LUCAS^GEORGE", false),
                FUZZY_MATCH, STORE_PARAM);
        ArrayList<String> result = patientIDResultList(query);
        String patIDs[] = { "FUZZY_GEORGE", "FUZZY_JOERG" };
        Collection<String> col = Arrays.asList(patIDs);
        assertTrue(equals(result, col));
        query.close();
    }
    
    @Test
    public void testByPatientNameSoundex2() throws Exception {
        query.findPatients(fuzzyPatientData, patientName("LUKAS^JÖRG", false),
                FUZZY_MATCH, STORE_PARAM);
        ArrayList<String> result = patientIDResultList(query);
        String patIDs[] = { "FUZZY_GEORGE", "FUZZY_JOERG" };
        Collection<String> col = Arrays.asList(patIDs);
        assertTrue(equals(result, col));
        query.close();
    }
    
    @Test
    public void testByPatientNameSoundex3() throws Exception {
        query.findPatients(fuzzyPatientData, patientName("LUKE", false),
                FUZZY_MATCH, STORE_PARAM);
        ArrayList<String> result = patientIDResultList(query);
        String patIDs[] = { "FUZZY_LUKE" };
        Collection<String> col = Arrays.asList(patIDs);
        assertTrue(equals(result, col));
        query.close();
    }
    
    @Test
    public void testByPatientNameSoundex4() throws Exception {
        query.findPatients(fuzzyPatientData, patientName("LU*", false),
                FUZZY_MATCH, STORE_PARAM);
        ArrayList<String> result = patientIDResultList(query);
        String patIDs[] = { "FUZZY_LUKE" , "FUZZY_JOERG", "FUZZY_GEORGE"};
        Collection<String> col = Arrays.asList(patIDs);
        assertTrue(equals(result, col));
        query.close();
    }
    
    @Test
    public void testByPatientNameSoundex5() throws Exception {
        query.findPatients(fuzzyPatientData, patientName("LU*", false),
                FUZZY_MATCH_UNKNOWN, STORE_PARAM);
        ArrayList<String> result = patientIDResultList(query);
        String patIDs[] = { "FUZZY_LUKE", "FUZZY_JOERG", "FUZZY_GEORGE", "FUZZY_NONE"};
        Collection<String> col = Arrays.asList(patIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    private Attributes patientBirthDate(String date) {
        Attributes attrs = new Attributes(1);
        attrs.setString(Tag.PatientBirthDate, VR.DA, date);
        return attrs;
    }

    private Attributes patientName(String name, boolean specificChar) {
        Attributes attrs = new Attributes(2);
        if(specificChar)
            attrs.setString(Tag.SpecificCharacterSet, VR.CS, "ISO 2022 IR 6",
                "ISO 2022 IR 87");
        attrs.setString(Tag.PatientName, VR.PN, name);
        return attrs;
    }
    

    private ArrayList<String> patientIDResultList(CompositeQuery query)
            throws Exception {
        ArrayList<String> result = new ArrayList<String>();
        while (query.hasMoreMatches()) {
            result.add(query.nextMatch().getString(Tag.PatientID));
        }
        return result;
    }

    private boolean equals(ArrayList<String> result, Collection<String> col) {
        if (result.containsAll(col) && result.size() == col.size())
            return true;
        else
            return false;
    }
}
