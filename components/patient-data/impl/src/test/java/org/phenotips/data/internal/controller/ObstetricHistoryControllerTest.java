/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.internal.controller;

import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ObstetricHistoryControllerTest
{
    private static final Integer NON_ZERO = 1;

    private static final Integer ZERO = 0;

    private static final String PREFIX = "pregnancy_history__";

    private static final String GRAVIDA = "gravida";

    private static final String PARA = "para";

    private static final String TERM = "term";

    private static final String PRETERM = "preterm";

    private static final String SAB = "sab";

    private static final String TAB = "tab";

    private static final String LIVE_BIRTHS = "births";

    private static final String TEST_PATIENT_ID = "00000001";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Integer>> mocker =
        new MockitoComponentMockingRule<>(ObstetricHistoryController.class);

    private ObstetricHistoryController obstetricHistoryController;

    @Mock
    private Patient patient;

    @Mock
    private PatientData<Integer> mockPatientData;

    @Mock
    private Logger logger;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject data;

    private XWikiContext xWikiContext;

    @Mock
    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.obstetricHistoryController = (ObstetricHistoryController) this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();

        final Provider<XWikiContext> provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.xWikiContext = provider.get();
        doReturn(this.xwiki).when(this.xWikiContext).getWiki();

        DocumentReference patientDocRef = new DocumentReference("wiki", "patient", TEST_PATIENT_ID);
        doReturn(patientDocRef).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getXDocument();
        doReturn(patientDocRef.getName()).when(this.patient).getId();

        when(this.doc.getXObject(this.obstetricHistoryController.getXClassReference(), true, this.xWikiContext))
            .thenReturn(this.data);
    }

    @Test
    public void loadHandlesEmptyPatientTest()
    {
        doReturn(null).when(this.doc).getXObject(any(EntityReference.class));

        PatientData<Integer> testPatientData = this.obstetricHistoryController.load(this.patient);

        Assert.assertNull(testPatientData);
        verify(this.logger).debug("No data for patient [{}]", TEST_PATIENT_ID);
    }

    @Test
    public void loadDefaultBehaviourTest()
    {
        doReturn(this.data).when(this.doc).getXObject(any(EntityReference.class));
        doReturn(ZERO).when(this.data).getIntValue(PREFIX + GRAVIDA);
        doReturn(ZERO).when(this.data).getIntValue(PREFIX + PARA);
        doReturn(NON_ZERO).when(this.data).getIntValue(PREFIX + TERM);
        doReturn(NON_ZERO).when(this.data).getIntValue(PREFIX + PRETERM);
        doReturn(ZERO).when(this.data).getIntValue(PREFIX + SAB);
        doReturn(NON_ZERO).when(this.data).getIntValue(PREFIX + TAB);
        doReturn(NON_ZERO).when(this.data).getIntValue(PREFIX + LIVE_BIRTHS);

        PatientData<Integer> testPatientData = this.obstetricHistoryController.load(this.patient);

        Assert.assertEquals(testPatientData.getName(), "obstetric-history");
        Assert.assertTrue(testPatientData.get(TERM) == 1);
        Assert.assertTrue(testPatientData.get(PRETERM) == 1);
        Assert.assertTrue(testPatientData.get(TAB) == 1);
        Assert.assertTrue(testPatientData.get(LIVE_BIRTHS) == 1);
        Assert.assertEquals(testPatientData.size(), 4);
    }

    @Test
    public void loadIgnoresNoData()
    {
        doReturn(this.data).when(this.doc).getXObject(any(EntityReference.class));
        Assert.assertNull(this.obstetricHistoryController.load(this.patient));
    }

    @Test
    public void saveDoesNothingIfParentalInformationClassDoesNotExist()
    {
        when(this.doc.getXObject(this.obstetricHistoryController.getXClassReference(), true, this.xWikiContext))
            .thenReturn(null);
        this.obstetricHistoryController.save(this.patient);
        verify(this.doc, times(1)).getXObject(this.obstetricHistoryController.getXClassReference(), true,
            this.xWikiContext);
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.data);
    }

    @Test
    public void saveHandlesEmptyPatientTestWithUpdatePolicy()
    {
        doReturn(null).when(this.patient).getData(this.obstetricHistoryController.getName());

        this.obstetricHistoryController.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(this.obstetricHistoryController.getXClassReference(), true,
            this.xWikiContext);
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.data);
    }

    @Test
    public void saveHandlesEmptyPatientTestWithMergePolicy()
    {
        doReturn(null).when(this.patient).getData(this.obstetricHistoryController.getName());

        this.obstetricHistoryController.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(this.obstetricHistoryController.getXClassReference(), true,
            this.xWikiContext);
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.data);
    }

    @Test
    public void saveHandlesEmptyPatientTestWithReplacePolicy()
    {
        doReturn(null).when(this.patient).getData(this.obstetricHistoryController.getName());

        this.obstetricHistoryController.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(this.obstetricHistoryController.getXClassReference(), true,
            this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + GRAVIDA, null, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + PARA, null, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + TERM, null, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + PRETERM, null, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + SAB, null, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + TAB, null, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + LIVE_BIRTHS, null, this.xWikiContext);

        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.data);
    }

    @Test
    public void saveDefaultBehaviourTestWithUpdatePolicy()
    {
        doReturn(this.mockPatientData).when(this.patient).getData(this.obstetricHistoryController.getName());
        doReturn(true).when(this.mockPatientData).isNamed();

        when(this.mockPatientData.containsKey(GRAVIDA)).thenReturn(true);
        when(this.mockPatientData.containsKey(PARA)).thenReturn(true);
        when(this.mockPatientData.containsKey(TERM)).thenReturn(true);
        when(this.mockPatientData.containsKey(PRETERM)).thenReturn(true);
        when(this.mockPatientData.containsKey(SAB)).thenReturn(true);
        when(this.mockPatientData.containsKey(TAB)).thenReturn(true);
        when(this.mockPatientData.containsKey(LIVE_BIRTHS)).thenReturn(true);

        doReturn(NON_ZERO).when(this.mockPatientData).get(GRAVIDA);
        doReturn(NON_ZERO).when(this.mockPatientData).get(PARA);
        doReturn(ZERO).when(this.mockPatientData).get(TERM);
        doReturn(ZERO).when(this.mockPatientData).get(PRETERM);
        doReturn(NON_ZERO).when(this.mockPatientData).get(SAB);
        doReturn(NON_ZERO).when(this.mockPatientData).get(TAB);
        doReturn(ZERO).when(this.mockPatientData).get(LIVE_BIRTHS);

        this.obstetricHistoryController.save(this.patient);

        verify(this.doc, times(1)).getXObject(this.obstetricHistoryController.getXClassReference(), true,
            this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + GRAVIDA, NON_ZERO, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + PARA, NON_ZERO, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + TERM, ZERO, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + PRETERM, ZERO, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + SAB, NON_ZERO, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + TAB, NON_ZERO, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + LIVE_BIRTHS, ZERO, this.xWikiContext);

        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.data);
    }

    @Test
    public void saveDefaultBehaviourTestWithMergePolicy()
    {
        final ObstetricHistoryController spy = spy(this.obstetricHistoryController);
        doReturn(this.mockPatientData).when(this.patient).getData(this.obstetricHistoryController.getName());
        doReturn(true).when(this.mockPatientData).isNamed();

        when(this.mockPatientData.containsKey(GRAVIDA)).thenReturn(true);
        when(this.mockPatientData.containsKey(PARA)).thenReturn(false);
        when(this.mockPatientData.containsKey(TERM)).thenReturn(true);
        when(this.mockPatientData.containsKey(PRETERM)).thenReturn(true);
        when(this.mockPatientData.containsKey(SAB)).thenReturn(true);
        when(this.mockPatientData.containsKey(TAB)).thenReturn(true);
        when(this.mockPatientData.containsKey(LIVE_BIRTHS)).thenReturn(true);

        doReturn(NON_ZERO).when(this.mockPatientData).get(GRAVIDA);
        doReturn(null).when(this.mockPatientData).get(PARA);
        doReturn(ZERO).when(this.mockPatientData).get(TERM);
        doReturn(ZERO).when(this.mockPatientData).get(PRETERM);
        doReturn(NON_ZERO).when(this.mockPatientData).get(SAB);
        doReturn(NON_ZERO).when(this.mockPatientData).get(TAB);
        doReturn(ZERO).when(this.mockPatientData).get(LIVE_BIRTHS);

        spy.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(this.obstetricHistoryController.getXClassReference(), true,
            this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + GRAVIDA, NON_ZERO, this.xWikiContext);
        verify(this.data, never()).set(eq(PREFIX + PARA), any(), eq(this.xWikiContext));
        verify(this.data, times(1)).set(PREFIX + TERM, ZERO, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + PRETERM, ZERO, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + SAB, NON_ZERO, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + TAB, NON_ZERO, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + LIVE_BIRTHS, ZERO, this.xWikiContext);

        verify(spy, never()).load(this.patient);
        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.data);
    }

    @Test
    public void saveDefaultBehaviourTestWithReplacePolicy()
    {
        final ObstetricHistoryController spy = spy(this.obstetricHistoryController);
        doReturn(this.mockPatientData).when(this.patient).getData(this.obstetricHistoryController.getName());
        doReturn(true).when(this.mockPatientData).isNamed();

        doReturn(null).when(this.mockPatientData).get(GRAVIDA);
        doReturn(NON_ZERO).when(this.mockPatientData).get(PARA);
        doReturn(null).when(this.mockPatientData).get(TERM);
        doReturn(null).when(this.mockPatientData).get(PRETERM);
        doReturn(NON_ZERO).when(this.mockPatientData).get(SAB);
        doReturn(NON_ZERO).when(this.mockPatientData).get(TAB);
        doReturn(ZERO).when(this.mockPatientData).get(LIVE_BIRTHS);

        spy.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(this.obstetricHistoryController.getXClassReference(), true,
            this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + GRAVIDA, null, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + PARA, NON_ZERO, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + TERM, null, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + PRETERM, null, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + SAB, NON_ZERO, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + TAB, NON_ZERO, this.xWikiContext);
        verify(this.data, times(1)).set(PREFIX + LIVE_BIRTHS, ZERO, this.xWikiContext);

        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.data);
    }

    @Test
    public void writeJSONWithoutSelectedFieldsTest()
    {
        Map<String, Integer> testData = new LinkedHashMap<>();
        testData.put(GRAVIDA, NON_ZERO);
        testData.put(PARA, ZERO);
        JSONObject json = new JSONObject();
        PatientData<Integer> testObstetricHistoryData =
            new DictionaryPatientData<>("obstetric-history", testData);
        doReturn(testObstetricHistoryData).when(this.patient).getData(this.obstetricHistoryController.getName());

        this.obstetricHistoryController.writeJSON(this.patient, json);
        Assert.assertNotNull(json);
        Assert.assertTrue(new JSONObject(testData).similar(
            json.getJSONObject("prenatal_perinatal_history").get("obstetric-history")));
    }

    @Test
    public void writeJSONWithoutObstetricHistoryField()
    {
        JSONObject json = new JSONObject();
        Collection<String> fieldList = new ArrayList<>();
        fieldList.add("test field");

        this.obstetricHistoryController.writeJSON(this.patient, json, fieldList);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONIgnoresMissingData()
    {
        JSONObject json = new JSONObject();

        this.obstetricHistoryController.writeJSON(this.patient, json, null);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void readJSONObjectWithNoData()
    {
        JSONObject json = new JSONObject();
        json.put("prenatal_perinatal_history", "obstetric-history");

        PatientData<Integer> patientData = this.obstetricHistoryController.readJSON(json);
        Assert.assertNull(patientData);
    }

    @Test
    public void readJSONIgnoresNullParameter()
    {
        PatientData<Integer> patientData = this.obstetricHistoryController.readJSON(null);
        Assert.assertNull(patientData);
    }

    @Test
    public void readJSONIgnoresEmptyParameter()
    {
        PatientData<Integer> patientData = this.obstetricHistoryController.readJSON(new JSONObject());
        Assert.assertNull(patientData);
    }

    @Test
    public void readJSONDefaultBehaviour()
    {
        JSONObject obstetricData = new JSONObject();
        obstetricData.put(TERM, 0);
        JSONObject json = new JSONObject();
        JSONObject container = json;
        for (String path : StringUtils.split("prenatal_perinatal_history.obstetric-history", '.')) {
            JSONObject parent = container;
            container = parent.optJSONObject(path);
            if (container == null) {
                parent.put(path, new JSONObject());
                container = parent.optJSONObject(path);
            }
        }
        for (String key : obstetricData.keySet()) {
            container.put(key, obstetricData.get(key));
        }

        PatientData<Integer> patientData = this.obstetricHistoryController.readJSON(json);
        Assert.assertEquals("obstetric-history", patientData.getName());
        Assert.assertTrue(patientData.get(TERM) == 0);
    }
}
