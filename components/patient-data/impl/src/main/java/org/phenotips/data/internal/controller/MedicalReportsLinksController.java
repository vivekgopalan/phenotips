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

import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Provides a dictionary of medical document names to links.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component(roles = { PatientDataController.class })
@Named("medicalreportslinks")
@Singleton
public class MedicalReportsLinksController implements PatientDataController<String>
{
    /**
     * Logging helper object.
     */
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public PatientData<String> load(Patient patient)
    {
        try {
            XWikiContext context = this.contextProvider.get();
            XWikiDocument doc = patient.getXDocument();
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                throw new NullPointerException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }
            Map<String, String> result = new LinkedHashMap<>();

            /* Getting the documents which are reports instead of just getting all attachments */
            BaseProperty medicalReportsField = (BaseProperty) data.getField("reports_history");
            List<String> reports = (List<String>) medicalReportsField.getValue();

            for (String report : reports) {
                String url = doc.getAttachmentURL(report, "download", context);
                result.put(report, url);
            }

            return new DictionaryPatientData<>(getName(), result);
        } catch (Exception e) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        // Nothing to do, this is only used in memory
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        // Nothing to do, this is only used in memory
    }

    @Override
    public PatientData<String> readJSON(JSONObject json)
    {
        // Nothing to do, this is only used in memory
        return null;
    }

    @Override
    public String getName()
    {
        return "medicalreportslinks";
    }
}
