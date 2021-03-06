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

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;

import org.dcm4che.conf.api.ConfigurationException;
import org.dcm4che.conf.api.DicomConfiguration;
import org.dcm4che.io.TemplatesCache;
import org.dcm4che.net.Device;
import org.dcm4che.net.hl7.HL7Device;
import org.dcm4che.soundex.FuzzyStr;
import org.dcm4chee.archive.ejb.store.Entity;
import org.dcm4chee.archive.ejb.store.StoreParam;
import org.dcm4chee.archive.persistence.AttributeFilter;


/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class ArchiveDevice extends HL7Device implements ArchiveDeviceMBean {

    private static final long serialVersionUID = 2933279846751009427L;

    private String fuzzyAlgorithmClass;
    private final AttributeFilter[] attributeFilters =
            new AttributeFilter[Entity.values().length];
    private int configurationStaleTimeout;

    private transient FuzzyStr fuzzyStr;
    private transient TemplatesCache templatesCache;
    private transient DicomConfiguration configuration;

    public ArchiveDevice(String name) {
        super(name);
    }

    public DicomConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(DicomConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void reloadConfiguration() throws ConfigurationException, IOException, GeneralSecurityException {
        DicomConfiguration tmp = configuration;
        if (tmp == null)
            throw new IllegalStateException("configuration not initalized");
        reconfigure(tmp.findDevice(getDeviceName()));
        rebindConnections();
    }

    public String getFuzzyAlgorithmClass() {
        return fuzzyAlgorithmClass;
    }

    public void setFuzzyAlgorithmClass(String fuzzyAlgorithmClass) {
        this.fuzzyStr = fuzzyStr(fuzzyAlgorithmClass);
        this.fuzzyAlgorithmClass = fuzzyAlgorithmClass;
    }

    public FuzzyStr getFuzzyStr() {
        if (fuzzyStr == null)
            if (fuzzyAlgorithmClass == null)
                throw new IllegalStateException("No Fuzzy Algorithm Class configured");
            else
                fuzzyStr = fuzzyStr(fuzzyAlgorithmClass);
        return fuzzyStr;
    }

    private static FuzzyStr fuzzyStr(String s) {
        try {
            return (FuzzyStr) Class.forName(s).newInstance();
         } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(s);
        }
    }
    public int getConfigurationStaleTimeout() {
        return configurationStaleTimeout;
    }

    public void setConfigurationStaleTimeout(int configurationStaleTimeout) {
        this.configurationStaleTimeout = configurationStaleTimeout;
    }

    public void clearTemplatesCache() {
        TemplatesCache cache = templatesCache;
        if (cache != null)
            cache.clear();
    }

    public Templates getTemplates(String uri) throws TransformerConfigurationException {
        TemplatesCache tmp = templatesCache;
        if (tmp == null)
            templatesCache = tmp = new TemplatesCache();
        return tmp.get(uri);
    }

    public void setAttributeFilter(Entity entity, AttributeFilter filter) {
        attributeFilters[entity.ordinal()] = filter;
    }

    public AttributeFilter getAttributeFilter(Entity entity) {
        return attributeFilters[entity.ordinal()];
    }

    public AttributeFilter[] getAttributeFilters() {
        return attributeFilters;
    }

    public StoreParam getStoreParam() {
        StoreParam storeParam = new StoreParam();
        storeParam.setFuzzyStr(fuzzyStr);
        storeParam.setAttributeFilters(attributeFilters);
        return storeParam;
    }

    @Override
    protected void setDeviceAttributes(Device from) {
        super.setDeviceAttributes(from);
        ArchiveDevice arcdev = (ArchiveDevice) from;
        setFuzzyAlgorithmClass(arcdev.fuzzyAlgorithmClass);
        setConfigurationStaleTimeout(arcdev.configurationStaleTimeout);
        System.arraycopy(arcdev.attributeFilters, 0,
                attributeFilters, 0, attributeFilters.length);
    }
}
