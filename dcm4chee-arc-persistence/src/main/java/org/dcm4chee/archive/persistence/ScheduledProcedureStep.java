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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@NamedQueries({
    @NamedQuery(
        name="ScheduledProcedureStep.findBySPSID",
        query="SELECT sps FROM ScheduledProcedureStep sps WHERE sps.scheduledProcedureStepID = ?1")
})
@Entity
@Table(name = "sps")
public class ScheduledProcedureStep implements Serializable {

    private static final long serialVersionUID = 7056153659801553552L;

    public static final String FIND_BY_SPS_ID = "ScheduledProcedureStep.findBySPSID";

    @Id
    @GeneratedValue
    @Column(name = "pk")
    private long pk;

    @Basic(optional = false)
    @Column(name = "sps_id")
    private String scheduledProcedureStepID;

    @ManyToOne
    @JoinColumn(name = "req_proc_fk")
    private RequestedProcedure requestedProcedure;

    @ManyToMany(mappedBy="scheduledProcedureSteps")
    private Collection<Series> series;

    @Override
    public String toString() {
        return "RequestedProcedure[pk=" + pk
                + ", id=" + scheduledProcedureStepID
                + "]";
    }

    public long getPk() {
        return pk;
    }

    public String getScheduledProcedureStepID() {
        return scheduledProcedureStepID;
    }

    public RequestedProcedure getRequestedProcedure() {
        return requestedProcedure;
    }

    public void setRequestedProcedure(RequestedProcedure requestedProcedure) {
        this.requestedProcedure = requestedProcedure;
    }

    public Collection<Series> getSeries() {
        return series;
    }

    public void setAttributes(Attributes requestAttrs, StoreParam storeParam) {
        scheduledProcedureStepID = requestAttrs.getString(Tag.ScheduledProcedureStepID);
    }

}
