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
import org.dcm4chee.archive.ejb.query.metadata.Code_;
import org.dcm4chee.archive.ejb.query.metadata.Instance_;
import org.dcm4chee.archive.ejb.query.metadata.Patient_;
import org.dcm4chee.archive.ejb.query.metadata.Series_;
import org.dcm4chee.archive.ejb.query.metadata.Study_;
import org.dcm4chee.archive.persistence.Action;
import org.dcm4chee.archive.persistence.AttributeFilter;
import org.dcm4chee.archive.persistence.Code;
import org.dcm4chee.archive.persistence.Series;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 */
abstract class Criterions {

    static void addPatientLevelCriteriaTo(Criteria criteria, String[] pids, Attributes keys,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts) {
        boolean matchUnknown = filter.isMatchUnknown();
        addTo(criteria, pid(pids, matchUnknown));
        if (keys == null)
            return;
        addTo(criteria, pn(Patient_.patientName,
                Patient_.patientIdeographicName,
                Patient_.patientPhoneticName,
                Patient_.patientFamilyNameSoundex,
                Patient_.patientGivenNameSoundex,
                filter.getString(keys, Tag.PatientName),
                filter, queryOpts.contains(QueryOption.FUZZY), matchUnknown));
        addTo(criteria, wc(Patient_.patientSex,
                filter.getString(keys, Tag.PatientSex), matchUnknown));
        addTo(criteria,
                da(Patient_.patientBirthDate, keys, Tag.PatientBirthDate, matchUnknown));
        addTo(criteria, wc(Patient_.patientCustomAttribute1,
                filter.selectPatientCustomAttribute1(keys), matchUnknown));
        addTo(criteria, wc(Patient_.patientCustomAttribute2,
                filter.selectPatientCustomAttribute2(keys), matchUnknown));
        addTo(criteria, wc(Patient_.patientCustomAttribute3,
                filter.selectPatientCustomAttribute3(keys), matchUnknown));
    }

    static void addStudyLevelCriteriaTo(Criteria criteria, Attributes keys,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts, String[] roles) {
        if (keys == null)
            return;

        boolean matchUnknown = filter.isMatchUnknown();
        addTo(criteria, uids(Study_.studyInstanceUID, keys.getStrings(Tag.StudyInstanceUID)));
        addTo(criteria, wc(Study_.studyID, filter.getString(keys, Tag.StudyID), matchUnknown));
        addTo(criteria, datm(
                Study_.studyDate, 
                Study_.studyTime,
                Tag.StudyDate,
                Tag.StudyTime,
                Tag.StudyDateAndTime,
                keys, queryOpts.contains(QueryOption.DATETIME), matchUnknown));
        addTo(criteria, pn(
                Study_.referringPhysicianName,
                Study_.referringPhysicianIdeographicName,
                Study_.referringPhysicianPhoneticName,
                Study_.referringPhysicianFamilyNameSoundex,
                Study_.referringPhysicianGivenNameSoundex,
                filter.getString(keys, Tag.ReferringPhysicianName),
                filter, queryOpts.contains(QueryOption.FUZZY), matchUnknown));
        addTo(criteria, wc(Study_.studyDescription,
                filter.getString(keys, Tag.StudyDescription), matchUnknown));
        String accNo = filter.getString(keys, Tag.AccessionNumber);
        addTo(criteria, wc(Study_.accessionNumber, accNo, matchUnknown));
        if(!accNo.equals("*"))
            addTo(criteria, issuer(Study_.issuerOfAccessionNumber,
                    keys.getNestedDataset(Tag.IssuerOfAccessionNumberSequence),
                    filter, matchUnknown));
        addTo(criteria, modsInStudy(filter.getString(keys, Tag.ModalitiesInStudy), matchUnknown));
        addTo(criteria, codes(Study_.procedureCodes, "procedureCode", 
                keys.getNestedDataset(Tag.ProcedureCodeSequence), filter, matchUnknown));
        addTo(criteria, wc(Study_.studyCustomAttribute1,
                filter.selectStudyCustomAttribute1(keys), matchUnknown));
        addTo(criteria, wc(Study_.studyCustomAttribute2,
                filter.selectStudyCustomAttribute2(keys), matchUnknown));
        addTo(criteria, wc(Study_.studyCustomAttribute3,
                filter.selectStudyCustomAttribute3(keys), matchUnknown));
        addTo(criteria, permission(roles, Action.QUERY));
    }


    static void addSeriesLevelCriteriaTo(Criteria criteria, Attributes keys,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts) {
        if (keys == null)
            return;

        boolean matchUnknown = filter.isMatchUnknown();
        addTo(criteria, uids(Series_.seriesInstanceUID, keys.getStrings(Tag.SeriesInstanceUID)));
        addTo(criteria, wc(Series_.seriesNumber,
                filter.getString(keys, Tag.SeriesNumber), matchUnknown));
        addTo(criteria, wc(Series_.modality,
                filter.getString(keys, Tag.Modality), matchUnknown));
        addTo(criteria, datm(
                Series_.performedProcedureStepStartDate,
                Series_.performedProcedureStepStartTime,
                Tag.PerformedProcedureStepStartDate,
                Tag.PerformedProcedureStepStartTime,
                Tag.PerformedProcedureStepStartDateAndTime,
                keys, queryOpts.contains(QueryOption.DATETIME), matchUnknown));
        addTo(criteria, wc(Series_.seriesDescription,
                filter.getString(keys, Tag.SeriesDescription), matchUnknown));
        addTo(criteria, requestAttributes(keys.getNestedDataset(Tag.RequestAttributesSequence),
                filter, queryOpts, matchUnknown));
        addTo(criteria, code(Series_.institutionCode, "institutionCode",
                keys.getNestedDataset(Tag.InstitutionCodeSequence), filter, matchUnknown));
        addTo(criteria, wc(Series_.seriesCustomAttribute1,
                filter.selectSeriesCustomAttribute1(keys), matchUnknown));
        addTo(criteria, wc(Series_.seriesCustomAttribute2,
                filter.selectSeriesCustomAttribute2(keys), matchUnknown));
        addTo(criteria, wc(Series_.seriesCustomAttribute3,
                filter.selectSeriesCustomAttribute3(keys), matchUnknown));
    }

    static void addInstanceLevelCriteriaTo(Criteria criteria, Attributes keys,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts) {
        if (keys == null)
            return;

        boolean matchUnknown = filter.isMatchUnknown();
        addTo(criteria, uids(Instance_.sopInstanceUID, keys.getStrings(Tag.SOPInstanceUID)));
        addTo(criteria, uids(Instance_.sopClassUID, keys.getStrings(Tag.SOPClassUID)));
        addTo(criteria, wc(Instance_.instanceNumber,
                filter.getString(keys, Tag.InstanceNumber), matchUnknown));
        addTo(criteria, wc(Instance_.verificationFlag,
                filter.getString(keys, Tag.VerificationFlag), matchUnknown));
        addTo(criteria, datm(
                Instance_.contentDate,
                Instance_.contentTime,
                Tag.ContentDate,
                Tag.ContentTime, 
                Tag.ContentDateAndTime,
                keys, queryOpts.contains(QueryOption.DATETIME), matchUnknown));
        addTo(criteria, code(Instance_.conceptNameCode, "conceptNameCode",
                keys.getNestedDataset(Tag.ConceptNameCodeSequence), filter, matchUnknown));
        addTo(criteria, verifyingObserver(keys.getNestedDataset(Tag.VerifyingObserverSequence),
                filter, queryOpts, matchUnknown));
        Sequence contentSeq = keys.getSequence(Tag.ContentSequence);
        if (contentSeq != null)
            for (Attributes item : contentSeq)
                addTo(criteria, contentItem(item, filter));
        addTo(criteria, wc(Instance_.instanceCustomAttribute1,
                filter.selectInstanceCustomAttribute1(keys), matchUnknown));
        addTo(criteria, wc(Instance_.instanceCustomAttribute2,
                filter.selectInstanceCustomAttribute2(keys), matchUnknown));
        addTo(criteria, wc(Instance_.instanceCustomAttribute3,
                filter.selectInstanceCustomAttribute3(keys), matchUnknown));
    }

    static void addTo(Criteria criteria, Criterion criterion) {
        if (criterion != null)
            criteria.add(criterion);
    }

    static Criterion pid(String[] pids, boolean matchUnknown) {
        if (pids == null || pids.length == 0)
            return null;

        Criterion result = null;
        for (int i = 0; i < pids.length-1; i++, i++)
            result = or(result, pid(pids[i], pids[i+1], matchUnknown));

        return result;
    }

    static Criterion or(Criterion lhs, Criterion rhs) {
        return lhs == null ? rhs : rhs == null ? lhs : Restrictions.or(lhs, rhs);
    }

    static Criterion and(Criterion lhs, Criterion rhs) {
        return lhs == null ? rhs : rhs == null ? lhs : Restrictions.and(lhs, rhs);
    }

    static Criterion pid(String id, String issue, boolean matchUnknown) {
        Criterion c1 = wc(Patient_.patientID, id, matchUnknown);
        Criterion c2 = wc(Patient_.issuerOfPatientID, id, matchUnknown);
        return and(c1, c2);
    }

    static Criterion wc(String propertyName, String value, boolean matchUnknown) {
        if (value.equals("*"))
            return null;

        return containsWildcard(value)
            ? like(propertyName, toLikePattern(value), matchUnknown)
            : eq(propertyName, value, matchUnknown);
    }

    private static boolean containsWildcard(String s) {
        return s.indexOf('*') >= 0 || s.indexOf('?') >= 0;
    }


    static Criterion like(String propertyName, String value, boolean matchUnknown) {
        if (value.equals("%"))
            return null;

        Criterion criterion = Restrictions.like(propertyName, value);
        if (matchUnknown)
            criterion = Restrictions.or(criterion, Restrictions.eq(propertyName, "*"));
        return criterion ;
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

    static Criterion eq(String propertyName, String value, boolean matchUnknown) {
        if (value.equals("*"))
            return null;

        Criterion criterion = Restrictions.eq(propertyName, value);
        if (matchUnknown)
            criterion = Restrictions.or(criterion, Restrictions.eq(propertyName, "*"));
        return criterion ;
    }

    static Criterion uids(String propertyName, String[] values) {
        if (values == null || values.length == 0 || values[0].equals("*"))
            return null;

        return values.length == 1
                ? Restrictions.eq(propertyName, values[0])
                : Restrictions.in(propertyName, values);
    }

    private static Criterion da(String propertyName, Attributes keys, int tag,
            boolean matchUnknown) {
        // TODO Auto-generated method stub
        return null;
    }

    private static Criterion datm(String daProperty, String tmProperty,
            int daTag, int tmTag, long datmTag, Attributes keys,
            boolean datetime, boolean matchUnknown) {
        // TODO Auto-generated method stub
        return null;
    }

    private static Criterion pn(String alphabetic, String ideographic, String phonetic,
            String fnsoundex, String gnsoundex, String value, AttributeFilter filter,
            boolean fuzzy, boolean matchUnknown) {
        // TODO Auto-generated method stub
        return null;
    }

    private static Criterion code(String propertyName, String alias, Attributes item,
            AttributeFilter filter, boolean matchUnknown) {
        Criterion criterion = code(alias, item, filter);
        if (criterion == null)
            return null;

        String codePk = alias + ".pk";
        Criterion exists = Subqueries.exists(DetachedCriteria.forClass(Code.class, alias)
                .setProjection(Projections.property(codePk))
                .add(Restrictions.eqProperty(propertyName + ".pk", codePk))
                .add(criterion));

        return matchUnknown
                ? Restrictions.or(exists, Restrictions.isNull(propertyName))
                : exists;
    }

    private static Criterion codes(String procedurecodes, String string,
            Attributes nestedDataset, AttributeFilter filter,
            boolean matchUnknown) {
        // TODO Auto-generated method stub
        return null;
    }

    private static Criterion code(String alias, Attributes item, AttributeFilter filter) {
        if (item == null)
            return null;

        return and(
                eq(Code_.codeValue(alias), filter.getString(item, Tag.CodeValue), false),
                and(
                    eq(Code_.codingSchemeDesignator(alias),
                            filter.getString(item, Tag.CodingSchemeDesignator), false),
                    eq(Code_.codingSchemeVersion(alias),
                            filter.getString(item, Tag.CodingSchemeVersion), false)));
    }

    private static Criterion modsInStudy(String value, boolean matchUnknown) {
        if (value.equals("*"))
            return null;

        return Subqueries.exists(DetachedCriteria.forClass(Series.class, "series2")
            .setProjection(Projections.property("series2.pk"))
            .add(Restrictions.eqProperty("series2.study", "study"))
            .add(wc("series2.modality", value, matchUnknown)));
    }

    private static Criterion issuer(String propertyName, Attributes item,
            AttributeFilter filter, boolean matchUnknown) {
        // TODO Auto-generated method stub
        return null;
    }

    private static Criterion requestAttributes(Attributes nestedDataset,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts,
            boolean matchUnknown) {
        // TODO Auto-generated method stub
        return null;
    }

    private static Criterion verifyingObserver(Attributes nestedDataset,
            AttributeFilter filter, EnumSet<QueryOption> queryOpts,
            boolean matchUnknown) {
        // TODO Auto-generated method stub
        return null;
    }

    private static Criterion contentItem(Attributes item, AttributeFilter filter) {
        // TODO Auto-generated method stub
        return null;
    }

    private static Criterion permission(String[] roles, Action query) {
        // TODO Auto-generated method stub
        return null;
    }

}
