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
public class InstanceQueryTest {

    private static final String[] ConceptCodeSeqPIDs =
            { "CONCEPT_NAME_CODE_SEQ", "DCM4CHEE_TESTDATA" };

    private static final String[] VerifyingObserverPIDs =
            { "VERIFYING_OBSERVER_SEQ", "DCM4CHEE_TESTDATA" };

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar").addClasses(
                InstanceQuery.class, InstanceQueryBean.class, Matching.class,
                RangeMatching.class);
    }

    @EJB
    private InstanceQuery query;

    @Test
    public void testByVerificationFlag() throws Exception {
        query.find(null, VerifyingObserverPIDs, verificationFlag("VERIFIED",
                "SR"), false, false);
        ArrayList<String> result = sopInstanceUIDResultList(query);
        String SOPIUIDs[] =
                { "1.2.40.0.13.1.1.99.23.1.2", "1.2.40.0.13.1.1.99.23.1.3" };
        Collection<String> col = Arrays.asList(SOPIUIDs);
        assertTrue(equals(result, col));
        query.close();
        ;
    }

    @Test
    public void testByConceptCodeSequence() throws Exception {
        query.find(null, ConceptCodeSeqPIDs, conceptCodeSeq("CONCEPT_NAME_1",
                "99DCM4CHEE_TEST", null), false, false);
        ArrayList<String> result = sopInstanceUIDResultList(query);
        String SOPIUIDs[] = { "1.2.40.0.13.1.1.99.22.1.1" };
        Collection<String> col = Arrays.asList(SOPIUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByConceptCodeSequenceMatchUnknown() throws Exception {
        query.find(null, ConceptCodeSeqPIDs, conceptCodeSeq("CONCEPT_NAME_2",
                "99DCM4CHEE_TEST", null), true, false);
        ArrayList<String> result = sopInstanceUIDResultList(query);
        String SOPIUIDs[] =
                { "1.2.40.0.13.1.1.99.22.1.2", "1.2.40.0.13.1.1.99.22.1.3" };
        Collection<String> col = Arrays.asList(SOPIUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByVerifyingObserver() throws Exception {
        query.find(null, VerifyingObserverPIDs, verifyingObserver(
                "201106300830", "VerifyingObserver1"), false, false);
        ArrayList<String> result = sopInstanceUIDResultList(query);
        String SOPIUIDs[] =
                { "1.2.40.0.13.1.1.99.23.1.2", "1.2.40.0.13.1.1.99.23.1.3" };
        Collection<String> col = Arrays.asList(SOPIUIDs);
        assertTrue(equals(result, col));
        query.close();
    }

    @Test
    public void testByVerifyingObserverMatchUnknown() throws Exception {
        query.find(null, VerifyingObserverPIDs, verifyingObserver(
                "201106300830", "VerifyingObserver1"), true, false);
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
        query.find(null, VerifyingObserverPIDs, verifyingObserver(
                "201106300000-20110701235900", null), false, false);
        ArrayList<String> result = sopInstanceUIDResultList(query);
        String SOPIUIDs[] =
                { "1.2.40.0.13.1.1.99.23.1.2", "1.2.40.0.13.1.1.99.23.1.3" };
        Collection<String> col = Arrays.asList(SOPIUIDs);
        assertTrue(equals(result, col));
        query.close();
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

    private ArrayList<String> sopInstanceUIDResultList(InstanceQuery query)
            throws Exception {
        ArrayList<String> result = new ArrayList<String>();
        while (query.hasMoreMatches()) {
            result.add(query.nextMatch().getString(Tag.SOPInstanceUID, null));
        }
        return result;
    }
}
