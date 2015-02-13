package org.reactfx.value;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;

import org.junit.BeforeClass;
import org.junit.Test;
import org.reactfx.EventStreams;
import org.reactfx.util.Interpolator;

public class AnimatedValTest {

    private static class WaitUntilListener<T> implements ChangeListener<T> {
        private final Predicate<T> pred;
        private final CompletableFuture<?> toComplete;

        WaitUntilListener(Predicate<T> pred, CompletableFuture<?> toComplete) {
            this.pred = pred;
            this.toComplete = toComplete;
        }


        @Override
        public void changed(ObservableValue<? extends T> observable,
                T oldValue, T newValue) {
            if(pred.test(newValue)) {
                observable.removeListener(this);
                toComplete.complete(null);
            }
        }

    }

    private static <T> void waitUntil(
            ObservableValue<T> obs,
            Predicate<T> pred,
            int timeoutMillis) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            if(pred.test(obs.getValue())) {
                future.complete(null);
            } else {
                obs.addListener(new WaitUntilListener<>(pred, future));
            }
        });

        future.get(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @BeforeClass
    public static void setup() {
        new JFXPanel(); // initializes JavaFX toolkit
    }

    /**
     * Tests that the target value is reached, eventually.
     */
    @Test
    public void sanityTest() throws InterruptedException, ExecutionException, TimeoutException {
        Var<Double> src = Var.newSimpleVar(0.0);
        Val<Double> anim = src.animate(Duration.ofMillis(30), Interpolator.EASE_BOTH_DOUBLE);

        Platform.runLater(() -> {
            src.setValue(33.0);
        });

        waitUntil(anim, x -> x == 33.0, 100);
    }

    @Test
    public void testEqualNumberOfFramesForFixedDuration() throws InterruptedException, ExecutionException, TimeoutException {
        Var<Double> src1 = Var.newSimpleVar(0.0);
        Var<Double> src2 = Var.newSimpleVar(0.0);
        Val<Double> anim1 = src1.animate(Duration.ofMillis(500), Interpolator.LINEAR_DOUBLE);
        Val<Double> anim2 = src2.animate(Duration.ofMillis(500), Interpolator.LINEAR_DOUBLE);
        List<Double> vals1 = new ArrayList<>();
        List<Double> vals2 = new ArrayList<>();

        Platform.runLater(() -> {
            EventStreams.valuesOf(anim1).subscribe(vals1::add);
            EventStreams.valuesOf(anim2).subscribe(vals2::add);

            src1.setValue(10.0);
            src2.setValue(20.0);
        });

        waitUntil(anim1, x -> x == 10.0, 1000);
        waitUntil(anim2, x -> x == 20.0, 100);

        assertEquals(vals1.size(), vals2.size());
    }

    @Test
    public void testProportionalNumberOfFramesForFixedSpeed() throws InterruptedException, ExecutionException, TimeoutException {
        Var<Integer> src1 = Var.newSimpleVar(0);
        Var<Integer> src2 = Var.newSimpleVar(0);
        Val<Integer> anim1 = src1.animate((a, b) -> Duration.ofMillis(b - a), Interpolator.LINEAR_INTEGER);
        Val<Integer> anim2 = src2.animate((a, b) -> Duration.ofMillis(b - a), Interpolator.LINEAR_INTEGER);
        List<Integer> vals1 = new ArrayList<>();
        List<Integer> vals2 = new ArrayList<>();

        Platform.runLater(() -> {
            EventStreams.valuesOf(anim1).subscribe(vals1::add);
            EventStreams.valuesOf(anim2).subscribe(vals2::add);

            src1.setValue(100);
            src2.setValue(300);
        });

        waitUntil(anim2, x -> x == 300, 1000);

        assertEquals(100, anim1.getValue().intValue());

        // test that the number of frames for 0 -> 300 is at least
        // twice the number of frames for 0 -> 100 (i.e. a conservative test)
        assertThat(vals2.size(), greaterThan(2 * vals1.size()));
    }

    @Test
    public void midAnimationChangeTest() throws InterruptedException, ExecutionException, TimeoutException {
        Var<Double> src = Var.newSimpleVar(100.0);
        Val<Double> anim = src.animate(Duration.ofMillis(200), Interpolator.EASE_BOTH_DOUBLE);
        List<Double> vals = new ArrayList<>();

        Platform.runLater(() -> {
            EventStreams.valuesOf(anim).subscribe(vals::add);

            // when animated value reaches 200.0, set src to -1.0
            anim.addListener((obs, old, newVal) -> {
                if(newVal >= 200.0) {
                    src.setValue(-1.0);
                }
            });

            src.setValue(300.0);
        });

        waitUntil(anim, x -> x == -1.0, 1000);

        assertTrue("Value 300.0 never reached", vals.stream().noneMatch(x -> x == 300.0));
    }
}
