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

package org.dcm4chee.archive.conf.ldap;

import org.dcm4che.conf.ldap.LdapEnv;
import org.dcm4che.data.UID;
import org.dcm4che.net.ApplicationEntity;
import org.dcm4che.net.Connection;
import org.dcm4che.net.Device;
import org.dcm4chee.archive.net.ArchiveDevice;
import org.dcm4chee.archive.net.ArchiveHL7Application;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class ArchiveConfigurationIHEE2012 {

    private static final String[] IMAGE_TSUIDS = {
        UID.ImplicitVRLittleEndian,
        UID.ExplicitVRLittleEndian,
        UID.JPEGBaseline1,
    };
    private static final String[] VIDEO_TSUIDS = {
        UID.JPEGBaseline1,
        UID.MPEG2,
        UID.MPEG2MainProfileHighLevel,
        UID.MPEG4AVCH264BDCompatibleHighProfileLevel41,
        UID.MPEG4AVCH264HighProfileLevel41
    };
    private static final String[] OTHER_TSUIDS = {
        UID.ImplicitVRLittleEndian,
        UID.ExplicitVRLittleEndian,
    };

    public static void main(String[] args) throws Exception {
        String deviceName = args[0];
        String hostname = args[1];
        String aet = deviceName;
        String aetqr = aet + "_QR";
        String hl7appname = aet + "^AGFA";
        LdapEnv env = new LdapEnv();
        env.setUrl("ldap://localhost:389");
        env.setUserDN("cn=admin,dc=nodomain");
        env.setPassword("admin");
        LdapArchiveConfiguration config = new LdapArchiveConfiguration(env, "dc=nodomain");
        try {
            config.registerAETitle(aet);
            config.registerAETitle(aetqr);
            config.persist(createDevice(deviceName, hostname, aet, aetqr, hl7appname));
        } finally {
            config.close();
        }
    }

    private static Device createDevice(String deviceName, String hostname,
            String aet, String aetqr, String hl7appname) {
        Connection dicom = new Connection("dicom", hostname, 11112);
        dicom.setMaxOpsInvoked(0);
        dicom.setMaxOpsPerformed(0);
        Connection hl7 = new Connection("hl7", hostname, 2575);
        ArchiveDevice device = ArchiveConfigurationTestUtils.createArchiveDevice(deviceName);
        device.addConnection(dicom);
        device.addConnection(hl7);
        ApplicationEntity ae = ArchiveConfigurationTestUtils.createAE(aet,
                IMAGE_TSUIDS, VIDEO_TSUIDS, OTHER_TSUIDS, hl7appname, null);
        ae.addConnection(dicom);
        device.addApplicationEntity(ae);
        ApplicationEntity adminAE = ArchiveConfigurationTestUtils.createAdminAE(aetqr,
                IMAGE_TSUIDS, VIDEO_TSUIDS, OTHER_TSUIDS, hl7appname, null);
        adminAE.addConnection(dicom);
        device.addApplicationEntity(adminAE);
        ArchiveHL7Application hl7App = ArchiveConfigurationTestUtils.createHL7Application(hl7appname);
        hl7App.addConnection(hl7);
        device.addHL7Application(hl7App);
        return device;
    }
 
}
