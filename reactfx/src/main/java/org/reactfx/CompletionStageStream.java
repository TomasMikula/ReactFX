package org.reactfx;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import javafx.application.Platform;

import org.reactfx.util.Try;

public interface CompletionStageStream<T> extends EventStream<CompletionStage<T>> {

    /**
     * Returns a new stream that emits the results of completions stages emitted
     * from this stream when they become available.
     *
     * <p>Note that results from the returned stream may arrive in different
     * order than the completion stages emitted from this stream, due to
     * asynchrony.
     *
     * <p>If a completion stage emitted by this stream completes with an error,
     * the error is reported through the returned stream error-reporting
     * mechanism.
     */
    default AwaitingEventStream<T> await() {
        return await(Platform::runLater);
    }

    /**
     * Returns a new stream that emits the results of completions stages emitted
     * from this stream when they become available.
     *
     * <p>Note that results from the returned stream may arrive in different
     * order than the completion stages emitted from this stream, due to
     * asynchrony.
     *
     * <p>If a completion stage emitted by this stream fails with exception
     * {@code e}, {@code Try.failure(e)} is emitted from the returned stream.
     */
    default AwaitingEventStream<Try<T>> tryAwait() {
        return tryAwait(Platform::runLater);
    }

    /**
     * A variant of {@link #await()} for streams that do not live on the JavaFX
     * application thread.
     * @param clientThreadExecutor single-thread executor that executes actions
     * on the same thread on which this event stream lives.
     * @see #await()
     */
    default AwaitingEventStream<T> await(Executor clientThreadExecutor) {
        return Await.awaitCompletionStage(this, clientThreadExecutor);
    }

    /**
     * A variant of {@link #tryAwait()} for streams that do not live on the JavaFX
     * application thread.
     * @param clientThreadExecutor single-thread executor that executes actions
     * on the same thread on which this event stream lives.
     * @see #tryAwait()
     */
    default AwaitingEventStream<Try<T>> tryAwait(Executor clientThreadExecutor) {
        return Await.tryAwaitCompletionStage(this, clientThreadExecutor);
    }

    /**
     * Similar to {@link #await()}, with one difference: for completion stages
     * <i>s1</i> and <i>s2</i> emitted from this stream in this order, if
     * <i>s2</i> arrives before <i>s1</i> is completed, then the result of
     * <i>s1</i> is discarded (i.e. not emitted from the returned stream).
     * @see #await()
     */
    default AwaitingEventStream<T> awaitLatest() {
        return awaitLatest(Platform::runLater);
    }

    /**
     * Similar to {@link #tryAwait()}, with one difference: for completion
     * stages <i>s1</i> and <i>s2</i> emitted from this stream in this order,
     * if <i>s2</i> arrives before <i>s1</i> is completed, then the result of
     * <i>s1</i> is discarded (i.e. not emitted from the returned stream).
     * @see #tryAwait()
     */
    default AwaitingEventStream<Try<T>> tryAwaitLatest() {
        return tryAwaitLatest(Platform::runLater);
    }

    /**
     * A variant of {@link #awaitLatest()} for streams that do not live on the
     * JavaFX application thread.
     * @param clientThreadExecutor single-thread executor that executes actions
     * on the same thread on which this event stream lives.
     * @see #awaitLatest()
     */
    default AwaitingEventStream<T> awaitLatest(Executor clientThreadExecutor) {
        return AwaitLatest.awaitCompletionStage(this, clientThreadExecutor);
    }

    /**
     * A variant of {@link #tryAwaitLatest()} for streams that do not live on
     * the JavaFX application thread.
     * @param clientThreadExecutor single-thread executor that executes actions
     * on the same thread on which this event stream lives.
     * @see #tryAwaitLatest()
     */
    default AwaitingEventStream<Try<T>> tryAwaitLatest(Executor clientThreadExecutor) {
        return AwaitLatest.tryAwaitCompletionStage(this, clientThreadExecutor);
    }

    /**
     * Similar to {@link #awaitLatest()}, with one addition:
     * When an event is emitted from {@code canceller}, if the completion stage
     * most recently emitted from this stream has not yet completed, its result
     * is discarded (i.e. not emitted from the returned stream).
     * @param canceller An event from this stream causes the currently expected
     * result (if any) to be discarded. It can be used to signal that a new
     * completion stage will arrive from this stream shortly, which makes the
     * currently expected result outdated.
     * @see #awaitLatest()
     */
    default AwaitingEventStream<T> awaitLatest(EventStream<?> canceller) {
        return awaitLatest(canceller, Platform::runLater);
    }

    /**
     * Similar to {@link #tryAwaitLatest()}, with one addition:
     * When an event is emitted from {@code canceller}, if the completion stage
     * most recently emitted from this stream has not yet completed, its result
     * is discarded (i.e. not emitted from the returned stream).
     * @param canceller An event from this stream causes the currently expected
     * result (if any) to be discarded. It can be used to signal that a new
     * completion stage will arrive from this stream shortly, which makes the
     * currently expected result outdated.
     * @see #tryAwaitLatest()
     */
    default AwaitingEventStream<Try<T>> tryAwaitLatest(EventStream<?> canceller) {
        return tryAwaitLatest(canceller, Platform::runLater);
    }

    /**
     * A variant of {@link #awaitLatest(EventStream)} for streams that do not
     * live on the JavaFX application thread.
     * @param clientThreadExecutor single-thread executor that executes actions
     * on the same thread on which this event stream lives.
     * @see #awaitLatest(EventStream)
     */
    default AwaitingEventStream<T> awaitLatest(EventStream<?> canceller, Executor clientThreadExecutor) {
        return AwaitLatest.awaitCompletionStage(this, canceller, clientThreadExecutor);
    }

    /**
     * A variant of {@link #tryAwaitLatest(EventStream)} for streams that do not
     * live on the JavaFX application thread.
     * @param clientThreadExecutor single-thread executor that executes actions
     * on the same thread on which this event stream lives.
     * @see #tryAwaitLatest(EventStream)
     */
    default AwaitingEventStream<Try<T>> tryAwaitLatest(EventStream<?> canceller, Executor clientThreadExecutor) {
        return AwaitLatest.tryAwaitCompletionStage(this, canceller, clientThreadExecutor);
    }
}
