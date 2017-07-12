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
import java.util.Collections;
import java.util.Iterator;
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
 * Handles the patients disorders.
 *
 * @version $Id$
 * @since 1.3RC1
 */
@Component(roles = { PatientDataController.class })
@Named("disorders")
@Singleton
public class DisordersController extends AbstractComplexController<Disorder>
{
    protected static final String JSON_KEY_DISORDERS = "disorders";

    private static final String CONTROLLER_NAME = JSON_KEY_DISORDERS;

    private static final String DISORDER_PROPERTIES_OMIMID = "omim_id";

    private static final String[] DISORDER_PROPERTIES = new String[] { DISORDER_PROPERTIES_OMIMID };

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
    protected String getJsonPropertyName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    protected List<String> getProperties()
    {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getBooleanFields()
    {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getCodeFields()
    {
        return Collections.emptyList();
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

            for (String property : DISORDER_PROPERTIES) {
                ListProperty values = (ListProperty) data.get(property);
                if (values != null) {
                    for (String value : values.getList()) {
                        if (StringUtils.isNotBlank(value)) {
                            disorders.add(new PhenoTipsDisorder(values, value));
                        }
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
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (!isFieldIncluded(selectedFieldNames, DISORDER_PROPERTIES)) {
            return;
        }

        PatientData<Disorder> data = patient.getData(getName());
        json.put(JSON_KEY_DISORDERS, diseasesToJSON(data, selectedFieldNames));
    }

    /** creates & returns a new JSON array of all patient diseases (as JSON objects). */
    private JSONArray diseasesToJSON(PatientData<Disorder> data, Collection<String> selectedFieldNames)
    {
        JSONArray diseasesJSON = new JSONArray();
        if (data != null) {
            Iterator<Disorder> iterator = data.iterator();
            while (iterator.hasNext()) {
                Disorder disease = iterator.next();
                JSONObject diseaseJSON = disease.toJSON();
                if (diseaseJSON != null) {
                    diseasesJSON.put(diseaseJSON);
                }
            }
        }
        return diseasesJSON;
    }

    private boolean isFieldIncluded(Collection<String> selectedFields, String[] fieldNames)
    {
        if (selectedFields == null) {
            return true;
        }
        for (String fieldName : fieldNames) {
            if (selectedFields.contains(fieldName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PatientData<Disorder> readJSON(JSONObject json)
    {
        if (json == null || !json.has(JSON_KEY_DISORDERS)) {
            return null;
        }

        try {
            JSONArray inputDisorders = json.optJSONArray(JSON_KEY_DISORDERS);
            if (inputDisorders == null) {
                return null;
            }
            // keep this instance of PhenotipsPatient in sync with the document: reset disorders
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
        } catch (Exception e) {
            this.logger.error("Could not load disorders from JSON: [{}]", e.getMessage(), e);
            return null;
        }
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
            final BaseObject dataHolder = patient.getXDocument().getXObject(Patient.CLASS_REFERENCE, true, context);
            final PatientData<Disorder> disorders = patient.getData(getName());
            if (disorders == null) {
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    dataHolder.set(DISORDER_PROPERTIES_OMIMID, null, context);
                }
            } else {
                if (!disorders.isIndexed()) {
                    this.logger.error(ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);
                    return;
                }
                saveDisordersData(patient, dataHolder, disorders, policy, context);
            }
        } catch (final Exception ex) {
            this.logger.error("Failed to save disorders data: {}", ex.getMessage(), ex);
        }

    }

    /**
     * Writes {@code disorders} data to {@code dataHolder} according to the provided {@code policy}.
     *
     * @param patient the {@link Patient} object of interest
     * @param dataHolder the {@link BaseObject} where data will be written
     * @param disorders the {@link PatientData} object containing disorders data
     * @param policy the {@link PatientWritePolicy} according to which data will be saved
     * @param context the {@link XWikiContext}
     */
    private void saveDisordersData(
        @Nonnull final Patient patient,
        @Nonnull final BaseObject dataHolder,
        @Nonnull final PatientData<Disorder> disorders,
        @Nonnull final PatientWritePolicy policy,
        @Nonnull final XWikiContext context)
    {
        final PatientData<Disorder> storedDisorders = PatientWritePolicy.MERGE.equals(policy)
            ? load(patient)
            : null;

        final List<String> result = buildMergedDisorderList(storedDisorders, disorders);
        dataHolder.set(DISORDER_PROPERTIES_OMIMID, result.isEmpty() ? null : result, context);
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
}
