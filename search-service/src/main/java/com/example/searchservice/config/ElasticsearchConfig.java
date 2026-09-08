package com.example.searchservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.example.searchservice.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        URI firstUri = URI.create(elasticsearchUris.split(",")[0].trim());
        String[] endpoints = Arrays.stream(elasticsearchUris.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::toEndpoint)
                .toArray(String[]::new);

        ClientConfiguration.TerminalClientConfigurationBuilder builder =
                ClientConfiguration.builder()
                        .connectedTo(endpoints)
                        .usingSsl("https".equalsIgnoreCase(firstUri.getScheme()))
                        .withConnectTimeout(Duration.ofSeconds(5))
                        .withSocketTimeout(Duration.ofSeconds(30));

        if (StringUtils.hasText(username)) {
            builder = builder.withBasicAuth(username, password);
        }

        return builder.build();
    }

    private String toEndpoint(String uriValue) {
        URI uri = URI.create(uriValue);
        int port = uri.getPort() == -1 ? defaultPort(uri.getScheme()) : uri.getPort();
        return uri.getHost() + ":" + port;
    }

    private int defaultPort(String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }
}
