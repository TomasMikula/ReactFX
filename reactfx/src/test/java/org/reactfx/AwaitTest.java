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

public class AwaitTest {

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
    public void testAwaitCompletionStage() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Object>> emitted = new CompletableFuture<>();
        EventSource<Integer> src = new EventSource<>();
        AwaitingEventStream<Integer> mapped = src.mapToCompletionStage(x -> async(x*x)).await(executor);
        executor.execute(() -> {
            List<Object> res = aggregate(EventStreams.merge(
                    mapped,
                    EventStreams.valuesOf(mapped.pendingProperty())));
            src.push(1);
            src.push(2);
            src.push(3);
            src.push(4);
            executor.execute(() -> executor.execute(() -> emitted.complete(res)));
        });
        assertEquals(Arrays.asList(false, true, 1, 4, 9, 16, false), emitted.get());
    }

    @Test
    public void testAwaitTask() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Object>> emitted = new CompletableFuture<>();
        EventSource<Integer> src = new EventSource<>();
        AwaitingEventStream<Integer> mapped = src.mapToTask(x -> background(x*x)).await();
        Platform.runLater(() -> {
            List<Object> res = aggregate(EventStreams.merge(
                    mapped,
                    EventStreams.valuesOf(mapped.pendingProperty())));
            src.push(1);
            src.push(2);
            src.push(3);
            src.push(4);
            executor.execute(() -> Platform.runLater(() -> emitted.complete(res)));
        });
        assertEquals(Arrays.asList(false, true, 1, 4, 9, 16, false), emitted.get());
    }

    @Test
    public void testAwaitLatestCompletionStage() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Object>> emitted = new CompletableFuture<>();
        EventSource<Integer> src = new EventSource<>();
        AwaitingEventStream<Integer> mapped = src.mapToCompletionStage(x -> async(x*x)).awaitLatest(executor);
        executor.execute(() -> {
            List<Object> res = aggregate(EventStreams.merge(
                    mapped,
                    EventStreams.valuesOf(mapped.pendingProperty())));
            src.push(1);
            src.push(2);
            executor.execute(() -> executor.execute(() -> {
                src.push(3);
                src.push(4);
                executor.execute(() -> executor.execute(() -> emitted.complete(res)));
            }));
        });
        assertEquals(Arrays.asList(false, true, 4, false, true, 16, false), emitted.get());
    }

    @Test
    public void testAwaitLatestTask() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Object>> emitted = new CompletableFuture<>();
        EventSource<Integer> src = new EventSource<>();
        AwaitingEventStream<Integer> mapped = src.mapToTask(x -> background(x*x)).awaitLatest();
        executor.execute(() -> {
            List<Object> res = aggregate(EventStreams.merge(
                    mapped,
                    EventStreams.valuesOf(mapped.pendingProperty())));
            src.push(1);
            src.push(2);
            executor.execute(() -> Platform.runLater(() -> {
                src.push(3);
                src.push(4);
                executor.execute(() -> Platform.runLater(() -> emitted.complete(res)));
            }));
        });
        assertEquals(Arrays.asList(false, true, 4, false, true, 16, false), emitted.get());
    }

    @Test
    public void testAwaitLatestCompletionStageWithCanceller() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Object>> emitted = new CompletableFuture<>();
        EventSource<Integer> src = new EventSource<>();
        EventSource<Void> canceller = new EventSource<>();
        AwaitingEventStream<Integer> mapped = src.mapToCompletionStage(x -> async(x*x)).awaitLatest(canceller, executor);
        executor.execute(() -> {
            List<Object> res = aggregate(EventStreams.merge(
                    mapped,
                    EventStreams.valuesOf(mapped.pendingProperty())));
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
        assertEquals(Arrays.asList(false, true, false, true, 16, false, true, false), emitted.get());
    }

    @Test
    public void testAwaitLatestTaskWithCanceller() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Object>> emitted = new CompletableFuture<>();
        EventSource<Integer> src = new EventSource<>();
        EventSource<Void> canceller = new EventSource<>();
        AwaitingEventStream<Integer> mapped = src.mapToTask(x -> background(x*x)).awaitLatest(canceller);
        Platform.runLater(() -> {
            List<Object> res = aggregate(EventStreams.merge(
                    mapped,
                    EventStreams.valuesOf(mapped.pendingProperty())));
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
        assertEquals(Arrays.asList(false, true, false, true, 16, false, true, false), emitted.get());
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
