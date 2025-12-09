package com.example.rag.service;

import com.example.rag.service.VectorStore.SearchResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class RetrievalService {

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    public RetrievalService(EmbeddingService embeddingService, VectorStore vectorStore) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    public List<SearchResult> retrieve(String query, int topK) throws IOException {
        float[] qvec = embeddingService.embedText(query);
        return vectorStore.search(qvec, topK);
    }
}
