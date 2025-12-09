package com.example.rag.model;

public class Chunk {
    private String chunkId;
    private String url;
    private String title;
    private String text;

    public Chunk() {}

    public Chunk(String chunkId, String url, String title, String text) {
        this.chunkId = chunkId;
        this.url = url;
        this.title = title;
        this.text = text;
    }

    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
