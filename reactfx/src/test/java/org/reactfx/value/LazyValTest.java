package org.reactfx.value;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.reactfx.Subscription;
import org.reactfx.util.Try;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Created by Johannes on 27.06.2016.
 */
@RunWith(Enclosed.class)
public class LazyValTest {

    // FX Preparation

    private static final CountDownLatch latch = new CountDownLatch(1);

    @Ignore
    public static class AppLauncher extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            latch.countDown();
        }
    }

    @BeforeClass
    public static void startFx() {
        Thread t = new Thread(() -> Application.launch(AppLauncher.class));
        t.setDaemon(true);
        t.start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            fail("Interrupted!");
        }
    }

    @AfterClass
    public static void stopFx() {
        Platform.exit();
    }

    /**
     * Runs the task on the FX Thread, but waits until it completes. Any
     * exception thrown during {@code task.run()} will be rethrown on the test
     * thread.
     *
     * @param task
     */
    private static void doOnFx(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            try {
                task.run();
            } catch (AssertionError e) {
                throw new RuntimeException(e);
            }
        }
        else {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Throwable> excs = new AtomicReference<>();
            Platform.runLater(() -> {
                try {
                    doOnFx(task);
                }
                catch(Throwable e) {
                    excs.set(e);
                }
                finally {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                fail("Interrupted!");
            }
            finally {
                checkFxExceptions(excs.get());
            }
        }
    }

    private static void checkFxExceptions(Throwable e) {

        if (e != null)
        {
            if (e.getCause() instanceof AssertionError)
            {
                throw (AssertionError) e.getCause();
            }
            else {
                fail("Failed: " + e);
            }
        }
    }

    public static class Lazy {

        @Test
        public void ensureLaziness() {
            Val.lazy(() -> {
                fail("Synchronous method is not lazy.");
                return new Object();
            });
        }

        @Test
        public void ensureInitializedOnce() {
            Val<String> val = Val.lazy(new Supplier<String>() {
                boolean called = false;

                @Override
                public String get() {
                    if (called) {
                        fail("Supplier called twice.");
                    }
                    called = true;
                    return "Hello World!";
                }
            });

            assertEquals(
                    "lazyVal returned wrong value on first call.",
                    "Hello World!",
                    val.getValue());
            assertEquals(
                    "lazyVal returned wrong value on second call.",
                    "Hello World!",
                    val.getValue());
        }

        @Test
        public void ensureNotInvalidated() {
            Val<String> val = Val.lazy(() -> "Hello World!");

            val.observe(it -> fail("lazyVal is invalidated."));

            assertEquals(
                    "lazyVal returned wrong value.",
                    "Hello World!",
                    val.getValue());
        }
    }

    public static class LazyAsync {

        @Test(timeout = 1000L)
        public void ensureLaziness() {
            final Val<Try<String>> val = Val.lazyAsync(() -> {
                fail("Synchronous method is not lazy.");
                return "Failed!";
            });

            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                fail("Interrupted!");
            }

            assertTrue("Val is not empty.", val.isEmpty());
        }

        @Test(timeout = 2000L)
        public void ensureInitializedOnce() {
            // Create Val on FX thread
            AtomicReference<Val<Try<String>>> val = new AtomicReference<>();
            doOnFx(() -> val.set(Val.lazyAsync(new Callable<String>() {
                boolean called = false;

                @Override
                public String call() throws Exception {
                    if (called) {
                        fail("Supplier called twice.");
                    }
                    called = true;
                    Thread.sleep(200L);
                    return "Hello World!";
                }
            })));

            doOnFx(() -> assertTrue(
                    "New Val is not empty.",
                    val.get().isEmpty()));

            // Add two listeners on FX thread to try and force
            // double initialization
            final CountDownLatch observed = new CountDownLatch(2);

            AtomicReference<Subscription> subs = new AtomicReference<>();
            doOnFx(() -> {
                subs.set(val.get().observeChanges((obs, oldVal, newVal) -> {
                    doOnFx(() -> {
                        assertTrue(
                                "Old value after initialization was not null.",
                                oldVal == null);
                        assertTrue(
                                "Initialization was not a success.",
                                newVal.isSuccess());
                        assertEquals(
                                "New value after initialization is not correct.",
                                "Hello World!",
                                newVal.get());
                    });
                    observed.countDown();
                }));

                subs.set(subs.get().and(val.get().observeChanges(
                        (obs, oldVal, newVal) -> {
                            doOnFx(() -> {
                                assertTrue(
                                        "Old value after initialization was not null.",
                                        oldVal == null);
                                assertTrue(
                                        "Initialization was not a success.",
                                        newVal.isSuccess());
                                assertEquals(
                                        "New value after initialization is not correct.",
                                        "Hello World!",
                                        newVal.get());
                            });
                            observed.countDown();
                })));
            });

            // Initialization runs on background, so it should still be empty
            doOnFx(() -> assertTrue(
                    "New Val is not empty.",
                    val.get().isEmpty()));

            try {
                observed.await();
            } catch (InterruptedException e) {
                fail("Interrupted!");
            }

            // unsubscribe and subscribe again to try and force reinitialization
            doOnFx(() -> {
                subs.get().unsubscribe();
                val.get().observe(
                        it -> fail("Invalidated after initialization!"));
            });

            // wait a bit to make sure
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                fail("Interrupted!");
            }

            // check final values
            doOnFx(() -> {
                assertTrue("Val is not initialized.", val.get().isPresent());
                assertEquals(
                        "Final value is not correct.",
                        "Hello World!",
                        val.get().getValue().get());
            });
        }

        @Test(timeout = 1500L)
        public void ensureExceptionsGetCaught() {
            // Create Val on FX thread
            AtomicReference<Val<Try<String>>> val = new AtomicReference<>();
            doOnFx(() -> val.set(Val.lazyAsync(() -> {
                throw new IllegalStateException("Success!");
            })));

            final CountDownLatch observed = new CountDownLatch(1);

            // Add observer on FX thread
            doOnFx(() -> val.get().observeChanges((obs, oldVal, newVal) -> {
                doOnFx(() -> {
                    assertTrue(
                            "Old value after initialization was not null.",
                            oldVal == null);
                    assertTrue(
                            "Initialization was not a failure.",
                            newVal.isFailure());
                    assertTrue(
                            "Did not receive the expected Exception.",
                            newVal.getFailure() instanceof IllegalStateException);
                });
                observed.countDown();
            }));

            try {
                observed.await();
            } catch (InterruptedException e) {
                fail("Interrupted!");
            }

            // assert final error
            doOnFx(() -> {
                assertTrue("Val is not initialized.", val.get().isPresent());
                assertTrue(
                        "Final value is not a failure.",
                        val.get().getValue().isFailure());
            });
        }
    }


    public static class AwaitAsync {
        @Test(timeout = 1000L)
        public void ensureInitiallyEmpty() {
            final Val<Try<String>> val = Val.awaitAsync(
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            Thread.sleep(200L);
                        } catch (InterruptedException e) {
                            fail("Interrupted!");
                        }
                        fail("Synchronous method is not lazy.");
                        return "Fail";
                    }));

            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                fail("Interrupted!");
            }

            assertTrue("Val is not empty.", val.isEmpty());
        }

        @Test(timeout = 2000L)
        public void ensureInitialization() {
            // Create Val on FX thread
            AtomicReference<Val<Try<String>>> val = new AtomicReference<>();
            doOnFx(() -> val.set(Val.awaitAsync(
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            Thread.sleep(500L);
                        } catch (InterruptedException e) {
                            fail("Interrupted");
                        }
                        return "Hello World!";
            }))));

            doOnFx(() -> assertTrue(
                    "New Val is not empty.",
                    val.get().isEmpty()));

            // Add a listener on FX thread while the initialization is
            // still running
            final CountDownLatch observed = new CountDownLatch(1);

            AtomicReference<Subscription> subs = new AtomicReference<>();
            doOnFx(() -> {
                subs.set(val.get().observeChanges((obs, oldVal, newVal) -> {
                    doOnFx(() -> {
                        assertTrue(
                                "Old value after initialization was not null.",
                                oldVal == null);
                        assertTrue(
                                "Initialization was not a success.",
                                newVal.isSuccess());
                        assertEquals(
                                "New value after initialization is not correct.",
                                "Hello World!",
                                newVal.get());
                    });
                    observed.countDown();
                }));
            });

            // Initialization runs on background, so it should still be empty
            doOnFx(() -> assertTrue(
                    "New Val is not empty.",
                    val.get().isEmpty()));
            try {
                observed.await();
            } catch (InterruptedException e) {
                fail("Interrupted!");
            }

            // check final values
            doOnFx(() -> {
                assertTrue("Val is not initialized.", val.get().isPresent());
                assertEquals(
                        "Final value is not correct.",
                        "Hello World!",
                        val.get().getValue().get());
            });
        }

        @Test(timeout = 2000L)
        public void ensureExceptionsGetCaught() {
            // Create Val on FX thread
            AtomicReference<Val<Try<String>>> val = new AtomicReference<>();
            doOnFx(() -> val.set(Val.awaitAsync(
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException e) {
                            fail("Interrupted!");
                        }
                        throw new IllegalStateException("Success!");
            }))));

            final CountDownLatch observed = new CountDownLatch(1);

            // Add observer on FX thread
            doOnFx(() -> val.get().observeChanges((obs, oldVal, newVal) -> {
                doOnFx(() -> {
                    assertTrue(
                            "Old value after initialization was not null.",
                            oldVal == null);
                    assertTrue(
                            "Initialization was not a failure.",
                            newVal.isFailure());
                    assertEquals(
                            "Did not receive the expected Exception.",
                            IllegalStateException.class,
                            newVal.getFailure().getCause().getClass());
                });
                observed.countDown();
            }));

            try {
                observed.await();
            } catch (InterruptedException e) {
                fail("Interrupted!");
            }

            // assert final error
            doOnFx(() -> {
                assertTrue("Val is not initialized.", val.get().isPresent());
                assertTrue(
                        "Final value is not a failure.",
                        val.get().getValue().isFailure());
                assertEquals(
                        "Caught the wrong exception.",
                        "Success!",
                        val.get().getValue().getFailure().getCause().getMessage());
            });
        }

        @Test(timeout = 1500L)
        public void ensureCompletedStageGetsPickedUp() {
            final CompletableFuture<String> future = new CompletableFuture<>();
            future.complete("Success!");

            // Create Val on FX thread
            AtomicReference<Val<Try<String>>> val = new AtomicReference<>();
            doOnFx(() -> val.set(Val.awaitAsync(future)));

            // Add observer on FX thread
            doOnFx(() -> val.get().observeChanges((obs, oldVal, newVal) ->
                    fail("Reported change although CompletableFuture was already completed.")));

            // check final values
            doOnFx(() -> {
                assertTrue("Val is not initialized.", val.get().isPresent());
                assertEquals(
                        "Final value is not correct.",
                        "Success!",
                        val.get().getValue().get());
            });
        }
    }
}
