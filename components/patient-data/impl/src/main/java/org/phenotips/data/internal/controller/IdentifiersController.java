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

import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles the patient's date of birth and the exam date.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component(roles = { PatientDataController.class })
@Named("identifiers")
@Singleton
public class IdentifiersController implements PatientDataController<String>
{
    private static final String DATA_NAME = "identifiers";

    private static final String EXTERNAL_IDENTIFIER_PROPERTY_NAME = "external_id";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontext;

    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }
            Map<String, String> result = new LinkedHashMap<>();
            result.put(EXTERNAL_IDENTIFIER_PROPERTY_NAME, data.getStringValue(EXTERNAL_IDENTIFIER_PROPERTY_NAME));
            return new DictionaryPatientData<>(DATA_NAME, result);
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
            final BaseObject data = patient.getXDocument().getXObject(Patient.CLASS_REFERENCE, true, xcontext.get());
            final PatientData<String> identifiers = patient.getData(DATA_NAME);
            if (identifiers == null) {
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    data.setStringValue(EXTERNAL_IDENTIFIER_PROPERTY_NAME, null);
                }
            } else {
                if (!identifiers.isNamed()) {
                    this.logger.error(ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);
                    return;
                }

                final String externalId = identifiers.get(EXTERNAL_IDENTIFIER_PROPERTY_NAME);
                data.setStringValue(EXTERNAL_IDENTIFIER_PROPERTY_NAME, externalId);
            }
        } catch (final Exception ex) {
            this.logger.error("Failed to save identifiers data: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(EXTERNAL_IDENTIFIER_PROPERTY_NAME)) {
            return;
        }

        PatientData<String> patientData = patient.<String>getData(DATA_NAME);
        if (patientData != null && patientData.isNamed()) {
            Iterator<Entry<String, String>> values = patientData.dictionaryIterator();

            while (values.hasNext()) {
                Entry<String, String> datum = values.next();
                json.put(datum.getKey(), datum.getValue());
            }
        }
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        if (!json.has(EXTERNAL_IDENTIFIER_PROPERTY_NAME)) {
            // no data supported by this controller is present in provided JSON
            return null;
        }
        String externalId = json.getString(EXTERNAL_IDENTIFIER_PROPERTY_NAME);

        Map<String, String> result = new LinkedHashMap<>();
        result.put(EXTERNAL_IDENTIFIER_PROPERTY_NAME, externalId);
        return new DictionaryPatientData<>(DATA_NAME, result);
    }

    @Override
    public String getName()
    {
        return DATA_NAME;
    }
}
