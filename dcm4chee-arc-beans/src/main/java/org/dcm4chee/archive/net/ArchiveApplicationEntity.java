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

package org.dcm4chee.archive.net;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;

import org.dcm4che.conf.api.AttributeCoercion;
import org.dcm4che.conf.api.AttributeCoercions;
import org.dcm4che.net.ApplicationEntity;
import org.dcm4che.net.TransferCapability;
import org.dcm4che.net.TransferCapability.Role;
import org.dcm4che.util.AttributesFormat;
import org.dcm4chee.archive.ejb.store.StoreParam;
import org.dcm4chee.archive.ejb.store.StoreParam.StoreDuplicate;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class ArchiveApplicationEntity extends ApplicationEntity {

    public static final int DEF_STGCMT_RETRY_INTERVAL = 60;

    private StoreDuplicate storeDuplicate;
    private String modifyingSystem;
    private String[] retrieveAETs;
    private String externalRetrieveAET;
    private String fileSystemGroupID;
    private String digestAlgorithm;
    private String receivingDirectoryPath;
    private AttributesFormat storageFilePathFormat;
    private boolean storeOriginalAttributes;
    private boolean suppressWarningCoercionOfDataElements;
    private boolean matchUnknown;
    private boolean sendPendingCGet;
    private int sendPendingCMoveInterval;
    private int storageCommitmentDelay;
    private int storageCommitmentMaxRetries;
    private int storageCommitmentRetryInterval = DEF_STGCMT_RETRY_INTERVAL;
    private final AttributeCoercions attributeCoercions = new AttributeCoercions();

    public ArchiveApplicationEntity(String aeTitle) {
        super(aeTitle);
    }

    public ArchiveDevice getArchiveDevice() {
        return ((ArchiveDevice) getDevice());
    }

    public Boolean getMatchUnknown() {
        return matchUnknown;
    }

    public AttributeCoercion getAttributeCoercion(String sopClass,
            AttributeCoercion.DIMSE cmd, Role role, String aeTitle) {
        return attributeCoercions.get(sopClass, cmd, role, aeTitle);
    }

    public AttributeCoercion removeAttributeCoercion(String sopClass,
            AttributeCoercion.DIMSE cmd, Role role, String aeTitle) {
        return attributeCoercions.remove(sopClass, cmd, role, aeTitle);
    }

    public AttributeCoercions getAttributeCoercions() {
        return attributeCoercions;
    }

    public AttributeCoercion addAttributeCoercion(AttributeCoercion ac) {
        return attributeCoercions.add(ac);
    }

    public StoreDuplicate getStoreDuplicate() {
        return storeDuplicate;
    }

    public void setStoreDuplicate(StoreDuplicate storeDuplicate) {
        this.storeDuplicate = storeDuplicate;
    }

    public String getModifyingSystem() {
        return modifyingSystem;
    }

    public String getEffectiveModifyingSystem() {
        return modifyingSystem != null 
                ? modifyingSystem
                : getDevice().getDeviceName();
    }

    public void setModifyingSystem(String modifyingSystem) {
        this.modifyingSystem = modifyingSystem;
    }

    public String[] getRetrieveAETs() {
        return retrieveAETs;
    }

    public void setRetrieveAETs(String... retrieveAETs) {
        this.retrieveAETs = retrieveAETs;
    }

    public String getExternalRetrieveAET() {
        return externalRetrieveAET;
    }

    public void setExternalRetrieveAET(String externalRetrieveAET) {
        this.externalRetrieveAET = externalRetrieveAET;
    }

    public String getFileSystemGroupID() {
        return fileSystemGroupID;
    }

    public void setFileSystemGroupID(String fileSystemGroupID) {
        this.fileSystemGroupID = fileSystemGroupID;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    public String getReceivingDirectoryPath() {
        return receivingDirectoryPath;
    }

    public void setReceivingDirectoryPath(String receivingDirectoryPath) {
        this.receivingDirectoryPath = receivingDirectoryPath;
    }

    public AttributesFormat getStorageFilePathFormat() {
        return storageFilePathFormat;
    }

    public void setStorageFilePathFormat(AttributesFormat storageFilePathFormat) {
        this.storageFilePathFormat = storageFilePathFormat;
    }

    public Templates getAttributeCoercionTemplates(String cuid, AttributeCoercion.DIMSE cmd,
            TransferCapability.Role role, String aet) throws TransformerConfigurationException {
        AttributeCoercion ac = getAttributeCoercion(cuid, cmd, role, aet);
        return ac != null ? getArchiveDevice().getTemplates(ac.getURI()) : null;
    }

    public boolean isStoreOriginalAttributes() {
        return storeOriginalAttributes;
    }

    public void setStoreOriginalAttributes(boolean storeOriginalAttributes) {
        this.storeOriginalAttributes = storeOriginalAttributes;
    }

    public boolean isSuppressWarningCoercionOfDataElements() {
        return suppressWarningCoercionOfDataElements;
    }

    public void setSuppressWarningCoercionOfDataElements(
            boolean suppressWarningCoercionOfDataElements) {
        this.suppressWarningCoercionOfDataElements = suppressWarningCoercionOfDataElements;
    }

    public boolean isMatchUnknown() {
        return matchUnknown;
    }

    public void setMatchUnknown(boolean matchUnknown) {
        this.matchUnknown = matchUnknown;
    }

    public boolean isSendPendingCGet() {
        return sendPendingCGet;
    }

    public void setSendPendingCGet(boolean sendPendingCGet) {
        this.sendPendingCGet = sendPendingCGet;
    }

    public int getSendPendingCMoveInterval() {
        return sendPendingCMoveInterval;
    }

    public void setSendPendingCMoveInterval(int sendPendingCMoveInterval) {
        this.sendPendingCMoveInterval = sendPendingCMoveInterval;
    }

    public final int getStorageCommitmentDelay() {
        return storageCommitmentDelay;
    }

    public final void setStorageCommitmentDelay(int storageCommitmentDelay) {
        this.storageCommitmentDelay = storageCommitmentDelay;
    }

    public final int getStorageCommitmentMaxRetries() {
        return storageCommitmentMaxRetries;
    }

    public final void setStorageCommitmentMaxRetries(int storageCommitmentMaxRetries) {
        this.storageCommitmentMaxRetries = storageCommitmentMaxRetries;
    }

    public final int getStorageCommitmentRetryInterval() {
        return storageCommitmentRetryInterval;
    }

    public final void setStorageCommitmentRetryInterval(
            int storageCommitmentRetryInterval) {
        this.storageCommitmentRetryInterval = storageCommitmentRetryInterval;
    }

    public StoreParam getStoreParam() {
        ArchiveDevice dev = getArchiveDevice();
        StoreParam storeParam = new StoreParam();
        storeParam.setFuzzyStr(dev.getFuzzyStr());
        storeParam.setAttributeFilters(dev.getAttributeFilters());
        storeParam.setStoreOriginalAttributes(isStoreOriginalAttributes());
        storeParam.setModifyingSystem(getEffectiveModifyingSystem());
        storeParam.setRetrieveAETs(getRetrieveAETs());
        storeParam.setExternalRetrieveAET(getExternalRetrieveAET());
        storeParam.setStoreDuplicate(getStoreDuplicate());
        return storeParam;
    }
}
