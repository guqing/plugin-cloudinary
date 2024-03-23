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
    private static final Pattern SRC_PATTERN = Pattern.compile("src=\"([^\"]*)\"");
    private static final Pattern IMG_TAG_PATTERN =
        Pattern.compile("<img\\s[^>]*src=(\"[^\"]*\"|[^\\s>]*)\\s*[^>]*>",
            Pattern.CASE_INSENSITIVE);

    public static Mono<String> processImgTagWithSrcSet(String html,
        Function<String, Mono<String>> srcSetValueGenerator) {
        Matcher matcher = IMG_TAG_PATTERN.matcher(html);
        List<Mono<String>> thumbnailMonos = new ArrayList<>();
        StringBuffer sb = new StringBuffer();
        AtomicInteger counter = new AtomicInteger();

        while (matcher.find()) {
            String imgTag = matcher.group();
            String src = extractSrc(imgTag);
            if (invalidSrc(src)) {
                continue;
            }

            var thumbnailMono = srcSetValueGenerator.apply(src)
                .map(srcSetValue -> imgTag.replaceAll("src=\"[^\"]*\"",
                    "src=\"" + src + "\" srcset=\"" + srcSetValue + "\""));
            thumbnailMonos.add(thumbnailMono);
            matcher.appendReplacement(sb,
                "{" + buildPlaceholder(counter.getAndIncrement()) + "}");
        }
        matcher.appendTail(sb);

        return Flux.mergeSequential(thumbnailMonos)
            .collectList()
            .map(thumbnails -> {
                String resultHtml = sb.toString();
                for (int i = 0; i < thumbnails.size(); i++) {
                    resultHtml =
                        resultHtml.replace("{" + buildPlaceholder(i) + "}", thumbnails.get(i));
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

    private static String extractSrc(String imgTag) {
        Matcher srcMatcher = SRC_PATTERN.matcher(imgTag);
        if (srcMatcher.find()) {
            return srcMatcher.group(1);
        }
        return "";
    }
}
