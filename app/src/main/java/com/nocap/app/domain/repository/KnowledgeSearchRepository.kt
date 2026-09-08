package com.nocap.app.domain.repository

import com.nocap.app.domain.model.KnowledgeItemType
import com.nocap.app.domain.model.KnowledgeSearchResult
import com.nocap.app.domain.model.PublicationFormat

interface KnowledgeSearchRepository {
    suspend fun search(
        query: String,
        typeFilter: KnowledgeItemType = KnowledgeItemType.ALL,
        formatFilter: PublicationFormat? = null
    ): List<KnowledgeSearchResult>
}
