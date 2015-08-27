package org.reactfx.value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import org.junit.Test;
import org.reactfx.Change;

import static org.junit.Assert.assertArrayEquals;

public class ValTest {

    @Test
    public void changesTest() {
        IntegerProperty src = new SimpleIntegerProperty(0);
        Val<Number> val = Val.wrap(src);

        List<Change<Number>> changes = new ArrayList<>();
        val.changes().subscribe(changes::add);

        src.set(1);
        src.set(2);
        src.set(3);

        assertArrayEquals(Arrays.asList(0, 1, 2).toArray(),
            changes.stream().map(change -> change.getOldValue()).toArray());
        assertArrayEquals(Arrays.asList(1, 2, 3).toArray(),
            changes.stream().map(change -> change.getNewValue()).toArray());
    }

}
