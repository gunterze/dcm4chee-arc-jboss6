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
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;
import org.dcm4che.net.ApplicationEntity;
import org.dcm4che.net.Association;
import org.dcm4che.net.Status;
import org.dcm4che.net.pdu.ExtendedNegotiation;
import org.dcm4che.net.pdu.PresentationContext;
import org.dcm4che.net.pdu.QueryOption;
import org.dcm4che.net.service.BasicQueryTask;
import org.dcm4che.net.service.DicomServiceException;
import org.dcm4chee.archive.beans.util.Configuration;
import org.dcm4chee.archive.ejb.query.CompositeQuery;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class QueryTaskImpl extends BasicQueryTask {

    private final CompositeQuery query;

    public QueryTaskImpl(Association as, PresentationContext pc, Attributes rq,
            Attributes keys, CompositeQuery query) throws DicomServiceException {
        super(as, pc, rq, keys);
        String cuid = rq.getString(Tag.AffectedSOPClassUID, null);
        ExtendedNegotiation extNeg = as.getAAssociateAC().getExtNegotiationFor(cuid);
        ApplicationEntity ae = as.getApplicationEntity();
        try {
            query.find(pids(keys), keys, Configuration.attributeFilterFor(ae),
                    QueryOption.toOptions(extNeg), roles());
        } catch (Exception e) {
            throw wrapException(Status.UnableToProcess, e);
        }
        this.query = query;
    }

    @Override
    protected Attributes adjust(Attributes match, Attributes keys, Association as) {
        Attributes filtered = new Attributes(match.size());
        filtered.setString(Tag.QueryRetrieveLevel, VR.CS,
                keys.getString(Tag.QueryRetrieveLevel, null));
        filtered.addSelected(match, Tag.SpecificCharacterSet,
                Tag.RetrieveAETitle, Tag.InstanceAvailability);
        filtered.addSelected(match, keys);
        return filtered;
     }

    @Override
    protected void close() {
         query.close();
    }

    @Override
    protected boolean hasMoreMatches() throws DicomServiceException {
        try {
            return query.hasMoreMatches();
        }  catch (Exception e) {
            throw wrapException(Status.UnableToProcess, e);
        }
    }

    @Override
    protected Attributes nextMatch() throws DicomServiceException {
        try {
            return query.nextMatch();
        }  catch (Exception e) {
            throw wrapException(Status.UnableToProcess, e);
        }
    }

    @Override
    protected boolean optionalKeyNotSupported(Attributes match, Attributes keys) {
        return query.optionalKeyNotSupported();
    }

    private String[] roles() {
        // TODO Auto-generated method stub
        return null;
    }

    private String[] pids(Attributes keys) {
        String pid = keys.getString(Tag.PatientID, "*");
        return pid.equals("*")
                ? null 
                : new String[] { 
                    pid,
                    keys.getString(Tag.IssuerOfPatientID, "*")};
    }
}