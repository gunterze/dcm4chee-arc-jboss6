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

import java.util.List;

import javax.ejb.EJB;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.net.Association;
import org.dcm4che.net.Device;
import org.dcm4che.net.QueryOption;
import org.dcm4che.net.Status;
import org.dcm4che.net.pdu.ExtendedNegotiation;
import org.dcm4che.net.pdu.PresentationContext;
import org.dcm4che.net.service.BasicCGetSCP;
import org.dcm4che.net.service.DicomServiceException;
import org.dcm4che.net.service.InstanceLocator;
import org.dcm4che.net.service.QueryRetrieveLevel;
import org.dcm4che.net.service.RetrieveTask;
import org.dcm4che.util.AttributesValidator;
import org.dcm4chee.archive.ejb.query.LocateInstances;
import org.dcm4chee.archive.net.ArchiveApplicationEntity;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class CGetSCPImpl extends BasicCGetSCP {

    private final String[] qrLevels;
    private final QueryRetrieveLevel rootLevel;
    private final boolean withoutBulkData;

    @EJB
    private LocateInstances calculateMatches;

    public CGetSCPImpl(Device device, String[] sopClasses, String... qrLevels) {
        super(device, sopClasses);
        this.qrLevels = qrLevels;
        this.withoutBulkData = qrLevels.length == 0;
        this.rootLevel = withoutBulkData
                ? QueryRetrieveLevel.IMAGE
                : QueryRetrieveLevel.valueOf(qrLevels[0]);
   }

    @Override
    protected RetrieveTask calculateMatches(Association as, PresentationContext pc,
            Attributes rq, Attributes keys) throws DicomServiceException {
        AttributesValidator validator = new AttributesValidator(keys);
        QueryRetrieveLevel level = QueryRetrieveLevel.valueOf(validator, qrLevels);
        String cuid = rq.getString(Tag.AffectedSOPClassUID);
        ExtendedNegotiation extNeg = as.getAAssociateAC().getExtNegotiationFor(cuid);
        boolean relational = QueryOption.toOptions(extNeg).contains(QueryOption.RELATIONAL);
        level.validateRetrieveKeys(validator, rootLevel, relational);
        List<InstanceLocator> matches  = calculateMatches(rq, keys);
        ArchiveApplicationEntity ae = (ArchiveApplicationEntity) as.getApplicationEntity();
        RetrieveTaskImpl retrieveTask =
                new RetrieveTaskImpl(as, pc, rq, matches, withoutBulkData);
        retrieveTask.setSendPendingRSP(ae.isSendPendingCGet());
        return retrieveTask;
    }

    private List<InstanceLocator> calculateMatches(Attributes rq, Attributes keys)
            throws DicomServiceException {
        try {
            return calculateMatches.find(CFindSCPImpl.pids(keys), keys);
        }  catch (Exception e) {
            throw new DicomServiceException(Status.UnableToCalculateNumberOfMatches, e);
        }
    }
}
