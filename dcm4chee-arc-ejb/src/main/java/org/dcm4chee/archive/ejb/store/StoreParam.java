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

package org.dcm4chee.archive.ejb.store;

import org.dcm4che.soundex.FuzzyStr;
import org.dcm4chee.archive.persistence.AttributeFilter;


/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class StoreParam {

    public enum StoreDuplicate { IGNORE, STORE, REPLACE };

    private FuzzyStr fuzzyStr;
    private AttributeFilter[] attributeFilters;
    private boolean storeOriginalAttributes;
    private String modifyingSystem;
    private String[] retrieveAETs;
    private String externalRetrieveAET;
    private StoreDuplicate storeDuplicate;

    public final boolean isStoreOriginalAttributes() {
        return storeOriginalAttributes;
    }

    public final void setStoreOriginalAttributes(boolean storeOriginalAttributes) {
        this.storeOriginalAttributes = storeOriginalAttributes;
    }

    public final String getModifyingSystem() {
        return modifyingSystem;
    }

    public final void setModifyingSystem(String modifyingSystem) {
        this.modifyingSystem = modifyingSystem;
    }

    public final StoreDuplicate getStoreDuplicate() {
        return storeDuplicate;
    }

    public final void setStoreDuplicate(StoreDuplicate storeDuplicate) {
        this.storeDuplicate = storeDuplicate;
    }

    public final String[] getRetrieveAETs() {
        return retrieveAETs;
    }

    public final void setRetrieveAETs(String... retrieveAETs) {
        this.retrieveAETs = retrieveAETs;
    }

    public final String getExternalRetrieveAET() {
        return externalRetrieveAET;
    }

    public final void setExternalRetrieveAET(String externalRetrieveAET) {
        this.externalRetrieveAET = externalRetrieveAET;
    }

    public final void setFuzzyStr(FuzzyStr fuzzyStr) {
        this.fuzzyStr = fuzzyStr;
    }

    public final FuzzyStr getFuzzyStr() {
        return fuzzyStr;
    }

    public final void setAttributeFilters(AttributeFilter[] attributeFilters) {
        this.attributeFilters = attributeFilters;
    }

    public AttributeFilter getAttributeFilter(Entity entity) {
        return attributeFilters[entity.ordinal()];
    }
}