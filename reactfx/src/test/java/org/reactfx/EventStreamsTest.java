package org.reactfx;

import static org.junit.Assert.*;
import org.junit.Test;
import org.reactfx.inhibeans.property.SimpleIntegerProperty;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class EventStreamsTest {
    /**
     * Stream of property values should work normally even first value in stream (property
     * value on the moment of subscription) produces exception. The catch is first value produced in
     * the very moment of establishing subsription so order of initialization matters.
     */
    @Test
    public void property_values_stream_with_faulty_first_value_test() {
        SimpleIntegerProperty intProperty = new SimpleIntegerProperty(-1);
        List<String> emitted = new LinkedList<>();
        List<Throwable> errors = new LinkedList<>();

        EventStreams.valuesOf(intProperty)
                .map(i -> {
                    if (i.intValue() < 0) {
                        throw new IllegalArgumentException("Accepting only positive numbers");
                    }
                    return String.valueOf(i);
                })
                .handleErrors(errors::add)
                .subscribe(emitted::add);

        intProperty.set(10);
        intProperty.set(-2);
        intProperty.set(0);

        assertEquals(Arrays.asList("10", "0"), emitted);
        assertEquals(2, errors.size());
    }
}
