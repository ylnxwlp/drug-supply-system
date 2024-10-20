package com.supply.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.elasticsearch")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ElasticsearchProperties {
    private String uris;
    private String username;
    private String password;
}
