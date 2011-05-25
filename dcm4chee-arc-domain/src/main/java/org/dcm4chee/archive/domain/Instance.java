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
import javax.persistence.Table;

import org.dcm4che.data.Attributes;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Entity
@Table(name = "instance")
public class Instance implements Serializable {

    private static final long serialVersionUID = -6510894512195470408L;

    @Id
    @GeneratedValue
    @Column(name = "pk")
    private long pk;

    @Column(name = "created_time")
    private Date createdTime;

    @Column(name = "updated_time")
    private Date updatedTime;

    @Column(name = "sop_iuid", nullable = false)
    private String sopInstanceUID;

    @Column(name = "sop_cuid", nullable = false)
    private String sopClassUID;

    @Column(name = "inst_no")
    private String instanceNumber;

    @Column(name = "content_datetime")
    private Date contentDateTime;

    @Column(name = "sr_complete")
    private String completionFlag;

    @Column(name = "sr_verified")
    private String verificationFlag;

    @Column(name = "inst_custom1")
    private String instanceCustomAttribute1;

    @Column(name = "inst_custom2")
    private String instanceCustomAttribute2;

    @Column(name = "inst_custom3")
    private String instanceCustomAttribute3;

    @Column(name = "retrieve_aets")
    private String retrieveAETs;

    @Column(name = "ext_retr_aet")
    private String externalRetrieveAET;

    @Column(name = "availability", nullable = false)
    private Availability availability;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Column(name = "commitment", nullable = false)
    private boolean storageComitted;

    @Column(name = "inst_attrs", nullable = false)
    private byte[] encodedAttributes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "srcode_fk")
    private Code conceptNameCode;

    @OneToMany(mappedBy = "instance", fetch = FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<VerifyingObserver> verifyingObservers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_fk")
    private Series series;

    public void onPrePersist() {
        createdTime = new Date();
    }

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

    public String getSopInstanceUID() {
        return sopInstanceUID;
    }

    public String getSopClassUID() {
        return sopClassUID;
    }

    public String getInstanceNumber() {
        return instanceNumber;
    }

    public Date getContentDateTime() {
        return contentDateTime;
    }

    public String getCompletionFlag() {
        return completionFlag;
    }

    public String getVerificationFlag() {
        return verificationFlag;
    }

    public String getInstanceCustomAttribute1() {
        return instanceCustomAttribute1;
    }

    public String getInstanceCustomAttribute2() {
        return instanceCustomAttribute2;
    }

    public String getInstanceCustomAttribute3() {
        return instanceCustomAttribute3;
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

    public boolean isArchived() {
        return archived;
    }

    public boolean isStorageComitted() {
        return storageComitted;
    }

    public byte[] getEncodedAttributes() {
        return encodedAttributes;
    }

    public Code getConceptNameCode() {
        return conceptNameCode;
    }

    public Set<VerifyingObserver> getVerifyingObservers() {
        return verifyingObservers;
    }

    public Series getSeries() {
        return series;
    }
}
