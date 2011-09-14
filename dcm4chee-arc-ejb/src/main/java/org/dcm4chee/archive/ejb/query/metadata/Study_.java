package org.dcm4chee.archive.ejb.query.metadata;

import org.hibernate.criterion.Property;

public abstract class Study_ {

    public static final Property accessionNumber = Property.forName("study.accessionNumber");
    public static final Property studyInstanceUID = Property.forName("study.studyInstanceUID");
    public static final Property studyID = Property.forName("study.studyID");
    public static final Property studyDate = Property.forName("study.studyDate");
    public static final Property studyTime = Property.forName("study.studyTime");
    public static final Property studyDescription = Property.forName("study.studyDescription");
    public static final Property referringPhysicianFamilyNameSoundex = Property.forName("study.referringPhysicianFamilyNameSoundex");
    public static final Property referringPhysicianGivenNameSoundex = Property.forName("study.referringPhysicianGivenNameSoundex");
    public static final Property referringPhysicianName = Property.forName("study.referringPhysicianName");
    public static final Property referringPhysicianIdeographicName = Property.forName("study.referringPhysicianIdeographicName");
    public static final Property referringPhysicianPhoneticName = Property.forName("study.referringPhysicianPhoneticName");
    public static final Property numberOfStudyRelatedSeries = Property.forName("study.numberOfStudyRelatedSeries");
    public static final Property numberOfStudyRelatedInstances = Property.forName("study.numberOfStudyRelatedInstances");
    public static final Property modalitiesInStudy = Property.forName("study.modalitiesInStudy");
    public static final Property sopClassesInStudy = Property.forName("study.sopClassesInStudy");
    public static final Property retrieveAETs = Property.forName("study.retrieveAETs");
    public static final Property externalRetrieveAET = Property.forName("study.externalRetrieveAET");
    public static final Property availability = Property.forName("study.availability");
    public static final Property studyCustomAttribute1 = Property.forName("study.studyCustomAttribute1");
    public static final Property studyCustomAttribute2 = Property.forName("study.studyCustomAttribute2");
    public static final Property studyCustomAttribute3 = Property.forName("study.studyCustomAttribute3");
    public static final Property encodedAttributes = Property.forName("study.encodedAttributes");

}
