package org.reactfx;

import static javafx.concurrent.WorkerStateEvent.*;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.concurrent.Task;

import org.reactfx.util.TriConsumer;
import org.reactfx.util.Try;

class Await<T, F, R> extends EventStreamBase<R> implements AwaitingEventStream<R> {

    public static <T> AwaitingEventStream<T> awaitCompletionStage(
            EventStream<CompletionStage<T>> source,
            Executor clientThreadExecutor) {
        return new Await<>(
                source,
                (future, handler) -> addCompletionHandler(future, handler, clientThreadExecutor),
                reportingEmitter());
    }

    public static <T> AwaitingEventStream<Try<T>> tryAwaitCompletionStage(
            EventStream<CompletionStage<T>> source,
            Executor clientThreadExecutor) {
        return new Await<>(
                source,
                (future, handler) -> addCompletionHandler(future, handler, clientThreadExecutor),
                tryEmitter());
    }

    public static <T> AwaitingEventStream<T> awaitTask(
            EventStream<Task<T>> source) {
        return new Await<>(source, Await::addCompletionHandler, reportingEmitter());
    }

    public static <T> AwaitingEventStream<Try<T>> tryAwaitTask(
            EventStream<Task<T>> source) {
        return new Await<>(source, Await::addCompletionHandler, tryEmitter());
    }

    static <T> void addCompletionHandler(
            CompletionStage<T> future,
            TriConsumer<T, Throwable, Boolean> handler,
            Executor executor) {
        future.whenCompleteAsync((result, error) -> {
            handler.accept(result, error, false);
        }, executor);
    }

    static <T> void addCompletionHandler(
            Task<T> t,
            TriConsumer<T, Throwable, Boolean> handler) {
        t.addEventHandler(WORKER_STATE_SUCCEEDED, e -> handler.accept(t.getValue(), null, false));
        t.addEventHandler(WORKER_STATE_FAILED, e -> handler.accept(null, t.getException(), false));
        t.addEventHandler(WORKER_STATE_CANCELLED, e -> handler.accept(null, null, true));
    }

    static <T> TriConsumer<EventStreamBase<T>, T, Throwable> reportingEmitter() {
        return (stream, value, error) -> {
            if(error == null) {
                stream.emit(value);
            } else {
                stream.reportError(error);
            }
        };
    }

    static <T> TriConsumer<EventStreamBase<Try<T>>, T, Throwable> tryEmitter() {
        return (stream, value, error) -> {
            if(error == null) {
                stream.emit(Try.success(value));
            } else {
                stream.emit(Try.failure(error));
            }
        };
    }

    private final EventStream<F> source;
    private final Indicator pending = new Indicator();
    private final BiConsumer<F, TriConsumer<T, Throwable, Boolean>> addCompletionHandler;
    private final TriConsumer<EventStreamBase<R>, T, Throwable> emitter;

    private Await(
            EventStream<F> source,
            BiConsumer<F, TriConsumer<T, Throwable, Boolean>> addCompletionHandler,
            TriConsumer<EventStreamBase<R>, T, Throwable> emitter) {
        this.source = source;
        this.addCompletionHandler = addCompletionHandler;
        this.emitter = emitter;
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
    protected final Subscription bindToInputs() {
        return subscribeTo(source, future -> {
            Guard g = pending.on();
            addCompletionHandler.accept(future, (result, error, cancelled) -> {
                if(!cancelled) {
                    emitter.accept(this, result, error);
                }
                g.close();
            });
        });
    }
}


class AwaitLatest<T, F, R> extends EventStreamBase<R> implements AwaitingEventStream<R> {

    public static <T> AwaitingEventStream<T> awaitCompletionStage(
            EventStream<CompletionStage<T>> source,
            Executor clientThreadExecutor) {
        return new AwaitLatest<>(
                source,
                EventStreams.never(), // no cancel impulse
                future -> {}, // cannot cancel a CompletionStage
                (future, handler) -> Await.addCompletionHandler(future, handler, clientThreadExecutor),
                Await.reportingEmitter());
    }

    public static <T> AwaitingEventStream<Try<T>> tryAwaitCompletionStage(
            EventStream<CompletionStage<T>> source,
            Executor clientThreadExecutor) {
        return new AwaitLatest<>(
                source,
                EventStreams.never(), // no cancel impulse
                future -> {}, // cannot cancel a CompletionStage
                (future, handler) -> Await.addCompletionHandler(future, handler, clientThreadExecutor),
                Await.tryEmitter());
    }

    public static <T> AwaitingEventStream<T> awaitTask(
            EventStream<Task<T>> source) {
        return new AwaitLatest<>(
                source,
                EventStreams.never(), // no cancel impulse
                Task::cancel,
                Await::addCompletionHandler,
                Await.reportingEmitter());
    }

    public static <T> AwaitingEventStream<Try<T>> tryAwaitTask(
            EventStream<Task<T>> source) {
        return new AwaitLatest<>(
                source,
                EventStreams.never(), // no cancel impulse
                Task::cancel,
                Await::addCompletionHandler,
                Await.tryEmitter());
    }

    public static <T> AwaitingEventStream<T> awaitCompletionStage(
            EventStream<CompletionStage<T>> source,
            EventStream<?> cancelImpulse,
            Executor clientThreadExecutor) {
        return new AwaitLatest<>(
                source,
                cancelImpulse,
                future -> {}, // cannot cancel a CompletionStage
                (future, handler) -> Await.addCompletionHandler(future, handler, clientThreadExecutor),
                Await.reportingEmitter());
    }

    public static <T> AwaitingEventStream<Try<T>> tryAwaitCompletionStage(
            EventStream<CompletionStage<T>> source,
            EventStream<?> cancelImpulse,
            Executor clientThreadExecutor) {
        return new AwaitLatest<>(
                source,
                cancelImpulse,
                future -> {}, // cannot cancel a CompletionStage
                (future, handler) -> Await.addCompletionHandler(future, handler, clientThreadExecutor),
                Await.tryEmitter());
    }

    public static <T> AwaitingEventStream<T> awaitTask(
            EventStream<Task<T>> source,
            EventStream<?> cancelImpulse) {
        return new AwaitLatest<>(
                source,
                cancelImpulse,
                Task::cancel,
                Await::addCompletionHandler,
                Await.reportingEmitter());
    }

    public static <T> AwaitingEventStream<Try<T>> tryAwaitTask(
            EventStream<Task<T>> source,
            EventStream<?> cancelImpulse) {
        return new AwaitLatest<>(
                source,
                cancelImpulse,
                Task::cancel,
                Await::addCompletionHandler,
                Await.tryEmitter());
    }

    private final EventStream<F> source;
    private final EventStream<?> cancelImpulse;
    private final Consumer<F> canceller;
    private final BiConsumer<F, TriConsumer<T, Throwable, Boolean>> addCompletionHandler;
    private final TriConsumer<EventStreamBase<R>, T, Throwable> emitter;

    private long revision = 0;
    private F expectedFuture = null;

    private BooleanBinding pending = null;

    private AwaitLatest(
            EventStream<F> source,
            EventStream<?> cancelImpulse,
            Consumer<F> canceller,
            BiConsumer<F, TriConsumer<T, Throwable, Boolean>> addCompletionHandler,
            TriConsumer<EventStreamBase<R>, T, Throwable> emitter) {
        this.source = source;
        this.cancelImpulse = cancelImpulse;
        this.canceller = canceller;
        this.addCompletionHandler = addCompletionHandler;
        this.emitter = emitter;
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
    protected Subscription bindToInputs() {
        Subscription s1 = subscribeTo(source, future -> {
            long rev = replaceExpected(future);
            addCompletionHandler.accept(future, (result, error, cancelled) -> {
                if(rev == revision) {
                    if(!cancelled) {
                        emitter.accept(this, result, error); // emit before setting pending to false
                    }
                    setExpected(null);
                }
            });
        });

        Subscription s2 = subscribeTo(cancelImpulse, x -> replaceExpected(null));

        return s1.and(s2);
    }

    private final long replaceExpected(F newExpected) {
        ++revision; // increment before cancelling, so that the cancellation handler is not executed
        if(expectedFuture != null) {
            canceller.accept(expectedFuture);
        }
        setExpected(newExpected);
        return revision;
    }

    private void setExpected(F newExpected) {
        expectedFuture = newExpected;
        if(pending != null) {
            pending.invalidate();
        }
    }
}