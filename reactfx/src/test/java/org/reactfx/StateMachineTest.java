package org.reactfx;

import static org.junit.Assert.*;
import static org.reactfx.util.Tuples.*;

import java.util.Optional;
import java.util.function.BiFunction;

import org.junit.Test;
import org.reactfx.util.Tuple2;

public class StateMachineTest {

    private static class Counter {
        private int count = 0;
        public void inc() { ++count; }
        public int get() { return count; }
        public int getAndReset() { int res = count; count = 0; return res; }
    }

    @Test
    public void countDownTest() {
        EventSource<Void> src1 = new EventSource<Void>();
        EventSource<Void> src2 = new EventSource<Void>();
        EventSource<Void> reset = new EventSource<Void>();

        BiFunction<Integer, Void, Tuple2<Integer, Optional<String>>> countdown =
                (s, i) -> s == 1
                        ? t(3, Optional.of("COUNTDOWN REACHED"))
                        : t(s-1, Optional.empty());

        EventStream<String> countdowns = StateMachine.init(3)
                .on(src1).transmit(countdown)
                .on(src2).transmit(countdown)
                .on(reset).transition((s, i) -> 3)
                .toEventStream();

        Counter counter = new Counter();
        Subscription sub = countdowns.hook(x -> counter.inc()).pin();

        src1.push(null);
        src2.push(null);
        assertEquals(0, counter.get());

        src1.push(null);
        assertEquals(1, counter.getAndReset());

        src2.push(null);
        src2.push(null);
        reset.push(null);
        assertEquals(0, counter.get());
        src2.push(null);
        assertEquals(0, counter.get());
        src1.push(null);
        assertEquals(0, counter.get());
        src2.push(null);
        assertEquals(1, counter.getAndReset());

        sub.unsubscribe();
        src1.push(null);
        src1.push(null);
        src1.push(null);
        src1.push(null);
        src1.push(null);
        src1.push(null);
        assertEquals(0, counter.get());
    }

}
