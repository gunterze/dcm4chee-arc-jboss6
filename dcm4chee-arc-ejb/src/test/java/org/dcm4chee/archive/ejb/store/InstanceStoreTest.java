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

package org.dcm4chee.archive.ejb.store;

import static org.junit.Assert.*;

import javax.ejb.EJB;

import org.dcm4che.io.SAXReader;
import org.dcm4che.util.StringUtils;
import org.dcm4chee.archive.ejb.store.CodeFactory;
import org.dcm4chee.archive.ejb.store.InstanceStore;
import org.dcm4chee.archive.ejb.store.IssuerFactory;
import org.dcm4chee.archive.ejb.store.PatientFactory;
import org.dcm4chee.archive.persistence.Availability;
import org.dcm4chee.archive.persistence.Instance;
import org.dcm4chee.archive.persistence.Series;
import org.dcm4chee.archive.persistence.Study;
import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@RunWith(Arquillian.class)
public class InstanceStoreTest {

    private static final String SOURCE_AET = "SOURCE_AET";

    @Deployment
    public static JavaArchive createDeployment() {
       return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(InstanceStore.class,
                        CodeFactory.class,
                        IssuerFactory.class,
                        PatientFactory.class,
                        RemovePatient.class)
                .addAsResource("ct-1.xml")
                .addAsResource("ct-2.xml")
                .addAsResource("pr-1.xml");
    }

    @EJB
    private RemovePatient removePatient;

    @EJB
    private InstanceStore instanceStore;

    @After
    public void clearDB() {
        removePatient.removePatient("TEST-20110607", "DCM4CHEE_TESTDATA");
    }

    @Test
    public void storeTest() throws Exception {
        Instance ct1 = 
            instanceStore.store(SAXReader.parse("resource:ct-1.xml", null),
                SOURCE_AET, "AET_1\\AET_2", "AET_3", Availability.ONLINE);
        Instance ct2 =
            instanceStore.store(SAXReader.parse("resource:ct-2.xml", null),
                SOURCE_AET, "AET_2", "AET_3", Availability.NEARLINE);
        Instance pr1 =
            instanceStore.store(SAXReader.parse("resource:pr-1.xml", null),
                SOURCE_AET, "AET_1\\AET_2", "AET_4", Availability.ONLINE);
        instanceStore.close();
        Series ctSeries = ct1.getSeries();
        Series prSeries = pr1.getSeries();
        Study study = ctSeries.getStudy();
        assertEquals(ctSeries, ct2.getSeries());
        assertEquals(study, prSeries.getStudy());
        assertEquals(2, study.getNumberOfStudyRelatedSeries());
        assertEquals(3, study.getNumberOfStudyRelatedInstances());
        assertEquals(2, ctSeries.getNumberOfSeriesRelatedInstances());
        assertEquals(1, prSeries.getNumberOfSeriesRelatedInstances());
        assertTrue(equals(study.getModalitiesInStudy(), "CT", "PR"));
        assertTrue(equals(study.getSOPClassesInStudy(),
                "1.2.840.10008.5.1.4.1.1.2", "1.2.840.10008.5.1.4.1.1.11.1"));
        assertEquals("AET_2", ctSeries.getRetrieveAETs());
        assertEquals("AET_1\\AET_2", prSeries.getRetrieveAETs());
        assertEquals("AET_2", study.getRetrieveAETs());
        assertEquals("AET_3", ctSeries.getExternalRetrieveAET());
        assertEquals("AET_4", prSeries.getExternalRetrieveAET());
        assertNull(study.getExternalRetrieveAET());
        assertEquals(Availability.NEARLINE, ctSeries.getAvailability());
        assertEquals(Availability.ONLINE, prSeries.getAvailability());
        assertEquals(Availability.NEARLINE, study.getAvailability());
  }

    private boolean equals(String s, String... vals) {
        String[] ss = StringUtils.split(s, '\\');
        if (ss.length != vals.length)
            return false;
        for (String val : vals) {
            if (!contains(ss, val))
                return false;
        }
        return true;
    }

    private boolean contains(String[] ss, String val) {
        for (String s : ss) {
            if (val.equals(s))
                return true;
        }
        return false;
    }
}
