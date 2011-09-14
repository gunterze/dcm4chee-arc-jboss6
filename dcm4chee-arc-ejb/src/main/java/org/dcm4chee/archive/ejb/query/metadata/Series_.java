package org.dcm4chee.archive.ejb.query.metadata;

import org.hibernate.criterion.Property;

public abstract class Series_ {

    public static final Property pk = Property.forName("series.pk");
    public static final Property modality = Property.forName("series.modality");
    public static final Property seriesNumber = Property.forName("series.seriesNumber");
    public static final Property seriesInstanceUID = Property.forName("series.seriesInstanceUID");
    public static final Property seriesDescription = Property.forName("series.seriesDescription");
    public static final Property performedProcedureStepStartDate = Property.forName("series.performedProcedureStepStartDate");
    public static final Property performedProcedureStepStartTime = Property.forName("series.performedProcedureStepStartTime");
    public static final Property numberOfSeriesRelatedInstances = Property.forName("series.numberOfSeriesRelatedInstances");
    public static final Property retrieveAETs = Property.forName("series.retrieveAETs");
    public static final Property externalRetrieveAET = Property.forName("series.externalRetrieveAET");
    public static final Property availability = Property.forName("series.availability");
    public static final Property seriesCustomAttribute1 = Property.forName("series.seriesCustomAttribute1");
    public static final Property seriesCustomAttribute2 = Property.forName("series.seriesCustomAttribute2");
    public static final Property seriesCustomAttribute3 = Property.forName("series.seriesCustomAttribute3");
    public static final Property encodedAttributes = Property.forName("series.encodedAttributes");

}
