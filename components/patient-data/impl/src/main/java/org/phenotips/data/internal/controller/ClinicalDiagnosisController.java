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

import org.phenotips.data.Disorder;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.internal.PhenoTipsDisorder;

import org.xwiki.component.annotation.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ListProperty;

/**
 * Handles the patient's working clinical diagnosis.
 *
 * @version $Id$
 * @since 1.3
 */
@Component(roles = { PatientDataController.class })
@Named("clinical-diagnosis")
@Singleton
public class ClinicalDiagnosisController implements PatientDataController<Disorder>
{
    protected static final String JSON_KEY_CLINICAL_DIAGNOSIS = "clinical-diagnosis";

    private static final String CONTROLLER_NAME = JSON_KEY_CLINICAL_DIAGNOSIS;

    private static final String DIAGNOSIS_PROPERTY = "clinical_diagnosis";

    @Inject
    private Logger logger;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public String getName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    public IndexedPatientData<Disorder> load(Patient patient)
    {
        try {
            BaseObject data = patient.getXDocument().getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }

            List<Disorder> disorders = new ArrayList<>();

            ListProperty values = (ListProperty) data.get(DIAGNOSIS_PROPERTY);
            if (values != null) {
                for (String value : values.getList()) {
                    if (StringUtils.isNotBlank(value)) {
                        disorders.add(new PhenoTipsDisorder(values, value));
                    }
                }
            }
            if (disorders.isEmpty()) {
                return null;
            } else {
                return new IndexedPatientData<>(getName(), disorders);
            }
        } catch (Exception e) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        save(patient, PatientWritePolicy.UPDATE);
    }

    @Override
    public void save(@Nonnull final Patient patient, @Nonnull final PatientWritePolicy policy)
    {
        try {
            final XWikiContext context = this.xcontextProvider.get();
            final BaseObject xobject = patient.getXDocument().getXObject(Patient.CLASS_REFERENCE, true, context);
            // Disorders that need to be saved to the patient.
            final PatientData<Disorder> disorders = patient.getData(getName());
            if (disorders == null) {
                // If patient write policy is replace, need wipe all existing data if no data provided for controller
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    xobject.set(DIAGNOSIS_PROPERTY, null, context);
                }
            } else {
                if (!disorders.isIndexed()) {
                    this.logger.error(ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);
                    return;
                }
                saveDisordersData(patient, xobject, disorders, policy, context);
            }
        } catch (final Exception ex) {
            this.logger.error("Failed to save clinical diagnosis data: {}", ex.getMessage(), ex);
        }

    }

    /**
     * Writes the {@code disorders} data to {@code xobject} according to the specified {@code policy}.
     *
     * @param patient the {@link Patient} of interest
     * @param xobject the {@link BaseObject}
     * @param disorders a {@link PatientData} object containing {@link Disorder disorder} information.
     * @param policy the selected {@link PatientWritePolicy}
     * @param context the {@link XWikiContext}
     */
    private void saveDisordersData(
        @Nonnull final Patient patient,
        @Nonnull final BaseObject xobject,
        @Nonnull final PatientData<Disorder> disorders,
        @Nonnull final PatientWritePolicy policy,
        @Nonnull final XWikiContext context)
    {
        final PatientData<Disorder> storedDisorders = PatientWritePolicy.MERGE.equals(policy)
            ? load(patient)
            : null;
        // Get a set of disorder identifiers.
        final List<String> disorderValues = buildMergedDisorderList(storedDisorders, disorders);
        xobject.set(DIAGNOSIS_PROPERTY, disorderValues.isEmpty() ? null : disorderValues, context);
    }

    /**
     * Returns a list of disorders, merging {@code storedDisorders stored disorders}, if any, and {@code disorders}.
     *
     * @param storedDisorders {@link PatientData} already stored in patient
     * @param disorders {@link PatientData} to save for patient
     * @return a merged list of disorders
     */
    private List<String> buildMergedDisorderList(
        @Nullable final PatientData<Disorder> storedDisorders,
        @Nonnull final PatientData<Disorder> disorders)
    {
        // If there are no disorders stored, then just return a list of new disorder names.
        if (storedDisorders == null || storedDisorders.size() == 0) {
            return StreamSupport.stream(disorders.spliterator(), false)
                .map(Disorder::getValue)
                .collect(Collectors.toList());
        }
        // There are some stored disorders, merge them.
        final Set<String> disorderValues = Stream.of(storedDisorders, disorders)
            .flatMap(s -> StreamSupport.stream(s.spliterator(), false))
            .map(Disorder::getValue)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        return new ArrayList<>(disorderValues);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(DIAGNOSIS_PROPERTY)) {
            return;
        }

        PatientData<Disorder> data = patient.getData(getName());
        json.put(JSON_KEY_CLINICAL_DIAGNOSIS, diseasesToJSON(data));
    }

    /** creates & returns a new JSON array of all patient clinical diseases (as JSON objects). */
    private JSONArray diseasesToJSON(PatientData<Disorder> data)
    {
        JSONArray diseasesJSON = new JSONArray();
        if (data != null) {
            for (Disorder disease : data) {
                diseasesJSON.put(disease.toJSON());
            }
        }
        return diseasesJSON;
    }

    @Override
    public PatientData<Disorder> readJSON(JSONObject json)
    {
        if (json == null || !json.has(JSON_KEY_CLINICAL_DIAGNOSIS)) {
            return null;
        }

        JSONArray inputDisorders = json.optJSONArray(JSON_KEY_CLINICAL_DIAGNOSIS);
        if (inputDisorders == null) {
            return null;
        }

        List<Disorder> disorders = new ArrayList<>();
        for (int i = 0; i < inputDisorders.length(); i++) {
            JSONObject disorderJSON = inputDisorders.optJSONObject(i);
            if (disorderJSON == null) {
                continue;
            }

            Disorder phenotipsDisorder = new PhenoTipsDisorder(disorderJSON);
            disorders.add(phenotipsDisorder);
        }
        return new IndexedPatientData<>(getName(), disorders);
    }
}
