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

package org.dcm4chee.archive.testdata;

import javax.ejb.EJB;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.io.SAXReader;
import org.dcm4che.soundex.ESoundex;
import org.dcm4chee.archive.ejb.store.CodeFactory;
import org.dcm4chee.archive.ejb.store.InstanceStore;
import org.dcm4chee.archive.ejb.store.InstanceStoreBean;
import org.dcm4chee.archive.ejb.store.IssuerFactory;
import org.dcm4chee.archive.ejb.store.PatientFactory;
import org.dcm4chee.archive.ejb.store.RequestFactory;
import org.dcm4chee.archive.persistence.Availability;
import org.dcm4chee.archive.persistence.StoreParam;
import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@RunWith(Arquillian.class)
public class InitTestData {

    private static final String SOURCE_AET = "SOURCE_AET";
    private static final String RETRIEVE_AETS = "RETRIEVE_AET";
    private static final Class<?>[] CLASSES = {
        InstanceStore.class,
        InstanceStoreBean.class,
        CodeFactory.class,
        IssuerFactory.class,
        RequestFactory.class,
        PatientFactory.class
   };

    private static final String[] RESOURCES = {
        "date-range-1.xml",
        "date-range-2.xml",
        "date-range-3.xml",
        "date-range-4.xml",
        "date-range-5.xml",
        "date-range-6.xml",
        "date-range-7.xml",
        "accno-issuer-1.xml",
        "accno-issuer-2.xml",
        "accno-issuer-3.xml",
        "req-attrs-seq-1.xml",
        "req-attrs-seq-2.xml",
        "req-attrs-seq-3.xml",
        "mods-in-study-1.xml",
        "mods-in-study-2.xml",
        "mods-in-study-3.xml",
        "mods-in-study-4.xml",
        "mods-in-study-5.xml",
        "proc-code-seq-1.xml",
        "proc-code-seq-2.xml",
        "proc-code-seq-3.xml",
        "concept-name-code-seq-1.xml",
        "concept-name-code-seq-2.xml",
        "concept-name-code-seq-3.xml",
        "verifying-observer-seq-1.xml",
        "verifying-observer-seq-2.xml",
        "verifying-observer-seq-3.xml",
        "birthdate-1.xml",
        "birthdate-2.xml",
        "birthdate-3.xml",
        "tf-info-1.xml",
        "tf-info-2.xml",
        "fuzzy-1.xml",
        "fuzzy-2.xml",
        "fuzzy-3.xml",
        "fuzzy-4.xml",
        "person-name-1.xml",
   };

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
        STORE_PARAM.setRetrieveAETs(RETRIEVE_AETS);
    }

    @Deployment
    public static JavaArchive createDeployment() {
       JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "test.jar");
       for (Class<?> clazz : CLASSES)
           archive.addClass(clazz);
       for (String res : RESOURCES)
           archive.addAsResource(res);
       return archive;
    }

    @EJB
    private InstanceStore instanceStore;

    @Test
    public void storeTestData() throws Exception {
        for (String res : RESOURCES) {
            Attributes ds = SAXReader.parse("resource:" + res, null);
            instanceStore.newInstance(SOURCE_AET, ds, Availability.ONLINE, STORE_PARAM);
        }
        instanceStore.close();
    }

}
