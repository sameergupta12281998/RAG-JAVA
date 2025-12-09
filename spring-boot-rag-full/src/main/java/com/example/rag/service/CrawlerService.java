package com.example.rag.service;

import com.example.rag.model.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class CrawlerService {

    @Value("${crawler.maxDepth:2}")
    private int maxDepth;

    @Value("${crawler.maxPages:50}")
    private int maxPages;

    private static final List<String> SKIP_PATTERNS = Arrays.asList(
            "login", "signup", "register", "cart", "checkout",
            "logout", "admin", "dashboard", "account", "profile", "wp-login"
    );

    public List<Page> crawl(String baseUrl) {
        Set<String> visited = new HashSet<>();
        List<Page> pages = new ArrayList<>();

        Queue<Node> queue = new LinkedBlockingQueue<>();
        queue.add(new Node(baseUrl, 0));

        String domain = extractDomain(baseUrl);

        while (!queue.isEmpty() && visited.size() < maxPages) {
            Node node = queue.poll();
            String url = normalizeUrl(node.url);

            if (url == null) continue;
            if (visited.contains(url)) continue;
            if (node.depth > maxDepth) continue;
            if (shouldSkip(url)) continue;
            if (!isSameDomain(domain, url)) continue;

            try {
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (compatible; SimpleCrawler/1.0)")
                        .timeout(10000)
                        .get();

                String title = doc.title();
                String html = doc.html();

                pages.add(new Page(url, title != null ? title : "", html));
                visited.add(url);

                // extract links
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String href = link.attr("abs:href");
                    href = href.split("#")[0];
                    if (href == null || href.isEmpty()) continue;
                    if (!visited.contains(href)) {
                        queue.add(new Node(href, node.depth + 1));
                    }
                }

            } catch (IOException e) {
                // skip
            }
        }

        return pages;
    }

    private boolean shouldSkip(String url) {
        String lower = url.toLowerCase();
        for (String p : SKIP_PATTERNS) if (lower.contains(p)) return true;
        return false;
    }

    private static String normalizeUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath() == null ? "" : uri.getPath();
            String normalized = scheme + "://" + host + (port == -1 ? "" : ":"+port) + path;
            return normalized.replaceAll("/$", "");
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static boolean isSameDomain(String domain, String url) {
        if (domain == null) return false;
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return false;
            return host.endsWith(domain);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static class Node {
        String url;
        int depth;
        Node(String url, int depth) { this.url = url; this.depth = depth; }
    }
}
