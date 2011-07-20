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
import java.util.EnumSet;

import javax.ejb.EJB;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;
import org.dcm4che.net.pdu.QueryOption;
import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Michael Backhaus <michael.backhaus@agfa.com>
 */
@RunWith(Arquillian.class)
public class StudyQueryTest {

    private static final String[] RangeMatchingPIDs =
            { "RANGE-MATCHING", "DCM4CHEE_TESTDATA" };

    private static final String[] AccessionNumberPIDs =
            { "ISSUER_OF_ACCNO", "DCM4CHEE_TESTDATA" };
    
    private static final String[] ProcedureCodesPIDs = 
            { "PROC_CODE_SEQ", "DCM4CHEE_TESTDATA" };
    
    private static final String[] ModalitiesInStudyPIDs =
            { "MODS_IN_STUDY", "DCM4CHEE_TESTDATA" };

    private static final EnumSet<QueryOption> NO_QUERY_OPTION =
            EnumSet.noneOf(QueryOption.class);
    
    private static final EnumSet<QueryOption> COMBINED_DATE_TIME =
            EnumSet.of(QueryOption.DATETIME);

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar").addClasses(
                StudyQuery.class, StudyQueryBean.class, Matching.class,
                RangeMatching.class);
    }

    @EJB
    private StudyQuery query;

    @Test
    public void testByModalitiesInStudyPR() throws Exception {
        query.find(null, ModalitiesInStudyPIDs, modalitiesInStudy("PR"), NO_QUERY_OPTION, false);
        assertTrue(countMatches(query, 2));
        query.close();
    }
    
    @Test
    public void testByModalitiesInStudyMatchUnknownPR() throws Exception {
        query.find(null, ModalitiesInStudyPIDs, modalitiesInStudy("PR"), NO_QUERY_OPTION, true);
        assertTrue(countMatches(query, 3));
        query.close();
    }
    
    @Test
    public void testByModalitiesInStudyCT() throws Exception {
        query.find(null, ModalitiesInStudyPIDs, modalitiesInStudy("CT"), NO_QUERY_OPTION, false);
        assertTrue(countMatches(query, 1));
        query.close();
    }
    
    @Test
    public void testByModalitiesInStudyMatchUnknownCT() throws Exception {
        query.find(null, ModalitiesInStudyPIDs, modalitiesInStudy("CT"), NO_QUERY_OPTION, true);
        assertTrue(countMatches(query, 2));
        query.close();
    }

    @Test
    public void testByDateTime() throws Exception {
        query.find(null, RangeMatchingPIDs, studyDateTimeRange("20110620",
                "103000.000"), NO_QUERY_OPTION, false);
        assertTrue(query.hasMoreMatches());
        String studyUID =
                query.nextMatch().getString(Tag.StudyInstanceUID, null);
        assertFalse(query.hasMoreMatches());
        assertTrue(studyUID.equals("1.2.40.0.13.1.1.99.3"));
        query.close();
    }

    @Test
    public void testByOpenEndTime() throws Exception {
        query.find(null, RangeMatchingPIDs, studyDateTimeRange(null, "1030-"),
                NO_QUERY_OPTION, false);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyUIDResultList(query);
        String studyUIDs[] =
                { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
                        "1.2.40.0.13.1.1.99.5", "1.2.40.0.13.1.1.99.6" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByOpenStartTime() throws Exception {
        query.find(null, RangeMatchingPIDs, studyDateTimeRange(null, "-1430"),
                NO_QUERY_OPTION, false);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyUIDResultList(query);
        String studyUIDs[] =
                { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
                        "1.2.40.0.13.1.1.99.5", "1.2.40.0.13.1.1.99.6" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }
    
    @Test
    public void testByDateTimeMatchUnknown() throws Exception {
        query.find(null, RangeMatchingPIDs, studyDateTimeRange("20110620",
                "103000.000"), NO_QUERY_OPTION, true);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyUIDResultList(query);
        String studyUIDs[] = { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.8","1.2.40.0.13.1.1.99.9" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByTimeRange() throws Exception {
        query.find(null, RangeMatchingPIDs, studyDateTimeRange(null, "1030-1430"),
                NO_QUERY_OPTION, false);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyUIDResultList(query);
        String studyUIDs[] =
                { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
                        "1.2.40.0.13.1.1.99.5", "1.2.40.0.13.1.1.99.6" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByDateRange() throws Exception {
        query.find(null, RangeMatchingPIDs, studyDateTimeRange("20100620-20110620",
                null), NO_QUERY_OPTION, false);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyUIDResultList(query);
        String studyUIDs[] =
                { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
                        "1.2.40.0.13.1.1.99.5", "1.2.40.0.13.1.1.99.6",
                        "1.2.40.0.13.1.1.99.7", "1.2.40.0.13.1.1.99.8" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByDateTimeRange() throws Exception {
        query.find(null, RangeMatchingPIDs, studyDateTimeRange("20100620-20110620",
                "1030-1430"), NO_QUERY_OPTION, false);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyUIDResultList(query);
        String studyUIDs[] =
                { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
                        "1.2.40.0.13.1.1.99.5", "1.2.40.0.13.1.1.99.6" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByDateTimeRangeCombined() throws Exception {
        query.find(null, RangeMatchingPIDs, studyDateTimeRange("20100620-20110620",
                "1040-1430"), COMBINED_DATE_TIME, false);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyUIDResultList(query);
        String studyUIDs[] =
                { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
                        "1.2.40.0.13.1.1.99.8", "1.2.40.0.13.1.1.99.7",
                        "1.2.40.0.13.1.1.99.6" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByDateTimeRangeCombinedOpenEndRange() throws Exception {
        query.find(null, RangeMatchingPIDs,
                studyDateTimeRange("20100620-", "1040-"), COMBINED_DATE_TIME, false);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyUIDResultList(query);
        String studyUIDs[] =
                { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
                        "1.2.40.0.13.1.1.99.8", "1.2.40.0.13.1.1.99.7",
                        "1.2.40.0.13.1.1.99.6" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByDateTimeRangeCombinedOpenStartRange() throws Exception {
        query.find(null, RangeMatchingPIDs,
                studyDateTimeRange("-20110620", "-1420"), COMBINED_DATE_TIME, false);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyUIDResultList(query);
        String studyUIDs[] =
                { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.5",
                        "1.2.40.0.13.1.1.99.8", "1.2.40.0.13.1.1.99.7",
                        "1.2.40.0.13.1.1.99.6" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByDateTimeRangeCombinedMatchUnknown() throws Exception {
        query.find(null, RangeMatchingPIDs, studyDateTimeRange("20100620-20110620",
                "1040-1430"), COMBINED_DATE_TIME, true);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyUIDResultList(query);
        String studyUIDs[] =
                { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
                        "1.2.40.0.13.1.1.99.8", "1.2.40.0.13.1.1.99.7",
                        "1.2.40.0.13.1.1.99.6", "1.2.40.0.13.1.1.99.9" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByIssuerOfAccessionNumber() throws Exception {
        query.find(null, AccessionNumberPIDs, issuerOfAccessionNumber("A1234", 
                "DCM4CHEE_TESTDATA_ACCNO_ISSUER_1", null, null), NO_QUERY_OPTION, false);
        assertTrue(countMatches(query, 1));
        query.close();
    }

    @Test
    public void testByIssuerOfAccessionNumberMatchUnknown() throws Exception {
        query.find(null, AccessionNumberPIDs, issuerOfAccessionNumber("A1234",
                "DCM4CHEE_TESTDATA_ACCNO_ISSUER_2", null, null), NO_QUERY_OPTION, true);
        assertTrue(countMatches(query, 2));
        query.close();
    }

    @Test
    public void testByProcedureCodes() throws Exception {
        query.find(null, ProcedureCodesPIDs, procedureCodes("PROC_CODE_1", 
                "99DCM4CHEE_TEST", null), NO_QUERY_OPTION, false);
        assertTrue(countMatches(query, 1));
        query.close();
    }
    
    @Test
    public void testByProcedureCodesMatchUnknown() throws Exception {
        query.find(null, ProcedureCodesPIDs, procedureCodes("PROC_CODE_2", 
                "99DCM4CHEE_TEST", null), NO_QUERY_OPTION, true);
        assertTrue(countMatches(query, 2));
        query.close();
    }

    private boolean equals(ArrayList<String> result, Collection<String> col) {
        if (result.containsAll(col) && result.size()==col.size())
            return true;
        else 
            return false;
    }
    
    private boolean countMatches(StudyQuery query, int count) throws Exception {
        int i = 0;
        while(query.hasMoreMatches()){
            query.nextMatch();
            i++;
        }
        return count==i;
    }

    private Attributes procedureCodes(String value, String designator,
            String version) throws Exception {
        Attributes attrs = new Attributes(1);
        Attributes item = new Attributes(3);
        item.setString(Tag.CodeValue, VR.SH, value);
        item.setString(Tag.CodingSchemeDesignator, VR.SH, designator);
        item.setString(Tag.CodingSchemeVersion, VR.SH, version);
        attrs.newSequence(Tag.ProcedureCodeSequence, 1).add(item);
        return attrs;
    }

    private Attributes issuerOfAccessionNumber(String accno, String id,
            String uid, String type) {
        Attributes attrs = new Attributes(2);
        attrs.setString(Tag.AccessionNumber, VR.SH, accno);
        Attributes item = new Attributes(3);
        item.setString(Tag.LocalNamespaceEntityID, VR.UT, id);
        item.setString(Tag.UniversalEntityID, VR.UT, uid);
        item.setString(Tag.UniversalEntityIDType, VR.CS, type);
        attrs.newSequence(Tag.IssuerOfAccessionNumberSequence, 1).add(item);
        return attrs;
    }

    private Attributes studyDateTimeRange(String date, String time) {
        Attributes attrs = new Attributes(2);
        attrs.setString(Tag.StudyDate, VR.DA, date);
        attrs.setString(Tag.StudyTime, VR.TM, time);
        return attrs;
    }

    private static Attributes modalitiesInStudy(String value) {
        Attributes attrs = new Attributes(1);
        attrs.setString(Tag.ModalitiesInStudy, VR.CS, value);
        return attrs;
    }

    private ArrayList<String> studyUIDResultList(StudyQuery query)
            throws Exception {
        ArrayList<String> result = new ArrayList<String>();
        while (query.hasMoreMatches()) {
            result.add(query.nextMatch().getString(Tag.StudyInstanceUID, null));
        }
        return result;
    }
}
