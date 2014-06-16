package org.reactfx;

import static org.junit.Assert.*;

import org.junit.Test;

public class ConnectableTest {

    @Test
    public void test() {
        EventCounter counter = new EventCounter();
        EventCounter errorCounter = new EventCounter();
        ConnectableEventStream<Integer> cs = new ConnectableEventSource<>();
        EventStream<Integer> observed = cs.hook(counter);
        cs.monitor(errorCounter);
        EventSource<Integer> src1 = new EventSource<>();
        EventSource<Integer> src2 = new EventSource<>();
        Subscription con1 = cs.connectTo(src1.filter(x -> {
            if(x == 666) {
                throw new IllegalArgumentException();
            } else {
                return true;
            }
        }));
        cs.connectTo(src2);

        // test laziness
        src1.push(1);
        src2.push(2);
        assertEquals(0, counter.get());

        // test event propagation
        Subscription sub = observed.pin();
        src1.push(1);
        src2.push(2);
        assertEquals(2, counter.getAndReset());

        // test error propagation
        src1.push(666);
        assertEquals(0, counter.get());
        assertEquals(1, errorCounter.getAndReset());

        // test that disconnection works
        con1.unsubscribe();
        src1.push(1);
        src1.push(666);
        assertEquals(0, counter.get());
        assertEquals(0, errorCounter.getAndReset());
        src2.push(2);
        assertEquals(1, counter.getAndReset());

        // test that unsubscribe works
        sub.unsubscribe();
        src2.push(2);
        assertEquals(0, counter.get());
    }

}
