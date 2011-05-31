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
import java.util.Collection;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.util.DateUtils;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@NamedQueries({
@NamedQuery(
    name="Instance.findBySOPInstanceUID",
    query="SELECT i FROM Instance i WHERE i.sopInstanceUID = ?1")
})
@Entity
@Table(name = "instance")
public class Instance implements Serializable {

    private static final long serialVersionUID = -6510894512195470408L;

    public static final String FIND_BY_SOP_INSTANCE_UID =
        "Instance.findBySOPInstanceUID";

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
    @Column(name = "sop_iuid")
    private String sopInstanceUID;

    @Basic(optional = false)
    @Column(name = "sop_cuid")
    private String sopClassUID;

    @Basic(optional = false)
    @Column(name = "inst_no")
    private String instanceNumber;

    @Basic(optional = false)
    @Column(name = "content_date")
    private String contentDate;

    @Basic(optional = false)
    @Column(name = "content_time")
    private String contentTime;

    @Basic(optional = false)
    @Column(name = "sr_complete")
    private String completionFlag;

    @Basic(optional = false)
    @Column(name = "sr_verified")
    private String verificationFlag;

    @Basic(optional = false)
    @Column(name = "inst_custom1")
    private String instanceCustomAttribute1;

    @Basic(optional = false)
    @Column(name = "inst_custom2")
    private String instanceCustomAttribute2;

    @Basic(optional = false)
    @Column(name = "inst_custom3")
    private String instanceCustomAttribute3;

    @Column(name = "retrieve_aets")
    private String retrieveAETs;

    @Column(name = "ext_retr_aet")
    private String externalRetrieveAET;

    @Basic(optional = false)
    @Column(name = "availability")
    private Availability availability;

    @Basic(optional = false)
    @Column(name = "archived")
    private boolean archived;

    @Basic(optional = false)
    @Column(name = "commitment")
    private boolean storageComitted;

    @Basic(optional = false)
    @Column(name = "inst_attrs")
    private byte[] encodedAttributes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "srcode_fk")
    private Code conceptNameCode;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "instance_fk")
    private Collection<VerifyingObserver> verifyingObservers;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "series_fk")
    private Series series;

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

    public String getSopInstanceUID() {
        return sopInstanceUID;
    }

    public String getSopClassUID() {
        return sopClassUID;
    }

    public String getInstanceNumber() {
        return instanceNumber;
    }

    public String getContentDate() {
        return contentDate;
    }

    public String getContentTime() {
        return contentTime;
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

    public void setAvailability(Availability availability) {
        this.availability = availability;
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

    public void setConceptNameCode(Code conceptNameCode) {
        this.conceptNameCode = conceptNameCode;
    }

    public Collection<VerifyingObserver> getVerifyingObservers() {
        return verifyingObservers;
    }

    public void setVerifyingObservers(
            Collection<VerifyingObserver> verifyingObservers) {
        this.verifyingObservers = verifyingObservers;
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public void setAttributes(Attributes attrs) {
        sopInstanceUID = attrs.getString(Tag.SOPInstanceUID, null);
        sopClassUID = attrs.getString(Tag.SOPClassUID, null);
        instanceNumber = AttributeFilter.getString(attrs, Tag.InstanceNumber);
        Date dt = attrs.getDate(Tag.ContentDateAndTime, null);
        if (dt != null) {
            contentDate = DateUtils.formatDA(null, dt);
            contentTime = 
                attrs.containsValue(Tag.ContentTime)
                    ? DateUtils.formatTM(null, dt)
                    : "*";
        } else {
            contentDate = "*";
            contentTime = "*";
        }
        completionFlag = AttributeFilter.getString(attrs, Tag.CompletionFlag);
        verificationFlag = AttributeFilter.getString(attrs, Tag.VerificationFlag);

        //TODO
        instanceCustomAttribute1 = "*";
        instanceCustomAttribute2 = "*";
        instanceCustomAttribute3 = "*";

        encodedAttributes = Utils.encodeAttributes(attrs,
                AttributeFilter.instanceFilter);
        
    }
}
