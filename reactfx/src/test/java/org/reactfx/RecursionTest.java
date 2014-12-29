package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class RecursionTest {

    @Test
    public void allowRecursionWithOneSubscriber() {
        List<Integer> emitted = new ArrayList<>();
        EventSource<Integer> source = new EventSource<>();
        source.hook(emitted::add).subscribe(
                i -> { if(i > 0) source.push(i-1); });
        source.push(5);
        assertEquals(Arrays.asList(5, 4, 3, 2, 1, 0), emitted);
    }

    @Test(expected=IllegalStateException.class)
    public void preventRecursionWithTwoSubscribers() {
        EventSource<Integer> source = new EventSource<>();

        // XXX this test depends on the implementation detail
        // that subscribers are notified in registration order
        source.subscribe(i -> {
            if(i > 0) source.push(i-1);
        });
        source.pin();

        source.push(5);
    }

    @Test
    public void onRecurseQueueTest() {
        EventSource<Integer> source = new EventSource<>();
        EventStream<Integer> stream = source.onRecurseQueue();
        List<Integer> emitted1 = new ArrayList<>();
        List<Integer> emitted2 = new ArrayList<>();

        stream.subscribe(x -> {
            emitted1.add(x);
            if(x > 0) source.push(x - 1);
        });
        stream.subscribe(emitted2::add);

        source.push(5);
        assertEquals(Arrays.asList(5, 4, 3, 2, 1, 0), emitted1);
        assertEquals(Arrays.asList(5, 4, 3, 2, 1, 0), emitted2);
    }

    @Test
    public void onRecurseReduceTest() {
        EventSource<Integer> source = new EventSource<>();
        EventStream<Integer> stream = source.onRecurseReduce((a, b) -> a + b);
        List<Integer> emitted1 = new ArrayList<>();
        List<Integer> emitted2 = new ArrayList<>();

        stream.subscribe(x -> {
            emitted1.add(x);
            if(x > 0) source.push(x - 1);
        });
        stream.subscribe(emitted2::add);

        source.push(5);
        assertEquals(Arrays.asList(5, 4, 3, 2, 1, 0), emitted1);
        assertEquals(15, emitted2.stream().reduce(0, (a, b) -> a + b).intValue());
    }
}
