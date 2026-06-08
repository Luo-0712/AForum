package com.example.aforum.common.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.example.aforum.search.config.SearchProperties;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SearchProperties.class)
public class ElasticsearchConfig {

    private final SearchProperties searchProperties;

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
    public RestClient elasticsearchRestClient() {
        HttpHost[] hosts = searchProperties.getElasticsearch().getUris().stream()
                .map(URI::create)
                .map(uri -> new HttpHost(uri.getHost(), port(uri), uri.getScheme()))
                .toArray(HttpHost[]::new);

        RestClientBuilder builder = RestClient.builder(hosts);
        String username = searchProperties.getElasticsearch().getUsername();
        String password = searchProperties.getElasticsearch().getPassword();
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }
        return builder.build();
    }

    private int port(URI uri) {
        if (uri.getPort() > 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
    public ElasticsearchTransport elasticsearchTransport(RestClient elasticsearchRestClient) {
        return new RestClientTransport(elasticsearchRestClient, new JacksonJsonpMapper());
    }

    @Bean
    @ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport elasticsearchTransport) {
        return new ElasticsearchClient(elasticsearchTransport);
    }
}
