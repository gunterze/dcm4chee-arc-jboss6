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

package org.dcm4chee.archive.net.service;

import java.io.IOException;

import javax.ejb.EJB;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

import org.dcm4che.conf.api.DicomConfiguration;
import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.UID;
import org.dcm4che.net.ApplicationEntity;
import org.dcm4che.net.Association;
import org.dcm4che.net.DimseRSP;
import org.dcm4che.net.Status;
import org.dcm4che.net.TransferCapability;
import org.dcm4che.net.pdu.AAssociateRQ;
import org.dcm4che.net.pdu.PresentationContext;
import org.dcm4che.net.service.BasicMppsSCP;
import org.dcm4che.net.service.DicomServiceException;
import org.dcm4chee.archive.ejb.store.EntityAlreadyExistsException;
import org.dcm4chee.archive.ejb.store.EntityNotExistsException;
import org.dcm4chee.archive.ejb.store.IllegalEntityStateException;
import org.dcm4chee.archive.ejb.store.PerformedProcedureStepManager;
import org.dcm4chee.archive.ejb.store.StoreParam;
import org.dcm4chee.archive.net.ArchiveApplicationEntity;
import org.dcm4chee.archive.net.ArchiveDevice;
import org.dcm4chee.archive.net.service.JMSService.MessageCreator;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class MppsSCPImpl extends BasicMppsSCP implements MessageListener {

    @EJB
    private PerformedProcedureStepManager ppsmgr;
    
    private ArchiveDevice device;
    private DicomConfiguration dicomConfiguration;
    private JMSService jmsService;
    private Queue queue;

    public final ArchiveDevice getDevice() {
        return device;
    }

    public final void setDevice(ArchiveDevice device) {
        this.device = device;
    }

    public final DicomConfiguration getDicomConfiguration() {
        return dicomConfiguration;
    }

    public final void setDicomConfiguration(DicomConfiguration dicomConfiguration) {
        this.dicomConfiguration = dicomConfiguration;
    }

    public final JMSService getJmsService() {
        return jmsService;
    }

    public final void setJmsService(JMSService jmsService) {
        this.jmsService = jmsService;
    }

    public final Queue getQueue() {
        return queue;
    }

    public final void setQueue(Queue queue) {
        this.queue = queue;
    }

    public void start() throws JMSException {
        jmsService.addMessageListener(queue, this);
    }

    public void stop() throws JMSException {
        jmsService.removeMessageListener(this);
    }

    @Override
    protected Attributes create(Association as, Attributes rq,
            Attributes rqAttrs, Attributes rsp) throws DicomServiceException {
        String localAET = as.getLocalAET();
        String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
        ArchiveApplicationEntity ae = (ArchiveApplicationEntity) as.getApplicationEntity();
        StoreParam storeParam = ae.getStoreParam();
        try {
            for (String remoteAET : ae.getForwardMPPSDestinations())
                scheduleForwardMPPS(localAET, remoteAET, iuid, rqAttrs, true, 0, 0);
            ppsmgr.createPerformedProcedureStep(iuid , rqAttrs, storeParam);
        } catch (EntityAlreadyExistsException e) {
            throw new DicomServiceException(Status.DuplicateSOPinstance)
                .setUID(Tag.AffectedSOPInstanceUID, iuid);
        } catch (Exception e) {
            throw new DicomServiceException(Status.ProcessingFailure, e);
        }
        return null;
    }

    @Override
    protected Attributes set(Association as, Attributes rq, Attributes rqAttrs,
            Attributes rsp) throws DicomServiceException {
        String localAET = as.getLocalAET();
        String iuid = rq.getString(Tag.RequestedSOPInstanceUID);
        ArchiveApplicationEntity ae = (ArchiveApplicationEntity) as.getApplicationEntity();
        StoreParam storeParam = ae.getStoreParam();
        try {
            for (String remoteAET : ae.getForwardMPPSDestinations())
                scheduleForwardMPPS(localAET, remoteAET, iuid, rqAttrs, false, 0, 0);
            ppsmgr.updatePerformedProcedureStep(iuid , rqAttrs, storeParam);
        } catch (EntityNotExistsException e) {
            throw new DicomServiceException(Status.NoSuchObjectInstance)
                .setUID(Tag.AffectedSOPInstanceUID, iuid);
        } catch (IllegalEntityStateException e) {
            throw new DicomServiceException(Status.ProcessingFailure,
                    NOT_IN_PROGRESS_ERROR_COMMENT)
                .setErrorID(NOT_IN_PROGRESS_ERROR_ID);
        } catch (Exception e) {
            throw new DicomServiceException(Status.ProcessingFailure, e);
        }
        return null;
    }

    private void scheduleForwardMPPS(final String localAET, final String remoteAET,
            final String iuid, final Attributes rqAttrs, final boolean ncreate,
            final int retries, int delay) throws JMSException {
        jmsService.sendMessage(queue, new MessageCreator() {

            @Override
            public Message createMessage(Session session) throws JMSException {
                ObjectMessage msg = session.createObjectMessage(rqAttrs);
                msg.setStringProperty("LocalAET", localAET);
                msg.setStringProperty("RemoteAET", remoteAET);
                msg.setStringProperty("SOPInstancesUID", iuid);
                msg.setBooleanProperty("N_CREATE_RQ", ncreate);
                msg.setIntProperty("Retries", retries);
                return msg;
            }},
         delay);
    }

    @Override
    public void onMessage(Message msg) {
        try {
            process((ObjectMessage) msg);
        } catch (Throwable th) {
            LOG.warn("Failed to process " + msg, th);
        }
    }

    private void process(ObjectMessage msg) throws JMSException {
        String remoteAET = msg.getStringProperty("RemoteAET");
        String localAET = msg.getStringProperty("LocalAET");
        String iuid = msg.getStringProperty("SOPInstancesUID");
        boolean ncreate = msg.getBooleanProperty("N_CREATE_RQ");
        int retries = msg.getIntProperty("Retries");
        Attributes rqAttrs = (Attributes) msg.getObject();
        ArchiveApplicationEntity localAE =
                (ArchiveApplicationEntity) device.getApplicationEntity(localAET);
        if (localAE == null) {
            LOG.warn("Failed to forward MPPS to {} - no such local AE: {}",
                    remoteAET, localAET);
            return;
        }
        TransferCapability tc = localAE.getTransferCapabilityFor(
                UID.ModalityPerformedProcedureStepSOPClass, TransferCapability.Role.SCU);
        if (tc == null) {
            LOG.warn("Failed to forward MPPS to {} - "
                   + "local AE: {} does not support Modality Performed Procedure Step SOP Class in SCU Role",
                    remoteAET, localAET);
            return;
        }
        AAssociateRQ aarq = new AAssociateRQ();
        aarq.addPresentationContext(
                        new PresentationContext(
                                1,
                                UID.ModalityPerformedProcedureStepSOPClass,
                                tc.getTransferSyntaxes()));
        try {
            ApplicationEntity remoteAE = dicomConfiguration.findApplicationEntity(remoteAET);
            Association as = localAE.connect(remoteAE, aarq);
            DimseRSP rsp = ncreate 
                    ? as.ncreate(UID.ModalityPerformedProcedureStepSOPClass, iuid, rqAttrs, null)
                    : as.nset(UID.ModalityPerformedProcedureStepSOPClass, iuid, rqAttrs, null);
            rsp.next();
            try {
                as.release();
            } catch (IOException e) {
                LOG.info("{}: Failed to release association to {}", as, remoteAET);
            }
            if (!ncreate && rsp.getCommand().getInt(Tag.Status, -1 ) == Status.NoSuchObjectInstance) {
                throw new DicomServiceException(Status.NoSuchObjectInstance);
            }
        } catch (Exception e) {
            if (retries < localAE.getForwardMPPSMaxRetries()) {
                int delay = localAE.getForwardMPPSRetryInterval();
                LOG.info("Failed to forward MPPS to "
                            + remoteAET + " - retry in "  + delay + "s", e);
                scheduleForwardMPPS(localAET, remoteAET, iuid, rqAttrs, ncreate, retries + 1, delay);
            } else {
                LOG.warn("Failed to return Storage Commitment Result to " + remoteAET, e);
            }
        }
    }

}
