package org.reactfx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.reactfx.util.Either;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuple3;

class ThreadBridge<T> extends LazilyBoundStream<T> {
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
    protected Subscription subscribeToInputs() {
        CompletableFuture<Subscription> subscription = new CompletableFuture<>();
        sourceThreadExecutor.execute(() -> {
            subscription.complete(
                    Subscription.multi(
                            input.subscribe(e -> {
                                targetThreadExecutor.execute(() -> emit(e));
                            }),
                            input.monitor(error -> {
                                targetThreadExecutor.execute(() -> reportError(error));
                            })));
        });
        return () -> {
            subscription.thenAcceptAsync(
                    sub -> sub.unsubscribe(),
                    sourceThreadExecutor);
        };
    }
}

class BiThreadBridge<A, B>
extends ThreadBridge<Tuple2<A, B>>
implements PoorMansBiStream<A, B> {

    public BiThreadBridge(EventStream<Tuple2<A, B>> input,
            Executor sourceThreadExecutor, Executor targetThreadExecutor) {
        super(input, sourceThreadExecutor, targetThreadExecutor);
    }
}

class TriThreadBridge<A, B, C>
extends ThreadBridge<Tuple3<A, B, C>>
implements PoorMansTriStream<A, B, C> {

    public TriThreadBridge(EventStream<Tuple3<A, B, C>> input,
            Executor sourceThreadExecutor, Executor targetThreadExecutor) {
        super(input, sourceThreadExecutor, targetThreadExecutor);
    }
}

@Deprecated
class EitherThreadBridge<L, R>
extends ThreadBridge<Either<L, R>>
implements EitherEventStream<L, R> {

    public EitherThreadBridge(
            EventStream<Either<L, R>> input,
            Executor sourceThreadExecutor,
            Executor targetThreadExecutor) {
        super(input, sourceThreadExecutor, targetThreadExecutor);
    }

}