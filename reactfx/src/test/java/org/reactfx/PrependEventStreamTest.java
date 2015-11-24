package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.reactfx.value.Var;


public class PrependEventStreamTest {

    @Test
    public void test() {
        EventCounter countsTwice = new EventCounter();
        EventCounter countsOnce = new EventCounter();

        EventSource<Boolean> source = new EventSource<>();
        EventStream<Boolean> stream = source.prepend(true);

        stream.subscribe(countsTwice::accept);
        source.push(false);
        stream.subscribe(countsOnce::accept);

        assertEquals("Counts Twice failed", 2, countsTwice.get());
        assertEquals("Counts Once failed", 1, countsOnce.get());
    }

    @Test
    public void testAutoEmittingStream() {
        List<Integer> emitted1 = new ArrayList<>();
        List<Integer> emitted2 = new ArrayList<>();

        Var<Integer> source = Var.newSimpleVar(1);
        EventStream<Integer> stream = source.values().prepend(0);

        stream.subscribe(emitted1::add);
        stream.subscribe(emitted2::add);

        assertEquals(Arrays.asList(0, 1), emitted1);
        assertEquals(Arrays.asList(1), emitted2);

        source.setValue(2);

        assertEquals(Arrays.asList(0, 1, 2), emitted1);
        assertEquals(Arrays.asList(1, 2), emitted2);
    }
}
