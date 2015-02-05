package org.reactfx.inhibeans.demo;

import javafx.beans.binding.NumberExpression;
import javafx.beans.property.LongProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleLongProperty;

import org.reactfx.Guard;
import org.reactfx.value.SuspendableVar;
import org.reactfx.value.Var;

public class FibTest {

    private final int N;
    private final NumberExpression[] fib;

    private long invalidationCount = 0;

    private FibTest(int n) {
        N = n;
        fib = new NumberExpression[N];
        fib[0] = new SimpleLongProperty(0);
        fib[1] = new SimpleLongProperty(0);
        for(int i = 2; i < N; ++i) {
            fib[i] = fib[i-2].add(fib[i-1]);
        }
    }

    public void setupFor(Property<Number> resultHolder) {
        resultHolder.bind(fib[N-2].add(fib[N-1]));

        // count the invalidations of the result
        resultHolder.addListener(o -> {
            invalidationCount += 1;
            resultHolder.getValue(); // force recomputation
        });
    }

    public void run() {
        ((LongProperty) fib[1]).set(1);
    }

    public static void main(String[] args) {
        int n = 40;

        FibTest eagerTest = new FibTest(n);
        LongProperty eagerResult = new SimpleLongProperty();
        eagerTest.setupFor(eagerResult);

        FibTest lazyTest = new FibTest(n);
        SuspendableVar<Number> lazyResult = Var.suspendable(Var.newSimpleVar(0L));
        lazyTest.setupFor(lazyResult);

        long t1 = System.currentTimeMillis();
        eagerTest.run();
        long t2 = System.currentTimeMillis();
        double eagerTime = (t2-t1)/1000.0;

        t1 = System.currentTimeMillis();
        Guard g = lazyResult.suspend();
        lazyTest.run();
        g.close();
        t2 = System.currentTimeMillis();
        double lazyTime = (t2-t1)/1000.0;

        System.out.println("EAGER TEST:");
        System.out.println("    fib_" + n + " = " + eagerResult.get());
        System.out.println("    result invalidations: " + eagerTest.invalidationCount);
        System.out.println("    duration: " + eagerTime + " seconds");
        System.out.println();
        System.out.println("LAZY TEST:");
        System.out.println("    fib_" + n + " = " + lazyResult.getValue());
        System.out.println("    result invalidations: " + lazyTest.invalidationCount);
        System.out.println("    duration: " + lazyTime + " seconds");
    }
}
