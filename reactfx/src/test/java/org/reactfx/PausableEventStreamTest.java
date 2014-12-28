package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class PausableEventStreamTest {

    @Test
    public void test() {
        EventSource<Integer> source = new EventSource<>();
        SuspendableEventStream<Integer> suspendable = source.pausable();
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

    @Test
    public void testRecursion() {
        EventSource<Integer> source = new EventSource<>();
        SuspendableEventStream<Integer> suspendable = source.pausable();
        List<Integer> emitted = new ArrayList<>();
        suspendable.subscribe(emitted::add);
        suspendable.subscribe(i -> {
            if(i == 1) {
                source.push(3);
            }
        });
        suspendable.suspendWhile(() -> {
            source.push(1);
            source.push(2);
        });

        assertEquals(Arrays.asList(1, 2, 3), emitted);
    }
}
