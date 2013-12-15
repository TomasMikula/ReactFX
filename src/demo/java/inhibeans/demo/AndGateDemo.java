package inhibeans.demo;

import inhibeans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;

public class AndGateDemo {

    interface AndGate {
        void setInputs(boolean a, boolean b);
        ObservableBooleanValue a();
        ObservableBooleanValue b();
        ObservableBooleanValue output();
    }

    static class Counter {
        int count = 0;
        public void inc() { count += 1; }
        public int get() { return count; }
    }

    static void test(AndGate gate) {
        gate.setInputs(false, false);
        assert gate.output().get() == false;

        Counter na = new Counter();
        Counter nb = new Counter();
        Counter no = new Counter();

        gate.a().addListener(observable -> {
            assert consistent(gate);
            na.inc();
        });
        gate.b().addListener(observable -> {
            assert consistent(gate);
            nb.inc();
        });
        gate.output().addListener(observable -> {
            assert consistent(gate);
            no.inc();
        });

        gate.setInputs(true, true);
        assert gate.output().get() == true;

        assert na.get() == 1;
        assert nb.get() == 1;
        assert no.get() == 1;
    }

    static boolean consistent(AndGate gate) {
        return gate.output().get() == (gate.a().get() && gate.b().get());
    }

    public static void main(String[] args) {
        AndGate gate = new AndGate() {
            private final BooleanProperty a = new SimpleBooleanProperty();
            private final BooleanProperty b = new SimpleBooleanProperty();
            private final BooleanBinding output = new BooleanBinding() {
                { bind(a, b); }
                @Override
                protected boolean computeValue() {
                    return a.get() && b.get();
                }
            };

            @Override
            public void setInputs(boolean a, boolean b) {
                output.block();
                this.a.set(a);
                this.b.set(b);
                output.release();
            }

            @Override public ObservableBooleanValue a() { return a; }
            @Override public ObservableBooleanValue b() { return b; }
            @Override public ObservableBooleanValue output() { return output; }
        };

        test(gate);
        System.out.println("AndGate implementation passed the test.");
    }
}
