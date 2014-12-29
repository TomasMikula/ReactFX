package org.reactfx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

class ThreadBridge<T> extends EventStreamBase<T> {
    private final EventStream<T> input;
    private final Executor sourceThreadExecutor;
    private final Executor targetThreadExecutor;

    public ThreadBridge(
            EventStream<T> input,
            Executor sourceThreadExecutor,
            Executor targetThreadExecutor) {
        this.input = input;
        this.sourceThreadExecutor = sourceThreadExecutor;
        this.targetThreadExecutor = targetThreadExecutor;
    }

    @Override
    protected Subscription bindToInputs() {
        CompletableFuture<Subscription> subscription = new CompletableFuture<>();
        sourceThreadExecutor.execute(() -> {
            subscription.complete(
                    input.subscribe(e -> {
                        targetThreadExecutor.execute(() -> emit(e));
                    }));
        });
        return () -> {
            subscription.thenAcceptAsync(
                    Subscription::unsubscribe,
                    sourceThreadExecutor);
        };
    }
}