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

package org.dcm4chee.archive.beans.store;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.VR;
import org.dcm4che.io.DicomInputStream;
import org.dcm4che.net.ApplicationEntity;
import org.dcm4che.net.Association;
import org.dcm4che.net.Status;
import org.dcm4che.net.service.BasicCStoreSCP;
import org.dcm4che.net.service.DicomServiceException;
import org.dcm4che.util.FilePathFormat;
import org.dcm4che.util.SafeClose;
import org.dcm4che.util.TagUtils;
import org.dcm4chee.archive.beans.util.Configuration;
import org.dcm4chee.archive.beans.util.JNDIUtils;
import org.dcm4chee.archive.ejb.store.InstanceStore;
import org.dcm4chee.archive.persistence.FileRef;
import org.dcm4chee.archive.persistence.FileSystem;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
public class CStoreSCPImpl extends BasicCStoreSCP {

    private boolean initFileSystem = true;
 
    public CStoreSCPImpl(String... sopClasses) {
        super(sopClasses);
    }

    @Override
    protected Object selectStorage(Association as, Attributes rq) throws DicomServiceException {
        try {
            String fsGroupID = Configuration.fileSystemGroupIDFor(as.getApplicationEntity());
            InstanceStore store = initInstanceStore(as);
            if (initFileSystem) {
                store.initFileSystem(fsGroupID);
                initFileSystem = false;
            }
            return store.selectFileSystem(fsGroupID);
        } catch (Exception e) {
            LOG.warn(as + ": Failed to select filesystem:", e);
            throw new DicomServiceException(rq, Status.OutOfResources, e);
        }
    }

    @Override
    protected File createFile(Association as, Attributes rq, Object storage)
            throws DicomServiceException {
        try {
            FileSystem fs = (FileSystem) storage;
            File tmpDir = new File(fs.getDirectory(), "incoming");
            tmpDir.mkdirs();
            return File.createTempFile("dcm", ".dcm", tmpDir);
        } catch (Exception e) {
            LOG.warn(as + ": Failed to create temp file:", e);
            throw new DicomServiceException(rq, Status.OutOfResources, e);
        }
    }

    @Override
    protected MessageDigest getMessageDigest(Association as) {
        String algorithm = Configuration.messageDigestAlgorithmFor(as.getApplicationEntity());
        try {
            return algorithm != null ? MessageDigest.getInstance(algorithm) : null;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected File process(Association as, Attributes rq, String tsuid, Attributes rsp,
            Object storage, File file, MessageDigest digest) throws DicomServiceException {
        FileSystem fs = (FileSystem) storage;
        Attributes ds = readDataset(as, rq, file);
        ApplicationEntity ae = as.getApplicationEntity();
        FilePathFormat filePathFormatFor = Configuration.filePathFormatFor(ae);
        File dst = new File(fs.getDirectory(), filePathFormatFor.format(ds));
        File dir = dst.getParentFile();
        dir.mkdirs();
        while (dst.exists()) {
            dst = new File(dir, TagUtils.toHexString(new Random().nextInt()));
        }
        if (file.renameTo(dst))
            LOG.info(as + ": M-RENAME " + file + " to " + dst);
        else {
            LOG.warn(as + ": Failed to M-RENAME " + file + " to " + dst);
            throw new DicomServiceException(rq, Status.OutOfResources, "Failed to rename file");
        }
        String filePath = dst.toURI().toString().substring(fs.getURI().length());
        InstanceStore store = (InstanceStore) as.getProperty(InstanceStore.JNDI_NAME);
        try {
            ds.setString(InstanceStore.DCM4CHEE_ARC, InstanceStore.SOURCE_AET, VR.AE, as.getRemoteAET());
            ds.setString(Tag.RetrieveAETitle, VR.AE, as.getLocalAET());
            if (store.addFileRef(ds, rsp,
                    new FileRef(fs, filePath, tsuid, dst.length(), digest(digest)),
                    Configuration.storeParamFor(ae),
                    Configuration.storeDuplicateFor(ae)))
                return null;
        } catch (Exception e) {
            LOG.warn(as + ": Failed to update DB:", e);
            String errorComment = causeOf(e).getMessage();
            if (errorComment.length() > 64)
                errorComment = errorComment.substring(0, 64);
            rsp.setInt(Tag.Status, VR.US, Status.OutOfResources);
            rsp.setString(Tag.ErrorComment, VR.LO, errorComment);
        }
        return dst;
    }

    private String digest(MessageDigest digest) {
        return digest != null ? TagUtils.toHexString(digest.digest()) : null;
    }

    private Attributes readDataset(Association as, Attributes rq, File file)
            throws DicomServiceException {
        DicomInputStream in = null;
        try {
            in = new DicomInputStream(file);
            in.setIncludeBulkData(false);
            return in.readDataset(-1, Tag.PixelData);
        } catch (IOException e) {
            LOG.warn(as + ": Failed to decode dataset:", e);
            throw new DicomServiceException(rq, Status.CannotUnderstand);
        } finally {
            SafeClose.close(in);
        }
    }

    private static Throwable causeOf(Throwable e) {
        Throwable cause;
        while ((cause = e.getCause()) != null && e != cause)
            e = cause;
        return e;
    }

    private InstanceStore initInstanceStore(Association as) {
        InstanceStore store =
                    (InstanceStore) as.getProperty(InstanceStore.JNDI_NAME);
        if (store == null) {
            store = (InstanceStore) JNDIUtils.lookup(InstanceStore.JNDI_NAME);
            as.setProperty(InstanceStore.JNDI_NAME, store);
        }
        return store;
    }

    private void closeInstanceStore(Association as) {
        InstanceStore store =
                (InstanceStore) as.clearProperty(InstanceStore.JNDI_NAME);
        if (store != null)
            store.close();
    }

    @Override
    public void onClose(Association as) {
        closeInstanceStore(as);
    }
}
