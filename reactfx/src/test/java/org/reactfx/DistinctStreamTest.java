package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuple3;
import org.reactfx.util.Tuples;

public class DistinctStreamTest {
    <T> void testDistinct(List<T> input, List<T> expectedOutput) {
        EventSource<T> source = new EventSource<>();
        EventStream<T> distinct = new DistinctStream<>(source);
        List<T> distinctCollector = new ArrayList<>();
        distinct.subscribe(distinctCollector::add);
        input.forEach(source::push);
        assertEquals(expectedOutput, distinctCollector);
    }

    @Test
    public void testStream() {
        testDistinct(
            Arrays.asList(0, 1, 1, 0),
            Arrays.asList(0, 1, 0)
        );
    }

    @Test
    public void testStreamWithNulls() {
        testDistinct(
            Arrays.asList(null, null, 1, null, null),
            Arrays.asList(null, 1, null)
        );
    }

    @Test
    public void testBiStream() {
        EventSource<Long> aSource = new EventSource<>();
        EventSource<Long> bSource = new EventSource<>();
        BiEventStream<Long, Long> combination = EventStreams.combine(aSource, bSource);
        BiEventStream<Long, Long> distinct = new DistinctBiStream<>(combination);
        List<Tuple2<Long, Long>> distinctCollector = new ArrayList<>();
        distinct.subscribe((a, b) -> distinctCollector.add(Tuples.t(a, b)));

        aSource.push(null);
        assertTrue(distinctCollector.isEmpty());

        bSource.push(null);
        checkList(Tuples.t(null, null), distinctCollector);

        aSource.push(null);
        assertTrue(distinctCollector.isEmpty());

        aSource.push(1L);
        checkList(Tuples.t(1L, null), distinctCollector);

        bSource.push(2L);
        checkList(Tuples.t(1L, 2L), distinctCollector);

        aSource.push(1L);
        assertTrue(distinctCollector.isEmpty());

        bSource.push(2L);
        assertTrue(distinctCollector.isEmpty());

        aSource.push(3L);
        checkList(Tuples.t(3L, 2L), distinctCollector);

        bSource.push(4L);
        checkList(Tuples.t(3L, 4L), distinctCollector);
    }

    @Test
    public void testTriStream() {
        EventSource<Long> aSource = new EventSource<>();
        EventSource<Long> bSource = new EventSource<>();
        EventSource<Long> cSource = new EventSource<>();
        TriEventStream<Long, Long, Long> combination = EventStreams.combine(aSource, bSource, cSource);
        TriEventStream<Long, Long, Long> distinct = new DistinctTriStream<>(combination);
        List<Tuple3<Long, Long, Long>> distinctCollector = new ArrayList<>();
        distinct.subscribe((a, b, c) -> distinctCollector.add(Tuples.t(a, b, c)));

        aSource.push(null);
        assertTrue(distinctCollector.isEmpty());

        bSource.push(null);
        assertTrue(distinctCollector.isEmpty());

        cSource.push(null);
        checkList(Tuples.t(null, null, null), distinctCollector);

        aSource.push(null);
        assertTrue(distinctCollector.isEmpty());

        aSource.push(1L);
        checkList(Tuples.t(1L, null, null), distinctCollector);

        bSource.push(2L);
        checkList(Tuples.t(1L, 2L, null), distinctCollector);

        cSource.push(3L);
        checkList(Tuples.t(1L, 2L, 3L), distinctCollector);

        aSource.push(1L);
        assertTrue(distinctCollector.isEmpty());

        bSource.push(2L);
        assertTrue(distinctCollector.isEmpty());

        cSource.push(3L);
        assertTrue(distinctCollector.isEmpty());

        aSource.push(4L);
        checkList(Tuples.t(4L, 2L, 3L), distinctCollector);

        bSource.push(5L);
        checkList(Tuples.t(4L, 5L, 3L), distinctCollector);

        cSource.push(6L);
        checkList(Tuples.t(4L, 5L, 6L), distinctCollector);
    }

    private void checkList(Object elem, List<?> list) {
        assertEquals(Arrays.asList(elem), list);
        list.clear();
    }
}
