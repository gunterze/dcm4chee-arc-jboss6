package org.dcm4chee.archive.ejb.query.metadata;

import org.hibernate.criterion.Property;

public abstract class Instance_ {

    public static final Property pk = Property.forName("instance.pk");
    public static final Property contentDate = Property.forName("instance.contentDate");
    public static final Property contentTime = Property.forName("instance.contentTime");
    public static final Property sopClassUID = Property.forName("instance.sopClassUID");
    public static final Property sopInstanceUID = Property.forName("instance.sopInstanceUID");
    public static final Property instanceNumber = Property.forName("instance.instanceNumber");
    public static final Property verificationFlag = Property.forName("instance.verificationFlag");
    public static final Property retrieveAETs = Property.forName("instance.retrieveAETs");
    public static final Property externalRetrieveAET = Property.forName("instance.externalRetrieveAET");
    public static final Property availability = Property.forName("instance.availability");
    public static final Property instanceCustomAttribute1 = Property.forName("instance.instanceCustomAttribute1");
    public static final Property instanceCustomAttribute2 = Property.forName("instance.instanceCustomAttribute2");
    public static final Property instanceCustomAttribute3 = Property.forName("instance.instanceCustomAttribute3");
    public static final Property encodedAttributes = Property.forName("instance.encodedAttributes");

}
