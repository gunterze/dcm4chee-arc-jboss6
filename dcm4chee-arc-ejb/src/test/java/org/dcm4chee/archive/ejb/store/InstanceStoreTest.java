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
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import javax.ejb.EJB;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;
import org.dcm4che.io.SAXReader;
import org.dcm4che.soundex.ESoundex;
import org.dcm4chee.archive.persistence.Availability;
import org.dcm4chee.archive.persistence.Instance;
import org.dcm4chee.archive.persistence.Series;
import org.dcm4chee.archive.persistence.StoreParam;
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
                        InstanceStoreBean.class,
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

    private static final StoreParam STORE_PARAM = new StoreParam();
    static { 
        STORE_PARAM.setPatientAttributes(
                Tag.SpecificCharacterSet,
                Tag.PatientName,
                Tag.PatientID,
                Tag.IssuerOfPatientID,
                Tag.OtherPatientIDsSequence,
                Tag.PatientBirthDate,
                Tag.PatientSex,
                Tag.PatientComments);
        STORE_PARAM.setStudyAttributes(
                Tag.SpecificCharacterSet,
                Tag.StudyDate,
                Tag.StudyTime,
                Tag.AccessionNumber,
                Tag.IssuerOfAccessionNumberSequence,
                Tag.ReferringPhysicianName,
                Tag.StudyDescription,
                Tag.ProcedureCodeSequence,
                Tag.StudyInstanceUID,
                Tag.StudyID);
        STORE_PARAM.setSeriesAttributes(
                Tag.SpecificCharacterSet,
                Tag.Modality,
                Tag.Manufacturer,
                Tag.InstitutionName,
                Tag.InstitutionCodeSequence,
                Tag.StationName,
                Tag.SeriesDescription,
                Tag.InstitutionalDepartmentName,
                Tag.PerformingPhysicianName,
                Tag.ManufacturerModelName,
                Tag.ReferencedPerformedProcedureStepSequence,
                Tag.SeriesInstanceUID,
                Tag.SeriesNumber,
                Tag.Laterality,
                Tag.PerformedProcedureStepID,
                Tag.PerformedProcedureStepStartTime,
                Tag.RequestAttributesSequence);
        STORE_PARAM.setInstanceAttributes(
                Tag.SpecificCharacterSet,
                Tag.ImageType,
                Tag.SOPClassUID,
                Tag.SOPInstanceUID,
                Tag.AcquisitionDate,
                Tag.ContentDate,
                Tag.AcquisitionDateTime,
                Tag.AcquisitionTime,
                Tag.ContentTime,
                Tag.ReferencedSeriesSequence,
                Tag.InstanceNumber,
                Tag.PhotometricInterpretation,
                Tag.NumberOfFrames,
                Tag.Rows,
                Tag.Columns,
                Tag.BitsAllocated,
                Tag.ObservationDateTime,
                Tag.ConceptNameCodeSequence,
                Tag.VerifyingObserverSequence,
                Tag.ReferencedRequestSequence,
                Tag.CurrentRequestedProcedureEvidenceSequence,
                Tag.PertinentOtherEvidenceSequence,
                Tag.CompletionFlag,
                Tag.VerificationFlag,
                Tag.IdenticalDocumentsSequence,
                Tag.DocumentTitle,
                Tag.MIMETypeOfEncapsulatedDocument,
                Tag.ContentLabel,
                Tag.ContentDescription,
                Tag.PresentationCreationDate,
                Tag.PresentationCreationTime,
                Tag.ContentCreatorName,
                Tag.OriginalAttributesSequence);
        STORE_PARAM.setFuzzyStr(new ESoundex());
    }

    @Test
    public void storeTest() throws Exception {
        Instance ct1 = instanceStore.store(parse("resource:ct-1.xml", SOURCE_AET,
                new String[]{"AET_1","AET_2"}, "AET_3", Availability.ONLINE), STORE_PARAM);
        Instance ct2 = instanceStore.store(parse("resource:ct-2.xml", SOURCE_AET,
                    new String[]{"AET_2"}, "AET_3", Availability.NEARLINE), STORE_PARAM);
        Instance pr1 = instanceStore.store(parse("resource:pr-1.xml", SOURCE_AET,
                    new String[]{"AET_1", "AET_2"}, "AET_4", Availability.ONLINE), STORE_PARAM);
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

    private Attributes parse(String uri, String sourceAET, String[] retrieveAETs,
            String externalRetrieveAET, Availability availability) throws Exception {
        Attributes data = SAXReader.parse(uri);
        data.setString(InstanceStore.DCM4CHEE_ARC, InstanceStore.SOURCE_AET, VR.AE, sourceAET);
        data.setString(Tag.RetrieveAETitle, VR.AE, retrieveAETs);
        data.setString(InstanceStore.DCM4CHEE_ARC, InstanceStore.EXT_RETRIEVE_AET, VR.AE, externalRetrieveAET);
        data.setString(Tag.InstanceAvailability, VR.CS, availability.toString());
        return data;
    }
}
