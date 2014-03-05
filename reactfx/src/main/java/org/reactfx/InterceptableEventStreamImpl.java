package org.reactfx;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

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
        return input.subscribe(event -> consumer.consume(event));
    }

    @Override
    public Hold mute() {
        switch(consumer.getType()) {
            case MUTE: return Hold.EMPTY_HOLD; // second mute would have no effect
            default: return stack(new MutedConsumer<T>(consumer));
        }
    }

    @Override
    public Hold pause() {
        switch(consumer.getType()) {
            case NORMAL: return stack(new PausedConsumer<T>(consumer));
            default: return Hold.EMPTY_HOLD; // pausing has no effect if another interception is already in effect
        }
    }

    @Override
    public Hold retainLatest() {
        switch(consumer.getType()) {
            case MUTE: // retaining anything is pointless if it is going to be muted anyway
            case RETAIN_LATEST: // second retainLatest would have no effect
                return Hold.EMPTY_HOLD;
            default:
                return stack(new RetainLatestConsumer<T>(consumer));
        }
    }

    @Override
    public Hold reduce(BinaryOperator<T> fusor) {
        switch(consumer.getType()) {
            case MUTE: return Hold.EMPTY_HOLD;
            default: return stack(new FusionConsumer<T>(consumer, fusor));
        }
    }

    @Override
    public Hold tryReduce(BiFunction<T, T, ReductionResult<T>> fusor) {
        switch(consumer.getType()) {
        case MUTE: return Hold.EMPTY_HOLD;
        default: return stack(new OptionalFusionConsumer<T>(consumer, fusor));
    }
    }

    private Hold stack(StackedConsumer<T> newConsumer) {
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
    FUSE,
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

class FusionConsumer<T> extends StackedConsumer<T> {
    private final BinaryOperator<T> fusor;
    private boolean eventArrived = false;
    private T aggregate;

    public FusionConsumer(EventConsumer<T> previous, BinaryOperator<T> fusor) {
        super(previous);
        this.fusor = fusor;
    }

    @Override
    public void consume(T event) {
        if(eventArrived) {
            aggregate = fusor.apply(aggregate, event);
        } else {
            eventArrived = true;
            aggregate = event;
        }
    }

    @Override
    public ConsumerType getType() { return ConsumerType.FUSE; }

    @Override
    protected void feedToPrevious() {
        if(eventArrived) {
            getPrevious().consume(aggregate);
        }
    }
}

class OptionalFusionConsumer<T> extends StackedConsumer<T> {
    private final BiFunction<T, T, ReductionResult<T>> fusor;
    private final List<T> buffer = new ArrayList<>();

    public OptionalFusionConsumer(EventConsumer<T> previous, BiFunction<T, T, ReductionResult<T>> fusor) {
        super(previous);
        this.fusor = fusor;
    }

    @Override
    public void consume(T event) {
        if(buffer.isEmpty()) {
            buffer.add(event);
        } else {
            int lastIndex = buffer.size() - 1;
            T lastEvent = buffer.get(lastIndex);
            ReductionResult<T> res = fusor.apply(lastEvent, event);
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
    public ConsumerType getType() { return ConsumerType.FUSE; }

    @Override
    protected void feedToPrevious() {
        for(T evt: buffer) {
            getPrevious().consume(evt);
        }
    }
}