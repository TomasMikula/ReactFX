package org.reactfx;

import javafx.concurrent.Task;

import org.reactfx.util.Try;

public interface TaskStream<T> extends EventStream<Task<T>> {

    /**
     * Returns a new stream that emits the results of tasks emitted from this
     * stream when they become available.
     *
     * <p>Note that results from the returned stream may arrive in different
     * order than the tasks emitted from this stream, due to asynchrony.
     *
     * <p>If a task emitted by this stream fails, the error is reported through
     * the returned stream error-reporting mechanism.
     */
    default AwaitingEventStream<T> await() {
        return Await.awaitTask(this);
    }

    /**
     * Returns a new stream that emits the results of tasks emitted from this
     * stream when they become available.
     *
     * <p>Note that results from the returned stream may arrive in different
     * order than the tasks emitted from this stream, due to asynchrony.
     *
     * <p>If a task emitted by this stream fails with exception {@code e},
     * {@code Try.failure(e)} is emitted from the returned stream.
     */
    default AwaitingEventStream<Try<T>> tryAwait() {
        return Await.tryAwaitTask(this);
    }

    /**
     * Similar to {@link #await()}, with one difference: for tasks <i>t1</i> and
     * <i>t2</i> emitted from this stream in this order, if <i>t2</i> arrives
     * before <i>t1</i> is completed, then <i>t1</i> is cancelled and its result
     * is discarded (i.e. not emitted from the returned stream).
     * @see #await()
     */
    default AwaitingEventStream<T> awaitLatest() {
        return AwaitLatest.awaitTask(this);
    }

    /**
     * Similar to {@link #tryAwait()}, with one difference: for tasks <i>t1</i> and
     * <i>t2</i> emitted from this stream in this order, if <i>t2</i> arrives
     * before <i>t1</i> is completed, then <i>t1</i> is cancelled and its result
     * is discarded (i.e. not emitted from the returned stream).
     * @see #tryAwait()
     */
    default AwaitingEventStream<Try<T>> tryAwaitLatest() {
        return AwaitLatest.tryAwaitTask(this);
    }

    /**
     * Similar to {@link #awaitLatest()}, with one addition:
     * When an event is emitted from {@code canceller}, if the task most
     * recently emitted from this stream has not yet completed, it is cancelled
     * and its result discarded (i.e. not emitted from the returned stream).
     * @param canceller An event from this stream causes the currently expected
     * result (if any) to be discarded. It can be used to signal that a new
     * task will arrive from this stream shortly, which makes the currently
     * expected result outdated.
     * @see #awaitLatest()
     */
    default AwaitingEventStream<T> awaitLatest(EventStream<?> canceller) {
        return AwaitLatest.awaitTask(this, canceller);
    }

    /**
     * Similar to {@link #tryAwaitLatest()}, with one addition:
     * When an event is emitted from {@code canceller}, if the task most
     * recently emitted from this stream has not yet completed, it is cancelled
     * and its result discarded (i.e. not emitted from the returned stream).
     * @param canceller An event from this stream causes the currently expected
     * result (if any) to be discarded. It can be used to signal that a new
     * task will arrive from this stream shortly, which makes the currently
     * expected result outdated.
     * @see #tryAwaitLatest()
     */
    default AwaitingEventStream<Try<T>> tryAwaitLatest(EventStream<?> canceller) {
        return AwaitLatest.tryAwaitTask(this, canceller);
    }
}
