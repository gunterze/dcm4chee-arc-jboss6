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
import org.dcm4chee.archive.ejb.permission.StudyPermissionManager;
import org.dcm4chee.archive.ejb.permission.StudyPermissionManagerBean;
import org.dcm4chee.archive.ejb.store.Entity;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.dcm4chee.archive.persistence.Issuer;
import org.dcm4chee.archive.persistence.StudyPermissionAction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Michael Backhaus <michael.backhaus@agfa.com>
 */
@RunWith(Arquillian.class)
public class StudyQueryTest {

    private static final Issuer ISSUER = new Issuer("DCM4CHEE_TESTDATA", "*", "*");
    private static final AttributeFilter[] ATTR_FILTERS = {
        new AttributeFilter(),
        new AttributeFilter(),
        new AttributeFilter(),
        new AttributeFilter()
    };

    private static QueryParam queryParam(boolean matchUnknown, boolean datetime, String... roles) {
        QueryParam queryParam = new QueryParam();
        queryParam.setMatchUnknown(matchUnknown);
        queryParam.setCombinedDatetimeMatching(datetime);
        queryParam.setRoles(roles);
        queryParam.setAttributeFilters(ATTR_FILTERS);
        queryParam.setFuzzyStr(new ESoundex());
        return queryParam;
    }

    private static final QueryParam QUERY_PARAM = queryParam(false, false);
    private static final QueryParam MATCH_UNKNOWN = queryParam(true, false);
    private static final QueryParam COMBINED_DATE_TIME = queryParam(false, true);
    private static final QueryParam COMBINED_DATE_TIME_MATCH_UNKNOWN = queryParam(true, true);

    private static IDWithIssuer[] pids(String id) {
        return new IDWithIssuer[] { new IDWithIssuer(id, ISSUER) };
    }

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(
                        Entity.class,
                        QueryParam.class,
                        IDWithIssuer.class,
                        CompositeQuery.class,
                        CompositeQueryBean.class,
                        CompositeQueryImpl.class,
                        PatientQueryImpl.class,
                        StudyQueryImpl.class,
                        SeriesQueryImpl.class,
                        InstanceQueryImpl.class,
                        Builder.class,
                        MatchDateTimeRange.class,
                        MatchPersonName.class,
                        StudyPermissionManager.class,
                        StudyPermissionManagerBean.class)
                .addAsWebInfResource("META-INF/composite-query-ejb-jar.xml", "ejb-jar.xml");
    }

    @EJB()
    private CompositeQuery query;
    
    @EJB
    private StudyPermissionManager mgr;

    @Test
    public void testByModalitiesInStudyPR() throws Exception {
        query.findStudies(pids("MODS_IN_STUDY"), modalitiesInStudy("PR"), QUERY_PARAM);
        assertTrue(countMatches(query, 2));
        query.close();
    }
    
    @Test
    public void testByModalitiesInStudyMatchUnknownPR() throws Exception {
        query.findStudies(pids("MODS_IN_STUDY"), modalitiesInStudy("PR"), MATCH_UNKNOWN);
        assertTrue(countMatches(query, 3));
        query.close();
    }
    
    @Test
    public void testByModalitiesInStudyCT() throws Exception {
        query.findStudies(pids("MODS_IN_STUDY"), modalitiesInStudy("CT"), QUERY_PARAM);
        assertTrue(countMatches(query, 1));
        query.close();
    }
    
    @Test
    public void testByModalitiesInStudyMatchUnknownCT() throws Exception {
        query.findStudies(pids("MODS_IN_STUDY"), modalitiesInStudy("CT"), MATCH_UNKNOWN);
        assertTrue(countMatches(query, 2));
        query.close();
    }

    @Test
    public void testByDateTime() throws Exception {
        query.findStudies(pids("RANGE-MATCHING"), studyDateTimeRange("20110620", "103000.000"),
                QUERY_PARAM);
        assertTrue(query.hasMoreMatches());
        String studyUID = query.nextMatch().getString(Tag.StudyInstanceUID);
        assertFalse(query.hasMoreMatches());
        assertTrue(studyUID.equals("1.2.40.0.13.1.1.99.3"));
        query.close();
    }

    @Test
    public void testByOpenEndTime() throws Exception {
        query.findStudies(pids("RANGE-MATCHING"), studyDateTimeRange(null, "1030-"), QUERY_PARAM);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyIUIDResultList(query);
        String studyUIDs[] =
                { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
                        "1.2.40.0.13.1.1.99.5", "1.2.40.0.13.1.1.99.6" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByOpenStartTime() throws Exception {
        query.findStudies(pids("RANGE-MATCHING"), studyDateTimeRange(null, "-1430"), QUERY_PARAM);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyIUIDResultList(query);
        String studyUIDs[] =
                { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
                        "1.2.40.0.13.1.1.99.5", "1.2.40.0.13.1.1.99.6" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }
    
    @Test
    public void testByDateTimeMatchUnknown() throws Exception {
        query.findStudies(pids("RANGE-MATCHING"), studyDateTimeRange("20110620", "103000.000"),
                MATCH_UNKNOWN);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyIUIDResultList(query);
        String studyUIDs[] = { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.8","1.2.40.0.13.1.1.99.9" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByTimeRange() throws Exception {
        query.findStudies(pids("RANGE-MATCHING"), studyDateTimeRange(null, "1030-1430"),
                QUERY_PARAM);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyIUIDResultList(query);
        String studyUIDs[] =
                { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
                        "1.2.40.0.13.1.1.99.5", "1.2.40.0.13.1.1.99.6" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByDateRange() throws Exception {
        query.findStudies(pids("RANGE-MATCHING"),
                studyDateTimeRange("20100620-20110620", null),
                QUERY_PARAM);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyIUIDResultList(query);
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
        query.findStudies(pids("RANGE-MATCHING"),
                studyDateTimeRange("20100620-20110620", "1030-1430"),
                QUERY_PARAM);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyIUIDResultList(query);
        String studyUIDs[] =
                { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
                        "1.2.40.0.13.1.1.99.5", "1.2.40.0.13.1.1.99.6" };
        Collection<String> col = Arrays.asList(studyUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByDateTimeRangeCombined() throws Exception {
        query.findStudies(pids("RANGE-MATCHING"),
                studyDateTimeRange("20100620-20110620", "1040-1430"),
                COMBINED_DATE_TIME);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyIUIDResultList(query);
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
        query.findStudies(pids("RANGE-MATCHING"),
                studyDateTimeRange("20100620-", "1040-"),
                COMBINED_DATE_TIME);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyIUIDResultList(query);
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
        query.findStudies(pids("RANGE-MATCHING"),
                studyDateTimeRange("-20110620", "-1420"),
                COMBINED_DATE_TIME);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyIUIDResultList(query);
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
        query.findStudies(pids("RANGE-MATCHING"),
                studyDateTimeRange("20100620-20110620", "1040-1430"),
                COMBINED_DATE_TIME_MATCH_UNKNOWN);
        assertTrue(query.hasMoreMatches());
        ArrayList<String> result = studyIUIDResultList(query);
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
        query.findStudies(pids("ISSUER_OF_ACCNO"),
                issuerOfAccessionNumber("A1234", "DCM4CHEE_TESTDATA_ACCNO_ISSUER_1", null, null),
                QUERY_PARAM);
        assertTrue(countMatches(query, 1));
        query.close();
    }

    @Test
    public void testByIssuerOfAccessionNumberMatchUnknown() throws Exception {
        query.findStudies(pids("ISSUER_OF_ACCNO"),
                issuerOfAccessionNumber("A1234","DCM4CHEE_TESTDATA_ACCNO_ISSUER_2", null, null),
                MATCH_UNKNOWN);
        assertTrue(countMatches(query, 2));
        query.close();
    }

    @Test
    public void testByProcedureCodes() throws Exception {
        query.findStudies(pids("PROC_CODE_SEQ"),
                procedureCodes("PROC_CODE_1", "99DCM4CHEE_TEST", null), QUERY_PARAM);
        assertTrue(countMatches(query, 1));
        query.close();
    }
    
    @Test
    public void testByProcedureCodesMatchUnknown() throws Exception {
        query.findStudies(pids("PROC_CODE_SEQ"),
                procedureCodes("PROC_CODE_2", "99DCM4CHEE_TEST", null), MATCH_UNKNOWN);
        assertTrue(countMatches(query, 2));
        query.close();
    }
    
    @Before
    public void clearDB(){
        mgr.revokeStudyPermission("1.2.40.0.13.1.1.99.10", "DCM4CHEE_TEST", StudyPermissionAction.QUERY);
        mgr.revokeStudyPermission("1.2.40.0.13.1.1.99.11", "DCM4CHEE_TEST", StudyPermissionAction.QUERY);
        mgr.revokeStudyPermission("1.2.40.0.13.1.1.99.12", "DCM4CHEE_TEST", StudyPermissionAction.QUERY);
    }

    @Test
    public void testByStudyPermission() throws Exception {
        String StudyIUIDs[] = { "1.2.40.0.13.1.1.99.10", "1.2.40.0.13.1.1.99.11", 
        "1.2.40.0.13.1.1.99.12" };
        Collection<String> col = Arrays.asList(StudyIUIDs);
        for (String studyIUID : StudyIUIDs)
            assertTrue(mgr.grantStudyPermission(studyIUID, "DCM4CHEE_TEST", StudyPermissionAction.QUERY));
        query.findStudies(null, new Attributes(),
                queryParam(false, false, "DCM4CHEE_TEST", "FooBar"));
        ArrayList<String> result = studyIUIDResultList(query);
        assertTrue(equals(result, col));
        query.findStudies(null, new Attributes(), queryParam(false, false, "FooBar"));
        result = studyIUIDResultList(query);
        assertFalse(equals(result, col));
        query.close();
        for (String studyIUID : StudyIUIDs)
            assertTrue(mgr.revokeStudyPermission(studyIUID, "DCM4CHEE_TEST", StudyPermissionAction.QUERY));
    }

    private boolean equals(ArrayList<String> result, Collection<String> col) {
        if (result.containsAll(col) && result.size()==col.size())
            return true;
        else 
            return false;
    }
    
    private boolean countMatches(CompositeQuery query, int count) throws Exception {
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

    private Attributes issuerOfAccessionNumber(String accno, String id, String uid, String type) {
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

    private ArrayList<String> studyIUIDResultList(CompositeQuery query)
            throws Exception {
        ArrayList<String> result = new ArrayList<String>();
        while (query.hasMoreMatches()) {
            result.add(query.nextMatch().getString(Tag.StudyInstanceUID));
        }
        return result;
    }
}
