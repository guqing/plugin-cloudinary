package io.github.guqing.cloudinary;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/**
 * Tests for {@link ImageTagParser}.
 *
 * @author guqing
 * @since 1.1.0
 */
class ImageTagParserTest {

    @Test
    public void testWithNoSrc() {
        String html = "<img alt=\"no src tag\">";
        String expected = "<img alt=\"no src tag\">";
        Mono<String> result =
            ImageTagParser.processImgTagWithSrcSet(html, src -> Mono.just("100w, 200w"));
        assertEquals(expected, result.block());
    }

    @Test
    public void testWithExistingSrcset() {
        String html = "<img src=\"image.jpg\" srcset=\"existing-srcset\">";
        String expected = "<img src=\"image.jpg\" srcset=\"existing-srcset\">";
        Mono<String> result =
            ImageTagParser.processImgTagWithSrcSet(html, src -> Mono.just("100w, 200w"));
        assertEquals(expected, result.block());
    }

    @Test
    public void testWithValidSrc() {
        String html = "<img src=\"image.jpg\">";
        String expected = "<img src=\"image.jpg\" srcset=\"100w, 200w\">";
        Mono<String> result =
            ImageTagParser.processImgTagWithSrcSet(html, src -> Mono.just("100w, 200w"));
        assertEquals(expected, result.block());
    }

    @Test
    public void testWithSingleQuoteSrc() {
        String html = "<img src='image.jpg'>";
        String expected = "<img src=\"image.jpg\" srcset=\"100w, 200w\">";
        Mono<String> result =
            ImageTagParser.processImgTagWithSrcSet(html, src -> Mono.just("100w, 200w"));
        assertEquals(expected, result.block());
    }

    @Test
    public void testWithMultipleImages() {
        String html = "<img src=\"image1.jpg\"><img src=\"image2.jpg\">";
        String expected =
            "<img src=\"image1.jpg\" srcset=\"100w, 200w\"><img src=\"image2.jpg\" srcset=\"300w,"
                + " 400w\">";
        Mono<String> result = ImageTagParser.processImgTagWithSrcSet(html, src ->
            src.equals("image1.jpg") ? Mono.just("100w, 200w") : Mono.just("300w, 400w"));
        assertEquals(expected, result.block());
    }

    @Test
    public void testAsyncSrcsetGenerationFailure() {
        String html = "<img src=\"image.jpg\">";
        // Expect original tag on failure
        String expected = "<img src=\"image.jpg\">";
        Mono<String> result = ImageTagParser.processImgTagWithSrcSet(html,
            src -> Mono.error(new RuntimeException("Failed to generate srcset")));
        assertEquals(expected, result.block());
    }
}
