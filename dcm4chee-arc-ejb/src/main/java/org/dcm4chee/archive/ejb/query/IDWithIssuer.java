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

package org.dcm4chee.archive.ejb.query;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;
import org.dcm4che.util.StringUtils;
import org.dcm4chee.archive.persistence.Issuer;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class IDWithIssuer {

    public static final IDWithIssuer[] EMPTY = {};

    public final String id;
    public final Issuer issuer;

    public IDWithIssuer(String id, Issuer issuer) {
        this.id = id;
        this.issuer = issuer;
    }

    @Override
    public String toString() {
        return "IDWithIssuer[id=" + id + ", issuer=" + issuer + "]";
    }

    public String toHL7CX() {
        return id + "^^^" + issuer.toHL7HD('&');
    }

    public Attributes toPIDWithIssuer(Attributes attrs) {
        if (attrs == null)
            attrs = new Attributes(3);

        attrs.setString(Tag.PatientID, VR.LO, id);
        return issuer.toIssuerOfPatientID(attrs);
    }

    public static IDWithIssuer fromHL7CX(String cx) {
        String[] ss = StringUtils.split(cx, '^');
        Issuer issuer = null;
        if (ss.length > 3) {
            String[] ss3 = StringUtils.split(ss[3], '&');
            issuer = new Issuer();
            issuer.setLocalNamespaceEntityID(ss3[0]);
            if (ss3.length > 2) {
                issuer.setUniversalEntityID(ss3[1]);
                issuer.setUniversalEntityIDType(ss3[2]);
            }
        }
        return new IDWithIssuer(ss[0], issuer);
    }

    public static IDWithIssuer pidWithIssuer(Attributes keys,
            Issuer defaultIssuerWithPatientID) {
        String id = keys.getString(Tag.PatientID);
        if (id == null)
            return null;

        String entityID = keys.getString(Tag.IssuerOfPatientID);
        String entityUID = null;
        String entityUIDType = null;
        Attributes issuerItem = keys.getNestedDataset(Tag.IssuerOfPatientIDQualifiersSequence);
        if (issuerItem != null) {
            entityUID = issuerItem.getString(Tag.UniversalEntityID);
            entityUIDType = issuerItem.getString(Tag.UniversalEntityIDType);
        }
        Issuer issuer = entityID == null
                     && entityUID == null
                     && entityUIDType == null
                     ? defaultIssuerWithPatientID
                     : new Issuer(entityID, entityUID, entityUIDType);
        return new IDWithIssuer(id, issuer);
    }
}
