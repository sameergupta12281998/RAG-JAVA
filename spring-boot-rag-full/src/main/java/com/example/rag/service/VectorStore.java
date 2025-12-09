package com.example.rag.service;

import com.example.rag.model.Chunk;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class VectorStore implements Serializable {

    private final List<float[]> vectors = new ArrayList<>();
    private final List<Chunk> metadata = new ArrayList<>();

    public synchronized void clear() {
        vectors.clear();
        metadata.clear();
    }

    public synchronized void add(float[] vector, Chunk chunk) {
        vectors.add(vector);
        metadata.add(chunk);
    }

    public synchronized List<SearchResult> search(float[] query, int topK) {
        PriorityQueue<SearchResult> pq = new PriorityQueue<>(Comparator.comparingDouble(r -> -r.score));

        for (int i = 0; i < vectors.size(); i++) {
            float[] v = vectors.get(i);
            double score = cosineSimilarity(query, v);
            pq.add(new SearchResult(metadata.get(i), score));
        }

        List<SearchResult> out = new ArrayList<>();
        int n = Math.min(topK, pq.size());
        for (int i = 0; i < n; i++) out.add(pq.poll());
        return out;
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb) + 1e-10);
    }

    public synchronized void saveToFile(String path) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(this);
        }
    }

    public static VectorStore loadFromFile(String path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            return (VectorStore) ois.readObject();
        }
    }

    public static class SearchResult {
        public Chunk chunk;
        public double score;
        public SearchResult(Chunk chunk, double score) { this.chunk = chunk; this.score = score; }
    }
}
