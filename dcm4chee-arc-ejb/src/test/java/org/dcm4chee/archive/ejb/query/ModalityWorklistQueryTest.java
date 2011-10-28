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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.ejb.EJB;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;
import org.dcm4che.soundex.ESoundex;
import org.dcm4chee.archive.persistence.Issuer;
import org.dcm4chee.archive.persistence.ScheduledProcedureStep;
import org.dcm4chee.archive.persistence.StoreParam;
import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@RunWith(Arquillian.class)
public class ModalityWorklistQueryTest {

    private static final QueryParam QUERY_PARAM =
            new QueryParam().setCombinedDatetimeMatching(true);
    private static final StoreParam STORE_PARAM = new StoreParam();
    static { STORE_PARAM.setFuzzyStr(new ESoundex()); }
    private static final Issuer ISSUER = new Issuer("DCM4CHEE_TESTDATA", "*", "*");

    private static IDWithIssuer[] pids(String id) {
        return new IDWithIssuer[] { new IDWithIssuer(id, ISSUER) };
    }

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(
                        QueryParam.class,
                        IDWithIssuer.class,
                        ModalityWorklistQuery.class,
                        ModalityWorklistQueryBean.class,
                        Builder.class,
                        MatchDateTimeRange.class,
                        MatchPersonName.class)
                .addAsWebInfResource("META-INF/modality-worklist-query-ejb-jar.xml", "ejb-jar.xml");
    }

    @EJB
    private ModalityWorklistQuery query;

    @Test
    public void testByPatientID() throws Exception {
        query.findScheduledProcedureSteps(
                pids("MWL_TEST"),
                new Attributes(),
                QUERY_PARAM, STORE_PARAM);
        assertSetEquals(spsids(), "9933.1", "9934.1", "9934.2");
        query.close();
    }

    @Test
    public void testByModality() throws Exception {
        query.findScheduledProcedureSteps(
                null,
                sps(Tag.Modality, VR.CS, "CT"),
                QUERY_PARAM, STORE_PARAM);
        assertSetEquals(spsids(), "9933.1");
        query.close();
    }

    @Test
    public void testByAccessionNumber() throws Exception {
        query.findScheduledProcedureSteps(
                null,
                attrs(Tag.AccessionNumber, VR.SH, "MWL_TEST"),
                QUERY_PARAM, STORE_PARAM);
        assertSetEquals(spsids(), "9933.1", "9934.1", "9934.2");
        query.close();
    }

    @Test
    public void testByStudyInstanceUID() throws Exception {
        query.findScheduledProcedureSteps(
                null,
                attrs(Tag.StudyInstanceUID, VR.UI, "1.2.40.0.13.1.1.99.33"),
                QUERY_PARAM, STORE_PARAM);
        assertSetEquals(spsids(), "9933.1");
        query.close();
    }

    @Test
    public void testByRequestedProcedureID() throws Exception {
        query.findScheduledProcedureSteps(
                null,
                attrs(Tag.RequestedProcedureID, VR.SH, "P-9934"),
                QUERY_PARAM, STORE_PARAM);
        assertSetEquals(spsids(), "9934.1", "9934.2");
        query.close();
    }

    @Test
    public void testByScheduledProcedureID() throws Exception {
        query.findScheduledProcedureSteps(
                null,
                sps(Tag.ScheduledProcedureStepID, VR.SH, "9934.2"),
                QUERY_PARAM, STORE_PARAM);
        assertSetEquals(spsids(), "9934.2");
        query.close();
    }

    @Test
    public void testByStatus() throws Exception {
        query.findScheduledProcedureSteps(
                null,
                sps(Tag.ScheduledProcedureStepStatus, VR.CS, 
                        ScheduledProcedureStep.ARRIVED, ScheduledProcedureStep.READY),
                QUERY_PARAM, STORE_PARAM);
        assertSetEquals(spsids(), "9934.1");
        query.close();
    }

    @Test
    public void testByAET1() throws Exception {
        query.findScheduledProcedureSteps(
                null,
                sps(Tag.ScheduledStationAETitle, VR.AE, "AET_MR1"),
                QUERY_PARAM, STORE_PARAM);
        assertSetEquals(spsids(), "9934.1");
        query.close();
    }

    @Test
    public void testByAET2() throws Exception {
        query.findScheduledProcedureSteps(
                null,
                sps(Tag.ScheduledStationAETitle, VR.AE, "AET_MR2"),
                QUERY_PARAM, STORE_PARAM);
        assertSetEquals(spsids(), "9934.1", "9934.2");
        query.close();
    }

    @Test
    public void testByPerformingPhysican() throws Exception {
        query.findScheduledProcedureSteps(
                null,
                sps(Tag.ScheduledPerformingPhysicianName, VR.PN,
                        "ScheduledPerformingPhysicianName3"),
                QUERY_PARAM, STORE_PARAM);
        assertSetEquals(spsids(), "9934.2");
        query.close();
    }

    @Test
    public void testByStartDate() throws Exception {
        query.findScheduledProcedureSteps(
                null,
                sps(Tag.ScheduledProcedureStepStartDate, VR.DA, "20111025"),
                QUERY_PARAM, STORE_PARAM);
        assertSetEquals(spsids(), "9934.1", "9934.2");
        query.close();
    }

    @Test
    public void testByStartDateTime() throws Exception {
        query.findScheduledProcedureSteps(
                null,
                spsStartDateTime("20111025", "14-15"),
                QUERY_PARAM, STORE_PARAM);
        assertSetEquals(spsids(), "9934.1");
        query.close();
    }

    private Attributes attrs(int tag, VR vr, String value) {
        Attributes attrs = new Attributes(1);
        attrs.setString(tag, vr, value);
        return attrs;
    }

    private static Attributes sps(int tag, VR vr, String... values) {
        Attributes attrs = new Attributes(1);
        Attributes item = new Attributes(1);
        attrs.newSequence(Tag.ScheduledProcedureStepSequence, 1).add(item);
        item.setString(tag, vr, values);
        return attrs;
    }

    private static Attributes spsStartDateTime(String da, String tm) {
        Attributes attrs = new Attributes(1);
        Attributes item = new Attributes(2);
        attrs.newSequence(Tag.ScheduledProcedureStepSequence, 1).add(item);
        item.setString(Tag.ScheduledProcedureStepStartDate, VR.DA, da);
        item.setString(Tag.ScheduledProcedureStepStartTime, VR.TM, da);
        return attrs;
    }

    private <T> void assertSetEquals(List<T> resultSPSIDs, T ... expected) {
        assertEquals(new HashSet<T>(Arrays.asList(expected)), new HashSet<T>(resultSPSIDs));
    }

    private List<String> spsids() {
        List<String> list = new ArrayList<String>();
        while (query.hasMoreMatches())
            list.add(query.nextMatch()
                    .getNestedDataset(Tag.ScheduledProcedureStepSequence)
                    .getString(Tag.ScheduledProcedureStepID));
        return list ;
    }
}
