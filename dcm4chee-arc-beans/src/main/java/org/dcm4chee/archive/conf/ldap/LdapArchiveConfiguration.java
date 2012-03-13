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

import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import org.dcm4che.conf.ldap.hl7.LdapHL7Configuration;
import org.dcm4che.data.ValueSelector;
import org.dcm4che.net.ApplicationEntity;
import org.dcm4che.net.Device;
import org.dcm4che.net.hl7.HL7Application;
import org.dcm4che.soundex.FuzzyStr;
import org.dcm4che.util.AttributesFormat;
import org.dcm4che.util.TagUtils;
import org.dcm4chee.archive.ejb.store.Entity;
import org.dcm4chee.archive.ejb.store.StoreParam.StoreDuplicate;
import org.dcm4chee.archive.net.ArchiveApplicationEntity;
import org.dcm4chee.archive.net.ArchiveDevice;
import org.dcm4chee.archive.net.ArchiveHL7Application;
import org.dcm4chee.archive.persistence.AttributeFilter;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class LdapArchiveConfiguration extends LdapHL7Configuration {

    public LdapArchiveConfiguration(Hashtable<String, Object> env, String baseDN)
            throws NamingException {
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
    protected Attribute objectClassesOf(HL7Application app, Attribute attr) {
        super.objectClassesOf(app, attr);
        if (app instanceof ArchiveHL7Application)
            attr.add("dcmArchiveHL7Application");
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
    protected HL7Application newHL7Application(Attributes attrs) throws NamingException {
        if (!hasObjectClass(attrs, "dcmArchiveHL7Application"))
            return super.newHL7Application(attrs);
        return new ArchiveHL7Application(stringValue(attrs.get("hl7ApplicationName")));
    }

    @Override
    protected Attributes storeTo(Device device, Attributes attrs) {
        super.storeTo(device, attrs);
        if (!(device instanceof ArchiveDevice))
            return attrs;
        ArchiveDevice arcDev = (ArchiveDevice) device;
        storeNotNull(attrs, "dcmFuzzyAlgorithmClass",
                arcDev.getFuzzyAlgorithmClass());
        return attrs;
    }

    @Override
    protected void storeChilds(String deviceDN, Device device) throws NamingException {
        super.storeChilds(deviceDN, device);
        if (!(device instanceof ArchiveDevice))
            return;
        ArchiveDevice arcDev = (ArchiveDevice) device;
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
        store(arcAE.getAttributeCoercions(), aeDN);
    }

    private static Attributes storeTo(AttributeFilter filter, Entity entity, BasicAttributes attrs) {
        attrs.put("objectclass", "dcmAttributeFilter");
        attrs.put("dcmEntity", entity.name());
        attrs.put(tagsAttr("dcmTag", filter.getSelection()));
        storeNotNull(attrs, "dcmCustomAttribute1", filter.getCustomAttribute1());
        storeNotNull(attrs, "dcmCustomAttribute2", filter.getCustomAttribute2());
        storeNotNull(attrs, "dcmCustomAttribute3", filter.getCustomAttribute3());
        return attrs;
    }

    private static Attribute tagsAttr(String attrID, int[] tags) {
        Attribute attr = new BasicAttribute(attrID);
        for (int tag : tags)
            attr.add(TagUtils.toHexString(tag));
        return attr;
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
        storeNotDef(attrs, "dcmMatchUnknown", arcAE.isMatchUnknown(), false);
        storeNotDef(attrs, "dcmSendPendingCGet", arcAE.isSendPendingCGet(), false);
        storeNotDef(attrs, "dcmSendPendingCMoveInterval", arcAE.getSendPendingCMoveInterval(), 0);
        storeNotDef(attrs, "dcmSuppressWarningCoercionOfDataElements",
                arcAE.isSuppressWarningCoercionOfDataElements(), false);
        storeNotDef(attrs, "dcmStoreOriginalAttributes",
                arcAE.isStoreOriginalAttributes(), false);
        storeNotNull(attrs, "dcmModifyingSystem", arcAE.getModifyingSystem());
        storeNotDef(attrs, "dcmStgCmtDelay", arcAE.getStorageCommitmentDelay(), 0);
        storeNotDef(attrs, "dcmStgCmtMaxRetries", arcAE.getStorageCommitmentMaxRetries(), 0);
        storeNotDef(attrs, "dcmStgCmtRetryInterval", arcAE.getStorageCommitmentRetryInterval(),
                    ArchiveApplicationEntity.DEF_RETRY_INTERVAL);
        storeNotEmpty(attrs, "dcmFwdMppsDestination", arcAE.getForwardMPPSDestinations());
        storeNotDef(attrs, "dcmFwdMppsMaxRetries", arcAE.getForwardMPPSMaxRetries(), 0);
        storeNotDef(attrs, "dcmFwdMppsRetryInterval", arcAE.getForwardMPPSRetryInterval(),
                    ArchiveApplicationEntity.DEF_RETRY_INTERVAL);
        storeNotEmpty(attrs, "dcmIanDestination", arcAE.getIANDestinations());
        storeNotDef(attrs, "dcmIanMaxRetries", arcAE.getIANMaxRetries(), 0);
        storeNotDef(attrs, "dcmIanRetryInterval", arcAE.getIANRetryInterval(),
                    ArchiveApplicationEntity.DEF_RETRY_INTERVAL);
        return attrs;
    }

    @Override
    protected Attributes storeTo(HL7Application hl7App, String deviceDN, Attributes attrs) {
        super.storeTo(hl7App, deviceDN, attrs);
        if (!(hl7App instanceof ArchiveHL7Application))
            return attrs;

        ArchiveHL7Application arcHL7App = (ArchiveHL7Application) hl7App;
        storeNotNull(attrs, "hl7DefaultCharacterSet", arcHL7App.getHL7DefaultCharacterSet());
        storeNotEmpty(attrs, "labeledURI", arcHL7App.getTemplatesURIs());
        return attrs;
    }
 
    @Override
    protected void loadFrom(Device device, Attributes attrs)
            throws NamingException, CertificateException {
        super.loadFrom(device, attrs);
        if (!(device instanceof ArchiveDevice))
            return;
        ArchiveDevice arcdev = (ArchiveDevice) device;
        arcdev.setFuzzyStr(fuzzyStr(attrs.get("dcmFuzzyAlgorithmClass")));
    }

    @Override
    protected void loadChilds(Device device, String deviceDN) throws NamingException {
        super.loadChilds(device, deviceDN);
        if (!(device instanceof ArchiveDevice))
            return;
        ArchiveDevice arcdev = (ArchiveDevice) device;
        loadAttributeFilters(arcdev, deviceDN);
        
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

    private static ValueSelector valueSelector(Attribute attr) throws NamingException {
        return attr != null ? ValueSelector.valueOf((String) attr.get()) : null;
   }

    private static FuzzyStr fuzzyStr(Attribute attr) throws NamingException {
        try {
            return (FuzzyStr) Class.forName((String) attr.get()).newInstance();
        } catch (NamingException ne) {
            throw ne;
        } catch (Exception e) {
            return null;
        }
    }

    private static StoreDuplicate storeDuplicate(Attribute attr) throws NamingException {
        return attr != null ? StoreDuplicate.valueOf((String) attr.get()) : null;
    }

    private  static AttributesFormat attributesFormat(Attribute attr) throws NamingException {
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
       ArchiveApplicationEntity arcae = (ArchiveApplicationEntity) ae;
       arcae.setFileSystemGroupID(stringValue(attrs.get("dcmFileSystemGroupID")));
       arcae.setReceivingDirectoryPath(stringValue(attrs.get("dcmReceivingDirectoryPath")));
       arcae.setStorageFilePathFormat(attributesFormat(attrs.get("dcmStorageFilePathFormat")));
       arcae.setDigestAlgorithm(stringValue(attrs.get("dcmDigestAlgorithm")));
       arcae.setStoreDuplicate(storeDuplicate(attrs.get("dcmStoreDuplicate")));
       arcae.setExternalRetrieveAET(stringValue(attrs.get("dcmExternalRetrieveAET")));
       arcae.setRetrieveAETs(stringArray(attrs.get("dcmRetrieveAET")));
       arcae.setMatchUnknown(booleanValue(attrs.get("dcmMatchUnknown"), false));
       arcae.setSendPendingCGet(booleanValue(attrs.get("dcmSendPendingCGet"), false));
       arcae.setSendPendingCMoveInterval(intValue(attrs.get("dcmSendPendingCMoveInterval"), 0));
       arcae.setSuppressWarningCoercionOfDataElements(
               booleanValue(attrs.get("dcmSuppressWarningCoercionOfDataElements"), false));
       arcae.setStoreOriginalAttributes(
               booleanValue(attrs.get("dcmStoreOriginalAttributes"), false));
       arcae.setModifyingSystem(stringValue(attrs.get("dcmModifyingSystem")));
       arcae.setStorageCommitmentDelay(intValue(attrs.get("dcmStgCmtDelay"), 0));
       arcae.setStorageCommitmentMaxRetries(intValue(attrs.get("dcmStgCmtMaxRetries"), 0));
       arcae.setStorageCommitmentRetryInterval(intValue(attrs.get("dcmStgCmtRetryInterval"),
               ArchiveApplicationEntity.DEF_RETRY_INTERVAL));
       arcae.setForwardMPPSDestinations(stringArray(attrs.get("dcmFwdMppsDestination")));
       arcae.setForwardMPPSMaxRetries(intValue(attrs.get("dcmFwdMppsMaxRetries"), 0));
       arcae.setForwardMPPSRetryInterval(intValue(attrs.get("dcmFwdMppsRetryInterval"),
               ArchiveApplicationEntity.DEF_RETRY_INTERVAL));
       arcae.setIANDestinations(stringArray(attrs.get("dcmIanDestination")));
       arcae.setIANMaxRetries(intValue(attrs.get("dcmIanMaxRetries"), 0));
       arcae.setIANRetryInterval(intValue(attrs.get("dcmIanRetryInterval"),
               ArchiveApplicationEntity.DEF_RETRY_INTERVAL));
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
    protected void loadFrom(HL7Application hl7App, Attributes attrs) throws NamingException {
       super.loadFrom(hl7App, attrs);
       if (!(hl7App instanceof ArchiveHL7Application))
           return;
       ArchiveHL7Application arcHL7App = (ArchiveHL7Application) hl7App;
       arcHL7App.setHL7DefaultCharacterSet(stringValue(attrs.get("hl7DefaultCharacterSet")));
       arcHL7App.setTemplatesURIs(stringArray(attrs.get("labeledURI")));
    }

    @Override
    protected List<ModificationItem> storeDiffs(Device a, Device b,
            List<ModificationItem> mods) {
        super.storeDiffs(a, b, mods);
        if (!(a instanceof ArchiveDevice && b instanceof ArchiveDevice))
            return mods;
        
        ArchiveDevice aa = (ArchiveDevice) a;
        ArchiveDevice bb = (ArchiveDevice) b;
        storeDiff(mods, "dcmFuzzyAlgorithmClass",
                aa.getFuzzyAlgorithmClass(),
                bb.getFuzzyAlgorithmClass());
        return mods;
    }

    @Override
    protected List<ModificationItem> storeDiffs(ApplicationEntity a,
            ApplicationEntity b, String deviceDN, List<ModificationItem> mods) {
        super.storeDiffs(a, b, deviceDN, mods);
        if (!(a instanceof ArchiveApplicationEntity 
           && b instanceof ArchiveApplicationEntity))
            return mods;
        
        ArchiveApplicationEntity aa = (ArchiveApplicationEntity) a;
        ArchiveApplicationEntity bb = (ArchiveApplicationEntity) b;
        storeDiff(mods, "dcmFileSystemGroupID",
                aa.getFileSystemGroupID(),
                bb.getFileSystemGroupID());
        storeDiff(mods, "dcmReceivingDirectoryPath",
                aa.getReceivingDirectoryPath(),
                bb.getReceivingDirectoryPath());
        storeDiff(mods, "dcmStorageFilePathFormat",
                aa.getStorageFilePathFormat(),
                bb.getStorageFilePathFormat());
        storeDiff(mods, "dcmDigestAlgorithm",
                aa.getDigestAlgorithm(),
                bb.getDigestAlgorithm());
        storeDiff(mods, "dcmStoreDuplicate",
                aa.getStoreDuplicate(),
                bb.getStoreDuplicate());
        storeDiff(mods, "dcmExternalRetrieveAET",
                aa.getExternalRetrieveAET(),
                bb.getExternalRetrieveAET());
        storeDiff(mods, "dcmRetrieveAET",
                aa.getRetrieveAETs(),
                bb.getRetrieveAETs());
        storeDiff(mods, "dcmMatchUnknown",
                aa.isMatchUnknown(),
                bb.isMatchUnknown(),
                false);
        storeDiff(mods, "dcmSendPendingCGet",
                aa.isSendPendingCGet(),
                bb.isSendPendingCGet(),
                false);
        storeDiff(mods, "dcmSendPendingCMoveInterval",
                aa.getSendPendingCMoveInterval(),
                bb.getSendPendingCMoveInterval(),
                0);
        storeDiff(mods, "dcmSuppressWarningCoercionOfDataElements",
                aa.isSuppressWarningCoercionOfDataElements(),
                bb.isSuppressWarningCoercionOfDataElements(),
                false);
        storeDiff(mods, "dcmStoreOriginalAttributes",
                aa.isStoreOriginalAttributes(),
                bb.isStoreOriginalAttributes(),
                false);
        storeDiff(mods, "dcmModifyingSystem",
                aa.getModifyingSystem(),
                bb.getModifyingSystem());
        storeDiff(mods, "dcmStgCmtDelay",
                aa.getStorageCommitmentDelay(),
                bb.getStorageCommitmentDelay(),
                0);
        storeDiff(mods, "dcmStgCmtMaxRetries",
                aa.getStorageCommitmentMaxRetries(),
                bb.getStorageCommitmentMaxRetries(),
                0);
        storeDiff(mods, "dcmStgCmtRetryInterval",
                aa.getStorageCommitmentRetryInterval(),
                bb.getStorageCommitmentRetryInterval(),
                ArchiveApplicationEntity.DEF_RETRY_INTERVAL);
        storeDiff(mods, "dcmFwdMppsDestination",
                aa.getForwardMPPSDestinations(),
                bb.getForwardMPPSDestinations());
        storeDiff(mods, "dcmFwdMppsMaxRetries",
                aa.getForwardMPPSMaxRetries(),
                bb.getForwardMPPSMaxRetries(),
                0);
        storeDiff(mods, "dcmFwdMppsRetryInterval",
                aa.getForwardMPPSRetryInterval(),
                bb.getForwardMPPSRetryInterval(),
                ArchiveApplicationEntity.DEF_RETRY_INTERVAL);
        storeDiff(mods, "dcmIanDestination",
                aa.getIANDestinations(),
                bb.getIANDestinations());
        storeDiff(mods, "dcmIanMaxRetries",
                aa.getIANMaxRetries(),
                bb.getIANMaxRetries(),
                0);
        storeDiff(mods, "dcmIanRetryInterval",
                aa.getIANRetryInterval(),
                bb.getIANRetryInterval(),
                ArchiveApplicationEntity.DEF_RETRY_INTERVAL);
        return mods;
    }

    @Override
    protected List<ModificationItem> storeDiffs(HL7Application a,
            HL7Application b, String deviceDN, List<ModificationItem> mods) {
        super.storeDiffs(a, b, deviceDN, mods);
        if (!(a instanceof ArchiveHL7Application 
           && b instanceof ArchiveHL7Application))
            return mods;
        
        ArchiveHL7Application aa = (ArchiveHL7Application) a;
        ArchiveHL7Application bb = (ArchiveHL7Application) b;
        storeDiff(mods, "hl7DefaultCharacterSet",
                aa.getHL7DefaultCharacterSet(),
                bb.getHL7DefaultCharacterSet());
        storeDiff(mods, "labeledURI",
                aa.getTemplatesURIs(),
                bb.getTemplatesURIs());
        return mods;
    }

    @Override
    protected void mergeChilds(Device prev, Device device, String deviceDN)
            throws NamingException {
        super.mergeChilds(prev, device, deviceDN);
        if (!(prev instanceof ArchiveDevice && device instanceof ArchiveDevice))
            return;
        
        ArchiveDevice aa = (ArchiveDevice) prev;
        ArchiveDevice bb = (ArchiveDevice) device;
        for (Entity entity : Entity.values())
            modifyAttributes(dnOf("dcmEntity", entity.toString(), deviceDN),
                    storeDiffs(aa.getAttributeFilter(entity), bb.getAttributeFilter(entity),
                            new ArrayList<ModificationItem>()));
    }

    @Override
    protected void mergeChilds(ApplicationEntity prev, ApplicationEntity ae, String aeDN)
            throws NamingException {
        super.mergeChilds(prev, ae, aeDN);
        if (!(prev instanceof ArchiveApplicationEntity 
             && ae instanceof ArchiveApplicationEntity))
            return;

        ArchiveApplicationEntity aa = (ArchiveApplicationEntity) prev;
        ArchiveApplicationEntity bb = (ArchiveApplicationEntity) ae;
        merge(aa.getAttributeCoercions(), bb.getAttributeCoercions(), aeDN);
    }

    private List<ModificationItem> storeDiffs(AttributeFilter prev,
            AttributeFilter filter, List<ModificationItem> mods) {
        storeDiffTags(mods, "dcmTag", 
                prev.getSelection(),
                filter.getSelection());
        storeDiff(mods, "dcmCustomAttribute1",
                prev.getCustomAttribute1(),
                filter.getCustomAttribute1());
        storeDiff(mods, "dcmCustomAttribute2",
                prev.getCustomAttribute2(),
                filter.getCustomAttribute2());
        storeDiff(mods, "dcmCustomAttribute3",
                prev.getCustomAttribute3(),
                filter.getCustomAttribute3());
        return mods;
    }

    private void storeDiffTags(List<ModificationItem> mods, String attrId,
            int[] prevs, int[] vals) {
        if (!Arrays.equals(prevs, vals))
            mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    tagsAttr(attrId, vals)));
    }

}
