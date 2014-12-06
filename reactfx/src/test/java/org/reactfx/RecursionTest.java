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
        List<Throwable> errors = new ArrayList<>();
        EventSource<Integer> source = new EventSource<>();
        source.hook(emitted::add).subscribe(
                i -> { if(i > 0) source.push(i-1); },
                errors::add);
        source.push(5);
        assertEquals(Arrays.asList(5, 4, 3, 2, 1, 0), emitted);
        assertEquals(0, errors.size());
    }

    @Test
    public void preventRecursionWithTwoSubscribers() {
        List<Integer> emitted = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        EventSource<Integer> source = new EventSource<>();
        source.subscribe(emitted::add);
        source.subscribe(i -> {
            if(i > 0) source.push(i-1);
        });
        source.monitor(errors::add);
        source.push(5);
        assertTrue("At most one event got emitted", emitted.size() <= 1);
        assertEquals(1, errors.size());
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
