package inhibeans.property;

import static org.junit.Assert.*;
import inhibeans.Hold;
import javafx.beans.value.ObservableValue;

import org.junit.Test;

public class PropertyTest {

    private void testBooleanProperty(Property<Boolean> tested) {
        testBooleanProperty(tested, tested);
    }

    private void testBooleanProperty(Property<Boolean> tested, javafx.beans.property.Property<Boolean> source) {
        CountingListener counter = new CountingListener(tested);

        source.setValue(true);
        source.setValue(false);
        assertEquals(2, counter.getAndReset());

        Hold b = tested.block();
        source.setValue(true);
        source.setValue(false);
        b.close();
        assertEquals(1, counter.get());
    }

    @Test
    public void simpleBooleanPropertyTest() {
        SimpleBooleanProperty property = new SimpleBooleanProperty(false);
        testBooleanProperty(property);
    }

    @Test
    public void booleanPropertyBaseTest() {
        BooleanPropertyBase property = new BooleanPropertyBase(false) {
            @Override public Object getBean() { return null; }
            @Override public String getName() { return null; }
        };
        testBooleanProperty(property);
    }

    @Test
    public void readOnlyBooleanWrapperTest() {
        ReadOnlyBooleanWrapper wrapper = new ReadOnlyBooleanWrapper(false);
        testBooleanProperty(wrapper);
    }

    @Test
    public void readOnlyBooleanPropertyBaseTest() {
        SimpleBooleanProperty source = new SimpleBooleanProperty(false);
        ReadOnlyBooleanPropertyBase property = new ReadOnlyBooleanPropertyBase() {
            {
                source.addListener(obs -> fireValueChangedEvent());
            }

            @Override
            public boolean get() {
                return source.get();
            }

            @Override public void bind(ObservableValue<? extends Boolean> o) {}
            @Override public void bindBidirectional(javafx.beans.property.Property<Boolean> arg0) {}
            @Override public boolean isBound() { return false; }
            @Override public void unbind() {}
            @Override public void unbindBidirectional(javafx.beans.property.Property<Boolean> arg0) {}
            @Override public Object getBean() { return null; }
            @Override public String getName() { return null; }
            @Override public void setValue(Boolean arg0) {}
        };

        testBooleanProperty(property, source);
    }
}
