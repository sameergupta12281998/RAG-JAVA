package com.example.rag.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExtractorService {

    private static final List<String> REMOVE_SELECTORS = Arrays.asList(
            "nav", "header", "footer", "aside",
            ".navbar", ".nav", "#nav",
            ".footer", "#footer", ".header",
            ".cookie", ".cookie-banner", "#cookie-banner",
            ".ads", ".advertisement", ".promo",
            ".modal", ".popup", ".newsletter",
            "script", "style", "noscript"
    );

    public String cleanHtml(String html) {
        Document doc = Jsoup.parse(html);

        // remove selectors
        for (String sel : REMOVE_SELECTORS) {
            Elements els = doc.select(sel);
            for (Element e : els) e.remove();
        }

        // remove comments
        for (Comment c : doc.select("*").stream()
                .flatMap(e -> e.childNodes().stream())
                .filter(n -> n instanceof Comment)
                .map(n -> (Comment) n)
                .collect(Collectors.toList())) {
            c.remove();
        }

        // remove script/style explicitly
        doc.select("script, style, noscript").remove();

        String text = doc.body() == null ? "" : doc.body().text();

        // normalize whitespace and remove empty lines
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }
}
