package com.example.rag.model;

public class Page {
    private String url;
    private String title;
    private String html;

    public Page() {}
    public Page(String url, String title, String html) {
        this.url = url;
        this.title = title;
        this.html = html;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getHtml() { return html; }
    public void setHtml(String html) { this.html = html; }
}
