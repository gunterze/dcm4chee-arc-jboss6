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
 * Portions created by the Initial Developer are Copyright (C) 2012
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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;

import org.dcm4che.net.hl7.HL7Application;
import org.dcm4chee.archive.ejb.store.StoreParam;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class ArchiveHL7Application extends HL7Application {

    private final LinkedHashMap<String, String> templatesURIs =
            new LinkedHashMap<String, String>();

    public ArchiveHL7Application(String name) {
        super(name);
    }

    public final ArchiveDevice getArchiveDevice() {
        return ((ArchiveDevice) getDevice());
    }

    public void addTemplatesURI(String key, String uri) {
        templatesURIs.put(key, uri);
    }

    public String getTemplatesURI(String key) {
        return templatesURIs.get(key);
    }

    public String removeTemplatesURI(String key) {
        return templatesURIs.remove(key);
    }

    public void clearTemplatesURIs() {
        templatesURIs.clear();
    }

    public final String[] getTemplatesURIs() {
        String[] ss = new String[templatesURIs.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : templatesURIs.entrySet())
            ss[i++] = entry.getValue() + " " + entry.getKey();
        return ss ;
    }

    public void setTemplatesURIs(String[] ss) {
        clearTemplatesURIs();
        for (String s : ss) {
            int end = s.indexOf(' ');
            addTemplatesURI(s.substring(end+1), s.substring(0, end));
        }
    }

    public Templates getTemplates(String key) throws TransformerConfigurationException {
        String uri = getTemplatesURI(key);
        if (uri == null)
            throw new TransformerConfigurationException(
                    "No templates for " + key + " configured");
        return uri != null ? getArchiveDevice().getTemplates(uri) : null;
    }

    public StoreParam getStoreParam() {
        return getArchiveDevice().getStoreParam();
    }

}
