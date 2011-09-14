package org.dcm4chee.archive.ejb.query.metadata;

import org.hibernate.criterion.Property;

public abstract class FileRef_ {

    public static final Property transferSyntaxUID = Property.forName("fileRef.transferSyntaxUID");
    public static final Property filePath = Property.forName("fileRef.filePath");
    public static final Property digest = Property.forName("fileRef.digest");
}
