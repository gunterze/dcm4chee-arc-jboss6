/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contentsOfthis file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copyOfthe License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is partOfdcm4che, an implementationOfDICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial DeveloperOfthe Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contentsOfthis file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisionsOfthe GPL or the LGPL are applicable instead
 *Ofthose above. If you wish to allow useOfyour versionOfthis file only
 * under the termsOfeither the GPL or the LGPL, and not to allow others to
 * use your versionOfthis file under the termsOfthe MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your versionOfthis file under
 * the termsOfany oneOfthe MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.archive.conf.prefs;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.prefs.Preferences;

import org.dcm4che.conf.api.AttributeCoercion;
import org.dcm4che.conf.api.ConfigurationNotFoundException;
import org.dcm4che.data.Tag;
import org.dcm4che.data.UID;
import org.dcm4che.net.ApplicationEntity;
import org.dcm4che.net.Connection;
import org.dcm4che.net.Device;
import org.dcm4che.net.QueryOption;
import org.dcm4che.net.TransferCapability;
import org.dcm4che.soundex.ESoundex;
import org.dcm4che.util.SafeClose;
import org.dcm4chee.archive.ejb.store.Entity;
import org.dcm4chee.archive.ejb.store.StoreParam.StoreDuplicate;
import org.dcm4chee.archive.net.ArchiveApplicationEntity;
import org.dcm4chee.archive.net.ArchiveDevice;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class PreferencesArchiveConfigurationTest {

    private static final String DCM4CHEE_ARCHIVE = "DCM4CHEE Archive";
    private static final String STORESCP_DEVICE = "STORESCP Device";
    private static final int[] PATIENT_ATTRS = {
        Tag.SpecificCharacterSet,
        Tag.PatientName,
        Tag.PatientID,
        Tag.IssuerOfPatientID,
        Tag.IssuerOfPatientIDQualifiersSequence,
        Tag.PatientBirthDate,
        Tag.PatientBirthTime,
        Tag.PatientSex,
        Tag.PatientInsurancePlanCodeSequence,
        Tag.PatientPrimaryLanguageCodeSequence,
        Tag.OtherPatientNames,
        Tag.OtherPatientIDsSequence,
        Tag.PatientBirthName,
        Tag.PatientAge,
        Tag.PatientSize,
        Tag.PatientSizeCodeSequence,
        Tag.PatientWeight,
        Tag.PatientAddress,
        Tag.PatientMotherBirthName,
        Tag.MilitaryRank,
        Tag.BranchOfService,
        Tag.MedicalRecordLocator,
        Tag.MedicalAlerts,
        Tag.Allergies,
        Tag.CountryOfResidence,
        Tag.RegionOfResidence,
        Tag.PatientTelephoneNumbers,
        Tag.EthnicGroup,
        Tag.Occupation,
        Tag.SmokingStatus,
        Tag.AdditionalPatientHistory,
        Tag.PregnancyStatus,
        Tag.LastMenstrualDate,
        Tag.PatientReligiousPreference,
        Tag.PatientSpeciesDescription,
        Tag.PatientSpeciesCodeSequence,
        Tag.PatientSexNeutered,
        Tag.PatientBreedDescription,
        Tag.PatientBreedCodeSequence,
        Tag.BreedRegistrationSequence,
        Tag.ResponsiblePerson,
        Tag.ResponsiblePersonRole,
        Tag.ResponsibleOrganization,
        Tag.PatientComments,
        Tag.ClinicalTrialSponsorName,
        Tag.ClinicalTrialProtocolID,
        Tag.ClinicalTrialProtocolName,
        Tag.ClinicalTrialSiteID,
        Tag.ClinicalTrialSiteName,
        Tag.ClinicalTrialSubjectID,
        Tag.ClinicalTrialSubjectReadingID,
        Tag.PatientIdentityRemoved,
        Tag.DeidentificationMethod,
        Tag.DeidentificationMethodCodeSequence,
        Tag.ClinicalTrialProtocolEthicsCommitteeName,
        Tag.ClinicalTrialProtocolEthicsCommitteeApprovalNumber,
        Tag.SpecialNeeds,
        Tag.PertinentDocumentsSequence,
        Tag.PatientState,
        Tag.PatientClinicalTrialParticipationSequence,
        Tag.ConfidentialityConstraintOnPatientDataDescription
    };
    private static final int[] STUDY_ATTRS = {
        Tag.SpecificCharacterSet,
        Tag.StudyDate,
        Tag.StudyTime,
        Tag.AccessionNumber,
        Tag.IssuerOfAccessionNumberSequence,
        Tag.ReferringPhysicianName,
        Tag.StudyDescription,
        Tag.ProcedureCodeSequence,
        Tag.PatientAge,
        Tag.PatientSize,
        Tag.PatientSizeCodeSequence,
        Tag.PatientWeight,
        Tag.Occupation,
        Tag.AdditionalPatientHistory,
        Tag.PatientSexNeutered,
        Tag.StudyInstanceUID,
        Tag.StudyID 
    };
    private static final int[] SERIES_ATTRS = {
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
        Tag.BodyPartExamined,
        Tag.SeriesInstanceUID,
        Tag.SeriesNumber,
        Tag.Laterality,
        Tag.PerformedProcedureStepStartDate,
        Tag.PerformedProcedureStepStartTime,
        Tag.RequestAttributesSequence
    };
    private static final int[] INSTANCE_ATTRS = {
        Tag.SpecificCharacterSet,
        Tag.ImageType,
        Tag.SOPClassUID,
        Tag.SOPInstanceUID,
        Tag.ContentDate,
        Tag.ContentTime,
        Tag.ReferencedSeriesSequence,
        Tag.InstanceNumber,
        Tag.NumberOfFrames,
        Tag.Rows,
        Tag.Columns,
        Tag.BitsAllocated,
        Tag.ConceptNameCodeSequence,
        Tag.VerifyingObserverSequence,
        Tag.ReferencedRequestSequence,
        Tag.CompletionFlag,
        Tag.VerificationFlag,
        Tag.DocumentTitle,
        Tag.MIMETypeOfEncapsulatedDocument,
        Tag.ContentLabel,
        Tag.ContentDescription,
        Tag.PresentationCreationDate,
        Tag.PresentationCreationTime,
        Tag.ContentCreatorName,
        Tag.OriginalAttributesSequence
    };
    private static final int[] VISIT_ATTRS = {
        Tag.SpecificCharacterSet,
        Tag.InstitutionName,
        Tag.InstitutionAddress,
        Tag.InstitutionCodeSequence,
        Tag.ReferringPhysicianName,
        Tag.ReferringPhysicianAddress,
        Tag.ReferringPhysicianTelephoneNumbers,
        Tag.ReferringPhysicianIdentificationSequence,
        Tag.AdmittingDiagnosesDescription,
        Tag.AdmittingDiagnosesCodeSequence,
        Tag.VisitStatusID,
        Tag.AdmissionID,
        Tag.IssuerOfAdmissionIDSequence,
        Tag.RouteOfAdmissions,
        Tag.AdmittingDate,
        Tag.AdmittingTime,
        Tag.ServiceEpisodeID,
        Tag.ServiceEpisodeDescription,
        Tag.IssuerOfServiceEpisodeIDSequence,
        Tag.CurrentPatientLocation,
        Tag.PatientInstitutionResidence,
        Tag.VisitComments
    };
    private static final int[] SERVICE_REQUEST_ATTRS = {
        Tag.SpecificCharacterSet,
        Tag.AccessionNumber,
        Tag.IssuerOfAccessionNumberSequence,
        Tag.RequestingPhysicianIdentificationSequence,
        Tag.RequestingPhysician,
        Tag.RequestingService,
        Tag.RequestingServiceCodeSequence,
        Tag.OrderPlacerIdentifierSequence,
        Tag.OrderFillerIdentifierSequence,
        Tag.IssueDateOfImagingServiceRequest,
        Tag.IssueTimeOfImagingServiceRequest,
        Tag.OrderEnteredBy,
        Tag.OrderEntererLocation,
        Tag.OrderCallbackPhoneNumber,
        Tag.PlacerOrderNumberImagingServiceRequest,
        Tag.FillerOrderNumberImagingServiceRequest,
        Tag.ImagingServiceRequestComments
    };
    private static final int[] REQUESTED_PROCEDURE_ATTRS = {
        Tag.SpecificCharacterSet,
        Tag.StudyDate,
        Tag.StudyTime,
        Tag.StudyInstanceUID,
        Tag.RequestedProcedureDescription,
        Tag.RequestedProcedureCodeSequence,
        Tag.RequestedProcedureID,
        Tag.ReasonForTheRequestedProcedure,
        Tag.RequestedProcedurePriority,
        Tag.PatientTransportArrangements,
        Tag.RequestedProcedureLocation,
        Tag.ConfidentialityCode,
        Tag.ReportingPriority,
        Tag.ReasonForRequestedProcedureCodeSequence,
        Tag.NamesOfIntendedRecipientsOfResults,
        Tag.IntendedRecipientsOfResultsIdentificationSequence,
        Tag.RequestedProcedureComments
    };
    private static final int[] SPS_ATTRS = {
        Tag.SpecificCharacterSet,
        Tag.Modality,
        Tag.AnatomicalOrientationType,
        Tag.RequestedContrastAgent,
        Tag.ScheduledStationAETitle,
        Tag.ScheduledProcedureStepStartDate,
        Tag.ScheduledProcedureStepStartTime,
        Tag.ScheduledProcedureStepEndDate,
        Tag.ScheduledProcedureStepEndTime,
        Tag.ScheduledPerformingPhysicianName,
        Tag.ScheduledProcedureStepDescription,
        Tag.ScheduledProtocolCodeSequence,
        Tag.ScheduledProcedureStepID,
        Tag.ScheduledPerformingPhysicianIdentificationSequence,
        Tag.ScheduledStationName,
        Tag.ScheduledProcedureStepLocation,
        Tag.PreMedication,
        Tag.ScheduledProcedureStepStatus,
        Tag.CommentsOnTheScheduledProcedureStep,
        Tag.ScheduledSpecimenSequence
    };
    private static final String[] IMAGE_TSUIDS = {
        UID.ImplicitVRLittleEndian,
        UID.ExplicitVRLittleEndian,
        UID.DeflatedExplicitVRLittleEndian,
        UID.ExplicitVRBigEndian,
        UID.JPEGBaseline1,
        UID.JPEGExtended24,
        UID.JPEGLossless,
        UID.JPEGLosslessNonHierarchical14,
        UID.JPEGLSLossless,
        UID.JPEGLSLossyNearLossless,
        UID.JPEG2000LosslessOnly,
        UID.JPEG2000,
        UID.RLELossless
    };
    private static final String[] VIDEO_TSUIDS = {
        UID.JPEGBaseline1,
        UID.MPEG2,
        UID.MPEG2MainProfileHighLevel,
        UID.MPEG4AVCH264BDCompatibleHighProfileLevel41,
        UID.MPEG4AVCH264HighProfileLevel41
    };
    private static final String[] OTHER_TSUIDS = {
        UID.ImplicitVRLittleEndian,
        UID.ExplicitVRLittleEndian,
        UID.DeflatedExplicitVRLittleEndian,
        UID.ExplicitVRBigEndian,
    };
    private static final String[] IMAGE_CUIDS = {
        UID.ComputedRadiographyImageStorage,
        UID.DigitalXRayImageStorageForPresentation,
        UID.DigitalXRayImageStorageForProcessing,
        UID.DigitalMammographyXRayImageStorageForPresentation,
        UID.DigitalMammographyXRayImageStorageForProcessing,
        UID.DigitalIntraOralXRayImageStorageForPresentation,
        UID.DigitalIntraOralXRayImageStorageForProcessing,
        UID.CTImageStorage,
        UID.EnhancedCTImageStorage,
        UID.UltrasoundMultiFrameImageStorageRetired,
        UID.UltrasoundMultiFrameImageStorage,
        UID.MRImageStorage,
        UID.EnhancedMRImageStorage,
        UID.EnhancedMRColorImageStorage,
        UID.NuclearMedicineImageStorageRetired,
        UID.UltrasoundImageStorageRetired,
        UID.UltrasoundImageStorage,
        UID.EnhancedUSVolumeStorage,
        UID.SecondaryCaptureImageStorage,
        UID.MultiFrameGrayscaleByteSecondaryCaptureImageStorage,
        UID.MultiFrameGrayscaleWordSecondaryCaptureImageStorage,
        UID.MultiFrameTrueColorSecondaryCaptureImageStorage,
        UID.XRayAngiographicImageStorage,
        UID.EnhancedXAImageStorage,
        UID.XRayRadiofluoroscopicImageStorage,
        UID.EnhancedXRFImageStorage,
        UID.XRayAngiographicBiPlaneImageStorageRetired,
        UID.XRay3DAngiographicImageStorage,
        UID.XRay3DCraniofacialImageStorage,
        UID.BreastTomosynthesisImageStorage,
        UID.IntravascularOpticalCoherenceTomographyImageStorageForPresentation,
        UID.IntravascularOpticalCoherenceTomographyImageStorageForProcessing,
        UID.NuclearMedicineImageStorage,
        UID.VLEndoscopicImageStorage,
        UID.VLMicroscopicImageStorage,
        UID.VLSlideCoordinatesMicroscopicImageStorage,
        UID.VLPhotographicImageStorage,
        UID.OphthalmicPhotography8BitImageStorage,
        UID.OphthalmicPhotography16BitImageStorage,
        UID.OphthalmicTomographyImageStorage,
        UID.VLWholeSlideMicroscopyImageStorage,
        UID.PositronEmissionTomographyImageStorage,
        UID.EnhancedPETImageStorage,
        UID.RTImageStorage,
    };
    private static final String[] VIDEO_CUIDS = {
        UID.VideoEndoscopicImageStorage,
        UID.VideoMicroscopicImageStorage,
        UID.VideoPhotographicImageStorage,
    };
    private static final String[] OTHER_CUIDS = {
        UID.MRSpectroscopyStorage,
        UID.MultiFrameSingleBitSecondaryCaptureImageStorage,
        UID.StandaloneOverlayStorageRetired,
        UID.StandaloneCurveStorageRetired,
        UID.TwelveLeadECGWaveformStorage,
        UID.GeneralECGWaveformStorage,
        UID.AmbulatoryECGWaveformStorage,
        UID.HemodynamicWaveformStorage,
        UID.CardiacElectrophysiologyWaveformStorage,
        UID.BasicVoiceAudioWaveformStorage,
        UID.GeneralAudioWaveformStorage,
        UID.ArterialPulseWaveformStorage,
        UID.RespiratoryWaveformStorage,
        UID.StandaloneModalityLUTStorageRetired,
        UID.StandaloneVOILUTStorageRetired,
        UID.GrayscaleSoftcopyPresentationStateStorageSOPClass,
        UID.ColorSoftcopyPresentationStateStorageSOPClass,
        UID.PseudoColorSoftcopyPresentationStateStorageSOPClass,
        UID.BlendingSoftcopyPresentationStateStorageSOPClass,
        UID.XAXRFGrayscaleSoftcopyPresentationStateStorage,
        UID.RawDataStorage,
        UID.SpatialRegistrationStorage,
        UID.SpatialFiducialsStorage,
        UID.DeformableSpatialRegistrationStorage,
        UID.SegmentationStorage,
        UID.SurfaceSegmentationStorage,
        UID.RealWorldValueMappingStorage,
        UID.StereometricRelationshipStorage,
        UID.LensometryMeasurementsStorage,
        UID.AutorefractionMeasurementsStorage,
        UID.KeratometryMeasurementsStorage,
        UID.SubjectiveRefractionMeasurementsStorage,
        UID.VisualAcuityMeasurementsStorage,
        UID.SpectaclePrescriptionReportStorage,
        UID.OphthalmicAxialMeasurementsStorage,
        UID.IntraocularLensCalculationsStorage,
        UID.MacularGridThicknessAndVolumeReportStorage,
        UID.OphthalmicVisualFieldStaticPerimetryMeasurementsStorage,
        UID.BasicStructuredDisplayStorage,
        UID.BasicTextSRStorage,
        UID.EnhancedSRStorage,
        UID.ComprehensiveSRStorage,
        UID.ProcedureLogStorage,
        UID.MammographyCADSRStorage,
        UID.KeyObjectSelectionDocumentStorage,
        UID.ChestCADSRStorage,
        UID.XRayRadiationDoseSRStorage,
        UID.ColonCADSRStorage,
        UID.ImplantationPlanSRStorage,
        UID.EncapsulatedPDFStorage,
        UID.EncapsulatedCDAStorage,
        UID.StandalonePETCurveStorageRetired,
        UID.RTDoseStorage,
        UID.RTStructureSetStorage,
        UID.RTBeamsTreatmentRecordStorage,
        UID.RTPlanStorage,
        UID.RTBrachyTreatmentRecordStorage,
        UID.RTTreatmentSummaryRecordStorage,
        UID.RTIonPlanStorage,
        UID.RTIonBeamsTreatmentRecordStorage,
    };
    private static final String[] QR_CUIDS = {
        UID.PatientRootQueryRetrieveInformationModelFIND,
        UID.StudyRootQueryRetrieveInformationModelFIND,
        UID.PatientStudyOnlyQueryRetrieveInformationModelFINDRetired,
        UID.ModalityWorklistInformationModelFIND,
        UID.PatientRootQueryRetrieveInformationModelGET,
        UID.PatientRootQueryRetrieveInformationModelMOVE,
        UID.StudyRootQueryRetrieveInformationModelGET,
        UID.StudyRootQueryRetrieveInformationModelMOVE,
        UID.PatientStudyOnlyQueryRetrieveInformationModelGETRetired,
        UID.PatientStudyOnlyQueryRetrieveInformationModelMOVERetired,
        UID.CompositeInstanceRetrieveWithoutBulkDataGET
    };

    private PreferencesArchiveConfiguration config;

    @Before
    public void setUp() throws Exception {
        config = new PreferencesArchiveConfiguration(Preferences.userRoot());
    }

    @After
    public void tearDown() throws Exception {
//        config.purgeConfiguration();
    }

    @Test
    public void testPersist() throws Exception {
        try {
            config.removeDevice(DCM4CHEE_ARCHIVE);
        }  catch (ConfigurationNotFoundException e) {}
        try {
            config.removeDevice(STORESCP_DEVICE);
        }  catch (ConfigurationNotFoundException e) {}
        config.unregisterAETitle("DCM4CHEE");
        config.unregisterAETitle("STORESCP");
        config.registerAETitle("DCM4CHEE");
        config.registerAETitle("STORESCP");
        config.persist(createArchiveDevice(DCM4CHEE_ARCHIVE));
        config.persist(createStoreSCP(STORESCP_DEVICE));
//        export();
        ApplicationEntity ae = config.findApplicationEntity("DCM4CHEE");
        config.removeDevice(DCM4CHEE_ARCHIVE);
        config.removeDevice(STORESCP_DEVICE);
        config.unregisterAETitle("DCM4CHEE");
        config.unregisterAETitle("STORESCP");
    }

//    private void export() throws Exception {
//        OutputStream os = new FileOutputStream(
//                "/home/gunter/dcm4chee-arc/dcm4chee-arc-beans/src/main/config/prefs/sample-config.xml");
//        try {
//            Preferences.userRoot().node("dicomConfigurationRoot").exportSubtree(os);
//        } finally {
//            SafeClose.close(os);
//        }
//    }

    private Device createStoreSCP(String name) throws Exception {
        Device device = new Device(name);
        ApplicationEntity ae = new ApplicationEntity("STORESCP");
        ae.setAssociationAcceptor(true);
        device.addApplicationEntity(ae);
        Connection dicom = new Connection("dicom", "localhost", 11113);
        device.addConnection(dicom);
        ae.addConnection(dicom);
        Connection dicomTLS = new Connection("dicom-tls", "localhost", 2763);
        dicomTLS.setTlsCipherSuites(
                Connection.TLS_RSA_WITH_AES_128_CBC_SHA, 
                Connection.TLS_RSA_WITH_3DES_EDE_CBC_SHA);
        dicomTLS.setInstalled(false);
        device.addConnection(dicomTLS);
        ae.addConnection(dicomTLS);
        return device;
    }

    private ArchiveDevice createArchiveDevice(String name) throws Exception {
        ArchiveDevice device = new ArchiveDevice(name);
        device.setFileSystemGroupID("DEFAULT");
        device.setReceivingDirectoryPath("incoming");
        device.setDigestAlgorithm("MD5");
        device.setRetrieveAETs("DCM4CHEE");
        device.setStoreOriginalAttributes(true);
        device.setSuppressWarningCoercionOfDataElements(false);
        device.setStoreDuplicate(StoreDuplicate.STORE);
        device.setFuzzyStr(new ESoundex());
        device.setMatchUnknown(true);
        device.setSendPendingCGet(true);
        device.setSendPendingCMoveInterval(5000);
        device.setAttributeFilter(Entity.Patient, new AttributeFilter(PATIENT_ATTRS));
        device.setAttributeFilter(Entity.Study, new AttributeFilter(STUDY_ATTRS));
        device.setAttributeFilter(Entity.Series, new AttributeFilter(SERIES_ATTRS));
        device.setAttributeFilter(Entity.Instance, new AttributeFilter(INSTANCE_ATTRS));
        device.setAttributeFilter(Entity.Visit, new AttributeFilter(VISIT_ATTRS));
        device.setAttributeFilter(Entity.ServiceRequest, new AttributeFilter(SERVICE_REQUEST_ATTRS));
        device.setAttributeFilter(Entity.RequestedProcedure, new AttributeFilter(REQUESTED_PROCEDURE_ATTRS));
        device.setAttributeFilter(Entity.ScheduledProcedureStep, new AttributeFilter(SPS_ATTRS));
        device.addAttributeCoercion(new AttributeCoercion(null, 
                AttributeCoercion.DIMSE.C_STORE_RQ, 
                TransferCapability.Role.SCP,
                "ENSURE_PID",
                "resource:dcm4chee-arc-ensure-pid.xsl"));
        device.addAttributeCoercion(new AttributeCoercion(null, 
                AttributeCoercion.DIMSE.C_STORE_RQ, 
                TransferCapability.Role.SCU,
                "WITHOUT_PN",
                "resource:dcm4chee-arc-nullify-pn.xsl"));
        ArchiveApplicationEntity ae = new ArchiveApplicationEntity("DCM4CHEE");
        ae.setAssociationAcceptor(true);
        ae.setAssociationInitiator(true);
        ae.setMaxOpsInvoked(0);
        ae.setMaxOpsPerformed(0);
        addVerificationStorageTransferCapabilities(ae);
        addStorageTransferCapabilities(ae, IMAGE_CUIDS, IMAGE_TSUIDS);
        addStorageTransferCapabilities(ae, VIDEO_CUIDS, VIDEO_TSUIDS);
        addStorageTransferCapabilities(ae, OTHER_CUIDS, OTHER_TSUIDS);
        addQRTransferCapabilities(ae, QR_CUIDS);
        device.addApplicationEntity(ae);
        Connection dicom = new Connection("dicom", "localhost", 11112);
        device.addConnection(dicom);
        ae.addConnection(dicom);
        Connection dicomTLS = new Connection("dicom-tls", "localhost", 2762);
        dicomTLS.setTlsCipherSuites(
                Connection.TLS_RSA_WITH_AES_128_CBC_SHA, 
                Connection.TLS_RSA_WITH_3DES_EDE_CBC_SHA);
        dicomTLS.setInstalled(false);
        device.addConnection(dicomTLS);
        ae.addConnection(dicomTLS);
        return device;
    }

    private void addVerificationStorageTransferCapabilities(
            ArchiveApplicationEntity ae) {
        String cuid = UID.VerificationSOPClass;
        String name = UID.nameOf(cuid).replace('/', ' ');
        ae.addTransferCapability(
                new TransferCapability(name + " SCP", cuid, TransferCapability.Role.SCP,
                        UID.ImplicitVRLittleEndian));
        ae.addTransferCapability(
                new TransferCapability(name + " SCU", cuid, TransferCapability.Role.SCU,
                        UID.ImplicitVRLittleEndian));
        
    }

    private void addQRTransferCapabilities(ArchiveApplicationEntity ae, String[] cuids) {
        for (String cuid : cuids) {
            String name = UID.nameOf(cuid).replace('/', ' ');
            TransferCapability tc = new TransferCapability(name + " SCP", cuid,
                    TransferCapability.Role.SCP, UID.ImplicitVRLittleEndian);
            tc.setQueryOptions(EnumSet.allOf(QueryOption.class));
            ae.addTransferCapability(tc);
        }
    }

    private void addStorageTransferCapabilities(ArchiveApplicationEntity ae,
            String[] cuids, String[] tss) {
        for (String cuid : cuids) {
            String name = UID.nameOf(cuid).replace('/', ' ');
            ae.addTransferCapability(
                    new TransferCapability(name + " SCP", cuid, TransferCapability.Role.SCP, tss));
            ae.addTransferCapability(
                    new TransferCapability(name + " SCU", cuid, TransferCapability.Role.SCU, tss));
        }
    }
}
