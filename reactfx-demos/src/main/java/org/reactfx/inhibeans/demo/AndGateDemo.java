package org.reactfx.inhibeans.demo;

import java.util.function.Predicate;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

import org.reactfx.Guard;
import org.reactfx.value.SuspendableVal;
import org.reactfx.value.Val;

public class AndGateDemo {

    interface AndGate {
        void setInputs(boolean a, boolean b);
        ObservableValue<Boolean> a();
        ObservableValue<Boolean> b();
        ObservableValue<Boolean> output();
    }

    static void test(AndGate gate) {
        class Counter {
            int count = 0;
            public void inc() { count += 1; }
            public int get() { return count; }
        }

        Predicate<AndGate> consistent = g ->
            g.output().getValue() == (g.a().getValue() && g.b().getValue());

        gate.setInputs(false, false);
        assert gate.output().getValue() == false;

        Counter na = new Counter();
        Counter nb = new Counter();
        Counter no = new Counter();

        gate.a().addListener(observable -> {
            assert consistent.test(gate);
            na.inc();
        });
        gate.b().addListener(observable -> {
            assert consistent.test(gate);
            nb.inc();
        });
        gate.output().addListener(observable -> {
            assert consistent.test(gate);
            no.inc();
        });

        gate.setInputs(true, true);
        assert gate.output().getValue() == true;

        assert na.get() == 1;
        assert nb.get() == 1;
        assert no.get() == 1;
    }

    static class AndGateImpl implements AndGate {
        private final BooleanProperty a = new SimpleBooleanProperty();
        private final BooleanProperty b = new SimpleBooleanProperty();
        private final SuspendableVal<Boolean> output = Val.suspendable(a.and(b));

        @Override
        public void setInputs(boolean a, boolean b) {
            Guard guard = output.suspend();
            this.a.set(a);
            this.b.set(b);
            guard.close();
        }

        @Override public ObservableBooleanValue a() { return a; }
        @Override public ObservableBooleanValue b() { return b; }
        @Override public ObservableValue<Boolean> output() { return output; }
    }

    public static void main(String[] args) {
        test(new AndGateImpl());
        System.out.println("AndGate implementation passed the test.");
    }
}
