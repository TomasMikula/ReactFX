package org.reactfx.value;

import static org.junit.Assert.*;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.junit.Test;
import org.reactfx.Counter;
import org.reactfx.Subscription;

public class FlatMapTest {

    private static class A {
        public final SimpleVar<B> b = (SimpleVar<B>) Var.<B>newSimpleVar(null);
    }

    private static class B {
        public final SimpleVar<String> s = (SimpleVar<String>) Var.<String>newSimpleVar(null);
    }

    @Test
    public void flatMapTest() {
        Property<A> base = new SimpleObjectProperty<>();
        Val<String> flat = Val.flatMap(base, a -> a.b).flatMap(b -> b.s);

        Counter invalidationCounter = new Counter();
        flat.addListener(obs -> invalidationCounter.inc());

        assertNull(flat.getValue());

        A a = new A();
        B b = new B();
        b.s.setValue("s1");
        a.b.setValue(b);
        base.setValue(a);
        assertEquals(1, invalidationCounter.getAndReset());
        assertEquals("s1", flat.getValue());

        a.b.setValue(new B());
        assertEquals(1, invalidationCounter.getAndReset());
        assertNull(flat.getValue());

        b.s.setValue("s2");
        assertEquals(0, invalidationCounter.getAndReset());
        assertNull(flat.getValue());

        a.b.getValue().s.setValue("x");
        assertEquals(1, invalidationCounter.getAndReset());
        assertEquals("x", flat.getValue());

        a.b.setValue(null);
        assertEquals(1, invalidationCounter.getAndReset());
        assertNull(flat.getValue());

        a.b.setValue(b);
        assertEquals(1, invalidationCounter.getAndReset());
        assertEquals("s2", flat.getValue());
    }

    @Test
    public void selectPropertyTest() {
        Property<A> base = new SimpleObjectProperty<>();
        Var<String> selected = Val.flatMap(base, a -> a.b).selectVar(b -> b.s);

        Counter invalidationCounter = new Counter();
        selected.addListener(obs -> invalidationCounter.inc());

        assertNull(selected.getValue());

        selected.setValue("will be discarded");
        assertNull(selected.getValue());
        assertEquals(0, invalidationCounter.getAndReset());

        Property<String> src = new SimpleStringProperty();

        selected.bind(src);
        assertNull(selected.getValue());
        assertEquals(0, invalidationCounter.getAndReset());

        src.setValue("1");
        assertNull(selected.getValue());
        assertEquals(0, invalidationCounter.getAndReset());

        A a = new A();
        B b = new B();
        b.s.setValue("X");
        a.b.setValue(b);
        base.setValue(a);

        assertEquals(1, invalidationCounter.getAndReset());
        assertEquals("1", selected.getValue());
        assertEquals("1", b.s.getValue());

        src.setValue("2");
        assertEquals(1, invalidationCounter.getAndReset());
        assertEquals("2", selected.getValue());
        assertEquals("2", b.s.getValue());

        B b2 = new B();
        b2.s.setValue("Y");
        a.b.setValue(b2);
        assertEquals(1, invalidationCounter.getAndReset());
        assertEquals("2", b2.s.getValue());
        assertEquals("2", selected.getValue());

        src.setValue("3");
        assertEquals(1, invalidationCounter.getAndReset());
        assertEquals("3", b2.s.getValue());
        assertEquals("3", selected.getValue());
        assertEquals("2", b.s.getValue());

        base.setValue(null);
        assertEquals(1, invalidationCounter.getAndReset());
        assertNull(selected.getValue());
        assertFalse(b2.s.isBound());

        base.setValue(a);
        assertEquals(1, invalidationCounter.getAndReset());
        assertEquals("3", selected.getValue());
        assertTrue(b2.s.isBound());

        selected.unbind();
        assertEquals(0, invalidationCounter.getAndReset());
        src.setValue("4");
        assertEquals("3", b2.s.getValue());
        assertEquals("3", selected.getValue());
        assertEquals("2", b.s.getValue());

        a.b.setValue(b);
        selected.setValue("5");
        assertEquals("5", b.s.getValue());

        a.b.setValue(null);
        selected.bind(src);
        a.b.setValue(b2);
        assertTrue(b2.s.isBound());
    }

    @Test
    public void selectPropertyResetTest() {
        Property<A> base = new SimpleObjectProperty<>();
        Var<String> selected = Val.flatMap(base, a -> a.b).selectVar(b -> b.s, "X");
        StringProperty source = new SimpleStringProperty("A");

        selected.bind(source);

        assertEquals(null, selected.getValue());

        A a = new A();
        B b = new B();
        a.b.setValue(b);
        base.setValue(a);
        assertEquals("A", selected.getValue());
        assertEquals("A", b.s.getValue());

        B b2 = new B();
        a.b.setValue(b2);
        assertEquals("A", b2.s.getValue());
        assertEquals("X", b.s.getValue());

        base.setValue(null);
        assertEquals("X", b2.s.getValue());
    }

    @Test
    public void lazinessTest() {
        SimpleVar<A> base = (SimpleVar<A>) Var.<A>newSimpleVar(null);
        Val<B> flatMapped = base.flatMap(a -> a.b);
        Var<String> selected = flatMapped.selectVar(b -> b.s);

        assertFalse(base.isObservingInputs());

        A a = new A();
        B b = new B();
        a.b.setValue(b);
        base.setValue(a);

        assertFalse(base.isObservingInputs());
        assertFalse(a.b.isObservingInputs());
        assertFalse(b.s.isObservingInputs());

        Subscription sub = selected.pin();

        assertTrue(base.isObservingInputs());
        assertTrue(a.b.isObservingInputs());
        assertTrue(b.s.isObservingInputs());

        B b2 = new B();
        a.b.setValue(b2);

        assertFalse(b.s.isObservingInputs()); // stopped observing b.s
        assertTrue(base.isObservingInputs());
        assertTrue(a.b.isObservingInputs());
        assertFalse(b2.s.isObservingInputs()); // no need to observe b2.s yet

        selected.setValue("Y");

        assertFalse(b2.s.isObservingInputs()); // still no need to observe b2.s

        selected.getValue();

        assertTrue(b2.s.isObservingInputs()); // now we have to observe b2.s for invalidations

        sub.unsubscribe();

        assertFalse(base.isObservingInputs());
        assertFalse(a.b.isObservingInputs());
        assertFalse(b2.s.isObservingInputs());
        assertFalse(((ValBase<B>) flatMapped).isObservingInputs());
    }
}
