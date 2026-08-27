package com.example.aforum.common.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    private boolean enabled = true;

    private long postListTtlSeconds = 120;

    private long postDetailTtlSeconds = 300;

    private long searchOptionsTtlSeconds = 600;

    private long negativeTtlSeconds = 30;
}
