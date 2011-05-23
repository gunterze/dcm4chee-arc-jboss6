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

package org.dcm4chee.archive.entity;

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

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Entity
@Table(name = "series")
public class Series implements Serializable {

    private static final long serialVersionUID = -8317105475421750944L;

    @Id
    @GeneratedValue
    @Column(name = "pk")
    private long pk;

    @Column(name = "created_time")
    private Date createdTime;

    @Column(name = "updated_time")
    private Date updatedTime;

    @Column(name = "series_iuid", nullable = false)
    private String seriesInstanceUID;

    @Column(name = "series_no")
    private String seriesNumber;

    @Column(name = "series_desc")
    private String seriesDescription;

    @Column(name = "modality")
    private String modality;

    @Column(name = "department")
    private String institutionalDepartmentName;

    @Column(name = "institution")
    private String institutionName;

    @Column(name = "station_name")
    private String stationName;

    @Column(name = "body_part")
    private String bodyPartExamined;

    @Column(name = "laterality")
    private String laterality;

    @Column(name = "perf_physician")
    private String performingPhysicianName;
    
    @Column(name = "perf_phys_fn_sx")
    private String performingPhysicianFamilyNameSoundex;
    
    @Column(name = "perf_phys_gn_sx")
    private String performingPhysicianGivenNameSoundex;

    @Column(name = "perf_phys_i_name")
    private String performingPhysicianIdeographicName;

    @Column(name = "perf_phys_p_name")
    private String performingPhysicianPhoneticName;

    @Column(name = "pps_start")
    private Date performedProcedureStepStartDateTime;

    @Column(name = "pps_iuid")
    private String performedProcedureStepInstanceUID;

    @Column(name = "series_custom1")
    private String seriesCustomAttribute1;

    @Column(name = "series_custom2")
    private String seriesCustomAttribute2;

    @Column(name = "series_custom3")
    private String seriesCustomAttribute3;

    @Column(name = "num_instances", nullable = false)
    private int numberOfSeriesRelatedInstances;

    @Column(name = "src_aet")
    private String sourceAET;

    @Column(name = "retrieve_aets")
    private String retrieveAETs;

    @Column(name = "ext_retr_aet")
    private String externalRetrieveAET;

    @Column(name = "availability", nullable = false)
    private Availability availability;

    @Column(name = "series_attrs", nullable = false)
    private byte[] encodedAttributes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inst_code_fk")
    private Code institutionCode;

    @OneToMany(mappedBy = "series", fetch = FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<RequestAttributes> requestAttributes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_fk")
    private Study study;

    @OneToMany(mappedBy = "series", fetch = FetchType.LAZY, cascade=CascadeType.REMOVE)
    private Set<Instance> instances;

    public void onPrePersist() {
        createdTime = new Date();
    }

    public void onPreUpdate() {
        updatedTime = new Date();
    }

}
