package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class UntilLaterTest {
    private static ExecutorService executor;

    @BeforeClass
    public static void setupExecutor() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterClass
    public static void shutdownExecutor() {
        executor.shutdown();
    }

    @Test
    public void retainLatestUntilLaterTest() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Integer>> future = new CompletableFuture<>();
        EventSource<Integer> source = new EventSource<>();
        EventStream<Integer> stream = source.retainLatestUntilLater(executor);
        executor.execute(() -> {
            List<Integer> emitted = new ArrayList<>();
            stream.subscribe(emitted::add);
            source.push(1);
            executor.execute(() -> {
                source.push(2);
                source.push(3);
                source.push(4);
                executor.execute(() -> {
                    source.push(5);
                    source.push(6);
                    executor.execute(() -> future.complete(emitted));
                });
                source.push(7);
                source.push(8);
            });
            source.push(9);
        });
        List<Integer> emitted = future.get();
        assertEquals(Arrays.asList(9, 8, 6), emitted);
    }

    @Test
    public void queueUntilLaterTest() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Integer>> future = new CompletableFuture<>();
        EventSource<Integer> source = new EventSource<>();
        EventStream<Integer> stream = source.queueUntilLater(executor);
        executor.execute(() -> {
            List<Integer> emitted = new ArrayList<>();
            stream.subscribe(emitted::add);
            source.push(1);
            executor.execute(() -> {
                source.push(2);
                source.push(3);
                source.push(4);
                executor.execute(() -> {
                    source.push(5);
                    source.push(6);
                    executor.execute(() -> future.complete(emitted));
                });
                source.push(7);
                source.push(8);
            });
            source.push(9);
        });
        List<Integer> emitted = future.get();
        assertEquals(Arrays.asList(1, 9, 2, 3, 4, 7, 8, 5, 6), emitted);
    }
}
