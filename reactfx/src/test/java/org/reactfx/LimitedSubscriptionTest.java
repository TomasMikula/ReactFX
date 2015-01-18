package org.reactfx;

import static org.junit.Assert.*;

import java.util.function.Consumer;

import org.junit.Test;

public class LimitedSubscriptionTest {

    @Test
    public void testBasic() {
        EventSource<Void> src = new EventSource<>();
        Counter counter = new Counter();
        src.subscribeFor(5, i -> counter.inc());
        for(int i = 0; i < 10; ++i) {
            src.push(null);
        }
        assertEquals(5, counter.getAndReset());
    }

    @Test
    public void testOnPausedStream() {
        EventSource<Void> src = new EventSource<>();
        SuspendableEventStream<Void> pausable = src.pausable();
        Counter counter = new Counter();
        pausable.subscribeFor(5, i -> counter.inc());
        pausable.suspendWhile(() -> {
            for(int i = 0; i < 10; ++i) {
                src.push(null);
            }
        });
        assertEquals(5, counter.getAndReset());
    }

    @Test
    public void testWithAutoEmittingStream() {
        EventStream<Void> stream = new EventStreamBase<Void>() {
            @Override
            protected Subscription observeInputs() {
                return Subscription.EMPTY;
            }
            @Override
            protected void newObserver(Consumer<? super Void> subscriber) {
                for(int i = 0; i < 10; ++i) {
                    subscriber.accept(null);
                }
            }
        };
        Counter counter = new Counter();
        stream.subscribeFor(5, i -> counter.inc());
        assertEquals(5, counter.getAndReset());
    }
}
