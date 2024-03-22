package io.github.guqing.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.infra.utils.PathUtils;
import run.halo.app.plugin.ReactiveSettingFetcher;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultImageOptimizer implements ImageOptimizer {

    private final static List<String> WIDTHS = List.of("480", "768", "1024", "1920");

    private final ExternalLinkProcessor externalLinkProcessor;
    private final ReactiveSettingFetcher settingFetcher;

    @Override
    public Mono<SrcSet> optimize(String imageUrl) {
        return Settings.getCredential(settingFetcher)
            .map(credential -> {
                var cloudinary = createCloudinary(credential);
                var srcSet = new SrcSet();
                var processedLink = processLink(imageUrl);
                for (String width : WIDTHS) {
                    var url = scaleImage(processedLink, width).apply(cloudinary);
                    srcSet.add(url, width);
                }
                return srcSet;
            })
            .doOnError(e -> log.error("Failed to optimize image: [{}]", imageUrl, e));
    }

    String processLink(String uri) {
        if (PathUtils.isAbsoluteUri(uri)) {
            return uri;
        }
        return externalLinkProcessor.processLink(uri);
    }

    private Function<Cloudinary, String> scaleImage(String imageUrl, String width) {
        return cloudinary -> cloudinary.url()
            .secure(true)
            .type("fetch")
            .transformation(new Transformation()
                .crop("limit")
                .quality("70")
                .fetchFormat("webp")
                .width(width)
            )
            .generate(imageUrl);
    }

    public Cloudinary createCloudinary(Settings.Credential credential) {
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", credential.getCloudName(),
            "api_key", credential.getApiKey(),
            "api_secret", credential.getApiSecret()));
    }
}
