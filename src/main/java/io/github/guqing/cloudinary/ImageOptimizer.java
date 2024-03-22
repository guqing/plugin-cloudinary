package io.github.guqing.cloudinary;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

public interface ImageOptimizer {
    Mono<SrcSet> optimize(String imageUrl);

    class SrcSet {
        private final Set<SrcSetItem> srcSet = new HashSet<>();

        public SrcSet add(String imageUrl, String width) {
            this.srcSet.add(new SrcSetItem(imageUrl, width));
            return this;
        }

        public String generateSrcSet() {
            return srcSet.stream()
                .map(item -> item.imageUrl() + " " + item.width() + "w")
                .collect(Collectors.joining(", "));
        }

        public record SrcSetItem(String imageUrl, String width) {
        }
    }
}
