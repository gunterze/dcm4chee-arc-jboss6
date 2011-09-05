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

package org.dcm4chee.archive.beans.qrscp;


import org.dcm4che.data.Attributes;
import org.dcm4che.data.AttributesValidator;
import org.dcm4che.data.Tag;
import org.dcm4che.net.Association;
import org.dcm4che.net.Device;
import org.dcm4che.net.Status;
import org.dcm4che.net.pdu.ExtendedNegotiation;
import org.dcm4che.net.pdu.PresentationContext;
import org.dcm4che.net.pdu.QueryOption;
import org.dcm4che.net.service.BasicCFindSCP;
import org.dcm4che.net.service.DicomServiceException;
import org.dcm4che.net.service.QueryRetrieveLevel;
import org.dcm4che.net.service.QueryTask;
import org.dcm4chee.archive.beans.util.JNDIUtils;
import org.dcm4chee.archive.ejb.query.CompositeQuery;
import org.dcm4chee.archive.ejb.query.InstanceQuery;
import org.dcm4chee.archive.ejb.query.PatientQuery;
import org.dcm4chee.archive.ejb.query.SeriesQuery;
import org.dcm4chee.archive.ejb.query.StudyQuery;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class CFindSCPImpl extends BasicCFindSCP {

    private final String[] qrLevels;
    private final boolean studyRoot;

    public CFindSCPImpl(Device device, String sopClass,
            String... qrLevels) {
        super(device, sopClass);
        this.qrLevels = qrLevels;
        this.studyRoot = "STUDY".equals(qrLevels[0]);
    }

    @Override
    protected QueryTask calculateMatches(Association as, PresentationContext pc,
            Attributes rq, Attributes keys) throws DicomServiceException {
        AttributesValidator validator = new AttributesValidator(keys);
        QueryRetrieveLevel level = QueryRetrieveLevel.valueOf(rq, validator, qrLevels);
        String cuid = rq.getString(Tag.AffectedSOPClassUID);
        ExtendedNegotiation extNeg = as.getAAssociateAC().getExtNegotiationFor(cuid);
        boolean relational = QueryOption.toOptions(extNeg).contains(QueryOption.RELATIONAL);
        level.validateQueryKeys(rq, validator, studyRoot, relational);
        try {
            return new QueryTaskImpl(as, pc, rq, keys,
                    (CompositeQuery) JNDIUtils.lookup(jndiNameOf(level)));
        } catch (Exception e) {
            throw new DicomServiceException(rq, Status.UnableToProcess, e);
        }
    }

    private String jndiNameOf(QueryRetrieveLevel level) {
        switch (level) {
        case PATIENT:
            return PatientQuery.JNDI_NAME;
        case STUDY:
            return StudyQuery.JNDI_NAME;
        case SERIES:
            return SeriesQuery.JNDI_NAME;
        case IMAGE:
            return InstanceQuery.JNDI_NAME;
        }
        throw new IllegalArgumentException(level.name());
    }
}
