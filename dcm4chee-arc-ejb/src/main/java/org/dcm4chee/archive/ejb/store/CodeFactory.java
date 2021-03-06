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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Sequence;
import org.dcm4che.data.Tag;
import org.dcm4chee.archive.persistence.Code;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public abstract class CodeFactory {

    public static Code getCode(EntityManager em, String codeValue,
            String codingSchemeDesignator, String codingSchemeVersion,
            String codeMeaning) {
        try {
            TypedQuery<Code> query = em.createNamedQuery(
                        codingSchemeVersion == null
                            ? Code.FIND_BY_CODE_VALUE_WITHOUT_SCHEME_VERSION
                            : Code.FIND_BY_CODE_VALUE_WITH_SCHEME_VERSION,
                        Code.class)
                    .setParameter(1, codeValue)
                    .setParameter(2, codingSchemeDesignator);
            if (codingSchemeVersion != null)
                query.setParameter(3, codingSchemeVersion);
            return query.getSingleResult();
        } catch (NoResultException e) {
            Code code = new Code(codeValue, codingSchemeDesignator,
                    codingSchemeVersion, codeMeaning);
            em.persist(code);
            return code;
        }
    }

    public static Code getCode(EntityManager em, Attributes codeItem) {
        return codeItem != null
                ? getCode(em,
                    codeItem.getString(Tag.CodeValue),
                    codeItem.getString(Tag.CodingSchemeDesignator),
                    codeItem.getString(Tag.CodingSchemeVersion),
                    codeItem.getString(Tag.CodeMeaning))
                : null;
    }

    public static Code getCode(EntityManager em, RejectionNote rn) {
        if (rn == null)
            return null;

        Code code = rn.getCode();
        if (code == null) {
            code = getCode(em,
                    rn.getCodeValue(),
                    rn.getCodingSchemeDesignator(),
                    rn.getCodingSchemeVersion(),
                    rn.getCodeMeaning());
            rn.setCode(code);
        }
        return code;
    }

    public static List<Code> createCodes(EntityManager em, Sequence seq) {
        if (seq == null || seq.isEmpty())
            return Collections.emptyList();

        ArrayList<Code> list = new ArrayList<Code>(seq.size());
        for (Attributes item : seq)
            list.add(CodeFactory.getCode(em, item));

        return list;
    }

    public static List<Code> createCodes(EntityManager em, List<RejectionNote> rns) {
        if (rns == null || rns.isEmpty())
            return Collections.emptyList();

        ArrayList<Code> list = new ArrayList<Code>(rns.size());
        for (RejectionNote rn : rns)
            list.add(CodeFactory.getCode(em, rn));

        return list;
    }

}
