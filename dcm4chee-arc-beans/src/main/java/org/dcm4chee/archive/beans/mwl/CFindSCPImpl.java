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

package org.dcm4chee.archive.beans.mwl;


import java.util.EnumSet;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.net.ApplicationEntity;
import org.dcm4che.net.Association;
import org.dcm4che.net.Device;
import org.dcm4che.net.Status;
import org.dcm4che.net.pdu.ExtendedNegotiation;
import org.dcm4che.net.pdu.PresentationContext;
import org.dcm4che.net.pdu.QueryOption;
import org.dcm4che.net.service.BasicCFindSCP;
import org.dcm4che.net.service.DicomServiceException;
import org.dcm4che.net.service.QueryTask;
import org.dcm4chee.archive.beans.util.Configuration;
import org.dcm4chee.archive.beans.util.JNDIUtils;
import org.dcm4chee.archive.ejb.query.IDWithIssuer;
import org.dcm4chee.archive.ejb.query.ModalityWorklistQuery;
import org.dcm4chee.archive.ejb.query.QueryParam;
import org.dcm4chee.archive.persistence.Issuer;
import org.dcm4chee.archive.persistence.StoreParam;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class CFindSCPImpl extends BasicCFindSCP {

    public CFindSCPImpl(Device device, String sopClass) {
        super(device, sopClass);
    }

    @Override
    protected QueryTask calculateMatches(Association as, PresentationContext pc,
            Attributes rq, Attributes keys) throws DicomServiceException {
        String cuid = rq.getString(Tag.AffectedSOPClassUID);
        ExtendedNegotiation extNeg = as.getAAssociateAC().getExtNegotiationFor(cuid);
        IDWithIssuer[] pids = pids(keys);
        ApplicationEntity ae = as.getApplicationEntity();
        StoreParam storeParam = Configuration.storeParamFor(ae);
        EnumSet<QueryOption> queryOpts = QueryOption.toOptions(extNeg);
        QueryParam queryParam = new QueryParam();
        queryParam.setCombinedDatetimeMatching(true);
        queryParam.setFuzzySemanticMatching(queryOpts.contains(QueryOption.FUZZY));
        try {
            ModalityWorklistQuery query = (ModalityWorklistQuery)
                    JNDIUtils.lookup(ModalityWorklistQuery.JNDI_NAME);
            query.findScheduledProcedureSteps(pids, keys, queryParam, storeParam);
            return new QueryTaskImpl(as, pc, rq, keys, query);
        } catch (Exception e) {
            throw new DicomServiceException(Status.UnableToProcess, e);
        }
    }

    static IDWithIssuer[] pids(Attributes keys) {
        String id = keys.getString(Tag.PatientID, "*");
        if (id.equals("*"))
            return null;

        String entityID = keys.getString(Tag.IssuerOfPatientID, "*");
        Attributes issuerItem = keys.getNestedDataset(Tag.IssuerOfPatientIDQualifiersSequence);
        String entityUID = issuerItem != null
                ? issuerItem.getString(Tag.UniversalEntityID, "*")
                : "*";
        String entityUIDType = issuerItem != null
                ? issuerItem.getString(Tag.UniversalEntityIDType, "*")
                : "*";
        Issuer issuer = entityID.equals("*")
                     && entityUID.equals("*")
                     && entityUIDType.equals("*")
                     ? null
                     : new Issuer(entityID, entityUID, entityUIDType);
        return new IDWithIssuer[] { new IDWithIssuer(id, issuer) };
    }
}
