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

package org.dcm4chee.archive.query;

import static org.junit.Assert.*;
import javax.ejb.EJB;

import org.dcm4che.io.SAXReader;
import org.dcm4chee.archive.domain.Availability;
import org.dcm4chee.archive.store.InstanceStore;
import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
@RunWith(Arquillian.class)
public class SeriesQueryTest {

    private static int remainingTests = 1;
    private static long[] patientPKs;

    @Deployment
    public static JavaArchive createDeployment() {
       return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(SeriesQuery.class,
                        Matching.class,
                        InstanceStore.class)
                .addAsResource("scsh31.xml");
    }

    @EJB
    private InstanceStore instanceStore;

    @EJB
    private SeriesQuery query;

    @Before
    public void storeTestData() throws Exception {
        // emulates @BeforeClass
        if (patientPKs == null) {
            patientPKs = new long[]{
                instanceStore.store(
                    SAXReader.parse("resource:scsh31.xml", null),
                    Availability.ONLINE)
                    .getSeries().getStudy().getPatient().getPk()
            };
        }
    }

    @After
    public void clearTestData() {
        // emulates @AfterClass
        if (--remainingTests <= 0)
            for (long pk : patientPKs) {
                instanceStore.removePatient(pk);
            }
    }

    @Test
    public void testByPatientID() throws Exception {
        query.find(new String[] { "H31EXAMPLE", null },
                null, false);
        assertTrue(query.hasNext());
        query.next();
        assertFalse(query.hasNext());
        query.close();
    }

}
