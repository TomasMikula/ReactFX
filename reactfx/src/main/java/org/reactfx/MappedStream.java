package org.reactfx;

import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import javafx.concurrent.Task;

import org.reactfx.util.TriFunction;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuple3;

class MappedStream<T, U> extends LazilyBoundStream<U> {
    private final EventStream<T> input;
    private final Function<? super T, ? extends U> f;

    public MappedStream(
            EventStream<T> input,
            Function<? super T, ? extends U> f) {
        this.input = input;
        this.f = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeTo(input, value -> {
            emit(f.apply(value));
        });
    }
}

class MappedBiStream<A, B, U> extends LazilyBoundStream<U> {
    private final BiEventStream<A, B> input;
    private final BiFunction<? super A, ? super B, ? extends U> f;

    public MappedBiStream(
            BiEventStream<A, B> input,
            BiFunction<? super A, ? super B, ? extends U> f) {
        this.input = input;
        this.f = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeToBi(input, (a, b) -> {
            emit(f.apply(a, b));
        });
    }
}

class MappedToBiStream<T, A, B> extends LazilyBoundBiStream<A, B> {
    private final EventStream<T> input;
    private final Function<? super T, Tuple2<A, B>> f;

    public MappedToBiStream(
            EventStream<T> input,
            Function<? super T, Tuple2<A, B>> f) {
        this.input = input;
        this.f = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeTo(input, value -> {
            Tuple2<A, B> tuple = f.apply(value);
            emit(tuple._1, tuple._2);
        });
    }
}

class MappedBiToBiStream<A, B, C, D> extends LazilyBoundBiStream<C, D> {
    private final BiEventStream<A, B> input;
    private final BiFunction<? super A, ? super B, Tuple2<C, D>> f;

    public MappedBiToBiStream(
            BiEventStream<A, B> input,
            BiFunction<? super A, ? super B, Tuple2<C, D>> f) {
        this.input = input;
        this.f = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeToBi(input, (a, b) -> {
            Tuple2<C, D> tuple = f.apply(a, b);
            emit(tuple._1, tuple._2);
        });
    }
}

class MappedBiToTriStream<A, B, C, D, E> extends LazilyBoundTriStream<C, D, E> {
    private final BiEventStream<A, B> input;
    private final BiFunction<? super A, ? super B, Tuple3<C, D, E>> f;

    public MappedBiToTriStream(
            BiEventStream<A, B> input,
            BiFunction<? super A, ? super B, Tuple3<C, D, E>> f) {
        this.input = input;
        this.f = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeToBi(input, (a, b) -> {
            Tuple3<C, D, E> tuple = f.apply(a, b);
            emit(tuple._1, tuple._2, tuple._3);
        });
    }
}

class MappedTriStream<A, B, C, U> extends LazilyBoundStream<U> {
    private final TriEventStream<A, B, C> input;
    private final TriFunction<? super A, ? super B, ? super C, ? extends U> f;

    public MappedTriStream(
            TriEventStream<A, B, C> input,
            TriFunction<? super A, ? super B, ? super C, ? extends U> f) {
        this.input = input;
        this.f = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeToTri(input, (a, b, c) -> {
            emit(f.apply(a, b, c));
        });
    }
}

class MappedToTriStream<T, A, B, C> extends LazilyBoundTriStream<A, B, C> {
    private final EventStream<T> input;
    private final Function<? super T, Tuple3<A, B, C>> f;

    public MappedToTriStream(
            EventStream<T> input,
            Function<? super T, Tuple3<A, B, C>> f) {
        this.input = input;
        this.f = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeTo(input, value -> {
            Tuple3<A, B, C> tuple = f.apply(value);
            emit(tuple._1, tuple._2, tuple._3);
        });
    }
}

class MappedTriToBiStream<A, B, C, D, E> extends LazilyBoundBiStream<D, E> {
    private final TriEventStream<A, B, C> input;
    private final TriFunction<? super A, ? super B, ? super C, Tuple2<D, E>> f;

    public MappedTriToBiStream(
            TriEventStream<A, B, C> input,
            TriFunction<? super A, ? super B, ? super C, Tuple2<D, E>> f) {
        this.input = input;
        this.f = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeToTri(input, (a, b, c) -> {
            Tuple2<D, E> tuple = f.apply(a, b, c);
            emit(tuple._1, tuple._2);
        });
    }
}

class MappedTriToTriStream<A, B, C, D, E, F> extends LazilyBoundTriStream<D, E, F> {
    private final TriEventStream<A, B, C> input;
    private final TriFunction<? super A, ? super B, ? super C, Tuple3<D, E, F>> f;

    public MappedTriToTriStream(
            TriEventStream<A, B, C> input,
            TriFunction<? super A, ? super B, ? super C, Tuple3<D, E, F>> f) {
        this.input = input;
        this.f = f;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeToTri(input, (a, b, c) -> {
            Tuple3<D, E, F> tuple = f.apply(a, b, c);
            emit(tuple._1, tuple._2, tuple._3);
        });
    }
}

class MappedToCompletionStageStream<T, U>
extends MappedStream<T, CompletionStage<U>>
implements CompletionStageStream<U> {

    public MappedToCompletionStageStream(
            EventStream<T> input,
            Function<? super T, CompletionStage<U>> f) {
        super(input, f);
    }
}

class MappedToCompletionStageBiStream<A, B, U>
extends MappedBiStream<A, B, CompletionStage<U>>
implements CompletionStageStream<U> {

    public MappedToCompletionStageBiStream(
            BiEventStream<A, B> input,
            BiFunction<? super A, ? super B, CompletionStage<U>> f) {
        super(input, f);
    }
}

class MappedToCompletionStageTriStream<A, B, C, U>
extends MappedTriStream<A, B, C, CompletionStage<U>>
implements CompletionStageStream<U> {

    public MappedToCompletionStageTriStream(
            TriEventStream<A, B, C> input,
            TriFunction<? super A, ? super B, ? super C, CompletionStage<U>> f) {
        super(input, f);
    }
}

class MappedToTaskStream<T, U>
extends MappedStream<T, Task<U>>
implements TaskStream<U> {

    public MappedToTaskStream(
            EventStream<T> input,
            Function<? super T, Task<U>> f) {
        super(input, f);
    }
}

class MappedToTaskBiStream<A, B, U>
extends MappedBiStream<A, B, Task<U>>
implements TaskStream<U> {

    public MappedToTaskBiStream(
            BiEventStream<A, B> input,
            BiFunction<? super A, ? super B, Task<U>> f) {
        super(input, f);
    }
}

class MappedToTaskTriStream<A, B, C, U>
extends MappedTriStream<A, B, C, Task<U>>
implements TaskStream<U> {

    public MappedToTaskTriStream(
            TriEventStream<A, B, C> input,
            TriFunction<? super A, ? super B, ? super C, Task<U>> f) {
        super(input, f);
    }
}