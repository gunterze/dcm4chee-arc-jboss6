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
 * Portions created by the Initial Developer are Copyright (C) 2012
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

package org.dcm4chee.archive.net.service;

import java.net.Socket;

import javax.ejb.EJB;

import org.dcm4che.data.Attributes;
import org.dcm4che.hl7.HL7Exception;
import org.dcm4che.hl7.HL7Segment;
import org.dcm4che.net.Connection;
import org.dcm4che.net.hl7.HL7Application;
import org.dcm4che.net.hl7.service.HL7Service;
import org.dcm4chee.archive.ejb.store.PatientUpdate;
import org.dcm4chee.archive.ejb.store.StudyUpdate;
import org.dcm4chee.archive.net.ArchiveHL7Application;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class ProcedureUpdateService extends HL7Service {

    public ProcedureUpdateService(String... messageTypes) {
        super(messageTypes);
    }

    @EJB
    private PatientUpdate patientUpdate;

    @EJB
    private StudyUpdate studyUpdate;

    @Override
    public byte[] onMessage(HL7Application hl7App, Connection conn,
            Socket s, HL7Segment msh, byte[] msg, int off, int len, int mshlen) throws HL7Exception {
        try {
            ArchiveHL7Application arcHL7App = (ArchiveHL7Application) hl7App;
            String hl7cs = msh.getField(17, arcHL7App.getHL7DefaultCharacterSet());
            Attributes attrs = HL7toDicom.transform(
                    arcHL7App.getTemplates("orm2dcm"), msg, off, len, hl7cs);
            patientUpdate.updatePatient(attrs, arcHL7App.getStoreParam());
            studyUpdate.updateStudy(attrs, arcHL7App.getStoreParam());
           return super.onMessage(hl7App, conn, s, msh, msg, off, len, mshlen);
        } catch (Exception e) {
            throw new HL7Exception(HL7Exception.AE, e);
        }
    }

}
