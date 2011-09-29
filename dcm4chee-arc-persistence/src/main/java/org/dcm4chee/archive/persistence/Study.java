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

package org.dcm4chee.archive.persistence;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.PersonName;
import org.dcm4che.data.Tag;
import org.dcm4che.util.DateUtils;
import org.dcm4che.util.StringUtils;


/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@NamedQueries({
@NamedQuery(
    name="Study.findByStudyInstanceUID",
    query="SELECT s FROM Study s WHERE s.studyInstanceUID = ?1"),
@NamedQuery(
    name="Study.modalitiesInStudy",
    query="SELECT DISTINCT(s.modality) FROM Series s WHERE s.study = ?1"),
@NamedQuery(
    name="Study.sopClassesInStudy",
    query="SELECT DISTINCT(i.sopClassUID) FROM Instance i WHERE i.series.study = ?1"),
@NamedQuery(
    name="Study.countRelatedSeries",
    query="SELECT COUNT(s) FROM Series s WHERE s.study = ?1"),
@NamedQuery(
    name="Study.countRelatedInstances",
    query="SELECT COUNT(i) FROM Instance i WHERE i.series.study = ?1"),
@NamedQuery(
    name="Study.retrieveAETs",
    query="SELECT DISTINCT(s.retrieveAETs) FROM Series s WHERE s.study = ?1"),
@NamedQuery(
    name="Study.externalRetrieveAET",
    query="SELECT DISTINCT(s.externalRetrieveAET) FROM Series s WHERE s.study = ?1"),
@NamedQuery(
    name="Study.availability",
    query="SELECT MAX(s.availability) FROM Series s WHERE s.study = ?1")
})
@Entity
@Table(name = "study")
public class Study implements Serializable {

    private static final long serialVersionUID = -6358525535057418771L;

    public static final String FIND_BY_STUDY_INSTANCE_UID = "Study.findByStudyInstanceUID";
    public static final String MODALITIES_IN_STUDY = "Study.modalitiesInStudy";
    public static final String SOP_CLASSES_IN_STUDY = "Study.sopClassesInStudy";
    public static final String COUNT_RELATED_SERIES = "Study.countRelatedSeries";
    public static final String COUNT_RELATED_INSTANCES = "Study.countRelatedInstances";
    public static final String RETRIEVE_AETS = "Study.retrieveAETs";
    public static final String EXTERNAL_RETRIEVE_AET = "Study.externalRetrieveAET";
    public static final String AVAILABILITY = "Study.availability";

    @Id
    @GeneratedValue
    @Column(name = "pk")
    private long pk;

    @Basic(optional = false)
    @Column(name = "created_time", updatable = false)
    private Date createdTime;

    @Basic(optional = false)
    @Column(name = "updated_time")
    private Date updatedTime;

    @Basic(optional = false)
    @Column(name = "study_iuid", updatable = false)
    private String studyInstanceUID;

    @Basic(optional = false)
    @Column(name = "study_id")
    private String studyID;

    @Basic(optional = false)
    @Column(name = "study_date")
    private String studyDate;

    @Basic(optional = false)
    @Column(name = "study_time")
    private String studyTime;

    @Basic(optional = false)
    @Column(name = "accession_no")
    private String accessionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accno_issuer_fk")
    private Issuer issuerOfAccessionNumber;

    @Basic(optional = false)
    @Column(name = "ref_physician")
    private String referringPhysicianName;
    
    @Basic(optional = false)
    @Column(name = "ref_phys_fn_sx")
    private String referringPhysicianFamilyNameSoundex;
    
    @Basic(optional = false)
    @Column(name = "ref_phys_gn_sx")
    private String referringPhysicianGivenNameSoundex;

    @Basic(optional = false)
    @Column(name = "ref_phys_i_name")
    private String referringPhysicianIdeographicName;

    @Basic(optional = false)
    @Column(name = "ref_phys_p_name")
    private String referringPhysicianPhoneticName;

    @Basic(optional = false)
    @Column(name = "study_desc")
    private String studyDescription;

    @Basic(optional = false)
    @Column(name = "study_custom1")
    private String studyCustomAttribute1;

    @Basic(optional = false)
    @Column(name = "study_custom2")
    private String studyCustomAttribute2;

    @Basic(optional = false)
    @Column(name = "study_custom3")
    private String studyCustomAttribute3;

    @Basic(optional = false)
    @Column(name = "num_series")
    private int numberOfStudyRelatedSeries;

    @Basic(optional = false)
    @Column(name = "num_instances")
    private int numberOfStudyRelatedInstances;

    @Column(name = "mods_in_study")
    private String modalitiesInStudy;

    @Column(name = "cuids_in_study")
    private String sopClassesInStudy;

    @Column(name = "retrieve_aets")
    private String retrieveAETs;

    @Column(name = "ext_retr_aet")
    private String externalRetrieveAET;

    @Basic(optional = false)
    @Column(name = "availability")
    private Availability availability;

    @Basic(optional = false)
    @Column(name = "study_attrs")
    private byte[] encodedAttributes;

    @ManyToMany
    @JoinTable(name = "rel_study_pcode", 
        joinColumns = @JoinColumn(name = "study_fk", referencedColumnName = "pk"),
        inverseJoinColumns = @JoinColumn(name = "pcode_fk", referencedColumnName = "pk"))
    private Collection<Code> procedureCodes;

    @ManyToOne
    @JoinColumn(name = "patient_fk")
    private Patient patient;

    @OneToMany(mappedBy = "study", orphanRemoval = true)
    private Collection<Series> series;

    @Override
    public String toString() {
        return "Study[pk=" + pk
                + ", uid=" + studyInstanceUID
                + ", id=" + studyID
                + ", mods=" + modalitiesInStudy
                + ", numS=" + numberOfStudyRelatedSeries
                + ", numI=" + numberOfStudyRelatedInstances
                + "]";
    }

    @PrePersist
    public void onPrePersist() {
        Date now = new Date();
        createdTime = now;
        updatedTime = now;
    }

    @PreUpdate
    public void onPreUpdate() {
        updatedTime = new Date();
    }

    public Attributes getAttributes() throws IOException {
        return Utils.decodeAttributes(encodedAttributes);
    }

    public long getPk() {
        return pk;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public String getStudyInstanceUID() {
        return studyInstanceUID;
    }

    public String getStudyID() {
        return studyID;
    }

    public String getStudyDate() {
        return studyDate;
    }

    public String getStudyTime() {
        return studyTime;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public Issuer getIssuerOfAccessionNumber() {
        return issuerOfAccessionNumber;
    }

    public void setIssuerOfAccessionNumber(Issuer issuerOfAccessionNumber) {
        this.issuerOfAccessionNumber = issuerOfAccessionNumber;
    }

    public String getReferringPhysicianName() {
        return referringPhysicianName;
    }

    public String getReferringPhysicianFamilyNameSoundex() {
        return referringPhysicianFamilyNameSoundex;
    }

    public String getReferringPhysicianGivenNameSoundex() {
        return referringPhysicianGivenNameSoundex;
    }

    public String getReferringPhysicianIdeographicName() {
        return referringPhysicianIdeographicName;
    }

    public String getReferringPhysicianPhoneticName() {
        return referringPhysicianPhoneticName;
    }

    public String getStudyDescription() {
        return studyDescription;
    }

    public String getStudyCustomAttribute1() {
        return studyCustomAttribute1;
    }

    public String getStudyCustomAttribute2() {
        return studyCustomAttribute2;
    }

    public String getStudyCustomAttribute3() {
        return studyCustomAttribute3;
    }

    public int getNumberOfStudyRelatedSeries() {
        return numberOfStudyRelatedSeries;
    }

    public void setNumberOfStudyRelatedSeries(int numberOfStudyRelatedSeries) {
        this.numberOfStudyRelatedSeries = numberOfStudyRelatedSeries;
    }

    public int getNumberOfStudyRelatedInstances() {
        return numberOfStudyRelatedInstances;
    }

    public void setNumberOfStudyRelatedInstances(int numberOfStudyRelatedInstances) {
        this.numberOfStudyRelatedInstances = numberOfStudyRelatedInstances;
    }

    public String[] getModalitiesInStudy() {
        return StringUtils.split(modalitiesInStudy, '\\');
    }

    public void setModalitiesInStudy(String... modalitiesInStudy) {
        this.modalitiesInStudy = StringUtils.join(modalitiesInStudy, '\\');
    }

    public String[] getSOPClassesInStudy() {
        return StringUtils.split(sopClassesInStudy, '\\');
    }

    public void setSOPClassesInStudy(String... sopClassesInStudy) {
        this.sopClassesInStudy = StringUtils.join(sopClassesInStudy, '\\');
    }

    public String[] getRetrieveAETs() {
        return StringUtils.split(retrieveAETs, '\\');
    }

    public void setRetrieveAETs(String... retrieveAETs) {
        this.retrieveAETs = StringUtils.join(retrieveAETs, '\\');
    }

    public String getExternalRetrieveAET() {
        return externalRetrieveAET;
    }

    public void setExternalRetrieveAET(String externalRetrieveAET) {
        this.externalRetrieveAET = externalRetrieveAET;
    }

    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(Availability availability) {
        this.availability = availability;
    }

    public byte[] getEncodedAttributes() {
        return encodedAttributes;
    }

    public Collection<Code> getProcedureCodes() {
        return procedureCodes;
    }

    public void setProcedureCodes(Collection<Code> procedureCodes) {
        this.procedureCodes = procedureCodes;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Collection<Series> getSeries() {
        return series;
    }

    public void setAttributes(Attributes attrs, StoreParam storeParam) {
        studyInstanceUID = attrs.getString(Tag.StudyInstanceUID);
        studyID = attrs.getString(Tag.StudyID, "*");
        studyDescription = attrs.getString(Tag.StudyDescription, "*");
        Date dt = attrs.getDate(Tag.StudyDateAndTime);
        if (dt != null) {
            studyDate = DateUtils.formatDA(null, dt);
            studyTime = attrs.containsValue(Tag.StudyTime)
                    ? DateUtils.formatTM(null, dt)
                    : "*";
        } else {
            studyDate = "*";
            studyTime = "*";
        }
        accessionNumber = attrs.getString(Tag.AccessionNumber, "*");
        PersonName pn = new PersonName(attrs.getString(Tag.ReferringPhysicianName), true);
        if (pn.isEmpty()) {
            referringPhysicianName = "*";
            referringPhysicianIdeographicName = "*";
            referringPhysicianPhoneticName = "*";
            referringPhysicianFamilyNameSoundex = "*";
            referringPhysicianGivenNameSoundex = "*";
        } else {
            referringPhysicianName = pn.contains(PersonName.Group.Alphabetic) 
                    ? pn.toString(PersonName.Group.Alphabetic, false) : "*";
            referringPhysicianIdeographicName = pn.contains(PersonName.Group.Ideographic)
                    ? pn.toString(PersonName.Group.Ideographic, false) : "*";
            referringPhysicianPhoneticName = pn.contains(PersonName.Group.Phonetic)
                    ? pn.toString(PersonName.Group.Phonetic, false) : "*";
            referringPhysicianFamilyNameSoundex =
                    storeParam.toFuzzy(pn.get(PersonName.Component.FamilyName), "*");
            referringPhysicianGivenNameSoundex =
                    storeParam.toFuzzy(pn.get(PersonName.Component.GivenName), "*");
        }
        studyCustomAttribute1 = StoreParam.selectStringValue(attrs,
                storeParam.getStudyCustomAttribute1(), "*");
        studyCustomAttribute2 = StoreParam.selectStringValue(attrs,
                storeParam.getStudyCustomAttribute2(), "*");
        studyCustomAttribute3 = StoreParam.selectStringValue(attrs,
                storeParam.getStudyCustomAttribute3(), "*");

        encodedAttributes = Utils.encodeAttributes(attrs, storeParam.getStudyAttributes());
        
    }
}
