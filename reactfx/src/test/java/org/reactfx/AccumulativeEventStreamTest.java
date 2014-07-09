package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class AccumulativeEventStreamTest {

    @Test
    public void test() {
        EventSource<Integer> source = new EventSource<>();
        SuspendableEventStream<Integer> suspendable = source.accumulative(
                () -> new ArrayList<Integer>(),
                (l, i) -> { l.add(i*i); return l; },
                l -> l);
        List<Integer> emitted = new ArrayList<>();
        suspendable.subscribe(emitted::add);

        source.push(1);
        suspendable.suspendWhile(() -> {
            source.push(2);
            source.push(3);
        });
        source.push(4);

        assertEquals(Arrays.asList(1, 4, 9, 4), emitted);
    }

}
