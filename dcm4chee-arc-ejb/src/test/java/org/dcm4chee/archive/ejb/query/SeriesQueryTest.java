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

import javax.ejb.EJB;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;
import org.dcm4chee.archive.ejb.query.Matching;
import org.dcm4chee.archive.ejb.query.SeriesQuery;
import org.dcm4chee.archive.ejb.query.SeriesQueryBean;
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
public class SeriesQueryTest {
    
    private static final String[] RequestedAttributesSeqPIDs =
            { "REQ_ATTRS_SEQ", "DCM4CHEE_TESTDATA" };

    private static final String[] ModalitiesInStudyPIDs =
            { "MODS_IN_STUDY", "DCM4CHEE_TESTDATA" };

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar").addClasses(
                SeriesQuery.class, SeriesQueryBean.class, Matching.class,
                RangeMatching.class);
    }

    @EJB
    private SeriesQuery query;

    @Test
    public void testByModality() throws Exception {
        query.find(null, ModalitiesInStudyPIDs,
                modality("PR"), false, false);
        assertTrue(countMatches(query, 2));
        query.close();
    }

    @Test
    public void testByModalitiesInStudyPR() throws Exception {
        query.find(null, ModalitiesInStudyPIDs, modalitiesInStudy("PR"), false, false);
        assertTrue(countMatches(query,4));
        query.close();
    }

    @Test
    public void testByModalitiesInStudyMatchUnknownPR() throws Exception {
        query.find(null, ModalitiesInStudyPIDs, modalitiesInStudy("PR"), true, false);
        assertTrue(countMatches(query, 5));
        query.close();
    }
    
    @Test
    public void testByModalitiesInStudyCT() throws Exception {
        query.find(null, ModalitiesInStudyPIDs, modalitiesInStudy("CT"), false, false);
        assertTrue(countMatches(query, 2));
        query.close();
    }

    @Test
    public void testByModalitiesInStudyMatchUnknownCT() throws Exception {
        query.find(null, ModalitiesInStudyPIDs, modalitiesInStudy("CT"), true, false);
        assertTrue(countMatches(query, 3));
        query.close();
    }

    @Test
    public void testByRequestAttributesSequence() throws Exception {
        query.find(null, RequestedAttributesSeqPIDs,
                requestAttributesSequence("P-9913", "9913.1", null,
                        "A1234", "DCM4CHEE_TESTDATA_ACCNO_ISSUER_1", null,
                        null), false, false);
        assertTrue(countMatches(query, 1));
        query.close();
    }

    private Attributes modality(String value) {
        Attributes attrs = new Attributes(1);
        attrs.setString(Tag.Modality, VR.CS, value);
        return attrs;
    }

    static Attributes modalitiesInStudy(String value) {
        Attributes attrs = new Attributes(1);
        attrs.setString(Tag.ModalitiesInStudy, VR.CS, value);
        return attrs;
    }

    private boolean countMatches(SeriesQuery query, int count) throws Exception {
        int i = 0;
        while(query.hasMoreMatches()){
            query.nextMatch();
            i++;
        }
        return count==i;
    }

    private Attributes requestAttributesSequence(String reqProcId,
            String schedProcId, String physName, String accNo, String entityId,
            String entityUid, String entityType) {
        Attributes item = new Attributes(4);
        item.setString(Tag.RequestedProcedureID, VR.SH, reqProcId);
        item.setString(Tag.ScheduledProcedureStepID, VR.SH, schedProcId);
        item.setString(Tag.ReferringPhysicianName, VR.PN, physName);
        item.setString(Tag.AccessionNumber, VR.SH, accNo);

        Attributes issuer = new Attributes(3);
        issuer.setString(Tag.LocalNamespaceEntityID, VR.UT, entityId);
        issuer.setString(Tag.UniversalEntityID, VR.UT, entityUid);
        issuer.setString(Tag.UniversalEntityIDType, VR.CS, entityType);

        item.newSequence(Tag.IssuerOfAccessionNumberSequence, 1).add(issuer);

        Attributes attrs = new Attributes(1);
        attrs.newSequence(Tag.RequestAttributesSequence, 1).add(item);
        return attrs;
    }
}
