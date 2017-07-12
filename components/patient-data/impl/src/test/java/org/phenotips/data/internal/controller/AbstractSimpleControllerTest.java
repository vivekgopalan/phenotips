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
import org.phenotips.data.SimpleValuePatientData;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.phenotips.data.internal.controller.AbstractSimpleControllerTestImplementation.DATA_NAME;
import static org.phenotips.data.internal.controller.AbstractSimpleControllerTestImplementation.PROPERTY_1;
import static org.phenotips.data.internal.controller.AbstractSimpleControllerTestImplementation.PROPERTY_2;
import static org.phenotips.data.internal.controller.AbstractSimpleControllerTestImplementation.PROPERTY_3;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link AbstractSimpleController} defined methods (load, save, writeJSON, readJSON). These methods are
 * tested using a mock implementation of {@link AbstractSimpleController} that provides simple definitions of the
 * abstract methods getName, getProperties, and getJsonPropertyName
 */
public class AbstractSimpleControllerTest
{
    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<>(
            AbstractSimpleControllerTestImplementation.class);

    @Mock
    protected Patient patient;

    @Mock
    protected XWikiDocument doc;

    @Mock
    protected BaseObject data;

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
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(DATA_NAME, this.component.getName());
    }

    // -----------------------------------load() tests-----------------------------------

    @Test
    public void loadCatchesInvalidDocument() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getXDocument();

        PatientData<String> result = this.component.load(this.patient);

        verify(this.mocker.getMockedLogger()).error(eq(PatientDataController.ERROR_MESSAGE_LOAD_FAILED), anyString());
        Assert.assertNull(result);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<String> result = this.component.load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadCatchesUnforeseenExceptions() throws Exception
    {
        Exception testException = new RuntimeException("Test Exception");
        doThrow(testException).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<String> result = this.component.load(this.patient);

        verify(this.mocker.getMockedLogger()).error(eq(PatientDataController.ERROR_MESSAGE_LOAD_FAILED), anyString());
        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsAllData() throws ComponentLookupException
    {
        String datum1 = "datum2";
        String datum2 = "datum2";
        String datum3 = "datum3";
        doReturn(datum1).when(this.data).getStringValue(PROPERTY_1);
        doReturn(datum2).when(this.data).getStringValue(PROPERTY_2);
        doReturn(datum3).when(this.data).getStringValue(PROPERTY_3);

        PatientData<String> result = this.component.load(this.patient);

        Assert.assertEquals(datum1, result.get(PROPERTY_1));
        Assert.assertEquals(datum2, result.get(PROPERTY_2));
        Assert.assertEquals(datum3, result.get(PROPERTY_3));
        Assert.assertEquals(3, result.size());
    }

    @Test
    public void loadNotIgnoresBlankFields() throws ComponentLookupException
    {
        String datum = "datum";
        doReturn(" ").when(this.data).getStringValue(PROPERTY_1);
        doReturn(null).when(this.data).getStringValue(PROPERTY_2);
        doReturn(datum).when(this.data).getStringValue(PROPERTY_3);

        PatientData<String> result = this.component.load(this.patient);

        Assert.assertEquals(datum, result.get(PROPERTY_3));
        Assert.assertEquals(3, result.size());
    }

    // -----------------------------------save() tests-----------------------------------

    @Test
    public void saveDoesNothingWhenPatientDoesNotHavePatientDocument()
    {
        when(this.doc.getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any())).thenReturn(null);
        this.component.save(this.patient);
    }

    @Test
    public void saveDoesNothingUnderUpdatePolicyWhenDataIsNull() throws XWikiException
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);
        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyZeroInteractions(this.data);
    }

    @Test
    public void saveDoesNothingUnderMergePolicyWhenDataIsNull() throws XWikiException
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyZeroInteractions(this.data);
    }

    @Test
    public void saveNullsAllPropertyDataUnderReplacePolicyWhenDataIsNull() throws XWikiException
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verify(this.data, times(1)).setStringValue(PROPERTY_1, null);
        verify(this.data, times(1)).setStringValue(PROPERTY_2, null);
        verify(this.data, times(1)).setStringValue(PROPERTY_3, null);
        verifyNoMoreInteractions(this.data);
    }

    @Test
    public void saveDoesNothingWhenDataIsNotKeyValueBased() throws ComponentLookupException, XWikiException
    {
        PatientData<String> patientData = new SimpleValuePatientData<>(DATA_NAME, "datum");
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient);
    }

    @Test
    public void saveSetsAllFields() throws XWikiException, ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);

        this.component.save(this.patient);

        verify(this.data).setStringValue(PROPERTY_1, "datum1");
        verify(this.data).setStringValue(PROPERTY_2, "datum2");
        verify(this.data).setStringValue(PROPERTY_3, "datum3");
    }

    @Test
    public void saveSetsOnlySpecifiedFieldsUnderUpdatePolicy()
    {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_1, PROPERTY_1);
        map.put(PROPERTY_3, PROPERTY_3);
        final PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);
        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verify(this.data, times(1)).setStringValue(PROPERTY_1, PROPERTY_1);
        verify(this.data, never()).setStringValue(Matchers.matches(PROPERTY_2), anyString());
        verify(this.data, times(1)).setStringValue(PROPERTY_3, PROPERTY_3);
        verifyNoMoreInteractions(this.data);
    }

    @Test
    public void saveSetsOnlySpecifiedFieldsUnderMergePolicy()
    {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_1, PROPERTY_1);
        map.put(PROPERTY_3, PROPERTY_3);
        final PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verify(this.data, times(1)).setStringValue(PROPERTY_1, PROPERTY_1);
        verify(this.data, never()).setStringValue(Matchers.matches(PROPERTY_2), anyString());
        verify(this.data, times(1)).setStringValue(PROPERTY_3, PROPERTY_3);
        verifyNoMoreInteractions(this.data);
    }

    @Test
    public void saveSetsOnlySpecifiedFieldsUnderReplacePolicyAndNullsTheRest()
    {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_1, PROPERTY_1);
        map.put(PROPERTY_3, PROPERTY_3);
        final PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verify(this.data, times(1)).setStringValue(PROPERTY_1, PROPERTY_1);
        verify(this.data, times(1)).setStringValue(PROPERTY_2, null);
        verify(this.data, times(1)).setStringValue(PROPERTY_3, PROPERTY_3);
        verifyNoMoreInteractions(this.data);
    }

    // -----------------------------------writeJSON() tests-----------------------------------

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONReturnsWhenDataIsNotKeyValueBased() throws ComponentLookupException
    {
        PatientData<String> patientData = new SimpleValuePatientData<>(DATA_NAME, "datum");
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenDataIsNotKeyValueBased() throws ComponentLookupException
    {
        PatientData<String> patientData = new SimpleValuePatientData<>(DATA_NAME, "datum");
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONAddsContainerWithAllValues() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum2", container.get(PROPERTY_2));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsContainerWithAllValues() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(PROPERTY_1);
        selectedFields.add(PROPERTY_2);
        selectedFields.add(PROPERTY_3);

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum2", container.get(PROPERTY_2));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsSelectedValues() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(PROPERTY_1);
        selectedFields.add(PROPERTY_3);

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
        Assert.assertFalse(container.has(PROPERTY_2));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsContainerWithAllValuesWhenSelectedFieldsNull()
        throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json, null);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum2", container.get(PROPERTY_2));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
    }

    @Test
    public void writeJSONDoesNotOverwriteContainer() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(PROPERTY_1);
        selectedFields.add(PROPERTY_3);

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
        Assert.assertFalse(container.has(PROPERTY_2));

        selectedFields.clear();
        selectedFields.add(PROPERTY_2);

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum2", container.get(PROPERTY_2));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
    }

    // -----------------------------------readJSON() tests-----------------------------------

    @Test
    public void readJSONReturnsNullWhenPassedEmptyJSONObject() throws ComponentLookupException
    {
        Assert.assertNull(this.component.readJSON(new JSONObject()));
    }

    @Test
    public void readJSONReturnsNullWhenDataContainerIsNotAJSONObject() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, "datum");
        Assert.assertNull(this.component.readJSON(json));
    }

    @Test
    public void readJSONReadsAllProperties() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        JSONObject container = new JSONObject();
        container.put(PROPERTY_1, "datum1");
        container.put(PROPERTY_2, "datum2");
        container.put(PROPERTY_3, "datum3");
        json.put(DATA_NAME, container);

        PatientData<String> result = this.component.readJSON(json);

        Assert.assertTrue(result.isNamed());
        Assert.assertEquals(DATA_NAME, result.getName());
        Assert.assertEquals("datum1", result.get(PROPERTY_1));
        Assert.assertEquals("datum2", result.get(PROPERTY_2));
        Assert.assertEquals("datum3", result.get(PROPERTY_3));
    }
}
