package org.dcm4chee.archive.ejb.query;

public interface PatientNameQuery {

    public String[] query(IDWithIssuer[] pids);

}