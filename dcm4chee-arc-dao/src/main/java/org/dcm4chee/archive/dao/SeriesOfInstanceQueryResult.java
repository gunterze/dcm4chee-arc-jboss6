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

package org.dcm4chee.archive.dao;

import java.io.IOException;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;
import org.dcm4chee.archive.domain.Utils;

class SeriesOfInstanceQueryResult {

    private final int numberOfStudyRelatedSeries;
    private final int numberOfStudyRelatedInstances;
    private final int numberOfSeriesRelatedInstances;
    private final String modalitiesInStudy;
    private final String sopClassesInStudy;
    private final byte[] seriesAttributes;
    private final byte[] studyAttributes;
    private final byte[] patientAttributes;

    public SeriesOfInstanceQueryResult(int numberOfStudyRelatedSeries,
            int numberOfStudyRelatedInstances,
            int numberOfSeriesRelatedInstances,
            String modalitiesInStudy,
            String sopClassesInStudy,
            byte[] seriesAttributes,
            byte[] studyAttributes,
            byte[] patientAttributes) {
        this.numberOfStudyRelatedSeries = numberOfStudyRelatedSeries;
        this.numberOfStudyRelatedInstances = numberOfStudyRelatedInstances;
        this.numberOfSeriesRelatedInstances = numberOfSeriesRelatedInstances;
        this.modalitiesInStudy = modalitiesInStudy;
        this.sopClassesInStudy = sopClassesInStudy;
        this.seriesAttributes = seriesAttributes;
        this.studyAttributes = studyAttributes;
        this.patientAttributes = patientAttributes;
    }

    public Attributes mergeAttributes() throws IOException {
        Attributes attrs = new Attributes();
        Utils.decodeAttributes(patientAttributes, attrs);
        Utils.decodeAttributes(studyAttributes, attrs);
        Utils.decodeAttributes(seriesAttributes, attrs);
        attrs.setInt(Tag.NumberOfStudyRelatedSeries, VR.US,
                numberOfStudyRelatedSeries);
        attrs.setInt(Tag.NumberOfStudyRelatedInstances, VR.US,
                numberOfStudyRelatedInstances);
        attrs.setInt(Tag.NumberOfSeriesRelatedInstances, VR.US,
                numberOfSeriesRelatedInstances);
        attrs.setString(Tag.ModalitiesInStudy, VR.CS,
                modalitiesInStudy);
        attrs.setString(Tag.SOPClassesInStudy, VR.CS,
                sopClassesInStudy);
        return attrs ;
    }

}