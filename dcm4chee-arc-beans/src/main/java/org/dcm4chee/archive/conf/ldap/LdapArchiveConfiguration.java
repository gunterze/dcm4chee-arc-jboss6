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

package org.dcm4chee.archive.conf.ldap;

import java.util.Collection;
import java.util.Hashtable;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import org.dcm4che.conf.ldap.ExtendedLdapDicomConfiguration;
import org.dcm4che.data.ValueSelector;
import org.dcm4che.net.ApplicationEntity;
import org.dcm4che.net.Device;
import org.dcm4che.net.TransferCapability;
import org.dcm4che.soundex.FuzzyStr;
import org.dcm4che.util.AttributesFormat;
import org.dcm4che.util.TagUtils;
import org.dcm4chee.archive.ejb.store.Entity;
import org.dcm4chee.archive.ejb.store.StoreParam.StoreDuplicate;
import org.dcm4chee.archive.net.ArchiveApplicationEntity;
import org.dcm4chee.archive.net.ArchiveDevice;
import org.dcm4chee.archive.net.AttributeCoercion;
import org.dcm4chee.archive.net.AttributeCoercions;
import org.dcm4chee.archive.persistence.AttributeFilter;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class LdapArchiveConfiguration extends ExtendedLdapDicomConfiguration {

    public LdapArchiveConfiguration(Hashtable<String, Object> env,
            String baseDN) throws NamingException {
        super(env, baseDN);
    }

    @Override
    protected Attribute objectClassesOf(Device device, Attribute attr) {
        super.objectClassesOf(device, attr);
        if (device instanceof ArchiveDevice)
            attr.add("dcmArchiveDevice");
        return attr;
    }

    @Override
    protected Attribute objectClassesOf(ApplicationEntity ae, Attribute attr) {
        super.objectClassesOf(ae, attr);
        if (ae instanceof ArchiveApplicationEntity)
            attr.add("dcmArchiveNetworkAE");
        return attr;
    }

    @Override
    protected Device newDevice(Attributes attrs) throws NamingException {
        if (!hasObjectClass(attrs, "dcmArchiveDevice"))
            return super.newDevice(attrs);
        return new ArchiveDevice(stringValue(attrs.get("dicomDeviceName")));
    }

    @Override
    protected ApplicationEntity newApplicationEntity(Attributes attrs) throws NamingException {
        if (!hasObjectClass(attrs, "dcmArchiveNetworkAE"))
            return super.newApplicationEntity(attrs);
        return new ArchiveApplicationEntity(stringValue(attrs.get("dicomAETitle")));
    }

    @Override
    protected Attributes storeTo(Device device, Attributes attrs) {
        super.storeTo(device, attrs);
        if (!(device instanceof ArchiveDevice))
            return attrs;
        ArchiveDevice arcDev = (ArchiveDevice) device;
        storeNotNull(attrs, "dcmFileSystemGroupID", arcDev.getFileSystemGroupID());
        storeNotNull(attrs, "dcmReceivingDirectoryPath", arcDev.getReceivingDirectoryPath());
        storeNotNull(attrs, "dcmStorageFilePathFormat", arcDev.getStorageFilePathFormat());
        storeNotNull(attrs, "dcmDigestAlgorithm", arcDev.getDigestAlgorithm());
        storeNotNull(attrs, "dcmStoreDuplicate", arcDev.getStoreDuplicate());
        storeNotNull(attrs, "dcmExternalRetrieveAET", arcDev.getExternalRetrieveAET());
        storeNotEmpty(attrs, "dcmRetrieveAET", arcDev.getRetrieveAETs());
        storeBoolean(attrs, "dcmMatchUnknown", arcDev.isMatchUnknown());
        storeBoolean(attrs, "dcmSendPendingCGet", arcDev.isSendPendingCGet());
        storeInt(attrs, "dcmSendPendingCMoveInterval", arcDev.getSendPendingCMoveInterval());
        storeBoolean(attrs, "dcmSuppressWarningCoercionOfDataElements",
                arcDev.isSuppressWarningCoercionOfDataElements());
        storeBoolean(attrs, "dcmStoreOriginalAttributes",
                arcDev.isStoreOriginalAttributes());
        storeNotNull(attrs, "dcmModifyingSystem", arcDev.getModifyingSystem());
        storeNotNull(attrs, "dcmFuzzyAlgorithmClass",
                arcDev.getFuzzyStr().getClass().getName());
        return attrs;
    }

    @Override
    protected void storeChilds(String deviceDN, Device device) throws NamingException {
        super.storeChilds(deviceDN, device);
        if (!(device instanceof ArchiveDevice))
            return;
        ArchiveDevice arcDev = (ArchiveDevice) device;
        for (AttributeCoercion ac : arcDev.getAttributeCoercions().getAll())
            createSubcontext(dnOf(ac, deviceDN), storeTo(ac, new BasicAttributes(true)));
        for (Entity entity : Entity.values())
            createSubcontext(dnOf("dcmEntity", entity.toString(), deviceDN),
                    storeTo(arcDev.getAttributeFilter(entity), entity, new BasicAttributes(true)));
    }

    @Override
    protected void storeChilds(String aeDN, ApplicationEntity ae) throws NamingException {
        super.storeChilds(aeDN, ae);
        if (!(ae instanceof ArchiveApplicationEntity))
            return;
        ArchiveApplicationEntity arcAE = (ArchiveApplicationEntity) ae;
        for (AttributeCoercion ac : arcAE.getAttributeCoercions().getAll())
            createSubcontext(dnOf(ac, aeDN), storeTo(ac, new BasicAttributes(true)));
    }

    private static String dnOf(AttributeCoercion ac, String parentDN) {
        StringBuilder sb = new StringBuilder();
        sb.append("dcmDIMSE=").append(ac.getDimse());
        sb.append("+dicomTransferRole=").append(ac.getRole());
        if (ac.getAETitle() != null)
            sb.append("+dicomAETitle=").append(ac.getAETitle());
        if (ac.getSopClass() != null)
            sb.append("+dicomSOPClass=").append(ac.getSopClass());
        sb.append(',').append(parentDN);
       return sb.toString();
    }

    private static Attributes storeTo(AttributeCoercion ac, BasicAttributes attrs) {
        attrs.put("objectclass", "dcmAttributeCoercion");
        storeNotNull(attrs, "dcmDIMSE", ac.getDimse());
        storeNotNull(attrs, "dicomTransferRole", ac.getRole());
        storeNotNull(attrs, "dicomAETitle", ac.getAETitle());
        storeNotNull(attrs, "dicomSOPClass", ac.getSopClass());
        storeNotNull(attrs, "labeledURI", ac.getURI());
        return attrs;
    }

    private static Attributes storeTo(AttributeFilter filter, Entity entity, BasicAttributes attrs) {
        attrs.put("objectclass", "dcmAttributeFilter");
        attrs.put("dcmEntity", entity.toString());
        storeTags(attrs, "dcmTag", filter.getSelection());
        storeNotNull(attrs, "dcmCustomAttribute1", filter.getCustomAttribute1());
        storeNotNull(attrs, "dcmCustomAttribute2", filter.getCustomAttribute2());
        storeNotNull(attrs, "dcmCustomAttribute3", filter.getCustomAttribute3());
        return attrs;
    }

    private static void storeTags(BasicAttributes attrs, String attrID, int[] tags) {
        Attribute attr = new BasicAttribute(attrID);
        for (int tag : tags)
            attr.add(TagUtils.toHexString(tag));
        attrs.put(attr);
    }

    @Override
    protected Attributes storeTo(ApplicationEntity ae, String deviceDN, Attributes attrs) {
        super.storeTo(ae, deviceDN, attrs);
        if (!(ae instanceof ArchiveApplicationEntity))
            return attrs;
        ArchiveApplicationEntity arcAE = (ArchiveApplicationEntity) ae;
        storeNotNull(attrs, "dcmFileSystemGroupID", arcAE.getFileSystemGroupID());
        storeNotNull(attrs, "dcmReceivingDirectoryPath", arcAE.getReceivingDirectoryPath());
        storeNotNull(attrs, "dcmStorageFilePathFormat", arcAE.getStorageFilePathFormat());
        storeNotNull(attrs, "dcmDigestAlgorithm", arcAE.getDigestAlgorithm());
        storeNotNull(attrs, "dcmStoreDuplicate", arcAE.getStoreDuplicate());
        storeNotNull(attrs, "dcmExternalRetrieveAET", arcAE.getExternalRetrieveAET());
        storeNotEmpty(attrs, "dcmRetrieveAET", arcAE.getRetrieveAETs());
        storeNotNull(attrs, "dcmMatchUnknown", arcAE.getMatchUnknown());
        storeNotNull(attrs, "dcmSendPendingCGet", arcAE.getSendPendingCGet());
        storeNotNull(attrs, "dcmSendPendingCMoveInterval",
                arcAE.getSendPendingCMoveIntervalOrNull());
        storeNotNull(attrs, "dcmSuppressWarningCoercionOfDataElements",
                arcAE.getSuppressWarningCoercionOfDataElements());
        storeNotNull(attrs, "dcmStoreOriginalAttributes",
                arcAE.getStoreOriginalAttributes());
        storeNotNull(attrs, "dcmModifyingSystem", arcAE.getModifyingSystem());
        return attrs;
    }

    @Override
    protected void loadFrom(Device device, Attributes attrs) throws NamingException {
        super.loadFrom(device, attrs);
        if (!(device instanceof ArchiveDevice))
            return;
        ArchiveDevice arcdev = (ArchiveDevice) device;
        arcdev.setFileSystemGroupID(stringValue(attrs.get("dcmFileSystemGroupID")));
        arcdev.setReceivingDirectoryPath(stringValue(attrs.get("dcmReceivingDirectoryPath")));
        arcdev.setStorageFilePathFormat(attributesFormat(attrs.get("dcmStorageFilePathFormat")));
        arcdev.setDigestAlgorithm(stringValue(attrs.get("dcmDigestAlgorithm")));
        arcdev.setStoreDuplicate(storeDuplicate(attrs.get("dcmStoreDuplicate")));
        arcdev.setExternalRetrieveAET(stringValue(attrs.get("dcmExternalRetrieveAET")));
        arcdev.setRetrieveAETs(stringArray(attrs.get("dcmRetrieveAET")));
        arcdev.setMatchUnknown(booleanValue(attrs.get("dcmMatchUnknown"), Boolean.TRUE));
        arcdev.setSendPendingCGet(booleanValue(attrs.get("dcmSendPendingCGet"), Boolean.FALSE));
        arcdev.setSendPendingCMoveInterval(intValue(attrs.get("dcmSendPendingCMoveInterval"), 0));
        arcdev.setSuppressWarningCoercionOfDataElements(
                booleanValue(attrs.get("dcmSuppressWarningCoercionOfDataElements"), Boolean.FALSE));
        arcdev.setStoreOriginalAttributes(
                booleanValue(attrs.get("dcmStoreOriginalAttributes"), Boolean.FALSE));
        arcdev.setModifyingSystem(stringValue(attrs.get("dcmModifyingSystem")));
        arcdev.setFuzzyStr(fuzzyStr(attrs.get("dcmFuzzyAlgorithmClass")));
    }

    @Override
    protected void loadChilds(Device device, String deviceDN) throws NamingException {
        super.loadChilds(device, deviceDN);
        if (!(device instanceof ArchiveDevice))
            return;
        ArchiveDevice arcdev = (ArchiveDevice) device;
        load(arcdev.getAttributeCoercions(), deviceDN);
        loadAttributeFilters(arcdev, deviceDN);
        
    }

    private void load(AttributeCoercions acs, String dn)
            throws NamingException {
        NamingEnumeration<SearchResult> ne = search(dn, "(objectclass=dcmAttributeCoercion)");
        try {
            while (ne.hasMore()) {
                SearchResult sr = ne.next();
                Attributes attrs = sr.getAttributes();
                acs.add(new AttributeCoercion(
                        stringValue(attrs.get("dicomSOPClass")),
                        AttributeCoercion.DIMSE.valueOf(
                                stringValue(attrs.get("dcmDIMSE"))),
                        TransferCapability.Role.valueOf(
                                stringValue(attrs.get("dicomTransferRole"))),
                        stringValue(attrs.get("dicomAETitle")),
                        stringValue(attrs.get("labeledURI"))));
            }
        } finally {
           safeClose(ne);
        }
    }

    private void loadAttributeFilters(ArchiveDevice device, String deviceDN)
            throws NamingException {
        NamingEnumeration<SearchResult> ne = 
                search(deviceDN, "(objectclass=dcmAttributeFilter)");
        try {
            while (ne.hasMore()) {
                SearchResult sr = ne.next();
                Attributes attrs = sr.getAttributes();
                AttributeFilter filter = new AttributeFilter(tags(attrs.get("dcmTag")));
                filter.setCustomAttribute1(valueSelector(attrs.get("dcmCustomAttribute1")));
                filter.setCustomAttribute2(valueSelector(attrs.get("dcmCustomAttribute2")));
                filter.setCustomAttribute3(valueSelector(attrs.get("dcmCustomAttribute3")));
                device.setAttributeFilter(
                        Entity.valueOf(stringValue(attrs.get("dcmEntity"))), filter);
            }
        } finally {
           safeClose(ne);
        }
    }

    private ValueSelector valueSelector(Attribute attr) throws NamingException {
        return attr != null ? ValueSelector.valueOf((String) attr.get()) : null;
   }

    private FuzzyStr fuzzyStr(Attribute attr) throws NamingException {
        try {
            return (FuzzyStr) Class.forName((String) attr.get()).newInstance();
        } catch (NamingException ne) {
            throw ne;
        } catch (Exception e) {
            return null;
        }
    }

    private StoreDuplicate storeDuplicate(Attribute attr) throws NamingException {
        return attr != null ? StoreDuplicate.valueOf((String) attr.get()) : null;
    }

    private AttributesFormat attributesFormat(Attribute attr) throws NamingException {
        return attr != null ? new AttributesFormat((String) attr.get()) : null;
    }

    protected static int[] tags(Attribute attr) throws NamingException {
        int[] is = new int[attr.size()];
        for (int i = 0; i < is.length; i++)
            is[i] = Integer.parseInt((String) attr.get(i), 16);

        return is;
    }


    @Override
    protected void loadFrom(ApplicationEntity ae, Attributes attrs) throws NamingException {
       super.loadFrom(ae, attrs);
       if (!(ae instanceof ArchiveApplicationEntity))
           return;
       ArchiveApplicationEntity arcse = (ArchiveApplicationEntity) ae;
       arcse.setFileSystemGroupID(stringValue(attrs.get("dcmFileSystemGroupID")));
       arcse.setReceivingDirectoryPath(stringValue(attrs.get("dcmReceivingDirectoryPath")));
       arcse.setStorageFilePathFormat(attributesFormat(attrs.get("dcmStorageFilePathFormat")));
       arcse.setDigestAlgorithm(stringValue(attrs.get("dcmDigestAlgorithm")));
       arcse.setStoreDuplicate(storeDuplicate(attrs.get("dcmStoreDuplicate")));
       arcse.setExternalRetrieveAET(stringValue(attrs.get("dcmExternalRetrieveAET")));
       arcse.setRetrieveAETs(stringArray(attrs.get("dcmRetrieveAET")));
       arcse.setMatchUnknown(booleanValue(attrs.get("dcmMatchUnknown"), Boolean.TRUE));
       arcse.setSendPendingCGet(booleanValue(attrs.get("dcmSendPendingCGet"), Boolean.FALSE));
       arcse.setSendPendingCMoveInterval(intValue(attrs.get("dcmSendPendingCMoveInterval"), 0));
       arcse.setSuppressWarningCoercionOfDataElements(
               booleanValue(attrs.get("dcmSuppressWarningCoercionOfDataElements"), Boolean.FALSE));
       arcse.setStoreOriginalAttributes(
               booleanValue(attrs.get("dcmStoreOriginalAttributes"), Boolean.FALSE));
       arcse.setModifyingSystem(stringValue(attrs.get("dcmModifyingSystem")));
    }

    @Override
    protected void loadChilds(ApplicationEntity ae, String aeDN) throws NamingException {
        super.loadChilds(ae, aeDN);
        if (!(ae instanceof ArchiveApplicationEntity))
            return;
        ArchiveApplicationEntity arcae = (ArchiveApplicationEntity) ae;
        load(arcae.getAttributeCoercions(), aeDN);
    }

    @Override
    protected void storeDiffs(Collection<ModificationItem> mods,
            ApplicationEntity a, ApplicationEntity b, String deviceDN) {
        // TODO Auto-generated method stub
        super.storeDiffs(mods, a, b, deviceDN);
    }

    @Override
    protected void storeDiffs(Collection<ModificationItem> mods, Device a, Device b) {
        // TODO Auto-generated method stub
        super.storeDiffs(mods, a, b);
    }

}
