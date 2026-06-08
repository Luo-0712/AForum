package com.example.aforum.search.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "search")
public class SearchProperties {

    private boolean enabled = true;
    private boolean fallbackToMysql = true;
    private Elasticsearch elasticsearch = new Elasticsearch();

    @Data
    public static class Elasticsearch {
        private List<String> uris = new ArrayList<>(List.of("http://localhost:9200"));
        private String username;
        private String password;
        private String postIndex = "aforum_posts";
        private String postIndexVersion = "aforum_posts_v1";
    }
}
