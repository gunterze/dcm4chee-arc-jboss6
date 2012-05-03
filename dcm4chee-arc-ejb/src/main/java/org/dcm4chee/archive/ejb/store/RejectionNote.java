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

package org.dcm4chee.archive.ejb.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4chee.archive.persistence.Code;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class RejectionNote implements Serializable {

    private static final long serialVersionUID = -1299501804796953248L;

    public enum Action {
        HIDE_REJECTED_INSTANCES,
        HIDE_REJECTION_NOTE,
        NOT_ACCEPT_SUBSEQUENT_OCCURRENCE,
        NOT_REJECT_SUBSEQUENT_OCCURRENCE
    }

    private final String codeValue;
    private final String codingSchemeDesignator;
    private final String codingSchemeVersion;
    private final String codeMeaning;
    private final EnumSet<Action> actions = EnumSet.noneOf(Action.class);
    private Code code;

    public RejectionNote(String codeValue, String codingSchemeDesignator,
            String codingSchemeVersion, String codeMeaning) {
        this.codeValue = codeValue;
        this.codingSchemeDesignator = codingSchemeDesignator;
        this.codingSchemeVersion = codingSchemeVersion;
        this.codeMeaning = codeMeaning;
    }

    public final String getCodeValue() {
        return codeValue;
    }

    public final String getCodingSchemeDesignator() {
        return codingSchemeDesignator;
    }

    public final String getCodingSchemeVersion() {
        return codingSchemeVersion;
    }

    public final String getCodeMeaning() {
        return codeMeaning;
    }

    public final Code getCode() {
        return code;
    }

    public final void setCode(Code code) {
        this.code = code;
    }

    public final EnumSet<Action> getActions() {
        return actions;
    }

    public RejectionNote addAction(Action action) {
        actions.add(action);
        return this;
    }

    public boolean matches(Code code) {
        return matches(code.getCodeValue(),
                code.getCodingSchemeDesignator(),
                code.getCodingSchemeVersion());
    }

    public boolean matches(Attributes codeItem) {
        return matches(codeItem.getString(Tag.CodeValue),
                codeItem.getString(Tag.CodingSchemeDesignator),
                codeItem.getString(Tag.CodingSchemeVersion));
    }

    public boolean matches(RejectionNote other) {
        return matches(other.codeValue, 
                other.codingSchemeDesignator,
                other.codingSchemeVersion);
    }

    public boolean matches(String codeValue, String codingSchemeDesignator,
            String codingSchemeVersion) {
        if (!this.codeValue.equals(codeValue))
            return false;
        if (!this.codingSchemeDesignator.equals(codingSchemeDesignator))
            return false;
        if (this.codingSchemeVersion != null
                ? !this.codingSchemeVersion.equals(codingSchemeVersion)
                : codingSchemeVersion != null)
            return false;
        return true;
    }

    public static List<RejectionNote> selectByAction(List<RejectionNote> rns,
            Action action) {
        List<RejectionNote> list = new ArrayList<RejectionNote>(rns.size());
        for (RejectionNote rn : rns) 
            if (rn.getActions().contains(action))
                list.add(rn);
        return list;
    }


}
