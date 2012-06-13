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
 * of those above. If you wish to allow useOfyour versionOfthis file only
 * under the termsOfeither the GPL or the LGPL, and not to allow others to
 * use your versionOfthis file under the termsOfthe MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your versionOfthis file under
 * the termsOfany oneOfthe MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.archive.conf.ldap;


import static org.dcm4che.net.TransferCapability.Role.SCP;
import static org.dcm4che.net.TransferCapability.Role.SCU;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.EnumSet;

import org.dcm4che.conf.api.AttributeCoercion;
import org.dcm4che.conf.api.ConfigurationException;
import org.dcm4che.conf.api.ConfigurationNotFoundException;
import org.dcm4che.conf.api.DicomConfiguration;
import org.dcm4che.data.Tag;
import org.dcm4che.data.UID;
import org.dcm4che.net.ApplicationEntity;
import org.dcm4che.net.Code;
import org.dcm4che.net.Connection;
import org.dcm4che.net.Device;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.Issuer;
import org.dcm4che.net.QueryOption;
import org.dcm4che.net.SSLManagerFactory;
import org.dcm4che.net.TransferCapability;
import org.dcm4che.net.hl7.HL7Application;
import org.dcm4che.net.hl7.HL7Device;
import org.dcm4che.util.AttributesFormat;
import org.dcm4chee.archive.ejb.store.Entity;
import org.dcm4chee.archive.ejb.store.RejectionNote;
import org.dcm4chee.archive.ejb.store.StoreDuplicate;
import org.dcm4chee.archive.net.ArchiveApplicationEntity;
import org.dcm4chee.archive.net.ArchiveDevice;
import org.dcm4chee.archive.net.ArchiveHL7Application;
import org.dcm4chee.archive.persistence.AttributeFilter;


/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Michael Backhaus <michael.backhaus@agfa.com>
 */
public class ArchiveConfigurationTestUtils {

    private static final String PIX_MANAGER = "HL7RCV^DCM4CHEE";
    private static int PENDING_CMOVE_INTERVAL = 5000;
    private static final int CONFIGURATION_STALE_TIMEOUT = 60;
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
        Tag.ObservationDateTime,
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
    private static final int[] PPS_ATTRS = {
        Tag.SpecificCharacterSet,
        Tag.Modality,
        Tag.ProcedureCodeSequence,
        Tag.AnatomicStructureSpaceOrRegionSequence,
        Tag.DistanceSourceToDetector,
        Tag.ImageAndFluoroscopyAreaDoseProduct,
        Tag.StudyID,
        Tag.AdmissionID,
        Tag.IssuerOfAdmissionIDSequence,
        Tag.ServiceEpisodeID,
        Tag.ServiceEpisodeDescription,
        Tag.IssuerOfServiceEpisodeIDSequence,
        Tag.PerformedStationAETitle,
        Tag.PerformedStationName,
        Tag.PerformedLocation,
        Tag.PerformedProcedureStepStartDate,
        Tag.PerformedProcedureStepStartTime,
        Tag.PerformedProcedureStepEndDate,
        Tag.PerformedProcedureStepEndTime,
        Tag.PerformedProcedureStepStatus,
        Tag.PerformedProcedureStepID,
        Tag.PerformedProcedureStepDescription,
        Tag.PerformedProcedureTypeDescription,
        Tag.PerformedProtocolCodeSequence,
        Tag.ScheduledStepAttributesSequence,
        Tag.CommentsOnThePerformedProcedureStep,
        Tag.PerformedProcedureStepDiscontinuationReasonCodeSequence,
        Tag.TotalTimeOfFluoroscopy,
        Tag.TotalNumberOfExposures,
        Tag.EntranceDose,
        Tag.ExposedArea,
        Tag.DistanceSourceToEntrance,
        Tag.ExposureDoseSequence,
        Tag.CommentsOnRadiationDose,
        Tag.BillingProcedureStepSequence,
        Tag.FilmConsumptionSequence,
        Tag.BillingSuppliesAndDevicesSequence,
        Tag.PerformedSeriesSequence,
        Tag.ReasonForPerformedProcedureCodeSequence,
        Tag.EntranceDoseInmGy
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
    private static final String[] QUERY_CUIDS = {
        UID.PatientRootQueryRetrieveInformationModelFIND,
        UID.StudyRootQueryRetrieveInformationModelFIND,
        UID.PatientStudyOnlyQueryRetrieveInformationModelFINDRetired,
        UID.ModalityWorklistInformationModelFIND
    };

    private static final String[] RETRIEVE_CUIDS = {
        UID.PatientRootQueryRetrieveInformationModelGET,
        UID.PatientRootQueryRetrieveInformationModelMOVE,
        UID.StudyRootQueryRetrieveInformationModelGET,
        UID.StudyRootQueryRetrieveInformationModelMOVE,
        UID.PatientStudyOnlyQueryRetrieveInformationModelGETRetired,
        UID.PatientStudyOnlyQueryRetrieveInformationModelMOVERetired
    };

    private static final RejectionNote INCORRECT_WORKLIST_ENTRY_SELECTED =
            new RejectionNote("110514", "DCM", null, "Incorrect worklist entry selected")
                .addAction(RejectionNote.Action.HIDE_REJECTED_INSTANCES)
                .addAction(RejectionNote.Action.NOT_ACCEPT_SUBSEQUENT_OCCURRENCE);
    private static final RejectionNote REJECTED_FOR_QUALITY_REASONS =
            new RejectionNote("113001", "DCM", null, "Rejected for Quality Reasons")
                .addAction(RejectionNote.Action.HIDE_REJECTED_INSTANCES);
    private static final RejectionNote REJECT_FOR_PATIENT_SAFETY_REASONS =
            new RejectionNote("113037", "DCM", null, "Rejected for Patient Safety Reasons")
                .addAction(RejectionNote.Action.HIDE_REJECTED_INSTANCES)
                .addAction(RejectionNote.Action.HIDE_REJECTION_NOTE)
                .addAction(RejectionNote.Action.NOT_ACCEPT_SUBSEQUENT_OCCURRENCE);
    private static final RejectionNote INCORRECT_MODALITY_WORKLIST_ENTRY =
            new RejectionNote("XXXXXX11", "99IHEIOCM", null, "Incorrect Modality Worklist Entry")
                .addAction(RejectionNote.Action.HIDE_REJECTED_INSTANCES)
                .addAction(RejectionNote.Action.HIDE_REJECTION_NOTE)
                .addAction(RejectionNote.Action.NOT_ACCEPT_SUBSEQUENT_OCCURRENCE);
    private static final RejectionNote DATA_RETENTION_PERIOD_EXPIRED =
            new RejectionNote("XXXXXX22", "99IHEIOCM", null, "Data Retention Period Expired")
                .addAction(RejectionNote.Action.HIDE_REJECTED_INSTANCES)
                .addAction(RejectionNote.Action.HIDE_REJECTION_NOTE)
                .addAction(RejectionNote.Action.NOT_REJECT_SUBSEQUENT_OCCURRENCE);

    private static final String[] OTHER_DEVICES = {
        "dcmqrscp",
        "stgcmtscu",
        "storescp",
        "mppsscp",
        "ianscp",
        "storescu",
        "mppsscu",
        "findscu",
        "getscu",
        "movescu",
        "hl7snd"
    };

    private static final String[] OTHER_AES = {
        "DCMQRSCP",
        "STGCMTSCU",
        "STORESCP",
        "MPPSSCP",
        "IANSCP",
        "STORESCU",
        "MPPSSCU",
        "FINDSCU",
        "GETSCU"
    };

    private static final Issuer SITE_A =
            new Issuer("Site A", "1.2.40.0.13.1.1.999.111.1111", "ISO");
    private static final Issuer SITE_B =
            new Issuer("Site B", "1.2.40.0.13.1.1.999.222.2222", "ISO");

    private static final Issuer[] OTHER_ISSUER = {
        SITE_B, // DCMQRSCP
        null, // STGCMTSCU
        SITE_A, // STORESCP
        SITE_A, // MPPSSCP
        null, // IANSCP
        SITE_A, // STORESCU
        SITE_A, // MPPSSCU
        SITE_A, // FINDSCU
        SITE_A, // GETSCU
    };

    private static final Code INST_A =
            new Code("111.1111", "99DCM4CHEE", null, "Site A");
    private static final Code INST_B =
            new Code("222.2222", "99DCM4CHEE", null, "Site B");

    private static final Code[] OTHER_INST_CODES = {
        INST_B, // DCMQRSCP
        null, // STGCMTSCU
        null, // STORESCP
        null, // MPPSSCP
        null, // IANSCP
        INST_A, // STORESCU
        null, // MPPSSCU
        null, // FINDSCU
        null, // GETSCU
    };

    private static final int[] OTHER_PORTS = {
        11113, 2763, // DCMQRSCP
        11114, 2765, // STGCMTSCU
        11115, 2766, // STORESCP
        11116, 2767, // MPPSSCP
        11117, 2768, // IANSCP
        Connection.NOT_LISTENING, Connection.NOT_LISTENING, // STORESCU
        Connection.NOT_LISTENING, Connection.NOT_LISTENING, // MPPSSCU
        Connection.NOT_LISTENING, Connection.NOT_LISTENING, // FINDSCU
        Connection.NOT_LISTENING, Connection.NOT_LISTENING, // GETSCU
    };

    private static final String[] HL7_MESSAGE_TYPES = {
        "ADT^A02",
        "ADT^A03",
        "ADT^A06",
        "ADT^A07",
        "ADT^A08",
        "ADT^A40",
        "ORM^O01"
    };

    private static final KeyStore KEYSTORE = loadKeyStore();
    private static KeyStore loadKeyStore() {
        try {
            return SSLManagerFactory.loadKeyStore("JKS", "resource:cacerts.jks", "secret");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void cleanUp(DicomConfiguration config) throws ConfigurationException {
        config.unregisterAETitle("DCM4CHEE");
        config.unregisterAETitle("DCM4CHEE_ADMIN");
        for (String aet : OTHER_AES)
            config.unregisterAETitle(aet);

        try {
            config.removeDevice("dcm4chee-arc");
        }  catch (ConfigurationNotFoundException e) {}
        try {
            config.removeDevice("hl7rcv");
        }  catch (ConfigurationNotFoundException e) {}
        for (String name : OTHER_DEVICES)
            try {
                config.removeDevice(name);
            }  catch (ConfigurationNotFoundException e) {}
    }

    public static void testPersist(DicomConfiguration config) throws Exception {
        for (int i = 0; i < OTHER_AES.length; i++) {
            String aet = OTHER_AES[i];
            config.registerAETitle(aet);
            config.persist(createDevice(config,
                    OTHER_DEVICES[i], OTHER_ISSUER[i], OTHER_INST_CODES[i],
                    aet, "localhost", OTHER_PORTS[i<<1], OTHER_PORTS[(i<<1)+1]));
        }
        for (int i = OTHER_AES.length; i < OTHER_DEVICES.length; i++)
            config.persist(init(new Device(OTHER_DEVICES[i]), config, null, null));
        config.persist(createHL7Device(config, "hl7rcv", SITE_A, INST_A, PIX_MANAGER,
                "localhost", 2576, 12576));
        config.registerAETitle("DCM4CHEE");
        config.registerAETitle("DCM4CHEE_ADMIN");
        config.persist(createArchiveDevice(config, "dcm4chee-arc"));
        config.findApplicationEntity("DCM4CHEE");
    }

    private static Device init(Device device, DicomConfiguration config,
            Issuer issuer, Code institutionCode) throws Exception {
        String name = device.getDeviceName();
        device.setThisNodeCertificates(config.deviceRef(name),
                (X509Certificate) KEYSTORE.getCertificate(name));
        device.setIssuerOfPatientID(issuer);
        device.setIssuerOfAccessionNumber(issuer);
        if (institutionCode != null) {
            device.setInstitutionNames(institutionCode.getCodeMeaning());
            device.setInstitutionCodes(institutionCode);
        }
        return device;
    }

    private static Device createDevice(DicomConfiguration config, String name,
           Issuer issuer, Code institutionCode, String aet,
           String host, int port, int tlsPort) throws Exception {
        Device device = init(new Device(name), config, issuer, institutionCode);
        ApplicationEntity ae = new ApplicationEntity(aet);
        ae.setAssociationAcceptor(true);
        device.addApplicationEntity(ae);
        Connection dicom = new Connection("dicom", host, port);
        device.addConnection(dicom);
        ae.addConnection(dicom);
        Connection dicomTLS = new Connection("dicom-tls", host, tlsPort);
        dicomTLS.setTlsCipherSuites(
                Connection.TLS_RSA_WITH_AES_128_CBC_SHA, 
                Connection.TLS_RSA_WITH_3DES_EDE_CBC_SHA);
        device.addConnection(dicomTLS);
        ae.addConnection(dicomTLS);
        return device;
    }

    private static HL7Device createHL7Device(DicomConfiguration config, String name,
            Issuer issuer, Code institutionCode, String appName,
            String host, int port, int tlsPort) throws Exception {
         HL7Device device = new HL7Device(name);
         init(device, config, issuer, institutionCode);
         HL7Application hl7app = new HL7Application(appName);
         device.addHL7Application(hl7app);
         Connection dicom = new Connection("hl7", host, port);
         device.addConnection(dicom);
         hl7app.addConnection(dicom);
         Connection dicomTLS = new Connection("hl7-tls", host, tlsPort);
         dicomTLS.setTlsCipherSuites(
                 Connection.TLS_RSA_WITH_AES_128_CBC_SHA, 
                 Connection.TLS_RSA_WITH_3DES_EDE_CBC_SHA);
         device.addConnection(dicomTLS);
         hl7app.addConnection(dicomTLS);
         return device;
     }

    static ArchiveDevice createArchiveDevice(String name) {
        ArchiveDevice device = new ArchiveDevice(name);
        device.setFuzzyAlgorithmClass("org.dcm4che.soundex.ESoundex");
        device.setConfigurationStaleTimeout(CONFIGURATION_STALE_TIMEOUT);
        setAttributeFilters(device);
        return device ;
    }

    private static ArchiveDevice createArchiveDevice(DicomConfiguration config, String name)
            throws Exception {
        ArchiveDevice device = createArchiveDevice(name);
        device.setThisNodeCertificates(config.deviceRef(name),
                (X509Certificate) KEYSTORE.getCertificate(name));
        for (String other : OTHER_DEVICES)
            device.setAuthorizedNodeCertificates(config.deviceRef(other),
                    (X509Certificate) KEYSTORE.getCertificate(other));
        Connection dicom = createConnection("dicom", "localhost", 11112);
        dicom.setMaxOpsInvoked(0);
        dicom.setMaxOpsPerformed(0);
        device.addConnection(dicom);
        Connection dicomTLS = new Connection("dicom-tls", "localhost", 2762);
        dicomTLS.setMaxOpsInvoked(0);
        dicomTLS.setMaxOpsPerformed(0);
        dicomTLS.setTlsCipherSuites(
                Connection.TLS_RSA_WITH_AES_128_CBC_SHA, 
                Connection.TLS_RSA_WITH_3DES_EDE_CBC_SHA);
        device.addConnection(dicomTLS);
        ArchiveApplicationEntity ae = createAE("DCM4CHEE",
                IMAGE_TSUIDS, VIDEO_TSUIDS, OTHER_TSUIDS, null, PIX_MANAGER);
        device.addApplicationEntity(ae);
        ae.addConnection(dicom);
        ae.addConnection(dicomTLS);
        ArchiveApplicationEntity adminAE = createAdminAE("DCM4CHEE_ADMIN",
                IMAGE_TSUIDS, VIDEO_TSUIDS, OTHER_TSUIDS, null, PIX_MANAGER);
        device.addApplicationEntity(adminAE);
        adminAE.addConnection(dicom);
        adminAE.addConnection(dicomTLS);
        ArchiveHL7Application hl7App = new ArchiveHL7Application("*");
        hl7App.setAcceptedMessageTypes(HL7_MESSAGE_TYPES);
        hl7App.setHL7DefaultCharacterSet("8859/1");
        hl7App.addTemplatesURI("adt2dcm", "resource:dcm4chee-arc-hl7-adt2dcm.xsl");
        device.addHL7Application(hl7App);
        Connection hl7 = new Connection("hl7", "localhost", 2575);
        device.addConnection(hl7);
        hl7App.addConnection(hl7);
        Connection hl7TLS = new Connection("hl7-tls", "localhost", 12575);
        hl7TLS.setTlsCipherSuites(
                Connection.TLS_RSA_WITH_AES_128_CBC_SHA, 
                Connection.TLS_RSA_WITH_3DES_EDE_CBC_SHA);
        device.addConnection(hl7TLS);
        hl7App.addConnection(hl7TLS);
        return device;
    }


    private static void setAttributeFilters(ArchiveDevice device) {
        device.setAttributeFilter(Entity.Patient,
                new AttributeFilter(PATIENT_ATTRS));
        device.setAttributeFilter(Entity.Study,
                new AttributeFilter(STUDY_ATTRS));
        device.setAttributeFilter(Entity.Series,
                new AttributeFilter(SERIES_ATTRS));
        device.setAttributeFilter(Entity.Instance,
                new AttributeFilter(INSTANCE_ATTRS));
        device.setAttributeFilter(Entity.Visit,
                new AttributeFilter(VISIT_ATTRS));
        device.setAttributeFilter(Entity.ServiceRequest,
                new AttributeFilter(SERVICE_REQUEST_ATTRS));
        device.setAttributeFilter(Entity.RequestedProcedure,
                new AttributeFilter(REQUESTED_PROCEDURE_ATTRS));
        device.setAttributeFilter(Entity.ScheduledProcedureStep,
                new AttributeFilter(SPS_ATTRS));
        device.setAttributeFilter(Entity.PerformedProcedureStep,
                new AttributeFilter(PPS_ATTRS));
    }

    private static Connection createConnection(String commonName,
            String hostname, int port, String... tlsCipherSuites) {
        Connection conn = new Connection(commonName, hostname, port);
        conn.setTlsCipherSuites(tlsCipherSuites);
        return conn;
    }


    static ArchiveApplicationEntity createAE(String aet,
            String[] image_tsuids, String[] video_tsuids, String[] other_tsuids,
            String pixConsumer, String pixManager) {
        ArchiveApplicationEntity ae = new ArchiveApplicationEntity(aet);
        ae.setAssociationAcceptor(true);
        ae.setAssociationInitiator(true);
        ae.setFileSystemGroupID("DEFAULT");
        ae.setSpoolFilePathFormat(new AttributesFormat(
                "archive/spool/{00020016,urlencoded}/{00020002}/{00020003}") );
        ae.setStorageFilePathFormat(new AttributesFormat(
                "archive/{now,date,yyyy/MM/dd}/{0020000D,hash}/{0020000E,hash}/{00080018,hash}") );
        ae.setDigestAlgorithm("MD5");
        ae.setRetrieveAETs(aet);
        ae.setStoreOriginalAttributes(true);
        ae.setPreserveSpoolFileOnFailure(true);
        ae.setSuppressWarningCoercionOfDataElements(false);
        ae.setMatchUnknown(true);
        ae.setSendPendingCGet(true);
        ae.setSendPendingCMoveInterval(5000);
        ae.addStoreDuplicate(
                new StoreDuplicate(
                        StoreDuplicate.Condition.NO_FILE,
                        StoreDuplicate.Action.STORE));
        ae.addStoreDuplicate(
                new StoreDuplicate(
                        StoreDuplicate.Condition.EQ_CHECKSUM,
                        StoreDuplicate.Action.IGNORE));
        ae.addStoreDuplicate(
                new StoreDuplicate(
                        StoreDuplicate.Condition.NE_CHECKSUM,
                        StoreDuplicate.Action.REPLACE));
        ae.addRejectionNote(INCORRECT_WORKLIST_ENTRY_SELECTED);
        ae.addRejectionNote(REJECTED_FOR_QUALITY_REASONS);
        ae.addRejectionNote(REJECT_FOR_PATIENT_SAFETY_REASONS);
        ae.addRejectionNote(INCORRECT_MODALITY_WORKLIST_ENTRY);
        ae.addRejectionNote(DATA_RETENTION_PERIOD_EXPIRED);
        ae.addAttributeCoercion(new AttributeCoercion(null, 
                Dimse.C_STORE_RQ, 
                SCP,
                "ENSURE_PID",
                "resource:dcm4chee-arc-ensure-pid.xsl"));
        ae.addAttributeCoercion(new AttributeCoercion(null, 
                Dimse.C_STORE_RQ, 
                SCU,
                "WITHOUT_PN",
                "resource:dcm4chee-arc-nullify-pn.xsl"));
        addTCs(ae, null, SCP, IMAGE_CUIDS, image_tsuids);
        addTCs(ae, null, SCP, VIDEO_CUIDS, video_tsuids);
        addTCs(ae, null, SCP, OTHER_CUIDS, other_tsuids);
        addTCs(ae, null, SCU, IMAGE_CUIDS, image_tsuids);
        addTCs(ae, null, SCU, VIDEO_CUIDS, video_tsuids);
        addTCs(ae, null, SCU, OTHER_CUIDS, other_tsuids);
        addTCs(ae, EnumSet.allOf(QueryOption.class), SCP, QUERY_CUIDS, UID.ImplicitVRLittleEndian);
        addTCs(ae, EnumSet.of(QueryOption.RELATIONAL), SCP, RETRIEVE_CUIDS, UID.ImplicitVRLittleEndian);
        addTC(ae, null, SCP, UID.CompositeInstanceRetrieveWithoutBulkDataGET, UID.ImplicitVRLittleEndian);
        addTC(ae, null, SCP, UID.StorageCommitmentPushModelSOPClass, UID.ImplicitVRLittleEndian);
        addTC(ae, null, SCP, UID.ModalityPerformedProcedureStepSOPClass, UID.ImplicitVRLittleEndian);
        addTC(ae, null, SCU, UID.ModalityPerformedProcedureStepSOPClass, UID.ImplicitVRLittleEndian);
        addTC(ae, null, SCU, UID.InstanceAvailabilityNotificationSOPClass, UID.ImplicitVRLittleEndian);
        addTC(ae, null, SCP, UID.VerificationSOPClass, UID.ImplicitVRLittleEndian);
        addTC(ae, null, SCU, UID.VerificationSOPClass, UID.ImplicitVRLittleEndian);
        ae.setShowEmptyStudy(false);
        ae.setShowEmptySeries(false);
        ae.setReturnOtherPatientIDs(true);
        ae.setReturnOtherPatientNames(true);
        ae.setLocalPIXConsumerApplication(pixConsumer);
        ae.setRemotePIXManagerApplication(pixManager);
        return ae;
    }

    static ArchiveApplicationEntity createAdminAE(String aet,
            String[] image_tsuids, String[] video_tsuids, String[] other_tsuids,
            String pixConsumer, String pixManager) {
        ArchiveApplicationEntity ae = new ArchiveApplicationEntity(aet);
        ae .setAssociationAcceptor(true);
        ae.setAssociationInitiator(true);
        ae.setMatchUnknown(true);
        ae.setSendPendingCGet(true);
        ae.setSendPendingCMoveInterval(PENDING_CMOVE_INTERVAL);
        ae.addRejectionNote(INCORRECT_WORKLIST_ENTRY_SELECTED);
        ae.addRejectionNote(REJECT_FOR_PATIENT_SAFETY_REASONS);
        ae.addRejectionNote(INCORRECT_MODALITY_WORKLIST_ENTRY);
        ae.addRejectionNote(DATA_RETENTION_PERIOD_EXPIRED);
        addTCs(ae, null, SCU, IMAGE_CUIDS, image_tsuids);
        addTCs(ae, null, SCU, VIDEO_CUIDS, video_tsuids);
        addTCs(ae, null, SCU, OTHER_CUIDS, other_tsuids);
        addTCs(ae, EnumSet.allOf(QueryOption.class), SCP, QUERY_CUIDS, UID.ImplicitVRLittleEndian);
        addTCs(ae, EnumSet.of(QueryOption.RELATIONAL), SCP, RETRIEVE_CUIDS, UID.ImplicitVRLittleEndian);
        addTC(ae, null, SCP, UID.CompositeInstanceRetrieveWithoutBulkDataGET, UID.ImplicitVRLittleEndian);
        addTC(ae, null, SCP, UID.VerificationSOPClass, UID.ImplicitVRLittleEndian);
        addTC(ae, null, SCU, UID.VerificationSOPClass, UID.ImplicitVRLittleEndian);
        ae.setShowEmptyStudy(true);
        ae.setShowEmptySeries(true);
        ae.setReturnOtherPatientIDs(true);
        ae.setReturnOtherPatientNames(true);
        ae.setLocalPIXConsumerApplication(pixConsumer);
        ae.setRemotePIXManagerApplication(pixManager);
        return ae;
    }

    static ArchiveHL7Application createHL7Application(String name) {
        ArchiveHL7Application hl7App = new ArchiveHL7Application(name);
        hl7App.setAcceptedMessageTypes(HL7_MESSAGE_TYPES);
        hl7App.setHL7DefaultCharacterSet("8859/1");
        hl7App.addTemplatesURI("adt2dcm", "resource:dcm4chee-arc-hl7-adt2dcm.xsl");
        return hl7App ;
    }


    private static void addTCs(ArchiveApplicationEntity ae, EnumSet<QueryOption> queryOpts,
            TransferCapability.Role role, String[] cuids, String... tss) {
        for (String cuid : cuids)
            addTC(ae, queryOpts, role, cuid, tss);
    }

    private static void addTC(ArchiveApplicationEntity ae, EnumSet<QueryOption> queryOpts,
            TransferCapability.Role role, String cuid, String... tss) {
        String name = UID.nameOf(cuid).replace('/', ' ');
        TransferCapability tc = new TransferCapability(name + ' ' + role, cuid, role, tss);
        tc.setQueryOptions(queryOpts);
        ae.addTransferCapability(tc);
    }
}
