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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.ejb.EJB;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.io.SAXReader;
import org.dcm4chee.archive.ejb.exception.DicomServiceRuntimeException;
import org.dcm4chee.archive.ejb.query.Builder;
import org.dcm4chee.archive.ejb.query.IANQuery;
import org.dcm4chee.archive.ejb.query.IANQueryBean;
import org.dcm4chee.archive.persistence.Availability;
import org.dcm4chee.archive.persistence.Instance;
import org.dcm4chee.archive.persistence.PerformedProcedureStep;
import org.dcm4chee.archive.persistence.Series;
import org.dcm4chee.archive.persistence.Study;
import org.jboss.arquillian.container.test.api.Deployment;
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

    private static final String MPPS_IUID = "1.2.40.0.13.1.1.99.20120130";

    @Deployment
    public static JavaArchive createDeployment() {
       return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(
                        Builder.class,
                        CodeFactory.class,
                        DicomServiceRuntimeException.class,
                        Entity.class,
                        EntityAlreadyExistsException.class,
                        EntityNotExistsException.class,
                        IANQuery.class,
                        IANQueryBean.class,
                        InstanceStore.class,
                        InstanceStoreBean.class,
                        IssuerFactory.class,
                        PatientFactory.class,
                        NonUniquePatientException.class,
                        PatientMismatchException.class,
                        PatientMergedException.class,
                        PerformedProcedureStepManager.class,
                        PerformedProcedureStepManagerBean.class,
                        PPSWithIAN.class,
                        RemovePatient.class,
                        RejectionNote.class,
                        RequestFactory.class,
                        SeriesUpdate.class,
                        StoreDuplicate.class,
                        StoreParam.class,
                        StoreParamFactory.class)
                .addAsResource("mpps-create.xml")
                .addAsResource("mpps-set.xml")
                .addAsResource("ct-1.xml")
                .addAsResource("ct-2.xml")
                .addAsResource("pr-1.xml");
    }

    @EJB
    private RemovePatient removePatient;

    @EJB
    private PerformedProcedureStepManager mppsMgr;

    @EJB
    private InstanceStore instanceStore;

    @After
    public void clearDB() {
        removePatient.removePatient("TEST-20110607", "DCM4CHEE_TESTDATA");
    }

    @Test
    public void storeTest() throws Exception {
        StoreParam storeParam = StoreParamFactory.create();
        PerformedProcedureStep pps = mppsMgr.createPerformedProcedureStep(
                MPPS_IUID, SAXReader.parse("resource:mpps-create.xml"), storeParam);
        assertTrue(pps.isInProgress());
        PPSWithIAN ppsWithIAN = mppsMgr.updatePerformedProcedureStep(MPPS_IUID,
                SAXReader.parse("resource:mpps-set.xml"), storeParam);
        assertTrue(ppsWithIAN.pps.isCompleted());
        storeParam.setRetrieveAETs("AET_1","AET_2");
        storeParam.setExternalRetrieveAET("AET_3");
        Attributes modified1 = new Attributes();
        Instance ct1 = instanceStore.newInstance(SOURCE_AET,
                SAXReader.parse("resource:ct-1.xml"), modified1,
                Availability.ONLINE, storeParam);
        assertTrue(modified1.isEmpty());
        storeParam.setRetrieveAETs("AET_2");
        storeParam.setExternalRetrieveAET("AET_3");
        Attributes modified2 = new Attributes();
        Instance ct2 = instanceStore.newInstance(SOURCE_AET,
                SAXReader.parse("resource:ct-2.xml"), modified2,
                Availability.NEARLINE, storeParam);
        assertEquals(2, modified2.size());
        assertEquals("TEST-REPLACE", modified2.getString(Tag.StudyID));
        assertEquals("0", modified2.getString(Tag.SeriesNumber));
        storeParam.setRetrieveAETs("AET_1","AET_2");
        storeParam.setExternalRetrieveAET("AET_4");
        Attributes modified3 = new Attributes();
        Instance pr1 = instanceStore.newInstance(SOURCE_AET,
                SAXReader.parse("resource:pr-1.xml"), modified3,
                Availability.ONLINE, storeParam);
        assertEquals(1, modified3.size());
        assertEquals("TEST-REPLACE", modified3.getString(Tag.StudyID));
        Attributes ian = instanceStore.createIANforCurrentMPPS();
        assertNotNull(ian);
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
        assertArrayEquals(new String[]{"CT", "PR"}, sort(study.getModalitiesInStudy()));
        assertArrayEquals(
                new String[]{"1.2.840.10008.5.1.4.1.1.11.1","1.2.840.10008.5.1.4.1.1.2" },
                sort(study.getSOPClassesInStudy()));
        assertArrayEquals(new String[]{"AET_2"}, ctSeries.getRetrieveAETs());
        assertArrayEquals(new String[]{"AET_1", "AET_2"}, sort(prSeries.getRetrieveAETs()));
        assertArrayEquals(new String[]{"AET_2"}, study.getRetrieveAETs());
        assertEquals("AET_3", ctSeries.getExternalRetrieveAET());
        assertEquals("AET_4", prSeries.getExternalRetrieveAET());
        assertNull(study.getExternalRetrieveAET());
        assertEquals(Availability.NEARLINE, ctSeries.getAvailability());
        assertEquals(Availability.ONLINE, prSeries.getAvailability());
        assertEquals(Availability.NEARLINE, study.getAvailability());
  }

    private Object[] sort(Object[] a) {
        Arrays.sort(a);
        return a;
    }
}
