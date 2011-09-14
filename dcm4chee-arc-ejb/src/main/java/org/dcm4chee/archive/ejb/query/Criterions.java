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

package org.dcm4chee.archive.ejb.query;

import java.util.EnumSet;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Sequence;
import org.dcm4che.data.Tag;
import org.dcm4che.net.pdu.QueryOption;
import org.dcm4chee.archive.ejb.query.metadata.Instance_;
import org.dcm4chee.archive.ejb.query.metadata.Patient_;
import org.dcm4chee.archive.ejb.query.metadata.Series_;
import org.dcm4chee.archive.ejb.query.metadata.Study_;
import org.dcm4chee.archive.persistence.Action;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
abstract class Criterions {

    static Criterion matchPatient(String[] pids, Attributes keys, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts) {
        Conjunction predicates = Restrictions.conjunction();
        addPatientMatch(pids, keys, filter, queryOpts, predicates);
        return predicates;
    }

    static Criterion matchStudy(String[] pids, Attributes keys, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, String[] roles) {
        Conjunction predicates = Restrictions.conjunction();
        addPatientMatch(pids, keys, filter, queryOpts, predicates);
        addStudyMatch(keys, filter, queryOpts, roles, predicates);
        return predicates;
    }

    static Criterion matchSeries(String[] pids, Attributes keys, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, String[] roles) {
        Conjunction predicates = Restrictions.conjunction();
        addPatientMatch(pids, keys, filter, queryOpts, predicates);
        addStudyMatch(keys, filter, queryOpts, roles, predicates);
        addSeriesMatch(keys, filter, queryOpts, roles, predicates);
        return predicates;
    }

    static Criterion matchInstance(String[] pids, Attributes keys, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, String[] roles) {
        Conjunction predicates = Restrictions.conjunction();
        addPatientMatch(pids, keys, filter, queryOpts, predicates);
        addStudyMatch(keys, filter, queryOpts, roles, predicates);
        addSeriesMatch(keys, filter, queryOpts, roles, predicates);
        addInstanceMatch(keys, filter, queryOpts, roles, predicates);
        return predicates;
    }

    static Criterion matchInstanceByUIDs(String[] pids, Attributes keys) {
        Conjunction predicates = Restrictions.conjunction();
        addPatientIDMatch(pids, false, predicates );
        addListOfUIDMatch(Study_.studyInstanceUID, keys.getStrings(Tag.StudyInstanceUID), predicates);
        addListOfUIDMatch(Series_.seriesInstanceUID, keys.getStrings(Tag.SeriesInstanceUID), predicates);
        addListOfUIDMatch(Instance_.sopInstanceUID, keys.getStrings(Tag.SOPInstanceUID), predicates);
        return predicates;
    }

    private static void addPatientMatch(String[] pids, Attributes keys,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts, Conjunction predicates) {
        boolean matchUnknown = filter.isMatchUnknown();
        addPatientIDMatch(pids, matchUnknown, predicates);

        if (keys == null)
            return;

        PersonNameMatching.addMatch(Patient_.patientName,
                    Patient_.patientIdeographicName,
                    Patient_.patientPhoneticName,
                    Patient_.patientFamilyNameSoundex,
                    Patient_.patientGivenNameSoundex,
                    filter.getString(keys, Tag.PatientName),
                    filter, queryOpts, matchUnknown, predicates);
        addWildCardMatch(Patient_.patientSex, filter.getString(keys, Tag.PatientSex),
                matchUnknown, predicates);
        RangeMatching.addMatch(Patient_.patientBirthDate, Tag.PatientBirthDate,
                RangeMatching.FormatDate.DA, keys, matchUnknown, predicates);
        addWildCardMatch(Patient_.patientCustomAttribute1,
                filter.selectPatientCustomAttribute1(keys), matchUnknown, predicates);
        addWildCardMatch(Patient_.patientCustomAttribute2,
                filter.selectPatientCustomAttribute2(keys), matchUnknown, predicates);
        addWildCardMatch(Patient_.patientCustomAttribute3,
                filter.selectPatientCustomAttribute3(keys), matchUnknown, predicates);
    }

   private static void addStudyMatch(Attributes keys, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, String[] roles, Conjunction predicates) {
        if (keys == null)
            return;

        boolean matchUnknown = filter.isMatchUnknown();
        addListOfUIDMatch(Study_.studyInstanceUID, keys.getStrings(Tag.StudyInstanceUID), predicates);
        addWildCardMatch(Study_.studyID, filter.getString(keys, Tag.StudyID), matchUnknown, predicates);
        PersonNameMatching.addMatch(Study_.referringPhysicianName,
                Study_.referringPhysicianIdeographicName,
                Study_.referringPhysicianPhoneticName,
                Study_.referringPhysicianFamilyNameSoundex,
                Study_.referringPhysicianGivenNameSoundex,
                filter.getString(keys, Tag.ReferringPhysicianName),
                filter, queryOpts, matchUnknown, predicates);
        RangeMatching.addMatch(Study_.studyDate, Study_.studyTime,
                Tag.StudyDate, Tag.StudyTime, Tag.StudyDateAndTime,
                keys, queryOpts, matchUnknown, predicates);
        addWildCardMatch(Study_.studyDescription, filter.getString(keys, Tag.StudyDescription), matchUnknown, predicates);
        String accNo = filter.getString(keys, Tag.AccessionNumber);
        addWildCardMatch(Study_.accessionNumber, accNo, matchUnknown, predicates);
        if(!accNo.equals("*"))
            addIssuerMatch("study.issuerOfAccessionNumber",
                    keys.getNestedDataset(Tag.IssuerOfAccessionNumberSequence),
                    filter, matchUnknown, predicates);
        addModalitiesInStudyMatch(filter.getString(keys, Tag.ModalitiesInStudy),
                matchUnknown, predicates);
        addCodeMatch("study.procedureCodes", keys.getNestedDataset(Tag.ProcedureCodeSequence),
                filter, matchUnknown, predicates);
        addWildCardMatch(Study_.studyCustomAttribute1,
                filter.selectStudyCustomAttribute1(keys), matchUnknown, predicates);
        addWildCardMatch(Study_.studyCustomAttribute2,
                filter.selectStudyCustomAttribute2(keys), matchUnknown, predicates);
        addWildCardMatch(Study_.studyCustomAttribute3,
                filter.selectStudyCustomAttribute3(keys), matchUnknown, predicates);
        addPermissionMatch(roles, Action.QUERY, predicates);
    }

    private static void addSeriesMatch(Attributes keys, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, String[] roles, Conjunction predicates) {
        if (keys == null)
            return;

        boolean matchUnknown = filter.isMatchUnknown();
        addListOfUIDMatch(Series_.seriesInstanceUID, keys.getStrings(Tag.SeriesInstanceUID), predicates);
        addWildCardMatch(Series_.seriesCustomAttribute1,
                filter.selectSeriesCustomAttribute1(keys), matchUnknown, predicates);
        addWildCardMatch(Series_.modality,
                filter.getString(keys, Tag.Modality), matchUnknown, predicates);
        RangeMatching.addMatch(
                Series_.performedProcedureStepStartDate,
                Series_.performedProcedureStepStartTime,
                Tag.PerformedProcedureStepStartDate,
                Tag.PerformedProcedureStepStartTime,
                Tag.PerformedProcedureStepStartDateAndTime, keys, queryOpts,
                matchUnknown, predicates);
        addWildCardMatch(Series_.seriesNumber,
                filter.getString(keys, Tag.SeriesNumber), matchUnknown, predicates);
        addWildCardMatch(Series_.seriesDescription,
                filter.getString(keys, Tag.SeriesDescription), matchUnknown, predicates);
        addRequestAttributesMatch(
                keys.getNestedDataset(Tag.RequestAttributesSequence), filter, queryOpts, 
                matchUnknown, predicates);
        addCodeMatch("series_.institutionCode",
                keys.getNestedDataset(Tag.InstitutionCodeSequence), filter,
                matchUnknown, predicates);
        addWildCardMatch(Series_.seriesCustomAttribute1,
                filter.selectSeriesCustomAttribute1(keys), matchUnknown, predicates);
        addWildCardMatch(Series_.seriesCustomAttribute2,
                filter.selectSeriesCustomAttribute2(keys), matchUnknown, predicates);
        addWildCardMatch(Series_.seriesCustomAttribute3,
                filter.selectSeriesCustomAttribute3(keys), matchUnknown, predicates);
    }

    private static void addInstanceMatch(Attributes keys, AttributeFilter filter,
            EnumSet<QueryOption> queryOpts, String[] roles, Conjunction predicates) {
        if (keys == null)
            return;

        boolean matchUnknown = filter.isMatchUnknown();
        addListOfUIDMatch(Instance_.sopInstanceUID, keys.getStrings(Tag.SOPInstanceUID), predicates);
        addListOfUIDMatch(Instance_.sopClassUID, keys.getStrings(Tag.SOPClassUID), predicates);
        addWildCardMatch(Instance_.instanceNumber,
                filter.getString(keys, Tag.InstanceNumber), matchUnknown, predicates);
        addWildCardMatch(Instance_.verificationFlag,
                filter.getString(keys, Tag.VerificationFlag), matchUnknown, predicates);
        RangeMatching.addMatch(Instance_.contentDate, Instance_.contentTime,
                Tag.ContentDate, Tag.ContentTime, 
                Tag.ContentDateAndTime, keys, queryOpts, 
                matchUnknown, predicates);
        addCodeMatch("instance_.conceptNameCode",
                keys.getNestedDataset(Tag.ConceptNameCodeSequence), filter,
                matchUnknown, predicates);
        addObserverMatch(keys.getNestedDataset(Tag.VerifyingObserverSequence),
                filter, queryOpts, matchUnknown, predicates);
        Sequence contentSeq = keys.getSequence(Tag.ContentSequence);
        if (contentSeq != null)
            for (Attributes item : contentSeq)
                ContentItemMatching.addMatch(item, filter, predicates);
        addWildCardMatch(Instance_.instanceCustomAttribute1,
                filter.selectInstanceCustomAttribute1(keys), matchUnknown, predicates);
        addWildCardMatch(Instance_.instanceCustomAttribute2,
                filter.selectInstanceCustomAttribute2(keys), matchUnknown, predicates);
        addWildCardMatch(Instance_.instanceCustomAttribute3,
                filter.selectInstanceCustomAttribute3(keys), matchUnknown, predicates);
    }

    private static boolean addPatientIDMatch(String[] pids, boolean matchUnknown,
            Conjunction predicates) {
        if (pids == null || pids.length == 0)
            return false;

        if (pids.length == 2)
            return addPatientIDMatch(pids[0], pids[1], matchUnknown, predicates);

        Disjunction or = Restrictions.disjunction();
        boolean result = false;
        for (int i = 0; i < pids.length-1; i++, i++) {
            Conjunction and = Restrictions.conjunction();
            if (addPatientIDMatch(pids[i], pids[i+1], matchUnknown, and)) {
                or.add(and);
                result = true;
            }
        }
        if (result)
            predicates.add(or);
        return result;
    }

    private static boolean addPatientIDMatch(String pid, String issuer, boolean matchUnknown,
            Conjunction predicates) {
        boolean result = addWildCardMatch(Patient_.patientID, pid, matchUnknown, predicates);
        result = addWildCardMatch(Patient_.issuerOfPatientID, issuer, matchUnknown, predicates)
                || result;
        return result;
    }

    static boolean addListOfUIDMatch(Property property, String[] values, Conjunction predicates) {
        if (values == null || values.length == 0 || values[0].equals("*"))
            return false;

        predicates.add(values.length == 1 ? property.eq(values[0]) : property.in(values));
        return true;
    }

    static boolean addWildCardMatch(Property property, String value, boolean matchUnknown,
            Conjunction predicates) {
        if (value.equals("*"))
            return false;

        Criterion criterion;
        if (containsWildcard(value)) {
            String like = toLikePattern(value);
            if (like.equals("%"))
                return false;

            criterion = property.like(like);
        } else
            criterion = property.eq(value);

        if (matchUnknown)
            criterion = Restrictions.or(criterion, property.eq("*"));

        predicates.add(criterion);
        return true;
    }

    private static boolean containsWildcard(String s) {
        return s.indexOf('*') >= 0 || s.indexOf('?') >= 0;
    }

    private static String toLikePattern(String s) {
        StringBuilder like = new StringBuilder(s.length());
        char[] cs = s.toCharArray();
        char p = 0;
        for (char c : cs) {
            switch (c) {
            case '*':
                if (c != p)
                    like.append('%');
                break;
            case '?':
                like.append('_');
                break;
            case '_':
            case '%':
                like.append('\\');
                // fall through
            default:
                like.append(c);
            }
            p = c;
        }
        return like.toString();
    }

    private static void addCodeMatch(String string, Attributes nestedDataset,
            AttributeFilter filter, boolean matchUnknown, Conjunction predicates) {
        // TODO Auto-generated method stub
        
    }

        private static void addModalitiesInStudyMatch(String string,
            boolean matchUnknown, Conjunction predicates) {
        // TODO Auto-generated method stub
        
    }

        private static void addIssuerMatch(String string, Attributes nestedDataset,
            AttributeFilter filter, boolean matchUnknown, Conjunction predicates) {
        // TODO Auto-generated method stub
        
    }

    private static void addPermissionMatch(String[] roles, Action query,
            Conjunction predicates) {
        // TODO Auto-generated method stub
        
    }

    private static void addRequestAttributesMatch(Attributes item,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts,
            boolean matchUnknown, Conjunction predicates) {
        // TODO Auto-generated method stub
        
    }

    private static void addObserverMatch(Attributes item,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts,
            boolean matchUnknown, Conjunction predicates) {
        // TODO Auto-generated method stub
        
    }


 }
