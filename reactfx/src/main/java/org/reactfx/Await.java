package org.reactfx;

import static javafx.concurrent.WorkerStateEvent.*;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.concurrent.Task;

abstract class AwaitBase<T, F> extends LazilyBoundStream<T> implements AwaitingEventStream<T> {
    private final EventStream<F> source;
    private final Indicator pending = new Indicator();

    public AwaitBase(EventStream<F> source) {
        this.source = source;
    }

    @Override
    public final ObservableBooleanValue pendingProperty() {
        return pending;
    }

    @Override
    public final boolean isPending() {
        return pending.isOn();
    }

    @Override
    protected final Subscription subscribeToInputs() {
        return source.subscribe(future -> {
            Guard g = pending.on();
            addCompletionHandler(future, (result, success) -> {
                if(success) {
                    emit(result);
                }
                g.close();
            });
        });
    }

    protected abstract void addCompletionHandler(F future, BiConsumer<T, Boolean> action);
}

class AwaitCompletionStage<T> extends AwaitBase<T, CompletionStage<T>> {
    private final Executor clientThreadExecutor;

    public AwaitCompletionStage(
            EventStream<CompletionStage<T>> source,
            Executor clientThreadExecutor) {
        super(source);
        this.clientThreadExecutor = clientThreadExecutor;
    }

    @Override
    protected void addCompletionHandler(CompletionStage<T> future, BiConsumer<T, Boolean> f) {
        future.whenCompleteAsync((result, error) -> {
            f.accept(result, error == null);
        }, clientThreadExecutor);
    }
}

class AwaitTask<T> extends AwaitBase<T, Task<T>> {

    public AwaitTask(EventStream<Task<T>> source) {
        super(source);
    }

    @Override
    protected void addCompletionHandler(Task<T> t, BiConsumer<T, Boolean> f) {
        t.addEventHandler(WORKER_STATE_SUCCEEDED, e -> f.accept(t.getValue(), true));
        t.addEventHandler(WORKER_STATE_FAILED, e -> f.accept(null, false));
        t.addEventHandler(WORKER_STATE_CANCELLED, e -> f.accept(null, false));
    }
}


abstract class AwaitLatestBase<T, F> extends LazilyBoundStream<T> implements AwaitingEventStream<T> {
    private final EventStream<F> source;

    private long revision = 0;
    private F expectedFuture = null;

    private BooleanBinding pending = null;

    public AwaitLatestBase(EventStream<F> source) {
        this.source = source;
    }

    @Override
    public ObservableBooleanValue pendingProperty() {
        if(pending == null) {
            pending = new BooleanBinding() {
                @Override
                protected boolean computeValue() {
                    return expectedFuture != null;
                }
            };
        }
        return pending;
    }

    @Override
    public boolean isPending() {
        return pending != null ? pending.get() : expectedFuture != null;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return source.subscribe(future -> {
            long rev = replaceExpected(future);
            addResultHandler(future, t -> {
                if(rev == revision) {
                    emit(t);
                    setExpected(null);
                }
            });
            addErrorHandler(future, () -> {
                if(rev == revision) {
                    setExpected(null);
                }
            });
        });
    }

    private final long replaceExpected(F newExpected) {
        if(expectedFuture != null) {
            cancel(expectedFuture);
        }
        ++revision;
        setExpected(newExpected);
        return revision;
    }

    private void setExpected(F newExpected) {
        expectedFuture = newExpected;
        if(pending != null) {
            pending.invalidate();
        }
    }

    protected final void cancelExpected() {
        replaceExpected(null);
    }

    protected abstract void addResultHandler(F future, Consumer<T> action);
    protected abstract void addErrorHandler(F future, Runnable action);
    protected abstract void cancel(F future);
}

class AwaitLatestCompletionStage<T> extends AwaitLatestBase<T, CompletionStage<T>> {
    private final Executor clientThreadExecutor;

    public AwaitLatestCompletionStage(
            EventStream<CompletionStage<T>> source,
            Executor clientThreadExecutor) {
        super(source);
        this.clientThreadExecutor = clientThreadExecutor;
    }

    @Override
    protected void addResultHandler(CompletionStage<T> future, Consumer<T> f) {
        future.thenAcceptAsync(f, clientThreadExecutor);
    }

    @Override
    protected void addErrorHandler(CompletionStage<T> future, Runnable action) {
        future.whenCompleteAsync((u, error) -> {
            if(error != null) {
                action.run();
            }
        }, clientThreadExecutor);
    }

    @Override
    protected void cancel(CompletionStage<T> future) {
        // do nothing (cannot cancel a CompletionStage)
    }
}

class CancellableAwaitLatestCompletionStage<T> extends AwaitLatestCompletionStage<T> {
    private final EventStream<?> canceller;

    public CancellableAwaitLatestCompletionStage(
            EventStream<CompletionStage<T>> source,
            EventStream<?> canceller,
            Executor clientThreadExecutor) {
        super(source, clientThreadExecutor);
        this.canceller = canceller;
    }

    @Override
    protected Subscription subscribeToInputs() {
        Subscription s1 = super.subscribeToInputs();
        Subscription s2 = canceller.subscribe(x -> cancelExpected());
        return s1.and(s2);
    }
}

class AwaitLatestTask<T> extends AwaitLatestBase<T, Task<T>> {
    public AwaitLatestTask(EventStream<Task<T>> source) {
        super(source);
    }

    @Override
    protected void addResultHandler(Task<T> t, Consumer<T> f) {
        t.addEventHandler(WORKER_STATE_SUCCEEDED, e -> f.accept(t.getValue()));
    }

    @Override
    protected void addErrorHandler(Task<T> t, Runnable f) {
        t.addEventHandler(WORKER_STATE_FAILED, e -> f.run());
    }

    @Override
    protected void cancel(Task<T> task) {
        task.cancel();
    }
}

class CancellableAwaitLatestTask<T> extends AwaitLatestTask<T> {
    private final EventStream<?> canceller;

    public CancellableAwaitLatestTask(
            EventStream<Task<T>> source,
            EventStream<?> canceller) {
        super(source);
        this.canceller = canceller;
    }

    @Override
    protected Subscription subscribeToInputs() {
        Subscription s1 = super.subscribeToInputs();
        Subscription s2 = canceller.subscribe(x -> cancelExpected());
        return s1.and(s2);
    }
}