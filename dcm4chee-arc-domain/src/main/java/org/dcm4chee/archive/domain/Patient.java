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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.PersonName;
import org.dcm4che.data.Tag;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Entity
@Table(name = "patient")
public class Patient implements Serializable {

    private static final long serialVersionUID = 6430339764844147679L;

    @Id
    @GeneratedValue
    @Column(name = "pk")
    private long pk;

    @Column(name = "created_time")
    private Date createdTime;

    @Column(name = "updated_time")
    private Date updatedTime;

    @Column(name = "pat_id")
    private String patientID;

    @Column(name = "pat_id_issuer")
    private String issuerOfPatientID;

    @Column(name = "pat_name")
    private String patientName;

    @Column(name = "pat_fn_sx")
    private String patientFamilyNameSoundex;

    @Column(name = "pat_gn_sx")
    private String patientGivenNameSoundex;

    @Column(name = "pat_i_name")
    private String patientIdeographicName;

    @Column(name = "pat_p_name")
    private String patientPhoneticName;

    @Column(name = "pat_birthdate")
    private String patientBirthDate;

    @Column(name = "pat_sex")
    private String patientSex;

    @Column(name = "pat_custom1")
    private String patientCustomAttribute1;

    @Column(name = "pat_custom2")
    private String patientCustomAttribute2;

    @Column(name = "pat_custom3")
    private String patientCustomAttribute3;

    @Column(name = "pat_attrs", nullable = false)
    private byte[] encodedAttributes;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "merge_fk")
    private Patient mergedWith;

    @OneToMany(mappedBy = "mergedWith", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<Patient> previous;

    @OneToMany(mappedBy = "patient", fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<Study> studies;

    @Override
    public String toString() {
        return "Patient[pk=" + pk
                + ", id=" + patientID
                + ", issuer=" + issuerOfPatientID
                + ", name=" + patientName
                + ", dob=" + patientBirthDate
                + ", sex=" + patientSex
                + "]";
    }

    @PrePersist
    public void onPrePersist() {
        createdTime = new Date();
    }

    @PreUpdate
    public void onPreUpdate() {
        updatedTime = new Date();
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

    public String getPatientID() {
        return patientID;
    }

    public String getIssuerOfPatientID() {
        return issuerOfPatientID;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientFamilyNameSoundex() {
        return patientFamilyNameSoundex;
    }

    public String getPatientGivenNameSoundex() {
        return patientGivenNameSoundex;
    }

    public String getPatientIdeographicName() {
        return patientIdeographicName;
    }

    public String getPatientPhoneticName() {
        return patientPhoneticName;
    }

    public String getPatientBirthDate() {
        return patientBirthDate;
    }

    public String getPatientSex() {
        return patientSex;
    }

    public String getPatientCustomAttribute1() {
        return patientCustomAttribute1;
    }

    public String getPatientCustomAttribute2() {
        return patientCustomAttribute2;
    }

    public String getPatientCustomAttribute3() {
        return patientCustomAttribute3;
    }

    public Patient getMergedWith() {
        return mergedWith;
    }

    public Set<Patient> getPrevious() {
        return previous;
    }

    public Set<Study> getStudies() {
        return studies;
    }

    public byte[] getEncodedAttributes() {
        return encodedAttributes;
    }

    public Attributes getAttributes() throws IOException {
        return Utils.decodeAttributes(encodedAttributes);
    }

    public void setAttributes(Attributes attrs) {
        patientID = attrs.getString(Tag.PatientID, null);
        issuerOfPatientID = attrs.getString(Tag.IssuerOfPatientID, null);
        PersonName pn = new PersonName(attrs.getString(Tag.PatientName, null));
        patientName =
                pn.toNormalizedString(PersonName.Group.Alphabetic);
        patientIdeographicName =
                pn.toNormalizedString(PersonName.Group.Ideographic);
        patientPhoneticName =
                pn.toNormalizedString(PersonName.Group.Phonetic);
        patientBirthDate = attrs.getString(Tag.PatientBirthDate, null);
        patientSex = attrs.getString(Tag.PatientSex, null);
        encodedAttributes = Utils.encodeAttributes(attrs,
                AttributeFilter.patientFilter);
    }
}
