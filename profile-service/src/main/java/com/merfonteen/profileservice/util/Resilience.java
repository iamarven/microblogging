package com.merfonteen.profileservice.util;

import com.merfonteen.exceptions.ServiceUnavailableException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
@Component
public class Resilience {
    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry cbRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final ExecutorService executor;

    public <T> T userCall(Supplier<T> supplier) {
        var cb = cbRegistry.circuitBreaker("userClient");
        var retry = retryRegistry.retry("userClient");
        var tl = timeLimiterRegistry.timeLimiter("userClient");
        var bh = bulkheadRegistry.bulkhead("userClient");

        Supplier<T> decorated = Decorators.ofSupplier(supplier)
                .withCircuitBreaker(cb)
                .withRetry(retry)
                .withBulkhead(bh)
                .decorate();

        Supplier<CompletableFuture<T>> futureSupplier = () -> CompletableFuture.supplyAsync(decorated, executor);
        Callable<T> restrictedCall = TimeLimiter.decorateFutureSupplier(tl, futureSupplier);

        try {
            return restrictedCall.call();
        } catch (Exception e) {
            log.error("Error getting general user info: {}", e.getMessage());
            throw new ServiceUnavailableException("User service is temporarily unavailable");
        }
    }
}
