package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.reactfx.util.AccumulatorSize;

public class AccumulativeEventStreamTest {

    @Test
    public void test() {
        EventSource<Integer> source = new EventSource<>();
        SuspendableEventStream<Integer> suspendable = source.accumulative(
                () -> new LinkedList<Integer>(),
                (l, i) -> { l.addLast(i); return l; },
                l -> AccumulatorSize.fromInt(l.size()),
                l -> l.getFirst(),
                l -> { l.removeFirst(); return l; });
        List<Integer> emitted = new ArrayList<>();
        suspendable.subscribe(emitted::add);

        source.push(1);
        suspendable.suspendWhile(() -> {
            source.push(2);
            source.push(3);
        });
        source.push(4);

        assertEquals(Arrays.asList(1, 2, 3, 4), emitted);
    }

}
