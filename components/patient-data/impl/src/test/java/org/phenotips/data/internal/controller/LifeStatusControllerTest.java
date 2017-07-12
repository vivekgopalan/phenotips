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

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
import java.util.LinkedList;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link LifeStatusController} component, implementation of the
 * {@link org.phenotips.data.PatientDataController} interface.
 */
public class LifeStatusControllerTest
{
    private static final String DATA_NAME = "life_status";

    private static final String ALIVE = "alive";

    private static final String DECEASED = "deceased";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<>(LifeStatusController.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject data;

    private PatientDataController<String> component;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();

        DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocRef).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getXDocument();
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);
        doReturn(this.data).when(this.doc).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
    }

    @Test
    public void loadCatchesInvalidDocument()
    {
        doReturn(null).when(this.patient).getXDocument();

        PatientData<String> result = this.component.load(this.patient);

        verify(this.mocker.getMockedLogger()).error(eq(PatientDataController.ERROR_MESSAGE_LOAD_FAILED), anyString());
        Assert.assertNull(result);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass()
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<String> result = this.component.load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull()
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNotNull()
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(DATA_NAME);

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONAddsLifeStatus()
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, ALIVE)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json);

        Assert.assertEquals(ALIVE, json.get(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsLifeStatus()
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, DECEASED)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(DATA_NAME);

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(DECEASED, json.get(DATA_NAME));
    }

    @Test
    public void readJSONEmptyJsonReturnsNull()
    {
        Assert.assertNull(this.component.readJSON(new JSONObject()));
    }

    @Test
    public void readJSONReturnsCorrectLifeStatus()
    {
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, ALIVE);
        PatientData<String> result = this.component.readJSON(json);
        Assert.assertEquals(ALIVE, result.getValue());

        json = new JSONObject();
        json.put(DATA_NAME, DECEASED);
        result = this.component.readJSON(json);
        Assert.assertEquals(DECEASED, result.getValue());
    }

    @Test
    public void readJSONDoesNotReturnUnexpectedValue()
    {
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, "!!!!!");
        Assert.assertNull(this.component.readJSON(json));
    }

    @Test
    public void checkGetName()
    {
        Assert.assertEquals(DATA_NAME, this.component.getName());
    }

    @Test
    public void saveDoesNothingWhenPatientHasNoPatientClass()
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(null);
        this.component.save(this.patient);
        verifyZeroInteractions(this.data);
    }

    @Test
    public void saveDoesNothingIfNoDataProvidedAndPolicyIsUpdate()
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyZeroInteractions(this.data);
    }

    @Test
    public void saveDoesNothingIfNoDataProvidedAndPolicyIsMerge()
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyZeroInteractions(this.data);
    }

    @Test
    public void saveSetsDefaultValueIfNoDataProvidedAndPolicyIsReplace()
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verify(this.data, times(1)).setStringValue(DATA_NAME, ALIVE);
        verifyNoMoreInteractions(this.data);
    }

    @Test
    public void saveAliveWhenAliveWithUpdatePolicy()
    {
        PatientData<String> lifeStatus = new SimpleValuePatientData<>(DATA_NAME, ALIVE);
        doReturn(lifeStatus).when(this.patient).getData(DATA_NAME);

        this.component.save(this.patient);

        verify(this.data).setStringValue(DATA_NAME, ALIVE);
    }

    @Test
    public void saveAliveWhenAliveWithMergePolicy()
    {
        PatientData<String> lifeStatus = new SimpleValuePatientData<>(DATA_NAME, ALIVE);
        doReturn(lifeStatus).when(this.patient).getData(DATA_NAME);

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.data).setStringValue(DATA_NAME, ALIVE);
    }

    @Test
    public void saveAliveWhenAliveWithReplacePolicy()
    {
        PatientData<String> lifeStatus = new SimpleValuePatientData<>(DATA_NAME, ALIVE);
        doReturn(lifeStatus).when(this.patient).getData(DATA_NAME);

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.data).setStringValue(DATA_NAME, ALIVE);
    }

    @Test
    public void saveDeceasedWhenDeceasedWithUpdatePolicy()
    {
        PatientData<String> lifeStatus = new SimpleValuePatientData<>(DATA_NAME, DECEASED);
        doReturn(lifeStatus).when(this.patient).getData(DATA_NAME);

        this.component.save(this.patient);

        verify(this.data).setStringValue(DATA_NAME, DECEASED);
    }

    @Test
    public void saveDeceasedWhenDeceasedWithMergePolicy()
    {
        PatientData<String> lifeStatus = new SimpleValuePatientData<>(DATA_NAME, DECEASED);
        doReturn(lifeStatus).when(this.patient).getData(DATA_NAME);

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.data).setStringValue(DATA_NAME, DECEASED);
    }

    @Test
    public void saveDeceasedWhenDeceasedWithReplacePolicy()
    {
        PatientData<String> lifeStatus = new SimpleValuePatientData<>(DATA_NAME, DECEASED);
        doReturn(lifeStatus).when(this.patient).getData(DATA_NAME);

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.data).setStringValue(DATA_NAME, DECEASED);
    }
}
