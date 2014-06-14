package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

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
}
