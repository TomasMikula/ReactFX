package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.JFXPanel;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AsyncMapTest {

    private ExecutorService executor;

    @BeforeClass
    public static void setUpBeforeClass() {
        new JFXPanel();
    }

    @Before
    public void setUp() throws Exception {
        executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdown();
    }

    @Test
    public void testMapAsync() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Integer>> emitted = new CompletableFuture<>();
        EventSource<Integer> src = new EventSource<>();
        EventStream<Integer> mapped = src.mapAsync(x -> async(x*x), executor);
        executor.execute(() -> {
            List<Integer> res = aggregate(mapped);
            src.push(1);
            src.push(2);
            src.push(3);
            src.push(4);
            executor.execute(() -> executor.execute(() -> emitted.complete(res)));
        });
        assertEquals(Arrays.asList(1, 4, 9, 16), emitted.get());
    }

    @Test
    public void testMapInBackground() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Integer>> emitted = new CompletableFuture<>();
        EventSource<Integer> src = new EventSource<>();
        EventStream<Integer> mapped = src.mapInBackground(x -> background(x*x));
        Platform.runLater(() -> {
            List<Integer> res = aggregate(mapped);
            src.push(1);
            src.push(2);
            src.push(3);
            src.push(4);
            executor.execute(() -> Platform.runLater(() -> emitted.complete(res)));
        });
        assertEquals(Arrays.asList(1, 4, 9, 16), emitted.get());
    }

    @Test
    public void testMapAsyncSkipOutdated() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Integer>> emitted = new CompletableFuture<>();
        EventSource<Integer> src = new EventSource<>();
        EventStream<Integer> mapped = src.mapAsyncSkipOutdated(x -> async(x*x), executor);
        executor.execute(() -> {
            List<Integer> res = aggregate(mapped);
            src.push(1);
            src.push(2);
            executor.execute(() -> executor.execute(() -> {
                src.push(3);
                src.push(4);
                executor.execute(() -> executor.execute(() -> emitted.complete(res)));
            }));
        });
        assertEquals(Arrays.asList(4, 16), emitted.get());
    }

    @Test
    public void testMapInBackgroundSkipOutdated() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Integer>> emitted = new CompletableFuture<>();
        EventSource<Integer> src = new EventSource<>();
        EventStream<Integer> mapped = src.mapInBackgroundSkipOutdated(x -> background(x*x));
        executor.execute(() -> {
            List<Integer> res = aggregate(mapped);
            src.push(1);
            src.push(2);
            executor.execute(() -> Platform.runLater(() -> {
                src.push(3);
                src.push(4);
                executor.execute(() -> Platform.runLater(() -> emitted.complete(res)));
            }));
        });
        assertEquals(Arrays.asList(4, 16), emitted.get());
    }

    @Test
    public void testMapAsyncSkipOutdatedWithCanceller() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Integer>> emitted = new CompletableFuture<>();
        EventSource<Integer> src = new EventSource<>();
        EventSource<Void> canceller = new EventSource<>();
        EventStream<Integer> mapped = src.mapAsyncSkipOutdated(x -> async(x*x), canceller, executor);
        executor.execute(() -> {
            List<Integer> res = aggregate(mapped);
            src.push(1);
            src.push(2);
            canceller.push(null);
            executor.execute(() -> executor.execute(() -> {
                src.push(3);
                src.push(4);
                executor.execute(() -> executor.execute(() -> {
                    src.push(5);
                    src.push(6);
                    canceller.push(null);
                    executor.execute(() -> executor.execute(() -> emitted.complete(res)));
                }));
            }));
        });
        assertEquals(Arrays.asList(16), emitted.get());
    }

    @Test
    public void testMapInBackgroundSkipOutdatedWithCanceller() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Integer>> emitted = new CompletableFuture<>();
        EventSource<Integer> src = new EventSource<>();
        EventSource<Void> canceller = new EventSource<>();
        EventStream<Integer> mapped = src.mapInBackgroundSkipOutdated(x -> background(x*x), canceller);
        Platform.runLater(() -> {
            List<Integer> res = aggregate(mapped);
            src.push(1);
            src.push(2);
            canceller.push(null);
            executor.execute(() -> Platform.runLater(() -> {
                src.push(3);
                src.push(4);
                executor.execute(() -> Platform.runLater(() -> {
                    src.push(5);
                    src.push(6);
                    canceller.push(null);
                    executor.execute(() -> Platform.runLater(() -> emitted.complete(res)));
                }));
            }));
        });
        assertEquals(Arrays.asList(16), emitted.get());
    }

    private CompletionStage<Integer> async(int x) {
        CompletableFuture<Integer> res = new CompletableFuture<>();
        executor.execute(() -> res.complete(x));
        return res;
    }

    private <T> Task<T> background(T t) {
        Task<T> task = new Task<T>() {
            @Override
            protected T call() throws Exception {
                return t;
            }
        };
        executor.execute(task);
        return task;
    }

    private static <T> List<T> aggregate(EventStream<T> stream) {
        List<T> res = new ArrayList<>();
        stream.subscribe(x -> res.add(x));
        return res;
    }
}
