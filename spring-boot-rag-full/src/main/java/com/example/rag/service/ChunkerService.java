package com.example.rag.service;

import com.example.rag.model.Chunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkerService {

    public List<Chunk> chunkText(String url, String title, String text, int chunkSize, int overlap) {
        List<Chunk> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) return chunks;

        int start = 0;
        int len = text.length();

        while (start < len) {
            int end = Math.min(len, start + chunkSize);
            String chunkText = text.substring(start, end);

            Chunk c = new Chunk(UUID.randomUUID().toString(), url, title, chunkText);
            chunks.add(c);

            if (end == len) break;
            start += (chunkSize - overlap);
        }

        return chunks;
    }
}
