package com.example.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    @Value("${openai.api.key:}")
    private String openaiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    public float[] embedText(String text) throws IOException {
        if (openaiKey == null || openaiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not set in application.properties or env");
        }

        String url = "https://api.openai.com/v1/embeddings";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);
        post.addHeader("Authorization", "Bearer " + openaiKey);
        post.addHeader("Content-Type", "application/json");

        // using text-embedding-3-small or all-MiniLM replacement? We'll use text-embedding-3-small as a general choice
        String body = mapper.writeValueAsString(new EmbeddingRequest("text-embedding-3-small", text));
        post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

        try (var resp = client.execute(post)) {
            var entity = resp.getEntity();
            JsonNode root = mapper.readTree(entity.getContent());
            JsonNode arr = root.get("data").get(0).get("embedding");
            float[] vec = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) vec[i] = (float) arr.get(i).asDouble();
            return vec;
        }
    }

    private static class EmbeddingRequest {
        public String model;
        public String input;
        public EmbeddingRequest(String model, String input) { this.model = model; this.input = input; }
    }
}
