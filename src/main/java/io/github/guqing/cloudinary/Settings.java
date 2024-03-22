package io.github.guqing.cloudinary;

import lombok.Data;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

public class Settings {

    public static Mono<Credential> getCredential(ReactiveSettingFetcher settingFetcher) {
        return settingFetcher.fetch(Credential.GROUP, Credential.class);
    }

    @Data
    public static class Credential {
        public static final String GROUP = "credential";
        private String cloudName;
        private String apiKey;
        private String apiSecret;
    }
}
