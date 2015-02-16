package org.reactfx.value;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import org.junit.Test;
import org.reactfx.EventStreams;

public class VarFromValTest {

    @Test
    public void test() {
        IntegerProperty src = new SimpleIntegerProperty(0);
        IntegerBinding twice = src.multiply(2);
        Var<Number> twiceVar = Var.fromVal(twice, n -> src.set(n.intValue() / 2));

        List<Number> values = new ArrayList<>();
        EventStreams.valuesOf(twiceVar).subscribe(values::add);

        src.set(1);
        twiceVar.setValue(4);
        twiceVar.setValue(5); // no effect
        twiceVar.setValue(7); // will become 6

        assertEquals(Arrays.asList(0, 2, 4, 6), values);
    }

}
