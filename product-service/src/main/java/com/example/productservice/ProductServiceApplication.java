package com.example.productservice;

import co.elastic.apm.attach.ElasticApmAttacher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        attachElasticApm();
        SpringApplication.run(ProductServiceApplication.class, args);
    }

    private static void attachElasticApm() {
        Map<String, String> config = new HashMap<>();
        config.put("service_name", env("ELASTIC_APM_SERVICE_NAME", "product-service"));
        config.put("server_urls", env("ELASTIC_APM_SERVER_URLS", "http://localhost:8200"));
        config.put("environment", env("ELASTIC_APM_ENVIRONMENT", "local"));
        config.put("application_packages", "com.example.productservice");
        config.put("transaction_sample_rate", env("ELASTIC_APM_TRANSACTION_SAMPLE_RATE", "1.0"));
        ElasticApmAttacher.attach(config);
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
