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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;

/**
 * @author Damien Evans <damien.daddy@gmail.com>
 * @author Justin Falk <jfalkmu@gmail.com>
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@NamedQueries({
@NamedQuery(
    name="Issuer.findByEntityID",
    query="SELECT i FROM Issuer i WHERE i.entityID = ?1"),
@NamedQuery(
    name="Issuer.findByEntityUID",
    query="SELECT i FROM Issuer i " +
          "WHERE i.entityUID = ?1 AND i.entityUIDType = ?2"),
@NamedQuery(
    name="Issuer.findByEntityIDorUID",
    query="SELECT i FROM Issuer i WHERE i.entityID = ?1 " +
          "OR (i.entityUID = ?2 AND i.entityUIDType = ?3)")
})
@Entity
@Table(name = "issuer")
public class Issuer implements Serializable {

    private static final long serialVersionUID = -5050458184841995777L;

    public static final String FIND_BY_ENTITY_ID = "Issuer.findByEntityID";

    public static final String FIND_BY_ENTITY_UID = "Issuer.findByEntityUID";

    public static final String FIND_BY_ENTITY_ID_OR_UID =
        "Issuer.findByEntityIDorUID";

    @Id
    @GeneratedValue
    @Column(name = "pk")
    private long pk;

    @Column(name = "entity_id")
    private String entityID;

    @Column(name = "entity_uid")
    private String entityUID;

    @Column(name = "entity_uid_type")
    private String entityUIDType;

    public Issuer() {}

    public Issuer(String entityID, String entityUID, String entityUIDType) {
        this.entityID = entityID;
        this.entityUID = entityUID;
        this.entityUIDType = entityUIDType;
    }

    public long getPk() {
        return pk;
    }

    public String getLocalNamespaceEntityID() {
        return entityID;
    }

    public void setLocalNamespaceEntityID(String entityId) {
        this.entityID = entityId;
    }

    public String getUniversalEntityID() {
        return entityUID;
    }

    public void setUniversalEntityID(String entityUid) {
        this.entityUID = entityUid;
    }

    public String getUniversalEntityIDType() {
        return entityUIDType;
    }

    public void setUniversalEntityIDType(String entityUidType) {
        this.entityUIDType = entityUidType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append("Issuer[pk=").append(pk);
        sb.append(", id=");
        if (entityID != null)
            sb.append(entityID);
        sb.append(", uid=");
        if (entityUID != null)
            sb.append(entityUID);
        sb.append(", type=");
        if (entityUIDType != null)
            sb.append(entityUIDType);
        sb.append("]");
        return sb.toString();
    }

    public Attributes toItem() {
        int size = 0;
        if (entityID != null)
            size++;
        if (entityUID != null)
            size++;
        if (entityUIDType != null)
            size++;

        Attributes item = new Attributes(size);
        if (entityID != null)
            item.setString(Tag.LocalNamespaceEntityID, VR.UT, entityID);
        if (entityUID != null)
            item.setString(Tag.UniversalEntityID, VR.UT, entityUID);
        if (entityUIDType != null)
            item.setString(Tag.UniversalEntityIDType, VR.UT, entityUIDType);
        return item ;
    }

    public Attributes toIssuerOfPatientID(Attributes attrs) {
        if (attrs == null)
            attrs = new Attributes(2);
        if (entityID != null)
            attrs.setString(Tag.IssuerOfPatientID, VR.LO, entityID);
        if (entityUID != null) {
            Attributes item = new Attributes(2);
            item.setString(Tag.UniversalEntityID, VR.UT, entityUID);
            item.setString(Tag.UniversalEntityIDType, VR.UT, entityUIDType);
            attrs.newSequence(Tag.IssuerOfPatientIDQualifiersSequence, 1).add(item);
        }
        return attrs;
    }

    public static Issuer valueOf(Attributes item) {
        if (item == null || item.isEmpty())
            return null;

        Issuer issuer = new Issuer();
        issuer.entityID = item.getString(Tag.LocalNamespaceEntityID, null);
        issuer.entityUID = item.getString(Tag.UniversalEntityID, null);
        issuer.entityUIDType = item.getString(Tag.UniversalEntityIDType, null);
        return issuer;
    }

    public static Issuer issuerOfPatientIDOf(Attributes attrs) {
        String entityID = attrs.getString(Tag.IssuerOfPatientID);
        Attributes item = attrs.getNestedDataset(Tag.IssuerOfPatientIDQualifiersSequence);
        if (entityID == null && item == null)
            return null;

        Issuer issuer = new Issuer();
        issuer.entityID = entityID;
        if (item != null) {
            issuer.entityUID = item.getString(Tag.UniversalEntityID, null);
            issuer.entityUIDType = item.getString(Tag.UniversalEntityIDType, null);
        }
        return issuer;
    }

    public String toHL7HD(char delim) {
        return entityUID == null ? maskNull(entityID, "")
                : (maskNull(entityID, "")
                        + delim + maskNull(entityUID, "")
                        + delim + maskNull(entityUIDType, ""));
    }
 
    private String maskNull(String s, String mask) {
        return s == null ? mask : s;
    }

    public boolean matches(Issuer other) {
        if (this == other)
            return true;

        int equalsID, equalsUID, equalsUIDType;
        if ((equalsID = equals(entityID, other.entityID)) < 0
                || (equalsUID = equals(entityUID, other.entityUID)) < 0
                || (equalsUIDType = equals(entityUIDType, other.entityUIDType)) < 0)
            return false;
        return equalsID > 0 || equalsUID > 0 && equalsUIDType > 0;
    }

    private int equals(String s1, String s2) {
        return s1 == null || s2 == null ? 0 : s1.equals(s2) ? 1 : -1;
    }
}
