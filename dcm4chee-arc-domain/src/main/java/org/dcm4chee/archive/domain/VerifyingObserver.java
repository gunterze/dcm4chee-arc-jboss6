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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Entity
@Table(name = "verify_observer")
public class VerifyingObserver implements Serializable {

    private static final long serialVersionUID = -4116421655961983539L;

    @Id
    @GeneratedValue
    @Column(name = "pk")
    private long pk;

    @Column(name = "verify_datetime")
    private String verificationDateTime;

    @Column(name = "observer_name")
    private String verifyingObserverName;
    
    @Column(name = "observer_fn_sx")
    private String verifyingObserverFamilyNameSoundex;
    
    @Column(name = "observer_gn_sx")
    private String verifyingObserverGivenNameSoundex;

    @Column(name = "observer_i_name")
    private String verifyingObserverIdeographicName;

    @Column(name = "observer_p_name")
    private String verifyingObserverPhoneticName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_fk")
    private Instance instance;

    public long getPk() {
        return pk;
    }

    public String getVerificationDateTime() {
        return verificationDateTime;
    }

    public String getVerifyingObserverName() {
        return verifyingObserverName;
    }

    public String getVerifyingObserverFamilyNameSoundex() {
        return verifyingObserverFamilyNameSoundex;
    }

    public String getVerifyingObserverGivenNameSoundex() {
        return verifyingObserverGivenNameSoundex;
    }

    public String getVerifyingObserverIdeographicName() {
        return verifyingObserverIdeographicName;
    }

    public String getVerifyingObserverPhoneticName() {
        return verifyingObserverPhoneticName;
    }

    public Instance getInstance() {
        return instance;
    }
}
