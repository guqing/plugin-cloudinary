package io.github.guqing.cloudinary;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ImageTagParser {
    private static final Pattern IMG_TAG_PATTERN =
        Pattern.compile("<img ([^>]*?)src=(\"([^\"]+)\"|'([^']+)')([^>]*?)>",
            Pattern.CASE_INSENSITIVE);

    public static Mono<String> processImgTagWithSrcSet(String html,
        Function<String, Mono<String>> srcSetValueGenerator) {
        Matcher matcher = IMG_TAG_PATTERN.matcher(html);
        List<Mono<String>> thumbnailMonos = new ArrayList<>();
        var sb = new StringBuilder();
        AtomicInteger counter = new AtomicInteger();

        while (matcher.find()) {
            String beforeSrc = matcher.group(1);
            String src = matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
            String afterSrc = matcher.group(5);
            if (invalidSrc(src) || afterSrc.contains("srcset=\"")) {
                continue;
            }

            var fallback =
                Mono.just(String.format("<img %ssrc=\"%s\"%s>", beforeSrc, src, afterSrc));
            var thumbnailMono = srcSetValueGenerator.apply(src)
                .map(srcSetValue -> String.format("<img %ssrc=\"%s\" srcset=\"%s\"%s>",
                    beforeSrc, src, srcSetValue, afterSrc))
                // Handle srcset generation failure
                .onErrorResume(e -> fallback)
                .switchIfEmpty(fallback);
            thumbnailMonos.add(thumbnailMono);
            // replacement can't include $
            matcher.appendReplacement(sb,
                "#({" + buildPlaceholder(counter.getAndIncrement()) + "})");
        }
        matcher.appendTail(sb);

        return Flux.mergeSequential(thumbnailMonos)
            .collectList()
            .map(thumbnails -> {
                String resultHtml = sb.toString();
                for (int i = 0; i < thumbnails.size(); i++) {
                    resultHtml =
                        resultHtml.replace("#({" + buildPlaceholder(i) + "})", thumbnails.get(i));
                }
                return resultHtml;
            });
    }

    static boolean invalidSrc(String src) {
        if (StringUtils.isBlank(src)) {
            return true;
        }
        try {
            // ignore return value
            URI.create(src);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static String buildPlaceholder(int count) {
        return "_cloudinary_envoy_" + count;
    }
}
