package org.reactfx;

import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import javafx.concurrent.Task;

import org.reactfx.util.Either;
import org.reactfx.util.TriFunction;

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

@Deprecated
class MappedToEitherStream<T, L, R>
extends MappedStream<T, Either<L, R>>
implements EitherEventStream<L, R> {

    public MappedToEitherStream(
            EventStream<T> input,
            Function<? super T, ? extends Either<L, R>> f) {
        super(input, f);
    }
}

@Deprecated
class MappedToEitherBiStream<A, B, L, R>
extends MappedBiStream<A, B, Either<L, R>>
implements EitherEventStream<L, R> {

    public MappedToEitherBiStream(
            BiEventStream<A, B> input,
            BiFunction<? super A, ? super B, ? extends Either<L, R>> f) {
        super(input, f);
    }
}

@Deprecated
class MappedToEitherTriStream<A, B, C, L, R>
extends MappedTriStream<A, B, C, Either<L, R>>
implements EitherEventStream<L, R> {

    public MappedToEitherTriStream(
            TriEventStream<A, B, C> input,
            TriFunction<? super A, ? super B, ? super C, ? extends Either<L, R>> f) {
        super(input, f);
    }
}