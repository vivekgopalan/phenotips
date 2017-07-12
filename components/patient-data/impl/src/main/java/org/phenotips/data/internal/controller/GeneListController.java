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

import org.phenotips.Constants;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.StringListProperty;

/**
 * Handles the patients genes.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component(roles = { PatientDataController.class })
@Named("gene")
@Singleton
public class GeneListController extends AbstractComplexController<Map<String, String>>
{
    /** The XClass used for storing gene data. */
    protected static final EntityReference GENE_CLASS_REFERENCE = new EntityReference("GeneClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String HGNC = "HGNC";

    private static final String ENSEMBL_ID_PROPERTY_NAME = "ensembl_gene_id";

    private static final String SYMBOL_PROPERTY_NAME = "symbol";

    private static final String GENES_STRING = "genes";

    private static final String CONTROLLER_NAME = GENES_STRING;

    private static final String GENES_ENABLING_FIELD_NAME = GENES_STRING;

    private static final String INTERNAL_GENE_KEY = "gene";

    private static final String INTERNAL_STATUS_KEY = "status";

    private static final String INTERNAL_STRATEGY_KEY = "strategy";

    private static final String INTERNAL_COMMENTS_KEY = "comments";

    private static final String INTERNAL_CANDIDATE_VALUE = "candidate";

    private static final String INTERNAL_REJECTED_VALUE = "rejected";

    private static final String INTERNAL_SOLVED_VALUE = "solved";

    private static final String INTERNAL_CARRIER_VALUE = "carrier";

    private static final String JSON_GENE_ID = "id";

    private static final String JSON_GENE_SYMBOL = INTERNAL_GENE_KEY;

    private static final String JSON_DEPRECATED_GENE_ID = JSON_GENE_SYMBOL;

    private static final String JSON_STATUS_KEY = INTERNAL_STATUS_KEY;

    private static final String JSON_STRATEGY_KEY = INTERNAL_STRATEGY_KEY;

    private static final String JSON_COMMENTS_KEY = INTERNAL_COMMENTS_KEY;

    private static final String JSON_SOLVED_KEY = INTERNAL_SOLVED_VALUE;

    private static final String JSON_REJECTEDGENES_KEY = "rejectedGenes";

    private static final List<String> STATUS_VALUES = Arrays.asList(INTERNAL_CANDIDATE_VALUE, INTERNAL_REJECTED_VALUE,
        INTERNAL_SOLVED_VALUE, INTERNAL_CARRIER_VALUE);

    private static final List<String> STRATEGY_VALUES = Arrays.asList("sequencing", "deletion", "familial_mutation",
        "common_mutations");

    @Inject
    private VocabularyManager vocabularyManager;

    private Vocabulary hgnc;

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
        return Arrays.asList(INTERNAL_GENE_KEY, INTERNAL_STATUS_KEY, INTERNAL_STRATEGY_KEY, INTERNAL_COMMENTS_KEY);
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
    public PatientData<Map<String, String>> load(Patient patient)
    {
        try {
            XWikiDocument doc = patient.getXDocument();
            List<BaseObject> geneXWikiObjects = doc.getXObjects(GENE_CLASS_REFERENCE);
            if (geneXWikiObjects == null || geneXWikiObjects.isEmpty()) {
                return null;
            }

            List<Map<String, String>> allGenes = new LinkedList<>();
            for (BaseObject geneObject : geneXWikiObjects) {
                if (geneObject == null || geneObject.getFieldList().isEmpty()) {
                    continue;
                }
                Map<String, String> singleGene = new LinkedHashMap<>();
                for (String property : getProperties()) {
                    String value = getFieldValue(geneObject, property);
                    if (value == null) {
                        continue;
                    }
                    singleGene.put(property, value);
                }
                allGenes.add(singleGene);
            }
            if (allGenes.isEmpty()) {
                return null;
            } else {
                return new IndexedPatientData<>(getName(), allGenes);
            }
        } catch (Exception e) {
            this.logger.error(ERROR_MESSAGE_LOAD_FAILED, e.getMessage());
        }
        return null;
    }

    private String getFieldValue(BaseObject geneObject, String property)
    {
        if (INTERNAL_STRATEGY_KEY.equals(property)) {
            StringListProperty fields = (StringListProperty) geneObject.getField(property);
            if (fields == null || fields.getList().size() == 0) {
                return null;
            }
            return fields.getTextValue();

        } else {
            BaseStringProperty field = (BaseStringProperty) geneObject.getField(property);
            if (field == null) {
                return null;
            }
            return field.getValue();
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        if (selectedFieldNames != null && !selectedFieldNames.contains(GENES_ENABLING_FIELD_NAME)) {
            return;
        }

        PatientData<Map<String, String>> data = patient.getData(getName());
        if (data == null || !data.isIndexed() || data.size() == 0) {
            if (selectedFieldNames == null || selectedFieldNames.contains(GENES_ENABLING_FIELD_NAME)) {
                json.put(getJsonPropertyName(), new JSONArray());
            }
            return;
        }

        // by this point we know there is some data since data.size() != 0
        JSONArray geneArray = new JSONArray();

        Iterator<Map<String, String>> iterator = data.iterator();
        while (iterator.hasNext()) {
            Map<String, String> item = iterator.next();
            if (!StringUtils.isBlank(item.get(INTERNAL_GENE_KEY))) {
                geneArray.put(generateGeneJSON(item));
            }
        }

        json.put(getJsonPropertyName(), geneArray);
    }

    private JSONObject generateGeneJSON(Map<String, String> data)
    {
        JSONObject newGene = new JSONObject();
        String geneId = data.get(INTERNAL_GENE_KEY).toString();
        newGene.put(JSON_GENE_ID, geneId);
        newGene.put(JSON_GENE_SYMBOL, getSymbol(geneId));
        setStringValueIfNotBlank(newGene, JSON_COMMENTS_KEY, data.get(INTERNAL_COMMENTS_KEY), null);
        setStringValueIfNotBlank(newGene, JSON_STATUS_KEY, data.get(INTERNAL_STATUS_KEY), INTERNAL_CANDIDATE_VALUE);
        setArrayValueIfNotBlank(newGene, JSON_STRATEGY_KEY, data.get(INTERNAL_STRATEGY_KEY));
        return newGene;
    }

    private void setStringValueIfNotBlank(JSONObject object, String key, String value, String defaultValue)
    {
        if (!StringUtils.isBlank(value)) {
            object.put(key, value);
        } else if (defaultValue != null) {
            object.put(key, defaultValue);
        }
    }

    private void setArrayValueIfNotBlank(JSONObject object, String key, String listString)
    {
        if (!StringUtils.isBlank(listString)) {
            object.put(key, new JSONArray(listString.split("\\|")));
        }
    }

    @Override
    public PatientData<Map<String, String>> readJSON(JSONObject json)
    {
        if (json == null
            || !(json.has(getJsonPropertyName()) || json.has(JSON_SOLVED_KEY) || json.has(JSON_REJECTEDGENES_KEY))) {
            return null;
        }

        try {
            List<Map<String, String>> accumulatedGenes = new LinkedList<>();

            parseGenesJson(json, accumulatedGenes);

            // v1.2.x json compatibility
            parseRejectedGenes(json, accumulatedGenes);
            parseSolvedGene(json, accumulatedGenes);

            return new IndexedPatientData<>(getName(), accumulatedGenes);
        } catch (Exception e) {
            this.logger.error("Could not load genes from JSON: [{}]", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Supports both 1.3-m5 and older 1.3-xx format.
     * 1.3-m5 and newer format:
     *   {"id": ENSEMBL_Id [[, "gene": HGNC_Symbol] , ...] }
     * 1.3-old format:
     *   {"gene": HGNC_Symbol [, ...] }
     */
    private void parseGenesJson(JSONObject json, List<Map<String, String>> allGenes)
    {
        JSONArray genesJson = json.optJSONArray(this.getJsonPropertyName());

        Set<String> alreadyCollectedGeneNames = new HashSet<>();

        if (genesJson != null) {
            for (int i = 0; i < genesJson.length(); ++i) {
                JSONObject geneJson = genesJson.getJSONObject(i);

                // gene ID is either the "id" field, or, if missing, the "gene" field
                String geneId = geneJson.optString(JSON_GENE_ID);
                if (StringUtils.isBlank(geneId)) {
                    geneId = geneJson.optString(JSON_GENE_SYMBOL);
                }
                geneId = getEnsemblId(geneId);

                // discard it if gene id is not present in the geneJson or is empty or is a duplicate
                // of an id that has been parsed and added to the list already
                if (StringUtils.isBlank(geneId) || alreadyCollectedGeneNames.contains(geneId)) {
                    continue;
                }

                Map<String, String> singleGene = parseOneGene(geneId, geneJson);

                // every gene should have a status; assume candidate if not specified in the input JSON
                if (!singleGene.containsKey(INTERNAL_STATUS_KEY)) {
                    singleGene.put(INTERNAL_STATUS_KEY, INTERNAL_CANDIDATE_VALUE);
                }

                allGenes.add(singleGene);
                alreadyCollectedGeneNames.add(geneId);
            }
        }
    }

    private void parseRejectedGenes(JSONObject json, List<Map<String, String>> allGenes)
    {
        Set<String> rejectedGeneNames = new HashSet<>();

        JSONArray rejectedGenes = json.optJSONArray(JSON_REJECTEDGENES_KEY);
        if (rejectedGenes != null && rejectedGenes.length() > 0) {
            for (int i = 0; i < rejectedGenes.length(); ++i) {
                JSONObject rejectedGeneJson = rejectedGenes.getJSONObject(i);

                String geneId = getEnsemblId(rejectedGeneJson.optString(JSON_DEPRECATED_GENE_ID));

                // discard it if gene symbol is blank or empty or duplicate
                if (StringUtils.isBlank(geneId) || rejectedGeneNames.contains(geneId)) {
                    continue;
                }

                Map<String, String> singleGene = new LinkedHashMap<>();
                singleGene.put(INTERNAL_GENE_KEY, geneId);
                singleGene.put(INTERNAL_STATUS_KEY, INTERNAL_REJECTED_VALUE);

                if (rejectedGeneJson.has(JSON_COMMENTS_KEY)
                    && !StringUtils.isBlank(rejectedGeneJson.getString(JSON_COMMENTS_KEY))) {
                    singleGene.put(INTERNAL_COMMENTS_KEY, rejectedGeneJson.getString(JSON_COMMENTS_KEY));
                }

                // overwrite the same gene if it was found to be a candidate
                addOrReplaceGene(allGenes, geneId, singleGene);

                rejectedGeneNames.add(rejectedGeneJson.getString(JSON_DEPRECATED_GENE_ID));
            }
        }
    }

    private void parseSolvedGene(JSONObject json, List<Map<String, String>> allGenes)
    {
        JSONObject solvedGene = json.optJSONObject(JSON_SOLVED_KEY);
        if (solvedGene == null) {
            return;
        }

        String geneId = getEnsemblId(solvedGene.optString(JSON_DEPRECATED_GENE_ID));

        if (!StringUtils.isBlank(geneId)) {
            Map<String, String> singleGene = new LinkedHashMap<>();
            singleGene.put(INTERNAL_GENE_KEY, geneId);
            singleGene.put(INTERNAL_STATUS_KEY, INTERNAL_SOLVED_VALUE);

            // overwrite the same gene if it was found to be a candidate or rejected
            addOrReplaceGene(allGenes, geneId, singleGene);
        }
    }

    private void addOrReplaceGene(List<Map<String, String>> allGenes, String geneId, Map<String, String> newGene)
    {
        // need index for replacement; performanc eis not critical since this code is only
        // used for old 1.2.x. patient JSONs
        for (int i = 0; i < allGenes.size(); i++) {
            if (StringUtils.equals(allGenes.get(i).get(INTERNAL_GENE_KEY), geneId)) {
                allGenes.set(i, newGene);
                return;
            }
        }
        allGenes.add(newGene);
    }

    // geneId has been parsed already by this point, so no need to parse it out again
    private Map<String, String> parseOneGene(String geneId, JSONObject geneJson)
    {
        Map<String, String> geneData = new LinkedHashMap<>();

        geneData.put(INTERNAL_GENE_KEY, geneId);

        addStringValue(geneJson, JSON_STATUS_KEY, geneData, INTERNAL_STATUS_KEY, STATUS_VALUES);

        // pass `null` since any value is accepted for the "comments" field
        addStringValue(geneJson, JSON_COMMENTS_KEY, geneData, INTERNAL_COMMENTS_KEY, null);

        addListValue(geneJson, JSON_STRATEGY_KEY, geneData, INTERNAL_STRATEGY_KEY, STRATEGY_VALUES);

        return geneData;
    }

    private void addStringValue(JSONObject geneJson, String jsonKey,
        Map<String, String> geneData, String internalKey, List<String> acceptedValues)
    {
        String value = geneJson.optString(jsonKey);
        if (!StringUtils.isBlank(value) && (acceptedValues == null || acceptedValues.contains(value))) {
            geneData.put(internalKey, value);
        }
    }

    private void addListValue(JSONObject geneJson, String jsonKey,
        Map<String, String> geneData, String internalKey, List<String> acceptedValues)
    {
        JSONArray valuesArray = geneJson.optJSONArray(jsonKey);
        if (valuesArray != null) {
            String internalValue = "";
            for (Object value : valuesArray) {
                if (acceptedValues.contains(value)) {
                    internalValue += "|" + value;
                }
            }
            geneData.put(internalKey, internalValue);
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
            final XWikiDocument docX = patient.getXDocument();
            final PatientData<Map<String, String>> genes = patient.getData(getName());
            if (genes == null) {
                if (PatientWritePolicy.REPLACE.equals(policy)) {
                    docX.removeXObjects(GENE_CLASS_REFERENCE);
                }
            } else {
                if (!genes.isIndexed()) {
                    this.logger.error(ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);
                    return;
                }
                saveGenes(docX, patient, genes, policy, this.xcontextProvider.get());
            }
        } catch (final Exception ex) {
            this.logger.error("Failed to save disorders data: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Saves {@code genes} data for {@code patient} according to the provided {@code policy}.
     *
     * @param docX the {@link XWikiDocument} for patient
     * @param patient the {@link Patient} object of interest
     * @param genes the newly added gene data
     * @param policy the policy according to which data should be saved
     * @param context the {@link XWikiContext} object
     */
    private void saveGenes(
        @Nonnull final XWikiDocument docX,
        @Nonnull final Patient patient,
        @Nonnull final PatientData<Map<String, String>> genes,
        @Nonnull final PatientWritePolicy policy,
        @Nonnull final XWikiContext context)
    {
        if (PatientWritePolicy.MERGE.equals(policy)) {
            final Map<String, Map<String, String>> mergedGenes = getMergedGenesMap(genes, load(patient));
            docX.removeXObjects(GENE_CLASS_REFERENCE);
            mergedGenes.forEach((id, gene) -> saveGene(docX, gene, context));
        } else {
            docX.removeXObjects(GENE_CLASS_REFERENCE);
            genes.forEach(gene -> saveGene(docX, gene, context));
        }
    }

    /**
     * Saves a {@code gene} for the provided {@code patient}.
     *
     * @param docX the {@link XWikiDocument} for patient
     * @param gene a gene to be saved
     * @param context the {@link XWikiContext} object
     */
    private void saveGene(
        @Nonnull final XWikiDocument docX,
        @Nonnull final Map<String, String> gene,
        @Nonnull final XWikiContext context)
    {
        try {
            final BaseObject xwikiObject = docX.newXObject(GENE_CLASS_REFERENCE, context);
            getProperties().forEach(property -> setGeneProperty(gene, property, xwikiObject, context));
        } catch (final XWikiException e) {
            this.logger.error("Failed to save a specific gene: [{}]", e.getMessage());
        }
    }

    /**
     * Saves gene {@code property} value provided in {@code gene} to the {@code xwikiObject}.
     *
     * @param gene a gene to be saved
     * @param property the property of interest
     * @param xwikiObject the {@link BaseObject} that will store the {@code gene} data
     * @param context the {@link XWikiContext} object
     */
    private void setGeneProperty(
        @Nonnull final Map<String, String> gene,
        @Nonnull final String property,
        @Nonnull final BaseObject xwikiObject,
        @Nonnull final XWikiContext context)
    {
        final String value = gene.get(property);
        if (value != null) {
            xwikiObject.set(property, value, context);
        }
    }

    /**
     * Create a map of gene ID to gene properties that merges existing and updated gene data.
     *
     * @param genes the gene data to add to patient
     * @param storedGenes the gene data already stored in patient
     * @return a map of gene ID to gene properties
     */
    private Map<String, Map<String, String>> getMergedGenesMap(
        @Nullable final PatientData<Map<String, String>> genes,
        @Nullable final PatientData<Map<String, String>> storedGenes)
    {
        // If map keys collide, merge genes in favour of the new value
        return Stream.of(storedGenes, genes)
            .filter(Objects::nonNull)
            .flatMap(s -> StreamSupport.stream(s.spliterator(), false))
            .collect(Collectors.toMap(gene
                -> gene.get(INTERNAL_GENE_KEY), Function.identity(), (v1, v2) -> v2, LinkedHashMap::new));
    }

    /**
     * Gets EnsemblID corresponding to the HGNC symbol.
     *
     * FIXME: refactor HGNC vocabulary to have "ensembl_gene_id" as a single value not a list?
     * FIXME: if done, refactor VocabularyTerm to simplify access to a String value using getString() instead of get()?
     *        currently VocabularyTerm.get() is only ever used here and in gene migrator
     * FIXME: in any case refactoring vocabulary code in _some_ way to avoid code duplication every time we need
     *        to convert geneSymbol to ensembleID?
     *
     * @param gene the string representation a gene, either geneSymbol (e.g. NOD2) or some other kind of ID
     * @return if gene is a vlaid geneSymbol, the corresponding Ensembl ID. Otherwise the original gene value
     */
    private String getEnsemblId(String gene)
    {
        final VocabularyTerm term = this.getTerm(gene);
        @SuppressWarnings("unchecked")
        final List<String> ensemblIdList = term != null ? (List<String>) term.get(ENSEMBL_ID_PROPERTY_NAME) : null;
        final String ensemblId = ensemblIdList != null && !ensemblIdList.isEmpty() ? ensemblIdList.get(0) : null;
        // retain information as is if we can't find Ensembl ID.
        return StringUtils.isBlank(ensemblId) ? gene : ensemblId;
    }

    private String getSymbol(String gene)
    {
        final VocabularyTerm term = this.getTerm(gene);
        final String symbol = (term != null) ? (String) term.get(SYMBOL_PROPERTY_NAME) : null;
        return StringUtils.isBlank(symbol) ? gene : symbol;
    }

    private VocabularyTerm getTerm(String gene)
    {
        // lazy-initialize HGNC
        if (this.hgnc == null) {
            this.hgnc = getHGNCVocabulary();
            if (this.hgnc == null) {
                return null;
            }
        }
        return this.hgnc.getTerm(gene);
    }

    private Vocabulary getHGNCVocabulary()
    {
        try {
            return this.vocabularyManager.getVocabulary(HGNC);
        } catch (Exception ex) {
            // this should not happen except when mocking, but does not hurt to catch in any case
            this.logger.error("Error loading component [{}]", ex.getMessage(), ex);
            return null;
        }
    }
}
