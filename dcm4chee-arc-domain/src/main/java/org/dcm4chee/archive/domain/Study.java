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

package org.dcm4chee.archive.domain;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
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


/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@NamedQueries({
@NamedQuery(
    name="Study.findByStudyInstanceUID",
    query="SELECT s FROM Study s WHERE s.studyInstanceUID = ?1")
})
@Entity
@Table(name = "study")
public class Study implements Serializable {

    private static final long serialVersionUID = -6358525535057418771L;

    public static final String FIND_BY_STUDY_INSTANCE_UID =
        "Study.findByStudyInstanceUID";

    @Id
    @GeneratedValue
    @Column(name = "pk")
    private long pk;

    @Basic(optional = false)
    @Column(name = "created_time")
    private Date createdTime;

    @Basic(optional = false)
    @Column(name = "updated_time")
    private Date updatedTime;

    @Basic(optional = false)
    @Column(name = "study_iuid")
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

    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(name = "rel_study_pcode", 
        joinColumns = @JoinColumn(name = "study_fk", referencedColumnName = "pk"),
        inverseJoinColumns = @JoinColumn(name = "pcode_fk", referencedColumnName = "pk"))
    private Set<Code> procedureCodes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accno_issuer_fk")
    private Issuer issuerOfAccessionNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_fk")
    private Patient patient;
    
    @OneToMany(mappedBy = "study", fetch = FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<Series> series;

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

    public int getNumberOfStudyRelatedInstances() {
        return numberOfStudyRelatedInstances;
    }

    public String getModalitiesInStudy() {
        return modalitiesInStudy;
    }

    public String getSopClassesInStudy() {
        return sopClassesInStudy;
    }

    public String getRetrieveAETs() {
        return retrieveAETs;
    }

    public String getExternalRetrieveAET() {
        return externalRetrieveAET;
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

    public Set<Code> getProcedureCodes() {
        return procedureCodes;
    }

    public Issuer getIssuerOfAccessionNumber() {
        return issuerOfAccessionNumber;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Set<Series> getSeries() {
        return series;
    }

    public void setAttributes(Attributes attrs) {
        studyInstanceUID = attrs.getString(Tag.StudyInstanceUID, null);
        studyID = AttributeFilter.getString(attrs, Tag.StudyID);
        studyDescription =
                AttributeFilter.getString(attrs, Tag.StudyDescription);
        Date dt = attrs.getDate(Tag.StudyDateAndTime, null);
        if (dt != null) {
            studyDate = DateUtils.formatDA(null, dt);
            studyTime = attrs.containsValue(Tag.StudyTime)
                    ? DateUtils.formatTM(null, dt)
                    : "*";
        } else {
            studyDate = "*";
            studyTime = "*";
        }
        accessionNumber =
                AttributeFilter.getString(attrs, Tag.AccessionNumber);
        String s = AttributeFilter.getString(attrs, Tag.ReferringPhysicianName);
        if (s.equals("*")) {
            referringPhysicianName = "*";
            referringPhysicianIdeographicName = "*";
            referringPhysicianPhoneticName = "*";
            referringPhysicianFamilyNameSoundex = "*";
            referringPhysicianGivenNameSoundex = "*";
        } else {
            PersonName pn = new PersonName(s);
            referringPhysicianName =
                    pn.getNormalizedString(PersonName.Group.Alphabetic, "*");
            referringPhysicianIdeographicName =
                    pn.getNormalizedString(PersonName.Group.Ideographic, "*");
            referringPhysicianPhoneticName =
                    pn.getNormalizedString(PersonName.Group.Phonetic, "*");
            //TODO
            referringPhysicianFamilyNameSoundex = "*";
            referringPhysicianGivenNameSoundex = "*";
        }
        //TODO
        studyCustomAttribute1 = "*";
        studyCustomAttribute2 = "*";
        studyCustomAttribute3 = "*";

        encodedAttributes = Utils.encodeAttributes(attrs,
                AttributeFilter.studyFilter);
        
    }
}
