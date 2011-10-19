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

import java.io.Serializable;
import java.util.Collection;

import javax.persistence.Basic;
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
import javax.persistence.Table;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.PersonName;
import org.dcm4che.data.Tag;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@NamedQueries({
    @NamedQuery(
        name="ServiceRequest.findByAccessionNumber",
        query="SELECT rq FROM ServiceRequest rq WHERE rq.accessionNumber = ?1")
})
@Entity
@Table(name = "request")
public class ServiceRequest implements Serializable {

    private static final long serialVersionUID = 4625226424616368458L;

    public static final String FIND_BY_ACCESSION_NUMBER = "ServiceRequest.findByAccessionNumber";

    @Id
    @GeneratedValue
    @Column(name = "pk")
    private long pk;

    @Basic(optional = false)
    @Column(name = "accession_no")
    private String accessionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accno_issuer_fk")
    private Issuer issuerOfAccessionNumber;

    @Basic(optional = false)
    @Column(name = "req_service")
    private String requestingService;

    @Basic(optional = false)
    @Column(name = "req_physician")
    private String requestingPhysician;
    
    @Basic(optional = false)
    @Column(name = "req_phys_fn_sx")
    private String requestingPhysicianFamilyNameSoundex;
    
    @Basic(optional = false)
    @Column(name = "req_phys_gn_sx")
    private String requestingPhysicianGivenNameSoundex;

    @Basic(optional = false)
    @Column(name = "req_phys_i_name")
    private String requestingPhysicianIdeographicName;

    @Basic(optional = false)
    @Column(name = "req_phys_p_name")
    private String requestingPhysicianPhoneticName;

    @ManyToOne
    @JoinColumn(name = "patient_fk")
    private Patient patient;

    @OneToMany(mappedBy = "serviceRequest", orphanRemoval = true)
    private Collection<RequestedProcedure> requestedProcedures;

    @Override
    public String toString() {
        return "ServiceRequest[pk=" + pk
                + ", accNo=" + accessionNumber
                + "]";
    }

    public long getPk() {
        return pk;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setIssuerOfAccessionNumber(Issuer issuerOfAccessionNumber) {
        this.issuerOfAccessionNumber = issuerOfAccessionNumber;
    }

    public String getRequestingService() {
        return requestingService;
    }

    public String getRequestingPhysician() {
        return requestingPhysician;
    }

    public String getRequestingPhysicianFamilyNameSoundex() {
        return requestingPhysicianFamilyNameSoundex;
    }

    public String getRequestingPhysicianGivenNameSoundex() {
        return requestingPhysicianGivenNameSoundex;
    }

    public String getRequestingPhysicianIdeographicName() {
        return requestingPhysicianIdeographicName;
    }

    public String getRequestingPhysicianPhoneticName() {
        return requestingPhysicianPhoneticName;
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

    public Collection<RequestedProcedure> getRequestedProcedures() {
        return requestedProcedures;
    }

    public void setAttributes(Attributes attrs, StoreParam storeParam) {
        accessionNumber = attrs.getString(Tag.AccessionNumber);
        requestingService = attrs.getString(Tag.RequestingService, "*");
        PersonName pn = new PersonName(attrs.getString(Tag.RequestingPhysician), true);
        if (pn.isEmpty()) {
            requestingPhysician = "*";
            requestingPhysicianIdeographicName = "*";
            requestingPhysicianPhoneticName = "*";
            requestingPhysicianFamilyNameSoundex = "*";
            requestingPhysicianGivenNameSoundex = "*";
        } else {
            requestingPhysician = pn.contains(PersonName.Group.Alphabetic) 
                    ? pn.toString(PersonName.Group.Alphabetic, false) : "*";
            requestingPhysicianIdeographicName = pn.contains(PersonName.Group.Ideographic)
                    ? pn.toString(PersonName.Group.Ideographic, false) : "*";
            requestingPhysicianPhoneticName = pn.contains(PersonName.Group.Phonetic)
                    ? pn.toString(PersonName.Group.Phonetic, false) : "*";
            requestingPhysicianFamilyNameSoundex =
                    storeParam.toFuzzy(pn.get(PersonName.Component.FamilyName), "*");
            requestingPhysicianGivenNameSoundex =
                    storeParam.toFuzzy(pn.get(PersonName.Component.GivenName), "*");
        }
    }
}
