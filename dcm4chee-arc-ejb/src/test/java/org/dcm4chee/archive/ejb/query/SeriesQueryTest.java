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

import java.util.EnumSet;

import javax.ejb.EJB;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;
import org.dcm4che.io.SAXReader;
import org.dcm4che.net.pdu.QueryOption;
import org.dcm4che.soundex.ESoundex;
import org.dcm4chee.archive.ejb.query.Matching;
import org.dcm4chee.archive.ejb.query.SeriesQuery;
import org.dcm4chee.archive.ejb.query.SeriesQueryBean;
import org.dcm4chee.archive.persistence.AttributeFilter;
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

    private static final EnumSet<QueryOption> NO_QUERY_OPTION =
            EnumSet.noneOf(QueryOption.class);
    
    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(
                        SeriesQuery.class,
                        SeriesQueryBean.class,
                        Matching.class,
                        RangeMatching.class,
                        PersonNameMatching.class);
    }

    @EJB
    private SeriesQuery query;

    private AttributeFilter filter() throws Exception {
        return new AttributeFilter(
                SAXReader.parse("resource:dcm4chee-arc/patient-attribute-filter.xml"),
                SAXReader.parse("resource:dcm4chee-arc/study-attribute-filter.xml"),
                SAXReader.parse("resource:dcm4chee-arc/series-attribute-filter.xml"),
                SAXReader.parse("resource:dcm4chee-arc/instance-attribute-filter.xml"),
                SAXReader.parse("resource:dcm4chee-arc/case-insensitive-attributes.xml"),
                new ESoundex());
    }

    @Test
    public void testByModality() throws Exception {
        query.find(null, ModalitiesInStudyPIDs, modality("PR"), filter(), NO_QUERY_OPTION, false, null);
        assertTrue(countMatches(query, 2));
        query.close();
    }

    @Test
    public void testByModalitiesInStudyPR() throws Exception {
        query.find(null, ModalitiesInStudyPIDs, modalitiesInStudy("PR"), filter(),
                NO_QUERY_OPTION, false, null);
        assertTrue(countMatches(query,4));
        query.close();
    }

    @Test
    public void testByModalitiesInStudyMatchUnknownPR() throws Exception {
        query.find(null, ModalitiesInStudyPIDs, modalitiesInStudy("PR"), filter(),
                NO_QUERY_OPTION, true, null);
        assertTrue(countMatches(query, 5));
        query.close();
    }
    
    @Test
    public void testByModalitiesInStudyCT() throws Exception {
        query.find(null, ModalitiesInStudyPIDs, modalitiesInStudy("CT"), filter(),
                NO_QUERY_OPTION, false, null);
        assertTrue(countMatches(query, 2));
        query.close();
    }

    @Test
    public void testByModalitiesInStudyMatchUnknownCT() throws Exception {
        query.find(null, ModalitiesInStudyPIDs, modalitiesInStudy("CT"), filter(),
                NO_QUERY_OPTION, true, null);
        assertTrue(countMatches(query, 3));
        query.close();
    }

    @Test
    public void testByRequestAttributesSequence() throws Exception {
        Attributes keys = new Attributes(1);

        Attributes item = new Attributes(4);
        keys.newSequence(Tag.RequestAttributesSequence, 1).add(item);
        item.setString(Tag.RequestedProcedureID, VR.SH, "P-9913");
        item.setString(Tag.ScheduledProcedureStepID, VR.SH, "9913.1");
        item.setNull(Tag.ReferringPhysicianName, VR.PN);
        item.setString(Tag.AccessionNumber, VR.SH, "A1234");

        Attributes issuer = new Attributes(3);
        item.newSequence(Tag.IssuerOfAccessionNumberSequence, 1).add(issuer);
        issuer.setString(Tag.LocalNamespaceEntityID, VR.UT, "DCM4CHEE_TESTDATA_ACCNO_ISSUER_1");
        issuer.setNull(Tag.UniversalEntityID, VR.UT);
        issuer.setNull(Tag.UniversalEntityIDType, VR.CS);
        
        query.find(null, RequestedAttributesSeqPIDs, keys, filter(), NO_QUERY_OPTION, false, null);
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
}
