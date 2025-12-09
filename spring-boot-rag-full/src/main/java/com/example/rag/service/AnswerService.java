package com.example.rag.service;

import com.example.rag.service.VectorStore.SearchResult;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnswerService {

    @Value("${openai.api.key:}")
    private String openaiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    public AnswerResponse generateAnswer(String question, List<SearchResult> retrieved) throws IOException {
        if (openaiKey == null || openaiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY not set");
        }

        StringBuilder context = new StringBuilder();
        for (SearchResult r : retrieved) {
            context.append("[Source: ").append(r.chunk.getUrl()).append("]\n");
            context.append(r.chunk.getText()).append("\n\n");
        }

        String prompt = "You are a strict assistant. Answer the user question using ONLY the context below. If the answer cannot be found, reply with 'The information is not available in the provided sources.'\n\nCONTEXT:\n" + context.toString()
                + "\nUSER QUESTION:\n" + question + "\n\nAnswer concisely and factually.";
        String url = "https://api.openai.com/v1/chat/completions";
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);
        post.addHeader("Authorization", "Bearer " + openaiKey);
        post.addHeader("Content-Type", "application/json");

        var payload = mapper.createObjectNode();
        payload.put("model", "gpt-4o-mini");
        var messages = mapper.createArrayNode();
        var sys = mapper.createObjectNode(); sys.put("role", "system"); sys.put("content", "You answer strictly from provided context.");
        var usr = mapper.createObjectNode(); usr.put("role", "user"); usr.put("content", prompt);
        messages.add(sys); messages.add(usr);
        payload.set("messages", messages);

        post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));

        try (var resp = client.execute(post)) {
            JsonNode root = mapper.readTree(resp.getEntity().getContent());
            String text = root.at("/choices/0/message/content").asText();

            Set<String> sources = retrieved.stream().map(r -> r.chunk.getUrl()).collect(Collectors.toSet());

            return new AnswerResponse(text.trim(), sources.stream().collect(Collectors.toList()));
        }
    }

    public static class AnswerResponse {
        public String answer;
        public List<String> sources;
        public AnswerResponse(String answer, List<String> sources) { this.answer = answer; this.sources = sources; }
    }
}
