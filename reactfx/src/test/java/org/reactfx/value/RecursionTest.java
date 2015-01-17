package org.reactfx.value;

import static org.junit.Assert.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import org.junit.Test;

public class RecursionTest {

    @Test
    public void test() {
        Var<Integer> var = Var.newSimpleVar(0);
        Var<Integer> lastObserved1 = Var.newSimpleVar(var.getValue());
        Var<Integer> lastObserved2 = Var.newSimpleVar(var.getValue());
        var.addListener((obs, oldVal, newVal) -> {
            assertEquals(lastObserved1.getValue(), oldVal);
            lastObserved1.setValue(newVal);
            if(newVal == 1) {
                var.setValue(2);
            }
        });
        var.addListener((obs, oldVal, newVal) -> {
            assertEquals(lastObserved2.getValue(), oldVal);
            lastObserved2.setValue(newVal);
            if(newVal == 1) {
                var.setValue(2);
            }
        });
        var.setValue(1);
    }

    /**
     * This is not a test of ReactFX functionality, but rather a showcase of
     * JavaFX disfunctionality.
     */
    @Test
    public void failingRecursionForJavaFxProperty() {
        IntegerProperty var = new SimpleIntegerProperty(0);
        IntegerProperty lastObserved1 = new SimpleIntegerProperty(var.get());
        IntegerProperty lastObserved2 = new SimpleIntegerProperty(var.get());
        BooleanProperty failedAsExpected = new SimpleBooleanProperty(false);
        var.addListener((obs, oldVal, newVal) -> {
            if(lastObserved1.get() != oldVal.intValue()) {
                failedAsExpected.set(true);
            }
            lastObserved1.set(newVal.intValue());
            if(newVal.intValue() == 1) {
                var.set(2);
            }
        });
        var.addListener((obs, oldVal, newVal) -> {
            if(lastObserved1.get() != oldVal.intValue()) {
                failedAsExpected.set(true);
            }
            lastObserved2.set(newVal.intValue());
            if(newVal.intValue() == 1) {
                var.set(2);
            }
        });
        var.set(1);
        assertTrue(failedAsExpected.get());
    }
}
