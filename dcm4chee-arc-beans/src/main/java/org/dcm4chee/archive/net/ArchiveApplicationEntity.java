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
import org.dcm4che.util.AttributesFormat;
import org.dcm4chee.archive.ejb.store.StoreParam;
import org.dcm4chee.archive.ejb.store.StoreParam.StoreDuplicate;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class ArchiveApplicationEntity extends ApplicationEntity {

    private StoreDuplicate storeDuplicate;
    private String modifyingSystem;
    private String[] retrieveAETs;
    private String externalRetrieveAET;
    private String fileSystemGroupID;
    private String digestAlgorithm;
    private String receivingDirectoryPath;
    private AttributesFormat storageFilePathFormat;
    private Boolean storeOriginalAttributes;
    private Boolean suppressWarningCoercionOfDataElements;
    private Boolean matchUnknown;
    private Boolean sendPendingCGet;
    private Integer sendPendingCMoveInterval;
    private final AttributeCoercions attributeCoercions = new AttributeCoercions();

    public ArchiveApplicationEntity(String aeTitle) {
        super(aeTitle);
    }

    private ArchiveDevice getArchiveDevice() {
        return ((ArchiveDevice) getDevice());
    }

    public Boolean getMatchUnknown() {
        return matchUnknown;
    }

    public boolean isMatchUnknown() {
        return matchUnknown != null
                ? matchUnknown
                : getArchiveDevice().isMatchUnknown();
    }

    public void setMatchUnknown(Boolean matchUnknown) {
        this.matchUnknown = matchUnknown;
    }

    public Boolean getSendPendingCGet() {
        return sendPendingCGet;
    }

    public boolean isSendPendingCGet() {
        return sendPendingCGet != null
                ? sendPendingCGet
                : getArchiveDevice().isSendPendingCGet();
    }

    public void setSendPendingCGet(Boolean sendPendingCGet) {
        this.sendPendingCGet = sendPendingCGet;
    }

    public Integer getSendPendingCMoveIntervalOrNull() {
        return sendPendingCMoveInterval;
    }

    public int getSendPendingCMoveInterval() {
        return sendPendingCMoveInterval != null
                ? sendPendingCMoveInterval
                : getArchiveDevice().getSendPendingCMoveInterval();
    }

    public void setSendPendingCMoveInterval(Integer sendPendingCMoveInterval) {
        this.sendPendingCMoveInterval = sendPendingCMoveInterval;
    }

    public void setFileSystemGroupID(String fileSystemGroupID) {
        this.fileSystemGroupID = fileSystemGroupID;
    }

    public String getFileSystemGroupID() {
        return fileSystemGroupID;
    }

    public String getEffectiveFileSystemGroupID() {
        return fileSystemGroupID != null
                ? fileSystemGroupID
                : getArchiveDevice().getFileSystemGroupID();
    }

    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public String getEffectiveDigestAlgorithm() {
        return digestAlgorithm != null
                ? digestAlgorithm
                : getArchiveDevice().getDigestAlgorithm();
    }

    public void setReceivingDirectoryPath(String receivingDirectoryPath) {
        this.receivingDirectoryPath = receivingDirectoryPath;
    }

    public String getReceivingDirectoryPath() {
        return receivingDirectoryPath;
    }

    public String getEffectiveReceivingDirectoryPath() {
        return receivingDirectoryPath != null
                ? receivingDirectoryPath
                : getArchiveDevice().getReceivingDirectoryPath();
    }

    public void setStorageFilePathFormat(AttributesFormat storageFilePathFormat) {
        this.storageFilePathFormat = storageFilePathFormat;
    }

    public AttributesFormat getStorageFilePathFormat() {
        return storageFilePathFormat;
    }

    public AttributesFormat getEffectiveStorageFilePathFormat() {
        return storageFilePathFormat != null
                ? storageFilePathFormat
                : getArchiveDevice().getStorageFilePathFormat();
    }

    public Templates getAttributeCoercionTemplates(String cuid, AttributeCoercion.DIMSE cmd,
            TransferCapability.Role role, String aet) throws TransformerConfigurationException {
        AttributeCoercion ac = getAttributeCoercion(cuid, cmd, role, aet);
        return ac != null ? getArchiveDevice().getTemplates(ac.getURI()) : null;
    }

    public Boolean getSuppressWarningCoercionOfDataElements() {
        return suppressWarningCoercionOfDataElements;
    }

    public boolean isSuppressWarningCoercionOfDataElements() {
        return suppressWarningCoercionOfDataElements != null
                ? suppressWarningCoercionOfDataElements
                : getArchiveDevice().isSuppressWarningCoercionOfDataElements();
    }


    public void setSuppressWarningCoercionOfDataElements(
            boolean suppressWarningCoercionOfDataElements) {
        this.suppressWarningCoercionOfDataElements = suppressWarningCoercionOfDataElements;
    }

    public void setStoreOriginalAttributes(Boolean storeOriginalAttributes) {
        this.storeOriginalAttributes = storeOriginalAttributes;
    }

    public Boolean getStoreOriginalAttributes() {
        return storeOriginalAttributes;
    }

    public boolean isStoreOriginalAttributes() {
        return storeOriginalAttributes != null
                ? storeOriginalAttributes
                : getArchiveDevice().isStoreOriginalAttributes();
    }

    public void setExternalRetrieveAET(String externalRetrieveAET) {
        this.externalRetrieveAET = externalRetrieveAET;
    }

    public String getExternalRetrieveAET() {
        return externalRetrieveAET;
    }

    public String getEffectiveExternalRetrieveAET() {
        return externalRetrieveAET != null
                ? externalRetrieveAET
                : getArchiveDevice().getExternalRetrieveAET();
    }

    public void setRetrieveAETs(String... retrieveAETs) {
        this.retrieveAETs = retrieveAETs;
    }

    public String[] getRetrieveAETs() {
        return retrieveAETs;
    }

    public String[] getEffectiveRetrieveAETs() {
        return retrieveAETs != null
                ? retrieveAETs
                : getArchiveDevice().getRetrieveAETs();
    }

    public void setModifyingSystem(String modifyingSystem) {
        this.modifyingSystem = modifyingSystem;
    }

    public String getModifyingSystem() {
        return modifyingSystem;
    }

    public String getEffectiveModifyingSystem() {
        return modifyingSystem != null
                ? modifyingSystem
                : getArchiveDevice().getEffectiveModifyingSystem();
    }

    public AttributeCoercion getAttributeCoercion(String sopClass, AttributeCoercion.DIMSE cmd,
            TransferCapability.Role role, String aeTitle) {
        AttributeCoercion ac = attributeCoercions.get(sopClass, cmd, role, aeTitle);
        return ac != null
                ? ac
                : getArchiveDevice().getAttributeCoercion(sopClass, cmd, role, aeTitle);
    }

    public AttributeCoercions getAttributeCoercions() {
        return attributeCoercions;
    }

    public void setStoreDuplicate(StoreDuplicate storeDuplicate) {
        this.storeDuplicate = storeDuplicate;
    }

    public StoreDuplicate getStoreDuplicate() {
        return storeDuplicate;
    }

    public StoreDuplicate getEffectiveStoreDuplicate() {
        return storeDuplicate != null
                ? storeDuplicate
                : getArchiveDevice().getStoreDuplicate();
    }

    public StoreParam getStoreParam() {
        ArchiveDevice dev = getArchiveDevice();
        StoreParam storeParam = new StoreParam();
        storeParam.setFuzzyStr(dev.getFuzzyStr());
        storeParam.setAttributeFilters(dev.getAttributeFilters());
        storeParam.setModifyingSystem(getEffectiveModifyingSystem());
        storeParam.setRetrieveAETs(getEffectiveRetrieveAETs());
        storeParam.setExternalRetrieveAET(getEffectiveExternalRetrieveAET());
        storeParam.setStoreDuplicate(getEffectiveStoreDuplicate());
        return storeParam;
    }
}
