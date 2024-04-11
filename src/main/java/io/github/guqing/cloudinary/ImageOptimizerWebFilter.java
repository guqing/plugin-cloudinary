package io.github.guqing.cloudinary;

import static java.nio.ByteBuffer.allocateDirect;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.MediaTypeServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.security.AdditionalWebFilter;

@Component
@RequiredArgsConstructor
public class ImageOptimizerWebFilter implements AdditionalWebFilter {
    private final ServerWebExchangeMatcher pathMatcher = createPathMatcher();

    private final ImageOptimizer imageOptimizer;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        return pathMatcher.matches(exchange)
            .flatMap(matchResult -> {
                if (matchResult.isMatch() && shouldOptimize(exchange)) {
                    var decoratedExchange = exchange.mutate()
                        .response(new ImageOptimizerResponseDecorator(exchange))
                        .build();
                    return chain.filter(decoratedExchange);
                }
                return chain.filter(exchange);
            });
    }

    boolean shouldOptimize(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        var statusCode = response.getStatusCode();
        return statusCode != null && statusCode.isSameCodeAs(HttpStatus.OK);
    }

    ServerWebExchangeMatcher createPathMatcher() {
        var pathMatcher = pathMatchers(HttpMethod.GET, "/**");
        var mediaTypeMatcher = new MediaTypeServerWebExchangeMatcher(MediaType.TEXT_HTML);
        mediaTypeMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        return new AndServerWebExchangeMatcher(pathMatcher, mediaTypeMatcher);
    }

    class ImageOptimizerResponseDecorator extends ServerHttpResponseDecorator {

        public ImageOptimizerResponseDecorator(ServerWebExchange exchange) {
            super(exchange.getResponse());
        }

        boolean isHtmlResponse(ServerHttpResponse response) {
            return response.getHeaders().getContentType() != null &&
                response.getHeaders().getContentType().includes(MediaType.TEXT_HTML);
        }

        @Override
        @NonNull
        public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
            var response = getDelegate();
            if (!isHtmlResponse(response)) {
                return super.writeWith(body);
            }
            var bodyWrap = Flux.from(body)
                .map(dataBuffer -> {
                    var byteBuffer = allocateDirect(dataBuffer.readableByteCount());
                    dataBuffer.toByteBuffer(byteBuffer);
                    DataBufferUtils.release(dataBuffer);
                    return byteBuffer.asReadOnlyBuffer();
                })
                .collectSortedList()
                .flatMap(byteBuffers -> {
                    var html = byteBuffersToString(byteBuffers);
                    return ImageTagParser.processImgTagWithSrcSet(html,
                            src -> imageOptimizer.optimize(src)
                                .map(ImageOptimizer.SrcSet::generateSrcSet)
                        )
                        .map(ImageOptimizerWebFilter.this::stringToByteBuffer);
                })
                .map(byteBuffer -> response.bufferFactory().wrap(byteBuffer));
            return super.writeWith(bodyWrap);
        }
    }

    private String byteBuffersToString(List<ByteBuffer> byteBuffers) {
        int total = byteBuffers.stream().mapToInt(ByteBuffer::remaining).sum();
        ByteBuffer combined = ByteBuffer.allocate(total);

        for (ByteBuffer buffer : byteBuffers) {
            combined.put(buffer);
        }

        combined.flip();
        byte[] byteArray = new byte[combined.remaining()];
        combined.get(byteArray);

        return new String(byteArray, StandardCharsets.UTF_8);
    }

    public ByteBuffer stringToByteBuffer(String str) {
        byte[] byteArray = str.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.wrap(byteArray);
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 100;
    }
}
