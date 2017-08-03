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
package org.phenotips.vocabulary;

import org.xwiki.cache.Cache;
import org.xwiki.component.annotation.Role;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.stability.Unstable;

import org.apache.solr.client.solrj.SolrClient;

/**
 * Provides methods for initializing the resources needed by vocabularies stored in a Solr index.
 *
 * @version $Id$
 * @since 1.2M4 (under different names since 1.0M10)
 */
@Unstable
@Role
public interface SolrVocabularyResourceManager
{
    /**
     * Get the cache instance created for handling vocabulary terms.
     *
     * @param vocabularyId the identifier of the target vocabulary
     * @return a cache instance
     */
    Cache<VocabularyTerm> getTermCache(String vocabularyId);

    /**
     * Get the Solr core used for a vocabulary.
     *
     * @param vocabularyId the identifier of the target vocabulary
     * @return a Solr client for communication with the target core
     */
    SolrClient getSolrConnection(String vocabularyId);

    /**
     * Copy the Solr configuration file in a separate temporary directory, then register this temporary core with the
     * Solr server. This core can be accessed with {@link #getReplacementSolrConnection(String)} during reindexing, and
     * then it can either be discarded with {@link #discardReplacementCore(String)}, or take the place of the
     * {@link #getSolrConnection(String) official vocabulary index} with {@link #replaceCore(String)}.
     *
     * @param vocabularyId the identifier of the target vocabulary
     * @throws InitializationException if the process fails
     * @since 1.4
     */
    void createReplacementCore(String vocabularyId) throws InitializationException;

    /**
     * Copy new index data from the temporary core to the main index location.
     *
     * @param vocabularyId the identifier of the target vocabulary
     * @throws InitializationException if the process fails
     * @since 1.4
     */
    void replaceCore(String vocabularyId) throws InitializationException;

    /**
     * Get the temporary Solr core used for a vocabulary during reindexing.
     *
     * @param vocabularyId the identifier of the target vocabulary
     * @return a Solr client for communication with the target temporary core
     * @since 1.4
     */
    SolrClient getReplacementSolrConnection(String vocabularyId);

    /**
     * Delete the temporary core, if one was already created by {@link #createReplacementCore(String)}. If no temporary
     * core was created, nothing happens.
     *
     * @param vocabularyId the identifier of the target vocabulary
     * @since 1.4
     */
    void discardReplacementCore(String vocabularyId);
}
