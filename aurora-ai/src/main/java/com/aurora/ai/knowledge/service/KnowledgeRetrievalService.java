package com.aurora.ai.knowledge.service;

import com.aurora.ai.knowledge.model.resp.KnowledgeCitation;

import java.util.List;

public interface KnowledgeRetrievalService {
    List<KnowledgeCitation> search(String query, int topK);
}