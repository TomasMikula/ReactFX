package org.reactfx;

import static javafx.concurrent.WorkerStateEvent.*;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.concurrent.Task;

abstract class AsyncMapBase<T, U, F> extends LazilyBoundStream<U> {
    private final EventStream<T> source;
    private final Function<T, F> f;

    public AsyncMapBase(
            EventStream<T> source,
            Function<T, F> f) {
        this.source = source;
        this.f = f;
    }

    @Override
    protected final Subscription subscribeToInputs() {
        return source.subscribe(evt -> {
            F future = f.apply(evt);
            execOnSuccess(future, this::emit);
        });
    }

    protected abstract void execOnSuccess(F future, Consumer<U> action);
}

class AsyncMap<T, U> extends AsyncMapBase<T, U, CompletionStage<U>> {
    private final Executor clientThreadExecutor;

    public AsyncMap(
            EventStream<T> source,
            Function<T, CompletionStage<U>> f,
            Executor clientThreadExecutor) {
        super(source, f);
        this.clientThreadExecutor = clientThreadExecutor;
    }

    @Override
    protected void execOnSuccess(CompletionStage<U> future, Consumer<U> f) {
        future.thenAcceptAsync(f, clientThreadExecutor);
    }
}

class BackgroundMap<T, U> extends AsyncMapBase<T, U, Task<U>> {

    public BackgroundMap(EventStream<T> source, Function<T, Task<U>> f) {
        super(source, f);
    }

    @Override
    protected void execOnSuccess(Task<U> t, Consumer<U> f) {
        t.addEventHandler(WORKER_STATE_SUCCEEDED, e -> f.accept(t.getValue()));
    }
}


abstract class LatestOnlyAsyncMapBase<T, U, F> extends LazilyBoundStream<U> {
    private final EventStream<T> source;
    private final Function<T, F> f;

    private long revision = 0;
    private F expectedFuture = null;

    public LatestOnlyAsyncMapBase(
            EventStream<T> source,
            Function<T, F> f) {
        this.source = source;
        this.f = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return source.subscribe(evt -> {
            F expected = f.apply(evt);
            long rev = replaceExpected(expected);
            execOnSuccess(expected, u -> {
                if(rev == revision) {
                    emit(u);
                }
            });
            execOnError(expected, () -> {
                if(rev == revision) {
                    cancelExpected();
                }
            });
        });
    }

    private final long replaceExpected(F newExpected) {
        if(expectedFuture != null) {
            cancel(expectedFuture);
            ++revision;
        }
        expectedFuture = newExpected;
        return revision;
    }

    protected final void cancelExpected() {
        replaceExpected(null);
    }

    protected abstract void execOnSuccess(F future, Consumer<U> action);
    protected abstract void execOnError(F future, Runnable action);
    protected abstract void cancel(F future);
}

class LatestOnlyAsyncMap<T, U> extends LatestOnlyAsyncMapBase<T, U, CompletionStage<U>> {
    private final Executor clientThreadExecutor;

    public LatestOnlyAsyncMap(
            EventStream<T> source,
            Function<T, CompletionStage<U>> f,
            Executor clientThreadExecutor) {
        super(source, f);
        this.clientThreadExecutor = clientThreadExecutor;
    }

    @Override
    protected void execOnSuccess(CompletionStage<U> future, Consumer<U> f) {
        future.thenAcceptAsync(f, clientThreadExecutor);
    }

    @Override
    protected void execOnError(CompletionStage<U> future, Runnable action) {
        future.whenCompleteAsync((u, error) -> {
            if(error != null) {
                action.run();
            }
        }, clientThreadExecutor);
    }

    @Override
    protected void cancel(CompletionStage<U> future) {
        // do nothing (cannot cancel a CompletionStage)
    }
}

class CancellableLatestOnlyAsyncMap<T, U> extends LatestOnlyAsyncMap<T, U> {
    private final EventStream<?> canceller;

    public CancellableLatestOnlyAsyncMap(
            EventStream<T> source,
            Function<T, CompletionStage<U>> f,
            EventStream<?> canceller,
            Executor clientThreadExecutor) {
        super(source, f, clientThreadExecutor);
        this.canceller = canceller;
    }

    @Override
    protected Subscription subscribeToInputs() {
        Subscription s1 = super.subscribeToInputs();
        Subscription s2 = canceller.subscribe(x -> cancelExpected());
        return s1.and(s2);
    }
}

class LatestOnlyBackgroundMap<T, U> extends LatestOnlyAsyncMapBase<T, U, Task<U>> {
    public LatestOnlyBackgroundMap(
            EventStream<T> source,
            Function<T, Task<U>> f) {
        super(source, f);
    }

    @Override
    protected void execOnSuccess(Task<U> t, Consumer<U> f) {
        t.addEventHandler(WORKER_STATE_SUCCEEDED, e -> f.accept(t.getValue()));
    }

    @Override
    protected void execOnError(Task<U> t, Runnable f) {
        t.addEventHandler(WORKER_STATE_FAILED, e -> f.run());
    }

    @Override
    protected void cancel(Task<U> future) {
        future.cancel();
    }
}

class CancellableLatestOnlyBackgroundMap<T, U> extends LatestOnlyBackgroundMap<T, U> {
    private final EventStream<?> canceller;

    public CancellableLatestOnlyBackgroundMap(
            EventStream<T> source,
            Function<T, Task<U>> f,
            EventStream<?> canceller) {
        super(source, f);
        this.canceller = canceller;
    }

    @Override
    protected Subscription subscribeToInputs() {
        Subscription s1 = super.subscribeToInputs();
        Subscription s2 = canceller.subscribe(x -> cancelExpected());
        return s1.and(s2);
    }
}