package org.reactfx;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.junit.Test;

public class ThreadBridgeTest {

    @Test
    public void test() throws InterruptedException, ExecutionException {
        ThreadFactory threadFactory1 = runnable -> new Thread(runnable, "thread 1");
        ThreadFactory threadFactory2 = runnable -> new Thread(runnable, "thread 2");
        ExecutorService exec1 = Executors.newSingleThreadExecutor(threadFactory1);
        ExecutorService exec2 = Executors.newSingleThreadExecutor(threadFactory2);
        EventSource<Integer> src1 = new EventSource<>();
        EventStream<Integer> stream2 = src1.threadBridge(exec1, exec2);
        EventStream<Integer> stream1 = stream2.threadBridge(exec2, exec1);

        List<Integer> emittedFrom2 = new ArrayList<>();
        List<Integer> emittedFrom1 = new ArrayList<>();
        List<String> emissionThreads2 = new ArrayList<>();
        List<String> emissionThreads1 = new ArrayList<>();

        CompletableFuture<List<Integer>> emittedFrom2Final = new CompletableFuture<>();
        CompletableFuture<List<Integer>> emittedFrom1Final = new CompletableFuture<>();
        CompletableFuture<List<String>> emissionThreads2Final = new CompletableFuture<>();
        CompletableFuture<List<String>> emissionThreads1Final = new CompletableFuture<>();

        CountDownLatch subscribed = new CountDownLatch(2);
        exec2.execute(() -> {
            stream2.subscribe(i -> {
                if(i != null) {
                    emittedFrom2.add(i);
                    emissionThreads2.add(Thread.currentThread().getName());
                } else {
                    emittedFrom2Final.complete(emittedFrom2);
                    emissionThreads2Final.complete(emissionThreads2);
                }
            });
            subscribed.countDown();
        });
        exec1.execute(() -> {
            stream1.subscribe(i -> {
                if(i != null) {
                    emittedFrom1.add(i);
                    emissionThreads1.add(Thread.currentThread().getName());
                } else {
                    emittedFrom1Final.complete(emittedFrom1);
                    emissionThreads1Final.complete(emissionThreads1);
                }
            });
            subscribed.countDown();
        });

        subscribed.await();
        exec1.execute(() -> {
            src1.push(1);
            src1.push(2);
            src1.push(3);
            src1.push(null);
        });

        assertEquals(Arrays.asList(1, 2, 3), emittedFrom2Final.get());
        assertEquals(Arrays.asList(1, 2, 3), emittedFrom1Final.get());
        assertEquals(Arrays.asList("thread 2", "thread 2", "thread 2"), emissionThreads2Final.get());
        assertEquals(Arrays.asList("thread 1", "thread 1", "thread 1"), emissionThreads1Final.get());

        exec1.shutdown();
        exec2.shutdown();
    }

}
