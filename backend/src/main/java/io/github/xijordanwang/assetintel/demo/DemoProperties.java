package io.github.xijordanwang.assetintel.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assetintel.demo")
public record DemoProperties(boolean seedOnStartup, long randomSeed, int assetCount, int requestCount) {
}
