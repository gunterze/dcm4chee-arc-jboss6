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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@Entity
@Table(name = "issuer")
public class Issuer implements Serializable {

    private static final long serialVersionUID = -5050458184841995777L;

    @Id
    @GeneratedValue
    @Column(name = "pk")
    private long pk;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "entity_uid")
    private String entityUid;

    @Column(name = "entity_uid_type")
    private String entityUidType;

    public long getPk() {
        return pk;
    }

    public String getLocalNamespaceEntityID() {
        return entityId;
    }

    public void setLocalNamespaceEntityID(String entityId) {
        this.entityId = entityId;
    }

    public String getUniversalEntityID() {
        return entityUid;
    }

    public void setUniversalEntityID(String entityUid) {
        this.entityUid = entityUid;
    }

    public String getUniversalEntityIDType() {
        return entityUidType;
    }

    public void setUniversalEntityIDType(String entityUidType) {
        this.entityUidType = entityUidType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append("Issuer[id=");
        if (entityId != null)
            sb.append(entityId);
        sb.append(", uid=");
        if (entityUid != null)
            sb.append(entityUid);
        sb.append(", type=");
        if (entityUidType != null)
            sb.append(entityUidType);
        sb.append("]");
        return sb.toString();
    }

    public Attributes toItem() {
        int size = 0;
        if (entityId != null)
            size++;
        if (entityUid != null)
            size++;
        if (entityUidType != null)
            size++;

        Attributes item = new Attributes(size);
        if (entityId != null)
            item.setString(Tag.LocalNamespaceEntityID, VR.UT, entityId);
        if (entityUid != null)
            item.setString(Tag.UniversalEntityID, VR.UT, entityUid);
        if (entityUidType != null)
            item.setString(Tag.UniversalEntityIDType, VR.UT, entityUid);
        return item ;
    }

    public static Issuer valueOf(Attributes item) {
        Issuer issuer = new Issuer();
        issuer.entityId = item.getString(Tag.LocalNamespaceEntityID, null);
        issuer.entityUid = item.getString(Tag.UniversalEntityID, null);
        issuer.entityUidType = item.getString(Tag.UniversalEntityIDType, null);
        return issuer;
    }
}
