package org.dcm4chee.archive.ejb.store;

import org.dcm4chee.archive.persistence.Patient;

public class PatientMismatchException extends RuntimeException {

    private static final long serialVersionUID = 6053634947778779905L;

    public PatientMismatchException(Object source, Patient expected, Patient actual) {
        super("Expected " + expected + " associated by " + source
                + ", but was actually " + actual);
    }

    public static void check(Object source, Patient expected, Patient actual) {
        if (actual != expected)
            throw new PatientMismatchException(source, expected, actual);
    }
}
