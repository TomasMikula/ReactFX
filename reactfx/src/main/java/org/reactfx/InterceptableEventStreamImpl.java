package org.reactfx;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

import org.reactfx.util.Either;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuple3;

class InterceptableEventStreamImpl<T> extends LazilyBoundStream<T> implements InterceptableEventStream<T> {

    private final EventStream<T> input;
    private EventConsumer<T> consumer;

    public InterceptableEventStreamImpl(EventStream<T> input) {
        this.input = input;
        this.consumer = new EventConsumer<T>() {
            @Override
            public void consume(T event) { emit(event); }

            @Override
            public ConsumerType getType() { return ConsumerType.NORMAL; }
        };
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeTo(input, event -> consumer.consume(event));
    }

    @Override
    public Guard mute() {
        switch(consumer.getType()) {
            case MUTE: return Guard.EMPTY_GUARD; // second mute would have no effect
            default: return stack(new MutedConsumer<T>(consumer));
        }
    }

    @Override
    public Guard pause() {
        switch(consumer.getType()) {
            case NORMAL: return stack(new PausedConsumer<T>(consumer));
            default: return Guard.EMPTY_GUARD; // pausing has no effect if another interception is already in effect
        }
    }

    @Override
    public Guard retainLatest() {
        switch(consumer.getType()) {
            case MUTE: // retaining anything is pointless if it is going to be muted anyway
            case RETAIN_LATEST: // second retainLatest would have no effect
                return Guard.EMPTY_GUARD;
            default:
                return stack(new RetainLatestConsumer<T>(consumer));
        }
    }

    @Override
    public Guard reduce(BinaryOperator<T> reduction) {
        switch(consumer.getType()) {
            case MUTE: return Guard.EMPTY_GUARD;
            default: return stack(new ReduceConsumer<T>(consumer, reduction));
        }
    }

    @Override
    public Guard tryReduce(BiFunction<T, T, ReductionResult<T>> reduction) {
        switch(consumer.getType()) {
        case MUTE: return Guard.EMPTY_GUARD;
        default: return stack(new ReduceOptionallyConsumer<T>(consumer, reduction));
    }
    }

    private Guard stack(StackedConsumer<T> newConsumer) {
        consumer = newConsumer;
        return () -> unstack(newConsumer);
    }

    private void unstack(StackedConsumer<T> consumer) {
        if(this.consumer != consumer) {
            throw new IllegalStateException("Wrong order of releasing interceptions.");
        }
        this.consumer = consumer.unstack();
    }
}

enum ConsumerType {
    NORMAL,
    MUTE,
    PAUSE,
    RETAIN_LATEST,
    REDUCE,
}

interface EventConsumer<T> {
    void consume(T event);
    ConsumerType getType();
}

abstract class StackedConsumer<T> implements EventConsumer<T> {
    private final EventConsumer<T> previous;

    protected StackedConsumer(EventConsumer<T> previous) {
        this.previous = previous;
    }

    public final EventConsumer<T> unstack() {
        feedToPrevious();
        return previous;
    }

    protected final EventConsumer<T> getPrevious() {
        return previous;
    }

    protected abstract void feedToPrevious();
}

class MutedConsumer<T> extends StackedConsumer<T> {

    public MutedConsumer(EventConsumer<T> previous) {
        super(previous);
    }

    @Override
    public void consume(T event) { /* ignore the event */ }

    @Override
    public ConsumerType getType() { return ConsumerType.MUTE; }

    @Override
    protected void feedToPrevious() { /* nothing to feed */ }
}

class PausedConsumer<T> extends StackedConsumer<T> {
    private final List<T> buffer = new ArrayList<>();

    public PausedConsumer(EventConsumer<T> previous) {
        super(previous);
    }

    @Override
    public void consume(T event) { buffer.add(event); }

    @Override
    public ConsumerType getType() { return ConsumerType.PAUSE; }

    @Override
    protected void feedToPrevious() {
        for(T evt: buffer) {
            getPrevious().consume(evt);
        }
    }
}

class RetainLatestConsumer<T> extends StackedConsumer<T> {
    private boolean eventArrived = false;
    private T latestEvent;

    public RetainLatestConsumer(EventConsumer<T> previous) {
        super(previous);
    }

    @Override
    public void consume(T event) {
        eventArrived = true;
        latestEvent = event;
    }

    @Override
    public ConsumerType getType() { return ConsumerType.RETAIN_LATEST; }

    @Override
    protected void feedToPrevious() {
        if(eventArrived) {
            getPrevious().consume(latestEvent);
        }
    }
}

class ReduceConsumer<T> extends StackedConsumer<T> {
    private final BinaryOperator<T> reduction;
    private boolean eventArrived = false;
    private T aggregate;

    public ReduceConsumer(EventConsumer<T> previous, BinaryOperator<T> reduction) {
        super(previous);
        this.reduction = reduction;
    }

    @Override
    public void consume(T event) {
        if(eventArrived) {
            aggregate = reduction.apply(aggregate, event);
        } else {
            eventArrived = true;
            aggregate = event;
        }
    }

    @Override
    public ConsumerType getType() { return ConsumerType.REDUCE; }

    @Override
    protected void feedToPrevious() {
        if(eventArrived) {
            getPrevious().consume(aggregate);
        }
    }
}

class ReduceOptionallyConsumer<T> extends StackedConsumer<T> {
    private final BiFunction<T, T, ReductionResult<T>> reduction;
    private final List<T> buffer = new ArrayList<>();

    public ReduceOptionallyConsumer(EventConsumer<T> previous, BiFunction<T, T, ReductionResult<T>> reduction) {
        super(previous);
        this.reduction = reduction;
    }

    @Override
    public void consume(T event) {
        if(buffer.isEmpty()) {
            buffer.add(event);
        } else {
            int lastIndex = buffer.size() - 1;
            T lastEvent = buffer.get(lastIndex);
            ReductionResult<T> res = reduction.apply(lastEvent, event);
            if(res.isAnnihilated()) {
                buffer.remove(lastIndex);
            } else if(res.isReduced()) {
                buffer.set(lastIndex, res.get());
            } else {
                assert res.isFailed();
                buffer.add(event);
            }
        }
    }

    @Override
    public ConsumerType getType() { return ConsumerType.REDUCE; }

    @Override
    protected void feedToPrevious() {
        for(T evt: buffer) {
            getPrevious().consume(evt);
        }
    }
}


class InterceptableBiEventStreamImpl<A, B>
extends InterceptableEventStreamImpl<Tuple2<A, B>>
implements InterceptableBiEventStream<A, B>, PoorMansBiStream<A, B> {

    public InterceptableBiEventStreamImpl(EventStream<Tuple2<A, B>> input) {
        super(input);
    }
}


class InterceptableTriEventStreamImpl<A, B, C>
extends InterceptableEventStreamImpl<Tuple3<A, B, C>>
implements InterceptableTriEventStream<A, B, C>, PoorMansTriStream<A, B, C> {

    public InterceptableTriEventStreamImpl(EventStream<Tuple3<A, B, C>> input) {
        super(input);
    }
}


class InterceptableEitherEventStreamImpl<L, R>
extends InterceptableEventStreamImpl<Either<L, R>>
implements InterceptableEitherEventStream<L, R> {

    public InterceptableEitherEventStreamImpl(EventStream<Either<L, R>> input) {
        super(input);
    }
}