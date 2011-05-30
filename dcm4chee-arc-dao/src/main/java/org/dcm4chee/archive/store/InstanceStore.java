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

package org.dcm4chee.archive.store;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4chee.archive.domain.Availability;
import org.dcm4chee.archive.domain.Instance;
import org.dcm4chee.archive.domain.Patient;
import org.dcm4chee.archive.domain.Series;
import org.dcm4chee.archive.domain.Study;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Stateless
public class InstanceStore {

    @PersistenceContext(unitName="dcm4chee-arc")
    private EntityManager em;

    public Instance store(Attributes attrs, Availability availability) {
        try {
            return em.createNamedQuery(
                Instance.FIND_BY_SOP_INSTANCE_UID, Instance.class)
                .setParameter(1, attrs.getString(Tag.SOPInstanceUID, null))
                .getSingleResult();
        } catch (NoResultException e) {
            Instance inst = new Instance();
            inst.setSeries(getSeries(attrs, availability));
            inst.setAvailability(availability);
            inst.setAttributes(attrs);
            em.persist(inst);
            return inst;
        }
    }

    private Series getSeries(Attributes attrs, Availability availability) {
        try {
            return em.createNamedQuery(
                Series.FIND_BY_SERIES_INSTANCE_UID, Series.class)
                .setParameter(1, attrs.getString(Tag.SeriesInstanceUID, null))
                .getSingleResult();
        } catch (NoResultException e) {
            Series series = new Series();
            series.setStudy(getStudy(attrs, availability));
            series.setAvailability(availability);
            series.setAttributes(attrs);
            em.persist(series);
            return series;
        }
    }

    private Study getStudy(Attributes attrs, Availability availability) {
        try {
            return em.createNamedQuery(
                Study.FIND_BY_STUDY_INSTANCE_UID, Study.class)
                .setParameter(1, attrs.getString(Tag.StudyInstanceUID, null))
                .getSingleResult();
        } catch (NoResultException e) {
            Study study = new Study();
            study.setPatient(getPatient(attrs));
            study.setAvailability(availability);
            study.setAttributes(attrs);
            em.persist(study);
            return study;
        }
    }

    private Patient getPatient(Attributes attrs) {
        Patient patient = new Patient();
        patient.setAttributes(attrs);
        em.persist(patient);
        return patient;
    }

    public void removePatient(long pk) {
        em.remove(em.getReference(Patient.class, pk));
    }
}
