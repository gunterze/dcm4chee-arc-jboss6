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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.ejb.EJB;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Sequence;
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
public class InstanceQueryTest {

    private static final String[] ConceptCodeSeqPIDs =
            { "CONCEPT_NAME_CODE_SEQ", "DCM4CHEE_TESTDATA" };

    private static final String[] VerifyingObserverPIDs =
            { "VERIFYING_OBSERVER_SEQ", "DCM4CHEE_TESTDATA" };
    
    private static final String[] TeachingFilePIDs =
            { "TF_INFO", "DCM4CHEE_TESTDATA" };

    private static final QueryParam QUERY_PARAM = new QueryParam();
    private static final QueryParam MATCH_UNKNOWN = new QueryParam().setMatchUnknown(true);
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
    public void testByVerificationFlag() throws Exception {
        query.findInstances(VerifyingObserverPIDs, verificationFlag("VERIFIED", "SR"),
                QUERY_PARAM, STORE_PARAM);
        ArrayList<String> result = sopInstanceUIDResultList(query);
        String SOPIUIDs[] = { "1.2.40.0.13.1.1.99.23.1.2", "1.2.40.0.13.1.1.99.23.1.3" };
        Collection<String> col = Arrays.asList(SOPIUIDs);
        assertTrue(equals(result, col));
        query.close();
    }


    @Test
    public void testByConceptCodeSequence() throws Exception {
        query.findInstances(ConceptCodeSeqPIDs,
                conceptCodeSeq("CONCEPT_NAME_1", "99DCM4CHEE_TEST", null),
                QUERY_PARAM, STORE_PARAM);
        ArrayList<String> result = sopInstanceUIDResultList(query);
        String SOPIUIDs[] = { "1.2.40.0.13.1.1.99.22.1.1" };
        Collection<String> col = Arrays.asList(SOPIUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByConceptCodeSequenceMatchUnknown() throws Exception {
        query.findInstances(ConceptCodeSeqPIDs,
                conceptCodeSeq("CONCEPT_NAME_2", "99DCM4CHEE_TEST", null), 
                MATCH_UNKNOWN, STORE_PARAM);
        ArrayList<String> result = sopInstanceUIDResultList(query);
        String SOPIUIDs[] =
                { "1.2.40.0.13.1.1.99.22.1.2", "1.2.40.0.13.1.1.99.22.1.3" };
        Collection<String> col = Arrays.asList(SOPIUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByVerifyingObserver() throws Exception {
        query.findInstances(VerifyingObserverPIDs,
                verifyingObserver("201106300830", "VerifyingObserver1"), QUERY_PARAM, STORE_PARAM);
        ArrayList<String> result = sopInstanceUIDResultList(query);
        String SOPIUIDs[] =
                { "1.2.40.0.13.1.1.99.23.1.2", "1.2.40.0.13.1.1.99.23.1.3" };
        Collection<String> col = Arrays.asList(SOPIUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByVerifyingObserverMatchUnknown() throws Exception {
        query.findInstances(VerifyingObserverPIDs,
                verifyingObserver("201106300830", "VerifyingObserver1"),
                MATCH_UNKNOWN, STORE_PARAM);
        ArrayList<String> result = sopInstanceUIDResultList(query);
        String SOPIUIDs[] =
                { "1.2.40.0.13.1.1.99.23.1.2", "1.2.40.0.13.1.1.99.23.1.3",
                        "1.2.40.0.13.1.1.99.23.1.1" };
        Collection<String> col = Arrays.asList(SOPIUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByVerifyingObserverRange() throws Exception {
        query.findInstances(VerifyingObserverPIDs,
                verifyingObserver("201106300000-20110701235900", null), QUERY_PARAM, STORE_PARAM);
        ArrayList<String> result = sopInstanceUIDResultList(query);
        String SOPIUIDs[] =
                { "1.2.40.0.13.1.1.99.23.1.2", "1.2.40.0.13.1.1.99.23.1.3" };
        Collection<String> col = Arrays.asList(SOPIUIDs);
        assertTrue(equals(result, col));
        query.close();
    }
    
    @Test
    public void testByContentItem() throws Exception {
        Attributes attrs = new Attributes(1);
        Sequence contentSeq = attrs.newSequence(Tag.ContentSequence, 2);
        contentSeq.add(contentSequenceItem("TCE101", "IHERADTF", null,
                "CONTAINS", "Max"));
        contentSeq.add(contentSequenceItem("TCE104", "IHERADTF", null,
                "CONTAINS", "Max's Abstract"));
        query.findInstances(TeachingFilePIDs, attrs, QUERY_PARAM, STORE_PARAM);
        ArrayList<String> result = sopInstanceUIDResultList(query);
        String SOPIUIDs[] = { "1.2.40.0.13.1.1.99.27.1.1" };
        Collection<String> col = Arrays.asList(SOPIUIDs);
        assertTrue(equals(result, col));
        query.close();
    }
    
    @Test
    public void testByContentItemSequence() throws Exception {
        Attributes attrs = new Attributes(1);
        Sequence contentSeq = attrs.newSequence(Tag.ContentSequence, 2);
        contentSeq.add(contentSequenceItem("TCE104", "IHERADTF", null,
                "CONTAINS", "Moritz's Abstract"));
        contentSeq.add(contentSequenceCodeItem("TCE105", "IHERADTF", null,
                "466.0", "I9C", null, "CONTAINS"));
        query.findInstances(TeachingFilePIDs, attrs, QUERY_PARAM, STORE_PARAM);
        ArrayList<String> result = sopInstanceUIDResultList(query);
        String SOPIUIDs[] = { "1.2.40.0.13.1.1.99.27.1.2" };
        Collection<String> col = Arrays.asList(SOPIUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    private Attributes contentSequenceCodeItem(String value, String designator,
            String version, String value2, String designator2, String version2,
            String relationshipType) {
        Attributes item = new Attributes(4);
        item.setString(Tag.RelationshipType, VR.CS, relationshipType);
        item.setString(Tag.ValueType, VR.CS, "CODE");
        Attributes conceptName = new Attributes(3);
        conceptName.setString(Tag.CodeValue, VR.SH, value);
        conceptName.setString(Tag.CodingSchemeDesignator, VR.SH, designator);
        conceptName.setString(Tag.CodingSchemeVersion, VR.SH, version);
        item.newSequence(Tag.ConceptNameCodeSequence, 1).add(conceptName);
        Attributes conceptCode = new Attributes(3);
        conceptCode.setString(Tag.CodeValue, VR.SH, value2);
        conceptCode.setString(Tag.CodingSchemeDesignator, VR.SH, designator2);
        conceptCode.setString(Tag.CodingSchemeVersion, VR.SH, version2);
        item.newSequence(Tag.ConceptCodeSequence, 1).add(conceptCode);
        return item;
    }

    private Attributes contentSequenceItem(String value, String designator,
            String version, String relationshipType, String textValue) {
        Attributes item = new Attributes(4);
        item.setString(Tag.RelationshipType, VR.CS, relationshipType);
        item.setString(Tag.TextValue, VR.UT, textValue);
        item.setString(Tag.ValueType, VR.CS, "TEXT");
        Attributes nestedDs = new Attributes(3);
        nestedDs.setString(Tag.CodeValue, VR.SH, value);
        nestedDs.setString(Tag.CodingSchemeDesignator, VR.SH, designator);
        nestedDs.setString(Tag.CodingSchemeVersion, VR.SH, version);
        item.newSequence(Tag.ConceptNameCodeSequence, 1).add(nestedDs);
        return item;
    }

    private boolean equals(ArrayList<String> result, Collection<String> col) {
        if (result.containsAll(col) && result.size() == col.size())
            return true;
        else
            return false;
    }

    private Attributes verifyingObserver(String dateTime, String name) {
        Attributes attrs = new Attributes(1);
        Attributes item = new Attributes(2);
        item.setString(Tag.VerificationDateTime, VR.DT, dateTime);
        item.setString(Tag.VerifyingObserverName, VR.PN, name);
        attrs.newSequence(Tag.VerifyingObserverSequence, 1).add(item);
        return attrs;
    }

    private Attributes verificationFlag(String flag, String modality) {
        Attributes attrs = new Attributes(2);
        attrs.setString(Tag.Modality, VR.CS, modality);
        attrs.setString(Tag.VerificationFlag, VR.CS, flag);
        return attrs;
    }

    private Attributes conceptCodeSeq(String value, String designator,
            String version) throws Exception {
        Attributes attrs = new Attributes(1);
        Attributes item = new Attributes(3);
        item.setString(Tag.CodeValue, VR.SH, value);
        item.setString(Tag.CodingSchemeDesignator, VR.SH, designator);
        item.setString(Tag.CodingSchemeVersion, VR.SH, version);
        attrs.newSequence(Tag.ConceptNameCodeSequence, 1).add(item);
        return attrs;
    }

    private ArrayList<String> sopInstanceUIDResultList(CompositeQuery query)
            throws Exception {
        ArrayList<String> result = new ArrayList<String>();
        while (query.hasMoreMatches()) {
            result.add(query.nextMatch().getString(Tag.SOPInstanceUID, null));
        }
        return result;
    }
}
