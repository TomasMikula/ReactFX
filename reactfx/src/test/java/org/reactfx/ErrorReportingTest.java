package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.reactfx.inhibeans.property.SimpleIntegerProperty;

public class ErrorReportingTest {

    @Test
    public void basicTest() {
        List<Throwable> errors = new ArrayList<>();
        EventSource<Integer> source = new EventSource<>();
        EventStream<Integer> divided = source.map(x -> 5/x);
        divided.pin();
        divided.errors().subscribe(errors::add);
        source.push(0);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof ArithmeticException);
    }

    @Test
    public void threadBridgeTest() throws InterruptedException, ExecutionException {
        ExecutorService ex1 = Executors.newSingleThreadExecutor();
        ExecutorService ex2 = Executors.newSingleThreadExecutor();
        EventSource<Integer> source = new EventSource<>();
        EventStream<Integer> divided = source.map(x -> 5/x);
        EventStream<Integer> bridged = divided.threadBridge(ex1, ex2);
        List<Throwable> errors = new ArrayList<>();
        CompletableFuture<Integer> nErrors = new CompletableFuture<>();
        ex2.execute(() -> {
            bridged.pin();
            bridged.errors().subscribe(errors::add);
            ex1.execute(() -> {
                source.push(0);
                ex2.execute(() -> nErrors.complete(errors.size()));
            });
        });

        assertEquals(1, nErrors.get().intValue());

        ex1.shutdown();
        ex2.shutdown();
    }
    /**
     * Stream of property values should work normally even when the first value
     * in the stream (property value at the moment of subscription) produces an
     * exception. The catch is the first value produced at the very moment of
     * establishing subscription, so order of initialization matters.
     */
    @Test
    public void property_values_stream_with_faulty_first_value_test() {
        SimpleIntegerProperty intProperty = new SimpleIntegerProperty(-1);
        List<String> emitted = new LinkedList<>();
        List<Throwable> errors = new LinkedList<>();

        EventStreams.valuesOf(intProperty)
                .map(i -> {
                    if (i.intValue() < 0) {
                        throw new IllegalArgumentException("Accepting only positive numbers");
                    }
                    return String.valueOf(i);
                })
                .handleErrors(errors::add)
                .subscribe(emitted::add);

        intProperty.set(10);
        intProperty.set(-2);
        intProperty.set(0);

        assertEquals(Arrays.asList("10", "0"), emitted);
        assertEquals(2, errors.size());
    }

    /**
     * Variation on
     * {@link #property_values_stream_with_faulty_first_value_test()} using the
     * {@link EventStream#watch(java.util.function.Consumer, java.util.function.Consumer)}
     * method.
     */
    @Test
    public void property_values_stream_with_faulty_first_value_test2() {
        SimpleIntegerProperty intProperty = new SimpleIntegerProperty(-1);
        List<String> emitted = new LinkedList<>();
        List<Throwable> errors = new LinkedList<>();

        EventStreams.valuesOf(intProperty)
                .map(i -> {
                    if (i.intValue() < 0) {
                        throw new IllegalArgumentException("Accepting only positive numbers");
                    }
                    return String.valueOf(i);
                })
                .watch(emitted::add, errors::add);

        intProperty.set(10);
        intProperty.set(-2);
        intProperty.set(0);

        assertEquals(Arrays.asList("10", "0"), emitted);
        assertEquals(2, errors.size());
    }
}
