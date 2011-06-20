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

    private static final String studyDesc = "Test data for date range matching";

    @Deployment
    public static JavaArchive createDeployment() {
	return ShrinkWrap.create(JavaArchive.class, "test.jar").addClasses(
	        StudyQuery.class, StudyQueryBean.class, Matching.class, 
	        FormatDate.class);
    }

    @EJB
    private StudyQuery query;

    // @Test
    // public void testByModalitiesInStudy() throws Exception {
    // query.find(null, new String[] { "CT5", "DCM4CHEE_TESTDATA" },
    // modalitiesInStudy("SR"), false, false);
    // assertTrue(query.hasMoreMatches());
    // query.nextMatch();
    // assertFalse(query.hasMoreMatches());
    // query.close();
    // }

    @Test
    public void testByDateTime() throws Exception {
	query
	        .find(null, null, studyDateTimeRange("20110620", "103000.000",
	                studyDesc), false, false);
	assertTrue(query.hasMoreMatches());
	String studyUID =
	        query.nextMatch().getString(Tag.StudyInstanceUID, null);
	assertFalse(query.hasMoreMatches());
	assertTrue(studyUID.equals("1.2.40.0.13.1.1.99.3"));
	query.close();
    }
    
    @Test
    public void testByOpenEndTime() throws Exception {
	query
	        .find(null, null, studyTimeRange("1030-",
	                studyDesc), false, false);
	assertTrue(query.hasMoreMatches());
	ArrayList<String> result = studyUIDResultList(query);
	String studyUIDs[] = { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
		"1.2.40.0.13.1.1.99.5", "1.2.40.0.13.1.1.99.6"};
	Collection<String> col = Arrays.asList(studyUIDs);
	assertTrue(result.containsAll(col));
	query.close();
    }
    
    @Test
    public void testByOpenStartTime() throws Exception {
	query
	        .find(null, null, studyTimeRange("-1430",
	                studyDesc), false, false);
	assertTrue(query.hasMoreMatches());
	ArrayList<String> result = studyUIDResultList(query);
	String studyUIDs[] = { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
		"1.2.40.0.13.1.1.99.5", "1.2.40.0.13.1.1.99.6"};
	Collection<String> col = Arrays.asList(studyUIDs);
	assertTrue(result.containsAll(col));
	query.close();
    }
    
    @Test
    public void testByDateTimeMatchUnknown() throws Exception {
	query
	        .find(null, null, studyDateTimeRange("20110620", "103000.000",
	                studyDesc), true, false);
	assertTrue(query.hasMoreMatches());
	ArrayList<String> result = studyUIDResultList(query);
	String studyUIDs[] = { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.9" };
	Collection<String> col = Arrays.asList(studyUIDs);
	assertTrue(result.containsAll(col));
	query.close();
    }

    @Test
    public void testByTimeRange() throws Exception {
	query.find(null, null, studyTimeRange("1030-1430", studyDesc), false,
	        false);
	assertTrue(query.hasMoreMatches());
	ArrayList<String> result = studyUIDResultList(query);
	String studyUIDs[] = { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
		"1.2.40.0.13.1.1.99.5", "1.2.40.0.13.1.1.99.6"};
	Collection<String> col = Arrays.asList(studyUIDs);
	assertTrue(result.containsAll(col));
	query.close();
    }

    @Test
    public void testByDateRange() throws Exception {
	query.find(null, null, studyDateRange("20100620-20110620", studyDesc),
	        false, false);
	assertTrue(query.hasMoreMatches());
	ArrayList<String> result = studyUIDResultList(query);
	String studyUIDs[] = { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
		"1.2.40.0.13.1.1.99.5", "1.2.40.0.13.1.1.99.6", 
		"1.2.40.0.13.1.1.99.7", "1.2.40.0.13.1.1.99.8"};
	Collection<String> col = Arrays.asList(studyUIDs);
	assertTrue(result.containsAll(col));
	query.close();
    }
    
    @Test
    public void testByDateTimeRange() throws Exception {
	query.find(null, null, studyDateTimeRange("20100620-20110620","1030-1430", studyDesc),
	        false, false);
	assertTrue(query.hasMoreMatches());
	ArrayList<String> result = studyUIDResultList(query);
	String studyUIDs[] = { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
		"1.2.40.0.13.1.1.99.5", "1.2.40.0.13.1.1.99.6"};
	Collection<String> col = Arrays.asList(studyUIDs);
	assertTrue(result.containsAll(col));
	query.close();
    }
    
    @Test
    public void testByDateTimeRangeCombined() throws Exception {
	query.find(null, null, studyDateTimeRange("20100620-20110620","1040-1430", studyDesc),
	        false, true);
	assertTrue(query.hasMoreMatches());
	ArrayList<String> result = studyUIDResultList(query);
	String studyUIDs[] = { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
		"1.2.40.0.13.1.1.99.8", "1.2.40.0.13.1.1.99.7",
		"1.2.40.0.13.1.1.99.6"};
	Collection<String> col = Arrays.asList(studyUIDs);
	assertTrue(result.containsAll(col));
	query.close();
    }
    
    @Test
    public void testByDateTimeRangeCombinedMatchUnknown() throws Exception {
	query.find(null, null, studyDateTimeRange("20100620-20110620","1040-1430", studyDesc),
	        true, true);
	assertTrue(query.hasMoreMatches());
	ArrayList<String> result = studyUIDResultList(query);
	String studyUIDs[] = { "1.2.40.0.13.1.1.99.3", "1.2.40.0.13.1.1.99.4",
		"1.2.40.0.13.1.1.99.8", "1.2.40.0.13.1.1.99.7",
		"1.2.40.0.13.1.1.99.6", "1.2.40.0.13.1.1.99.9"};
	Collection<String> col = Arrays.asList(studyUIDs);
	assertTrue(result.containsAll(col));
	query.close();
    }

    private Attributes studyDateTimeRange(String date, String time, String desc) {
	Attributes attrs = new Attributes(3);
	attrs.setString(Tag.StudyDate, VR.DA, date);
	attrs.setString(Tag.StudyTime, VR.TM, time);
	attrs.setString(Tag.StudyDescription, VR.LO, desc);
	return attrs;
    }

    private Attributes studyDateRange(String date, String desc) {
	Attributes attrs = new Attributes(3);
	attrs.setString(Tag.StudyDate, VR.DA, date);
	attrs.setString(Tag.StudyDescription, VR.LO, desc);
	return attrs;
    }

    private Attributes studyTimeRange(String time, String desc) {
	Attributes attrs = new Attributes(3);
	attrs.setString(Tag.StudyTime, VR.TM, time);
	attrs.setString(Tag.StudyDescription, VR.LO, desc);
	return attrs;
    }

    private Attributes modalitiesInStudy(String value) {
	Attributes attrs = new Attributes(1);
	attrs.setString(Tag.ModalitiesInStudy, VR.CS, value);
	return attrs;
    }
    
    private ArrayList<String> studyUIDResultList(StudyQuery query) throws Exception{
	ArrayList<String> result = new ArrayList<String>();
	while(query.hasMoreMatches()){
	    result.add(query.nextMatch().getString(Tag.StudyInstanceUID, null));
	}
	return result;
    }
}
