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

package org.dcm4chee.archive.net.service;


import java.util.EnumSet;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.net.Association;
import org.dcm4che.net.Device;
import org.dcm4che.net.QueryOption;
import org.dcm4che.net.Status;
import org.dcm4che.net.pdu.ExtendedNegotiation;
import org.dcm4che.net.pdu.PresentationContext;
import org.dcm4che.net.service.BasicCFindSCP;
import org.dcm4che.net.service.DicomServiceException;
import org.dcm4che.net.service.QueryRetrieveLevel;
import org.dcm4che.net.service.QueryTask;
import org.dcm4che.util.AttributesValidator;
import org.dcm4chee.archive.ejb.query.CompositeQuery;
import org.dcm4chee.archive.ejb.query.IDWithIssuer;
import org.dcm4chee.archive.ejb.query.QueryParam;
import org.dcm4chee.archive.net.ArchiveApplicationEntity;
import org.dcm4chee.archive.net.ArchiveDevice;
import org.dcm4chee.archive.persistence.Issuer;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class CFindSCPImpl extends BasicCFindSCP {

    private final String[] qrLevels;
    private final QueryRetrieveLevel rootLevel;

    public CFindSCPImpl(Device device, String sopClass,
            String... qrLevels) {
        super(device, sopClass);
        this.qrLevels = qrLevels;
        this.rootLevel = QueryRetrieveLevel.valueOf(qrLevels[0]);
    }

    private final ArchiveDevice getArchiveDevice() {
        return (ArchiveDevice) device;
    }

    @Override
    protected QueryTask calculateMatches(Association as, PresentationContext pc,
            Attributes rq, Attributes keys) throws DicomServiceException {
        AttributesValidator validator = new AttributesValidator(keys);
        QueryRetrieveLevel level = QueryRetrieveLevel.valueOf(validator, qrLevels);
        String cuid = rq.getString(Tag.AffectedSOPClassUID);
        ExtendedNegotiation extNeg = as.getAAssociateAC().getExtNegotiationFor(cuid);
        boolean relational = QueryOption.toOptions(extNeg).contains(QueryOption.RELATIONAL);
        level.validateQueryKeys(validator, rootLevel, relational);
        IDWithIssuer[] pids = pids(keys);
        ArchiveApplicationEntity ae = (ArchiveApplicationEntity) as.getApplicationEntity();
        ArchiveDevice dev = getArchiveDevice();
        EnumSet<QueryOption> queryOpts = QueryOption.toOptions(extNeg);
        QueryParam queryParam = new QueryParam();
        queryParam.setCombinedDatetimeMatching(queryOpts.contains(QueryOption.DATETIME));
        queryParam.setFuzzySemanticMatching(queryOpts.contains(QueryOption.FUZZY));
        queryParam.setMatchUnknown(ae.isMatchUnknown());
        queryParam.setFuzzyStr(dev.getFuzzyStr());
        queryParam.setAttributeFilters(dev.getAttributeFilters());
        queryParam.setRoles(roles());
        try {
            CompositeQuery query = (CompositeQuery) JNDIUtils.lookup(CompositeQuery.JNDI_NAME);
            switch (level) {
            case PATIENT:
                query.findPatients(pids, keys, queryParam);
                break;
            case STUDY:
                query.findStudies(pids, keys, queryParam);
                break;
            case SERIES:
                query.findSeries(pids, keys, queryParam);
                break;
            default: // case IMAGE:
                query.findInstances(pids, keys, queryParam);
                break;
            }
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

    private String[] roles() {
        // TODO Auto-generated method stub
        return null;
    }
}
