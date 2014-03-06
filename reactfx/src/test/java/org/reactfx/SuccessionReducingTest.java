package org.reactfx;

import static org.junit.Assert.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SuccessionReducingTest {
    private ScheduledExecutorService scheduler;

    @Before
    public void setUp() throws Exception {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        new JFXPanel(); // initializes JavaFX toolkit
    }

    @After
    public void tearDown() throws Exception {
        scheduler.shutdown();
    }

    @Test
    public void fxTest() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Integer>> future1 = new CompletableFuture<>();
        CompletableFuture<List<Integer>> future2 = new CompletableFuture<>();

        Platform.runLater(() -> {
            EventSource<Integer> source = new EventSource<>();

            EventStream<Integer> reducing1 = source.reduceCloseSuccessions((a,  b) -> a + b, Duration.ofMillis(200));
            List<Integer> emitted1 = new ArrayList<>();
            reducing1.subscribe(i -> emitted1.add(i));

            EventStream<Integer> reducing2 = source.reduceCloseSuccessions(() -> 0, (a,  b) -> a + b, Duration.ofMillis(200));
            List<Integer> emitted2 = new ArrayList<>();
            reducing2.subscribe(i -> emitted2.add(i));

            source.push(1);
            source.push(2);
            scheduler.schedule(() -> Platform.runLater(() -> source.push(3)), 50, TimeUnit.MILLISECONDS);

            scheduler.schedule(() -> Platform.runLater(() -> source.push(4)), 300, TimeUnit.MILLISECONDS);
            scheduler.schedule(() -> Platform.runLater(() -> source.push(5)), 350, TimeUnit.MILLISECONDS);

            scheduler.schedule(() -> Platform.runLater(() -> future1.complete(emitted1)), 600, TimeUnit.MILLISECONDS);
            scheduler.schedule(() -> Platform.runLater(() -> future2.complete(emitted2)), 600, TimeUnit.MILLISECONDS);
        });

        assertEquals(Arrays.asList(6, 9), future1.get());
        assertEquals(Arrays.asList(6, 9), future2.get());
    }

    @Test
    public void executorTest() throws InterruptedException, ExecutionException {
        CompletableFuture<List<Integer>> future1 = new CompletableFuture<>();
        CompletableFuture<List<Integer>> future2 = new CompletableFuture<>();

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            EventSource<Integer> source = new EventSource<>();

            EventStream<Integer> reducing1 = source.reduceCloseSuccessions((a,  b) -> a + b, Duration.ofMillis(200), scheduler, executor);
            List<Integer> emitted1 = new ArrayList<>();
            reducing1.subscribe(i -> emitted1.add(i));

            EventStream<Integer> reducing2 = source.reduceCloseSuccessions(() -> 0, (a,  b) -> a + b, Duration.ofMillis(200), scheduler, executor);
            List<Integer> emitted2 = new ArrayList<>();
            reducing2.subscribe(i -> emitted2.add(i));

            source.push(1);
            source.push(2);
            scheduler.schedule(() -> executor.execute(() -> source.push(3)), 50, TimeUnit.MILLISECONDS);

            scheduler.schedule(() -> executor.execute(() -> source.push(4)), 300, TimeUnit.MILLISECONDS);
            scheduler.schedule(() -> executor.execute(() -> source.push(5)), 350, TimeUnit.MILLISECONDS);

            scheduler.schedule(() -> executor.execute(() -> future1.complete(emitted1)), 600, TimeUnit.MILLISECONDS);
            scheduler.schedule(() -> executor.execute(() -> future2.complete(emitted2)), 600, TimeUnit.MILLISECONDS);
        });

        assertEquals(Arrays.asList(6, 9), future1.get());
        assertEquals(Arrays.asList(6, 9), future2.get());

        executor.shutdown();
    }
}
