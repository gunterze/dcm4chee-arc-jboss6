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

package org.dcm4chee.archive.conf.prefs;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.dcm4che.conf.prefs.PreferencesDicomConfiguration;
import org.dcm4che.data.ValueSelector;
import org.dcm4che.net.ApplicationEntity;
import org.dcm4che.net.Connection;
import org.dcm4che.net.Device;
import org.dcm4che.soundex.FuzzyStr;
import org.dcm4che.util.AttributesFormat;
import org.dcm4che.util.TagUtils;
import org.dcm4chee.archive.ejb.store.Entity;
import org.dcm4chee.archive.ejb.store.StoreParam.StoreDuplicate;
import org.dcm4chee.archive.net.ArchiveApplicationEntity;
import org.dcm4chee.archive.net.ArchiveDevice;
import org.dcm4chee.archive.persistence.AttributeFilter;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class PreferencesArchiveConfiguration extends PreferencesDicomConfiguration {

    public PreferencesArchiveConfiguration(Preferences rootPrefs) {
        super(rootPrefs);
        setConfigurationRoot("org/dcm4chee/archive");
    }

    @Override
    protected void storeTo(Device device, Preferences prefs) {
        super.storeTo(device, prefs);
        if (!(device instanceof ArchiveDevice))
            return;

        ArchiveDevice arcDev = (ArchiveDevice) device;
        storeBoolean(prefs, "dcmArchiveDevice", true);
        storeNotNull(prefs, "dcmFileSystemGroupID", arcDev.getFileSystemGroupID());
        storeNotNull(prefs, "dcmReceivingDirectoryPath", arcDev.getReceivingDirectoryPath());
        storeNotNull(prefs, "dcmStorageFilePathFormat", arcDev.getStorageFilePathFormat());
        storeNotNull(prefs, "dcmDigestAlgorithm", arcDev.getDigestAlgorithm());
        storeNotNull(prefs, "dcmStoreDuplicate", arcDev.getStoreDuplicate());
        storeNotNull(prefs, "dcmExternalRetrieveAET", arcDev.getExternalRetrieveAET());
        storeNotEmpty(prefs, "dcmRetrieveAET", arcDev.getRetrieveAETs());
        storeBoolean(prefs, "dcmMatchUnknown", arcDev.isMatchUnknown());
        storeBoolean(prefs, "dcmSendPendingCGet", arcDev.isSendPendingCGet());
        storeInt(prefs, "dcmSendPendingCMoveInterval", arcDev.getSendPendingCMoveInterval());
        storeBoolean(prefs, "dcmSuppressWarningCoercionOfDataElements",
                arcDev.isSuppressWarningCoercionOfDataElements());
        storeBoolean(prefs, "dcmStoreOriginalAttributes",
                arcDev.isStoreOriginalAttributes());
        storeNotNull(prefs, "dcmModifyingSystem", arcDev.getModifyingSystem());
        storeNotNull(prefs, "dcmFuzzyAlgorithmClass",
                arcDev.getFuzzyStr().getClass().getName());
    }

    @Override
    protected void storeChilds(Device device, Preferences deviceNode) {
        super.storeChilds(device, deviceNode);
        if (!(device instanceof ArchiveDevice))
            return;

        ArchiveDevice arcDev = (ArchiveDevice) device;
        store(arcDev.getAttributeCoercions(), deviceNode);
        Preferences afsNode = deviceNode.node("dcmAttributeFilter");
        for (Entity entity : Entity.values())
            storeTo(arcDev.getAttributeFilter(entity), afsNode.node(entity.name()));
    }

    @Override
    protected void storeChilds(ApplicationEntity ae, Preferences aeNode) {
        super.storeChilds(ae, aeNode);
        if (!(ae instanceof ArchiveApplicationEntity))
            return;
        ArchiveApplicationEntity arcAE = (ArchiveApplicationEntity) ae;
        store(arcAE.getAttributeCoercions(), aeNode);
    }

    private static void storeTo(AttributeFilter filter, Preferences prefs) {
        storeTags(prefs, "dcmTag", filter.getSelection());
        storeNotNull(prefs, "dcmCustomAttribute1", filter.getCustomAttribute1());
        storeNotNull(prefs, "dcmCustomAttribute2", filter.getCustomAttribute2());
        storeNotNull(prefs, "dcmCustomAttribute3", filter.getCustomAttribute3());
    }

    private static void storeTags(Preferences prefs, String key, int[] tags) {
        if (tags.length != 0) {
            int count = 0;
            for (int tag : tags)
                prefs.put(key + '.' + (++count), TagUtils.toHexString(tag));
            prefs.putInt(key + ".#", count);
        }
    }

    @Override
    protected void storeTo(ApplicationEntity ae, Preferences prefs,
            List<Connection> devConns) {
        super.storeTo(ae, prefs, devConns);
        if (!(ae instanceof ArchiveApplicationEntity))
            return;

        ArchiveApplicationEntity arcAE = (ArchiveApplicationEntity) ae;
        storeBoolean(prefs, "dcmArchiveNetworkAE", true);
        storeNotNull(prefs, "dcmFileSystemGroupID", arcAE.getFileSystemGroupID());
        storeNotNull(prefs, "dcmReceivingDirectoryPath", arcAE.getReceivingDirectoryPath());
        storeNotNull(prefs, "dcmStorageFilePathFormat", arcAE.getStorageFilePathFormat());
        storeNotNull(prefs, "dcmDigestAlgorithm", arcAE.getDigestAlgorithm());
        storeNotNull(prefs, "dcmStoreDuplicate", arcAE.getStoreDuplicate());
        storeNotNull(prefs, "dcmExternalRetrieveAET", arcAE.getExternalRetrieveAET());
        storeNotEmpty(prefs, "dcmRetrieveAET", arcAE.getRetrieveAETs());
        storeNotNull(prefs, "dcmMatchUnknown", arcAE.getMatchUnknown());
        storeNotNull(prefs, "dcmSendPendingCGet", arcAE.getSendPendingCGet());
        storeNotNull(prefs, "dcmSendPendingCMoveInterval",
                arcAE.getSendPendingCMoveIntervalOrNull());
        storeNotNull(prefs, "dcmSuppressWarningCoercionOfDataElements",
                arcAE.getSuppressWarningCoercionOfDataElements());
        storeNotNull(prefs, "dcmStoreOriginalAttributes",
                arcAE.getStoreOriginalAttributes());
        storeNotNull(prefs, "dcmModifyingSystem", arcAE.getModifyingSystem());
    }

    @Override
    protected Device newDevice(Preferences deviceNode) {
        if (!deviceNode.getBoolean("dcmArchiveDevice", false))
            return super.newDevice(deviceNode);

        return new ArchiveDevice(deviceNode.name());
    }

    @Override
    protected ApplicationEntity newApplicationEntity(Preferences aeNode) {
        if (!aeNode.getBoolean("dcmArchiveNetworkAE", false))
            return super.newApplicationEntity(aeNode);

        return new ArchiveApplicationEntity(aeNode.name());
    }

    @Override
    protected void loadFrom(Device device, Preferences prefs) {
        super.loadFrom(device, prefs);
        if (!(device instanceof ArchiveDevice))
            return;

        ArchiveDevice arcdev = (ArchiveDevice) device;
        arcdev.setFileSystemGroupID(prefs.get("dcmFileSystemGroupID", null));
        arcdev.setReceivingDirectoryPath(prefs.get("dcmReceivingDirectoryPath", null));
        arcdev.setStorageFilePathFormat(
                AttributesFormat.valueOf(prefs.get("dcmStorageFilePathFormat", null)));
        arcdev.setDigestAlgorithm(prefs.get("dcmDigestAlgorithm", null));
        arcdev.setStoreDuplicate(storeDuplicate(prefs.get("dcmStoreDuplicate", null)));
        arcdev.setExternalRetrieveAET(prefs.get("dcmExternalRetrieveAET", null));
        arcdev.setRetrieveAETs(stringArray(prefs, "dcmRetrieveAET"));
        arcdev.setMatchUnknown(prefs.getBoolean("dcmMatchUnknown", true));
        arcdev.setSendPendingCGet(prefs.getBoolean("dcmSendPendingCGet", false));
        arcdev.setSendPendingCMoveInterval(prefs.getInt("dcmSendPendingCMoveInterval", 0));
        arcdev.setSuppressWarningCoercionOfDataElements(
                prefs.getBoolean("dcmSuppressWarningCoercionOfDataElements", false));
        arcdev.setStoreOriginalAttributes(
                prefs.getBoolean("dcmStoreOriginalAttributes", false));
        arcdev.setModifyingSystem(prefs.get("dcmModifyingSystem", null));
        arcdev.setFuzzyStr(fuzzyStr(prefs.get("dcmFuzzyAlgorithmClass", null)));
    }

    @Override
    protected void loadChilds(Device device, Preferences deviceNode)
            throws BackingStoreException {
        super.loadChilds(device, deviceNode);
        if (!(device instanceof ArchiveDevice))
            return;
        ArchiveDevice arcdev = (ArchiveDevice) device;
        load(arcdev.getAttributeCoercions(), deviceNode);
        loadAttributeFilters(arcdev, deviceNode);
    }

    @Override
    protected void loadFrom(ApplicationEntity ae, Preferences prefs) {
        super.loadFrom(ae, prefs);
        if (!(ae instanceof ArchiveApplicationEntity))
            return;
        ArchiveApplicationEntity arcae = (ArchiveApplicationEntity) ae;
        arcae.setFileSystemGroupID(prefs.get("dcmFileSystemGroupID", null));
        arcae.setReceivingDirectoryPath(prefs.get("dcmReceivingDirectoryPath", null));
        arcae.setStorageFilePathFormat(
                AttributesFormat.valueOf(prefs.get("dcmStorageFilePathFormat", null)));
        arcae.setDigestAlgorithm(prefs.get("dcmDigestAlgorithm", null));
        arcae.setStoreDuplicate(storeDuplicate(prefs.get("dcmStoreDuplicate", null)));
        arcae.setExternalRetrieveAET(prefs.get("dcmExternalRetrieveAET", null));
        arcae.setRetrieveAETs(stringArray(prefs, "dcmRetrieveAET"));
        arcae.setMatchUnknown(prefs.getBoolean("dcmMatchUnknown", true));
        arcae.setSendPendingCGet(prefs.getBoolean("dcmSendPendingCGet", false));
        arcae.setSendPendingCMoveInterval(prefs.getInt("dcmSendPendingCMoveInterval", 0));
        arcae.setSuppressWarningCoercionOfDataElements(
                prefs.getBoolean("dcmSuppressWarningCoercionOfDataElements", false));
        arcae.setStoreOriginalAttributes(
                prefs.getBoolean("dcmStoreOriginalAttributes", false));
        arcae.setModifyingSystem(prefs.get("dcmModifyingSystem", null));
    }

    @Override
    protected void loadChilds(ApplicationEntity ae, Preferences aeNode)
            throws BackingStoreException {
        super.loadChilds(ae, aeNode);
        if (!(ae instanceof ArchiveApplicationEntity))
            return;
        ArchiveApplicationEntity arcae = (ArchiveApplicationEntity) ae;
        load(arcae.getAttributeCoercions(), aeNode);
    }

    private static void loadAttributeFilters(ArchiveDevice device, Preferences deviceNode)
            throws BackingStoreException {
        Preferences afsNode = deviceNode.node("dcmAttributeFilter");
        for (String entity : afsNode.childrenNames()) {
            Preferences acNode = afsNode.node(entity);
            AttributeFilter filter = new AttributeFilter(tags(acNode, "dcmTag"));
            filter.setCustomAttribute1(
                    ValueSelector.valueOf(acNode.get("dcmCustomAttribute1", null)));
            filter.setCustomAttribute2(
                    ValueSelector.valueOf(acNode.get("dcmCustomAttribute2", null)));
            filter.setCustomAttribute3(
                    ValueSelector.valueOf(acNode.get("dcmCustomAttribute3", null)));
            device.setAttributeFilter(
                    Entity.valueOf(entity), filter);
        }
    }

    private static StoreDuplicate storeDuplicate(String s) {
        return s != null ? StoreDuplicate.valueOf(s) : null;
    }

    private static int[] tags(Preferences prefs, String key) {
        int n = prefs.getInt(key + ".#", 0);
        int[] is = new int[n];
        for (int i = 0; i < n; i++)
            is[i] = Integer.parseInt(prefs.get(key + '.' + (i+1), null), 16);
        return is;
    }

    private static FuzzyStr fuzzyStr(String s) {
        try {
            return (FuzzyStr) Class.forName(s).newInstance();
         } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void storeDiffs(Preferences mods, Device a, Device b) {
        super.storeDiffs(mods, a, b);
        if (!(a instanceof ArchiveDevice && b instanceof ArchiveDevice))
            return;

        ArchiveDevice aa = (ArchiveDevice) a;
        ArchiveDevice bb = (ArchiveDevice) b;
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
                bb.isMatchUnknown());
        storeDiff(mods, "dcmSendPendingCGet",
                aa.isSendPendingCGet(),
                bb.isSendPendingCGet());
        storeDiff(mods, "dcmSendPendingCMoveInterval",
                aa.getSendPendingCMoveInterval(),
                bb.getSendPendingCMoveInterval());
        storeDiff(mods, "dcmSuppressWarningCoercionOfDataElements",
                aa.isSuppressWarningCoercionOfDataElements(),
                bb.isSuppressWarningCoercionOfDataElements());
        storeDiff(mods, "dcmStoreOriginalAttributes",
                aa.isStoreOriginalAttributes(),
                bb.isStoreOriginalAttributes());
        storeDiff(mods, "dcmModifyingSystem",
                aa.getModifyingSystem(),
                bb.getModifyingSystem());
        storeDiff(mods, "dcmFuzzyAlgorithmClass",
                aa.getFuzzyAlgorithmClass(),
                bb.getFuzzyAlgorithmClass());
    }

    @Override
    protected void storeDiffs(Preferences prefs, ApplicationEntity a,
            ApplicationEntity b) {
        super.storeDiffs(prefs, a, b);
        if (!(a instanceof ArchiveApplicationEntity 
           && b instanceof ArchiveApplicationEntity))
                 return;

         ArchiveApplicationEntity aa = (ArchiveApplicationEntity) a;
         ArchiveApplicationEntity bb = (ArchiveApplicationEntity) b;
         storeDiff(prefs, "dcmFileSystemGroupID",
                 aa.getFileSystemGroupID(),
                 bb.getFileSystemGroupID());
         storeDiff(prefs, "dcmReceivingDirectoryPath",
                 aa.getReceivingDirectoryPath(),
                 bb.getReceivingDirectoryPath());
         storeDiff(prefs, "dcmStorageFilePathFormat",
                 aa.getStorageFilePathFormat(),
                 bb.getStorageFilePathFormat());
         storeDiff(prefs, "dcmDigestAlgorithm",
                 aa.getDigestAlgorithm(),
                 bb.getDigestAlgorithm());
         storeDiff(prefs, "dcmStoreDuplicate",
                 aa.getStoreDuplicate(),
                 bb.getStoreDuplicate());
         storeDiff(prefs, "dcmExternalRetrieveAET",
                 aa.getExternalRetrieveAET(),
                 bb.getExternalRetrieveAET());
         storeDiff(prefs, "dcmRetrieveAET",
                 aa.getRetrieveAETs(),
                 bb.getRetrieveAETs());
         storeDiff(prefs, "dcmMatchUnknown",
                 aa.isMatchUnknown(),
                 bb.isMatchUnknown());
         storeDiff(prefs, "dcmSendPendingCGet",
                 aa.isSendPendingCGet(),
                 bb.isSendPendingCGet());
         storeDiff(prefs, "dcmSendPendingCMoveInterval",
                 aa.getSendPendingCMoveInterval(),
                 bb.getSendPendingCMoveInterval());
         storeDiff(prefs, "dcmSuppressWarningCoercionOfDataElements",
                 aa.isSuppressWarningCoercionOfDataElements(),
                 bb.isSuppressWarningCoercionOfDataElements());
         storeDiff(prefs, "dcmStoreOriginalAttributes",
                 aa.isStoreOriginalAttributes(),
                 bb.isStoreOriginalAttributes());
         storeDiff(prefs, "dcmModifyingSystem",
                 aa.getModifyingSystem(),
                 bb.getModifyingSystem());
    }

    @Override
    protected void mergeChilds(Device prev, Device device,
            Preferences deviceNode) throws BackingStoreException {
        super.mergeChilds(prev, device, deviceNode);
        if (!(prev instanceof ArchiveDevice && device instanceof ArchiveDevice))
            return;
        
        ArchiveDevice aa = (ArchiveDevice) prev;
        ArchiveDevice bb = (ArchiveDevice) device;
        merge(aa.getAttributeCoercions(), bb.getAttributeCoercions(), deviceNode);
        Preferences afsNode = deviceNode.node("dcmAttributeFilter");
        for (Entity entity : Entity.values())
            storeDiffs(afsNode.node(entity.name()), aa.getAttributeFilter(entity),
                    bb.getAttributeFilter(entity));
    }

    private void storeDiffs(Preferences prefs, AttributeFilter prev, AttributeFilter filter) {
        storeTags(prefs, "dcmTag", filter.getSelection());
        storeDiffTags(prefs, "dcmTag", 
                prev.getSelection(),
                filter.getSelection());
        storeDiff(prefs, "dcmCustomAttribute1",
                prev.getCustomAttribute1(),
                filter.getCustomAttribute1());
        storeDiff(prefs, "dcmCustomAttribute2",
                prev.getCustomAttribute2(),
                filter.getCustomAttribute2());
        storeDiff(prefs, "dcmCustomAttribute3",
                prev.getCustomAttribute3(),
                filter.getCustomAttribute3());
    }

    private void storeDiffTags(Preferences prefs, String key, int[] prevs, int[] vals) {
        if (!Arrays.equals(prevs, vals)) {
            removeKeys(prefs, key, vals.length, prevs.length);
            storeTags(prefs, key, vals);
        }
    }

    @Override
    protected void mergeChilds(ApplicationEntity prev, ApplicationEntity ae,
            Preferences aePrefs) throws BackingStoreException {
        super.mergeChilds(prev, ae, aePrefs);
        if (!(prev instanceof ApplicationEntity
             && ae instanceof ApplicationEntity))
            return;
        
        ArchiveApplicationEntity aa = (ArchiveApplicationEntity) prev;
        ArchiveApplicationEntity bb = (ArchiveApplicationEntity) ae;
        merge(aa.getAttributeCoercions(), bb.getAttributeCoercions(), aePrefs);
    }

}
