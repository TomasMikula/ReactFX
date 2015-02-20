package org.reactfx.collection;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.reactfx.value.Var;

public class ValAsListTest {

    @Test
    public void testNullToValChange() {
        Var<String> src = Var.newSimpleVar(null);
        LiveList<String> list = src.asList();
        assertEquals(0, list.size());

        List<ListModification<? extends String>> mods = new ArrayList<>();
        list.observeModifications(mods::add);

        src.setValue("foo");
        assertEquals(1, mods.size());
        ListModification<? extends String> mod = mods.get(0);
        assertEquals(0, mod.getRemovedSize());
        assertEquals(Collections.singletonList("foo"), mod.getAddedSubList());
        assertEquals(1, list.size());
    }

    @Test
    public void testValToNullChange() {
        Var<String> src = Var.newSimpleVar("foo");
        LiveList<String> list = src.asList();
        assertEquals(1, list.size());

        List<ListModification<? extends String>> mods = new ArrayList<>();
        list.observeModifications(mods::add);

        src.setValue(null);
        assertEquals(1, mods.size());
        ListModification<? extends String> mod = mods.get(0);
        assertEquals(Collections.singletonList("foo"), mod.getRemoved());
        assertEquals(0, mod.getAddedSize());
        assertEquals(0, list.size());
    }

    @Test
    public void testValToValChange() {
        Var<String> src = Var.newSimpleVar("foo");
        LiveList<String> list = src.asList();
        assertEquals(1, list.size());

        List<ListModification<? extends String>> mods = new ArrayList<>();
        list.observeModifications(mods::add);

        src.setValue("bar");
        assertEquals(1, mods.size());
        ListModification<? extends String> mod = mods.get(0);
        assertEquals(Collections.singletonList("foo"), mod.getRemoved());
        assertEquals(Collections.singletonList("bar"), mod.getAddedSubList());
        assertEquals(1, list.size());
    }

}
