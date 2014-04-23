package org.reactfx.inhibeans.demo;

import java.util.function.Predicate;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;

import org.reactfx.Guard;
import org.reactfx.inhibeans.binding.BooleanBinding;

public class AndGateDemo {

    interface AndGate {
        void setInputs(boolean a, boolean b);
        ObservableBooleanValue a();
        ObservableBooleanValue b();
        ObservableBooleanValue output();
    }

    static void test(AndGate gate) {
        class Counter {
            int count = 0;
            public void inc() { count += 1; }
            public int get() { return count; }
        }

        Predicate<AndGate> consistent = g ->
            g.output().get() == (g.a().get() && g.b().get());

        gate.setInputs(false, false);
        assert gate.output().get() == false;

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
        assert gate.output().get() == true;

        assert na.get() == 1;
        assert nb.get() == 1;
        assert no.get() == 1;
    }

    static class AndGateImpl implements AndGate {
        private final BooleanProperty a = new SimpleBooleanProperty();
        private final BooleanProperty b = new SimpleBooleanProperty();
        private final BooleanBinding output = BooleanBinding.wrap(a.and(b));

        @Override
        public void setInputs(boolean a, boolean b) {
            Guard guard = output.block();
            this.a.set(a);
            this.b.set(b);
            guard.close();
        }

        @Override public ObservableBooleanValue a() { return a; }
        @Override public ObservableBooleanValue b() { return b; }
        @Override public ObservableBooleanValue output() { return output; }
    }

    public static void main(String[] args) {
        test(new AndGateImpl());
        System.out.println("AndGate implementation passed the test.");
    }
}
