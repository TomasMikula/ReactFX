package inhibeans;

import static org.junit.Assert.*;

import org.junit.Test;

public class ImpulseTest {

    @Test
    public void test() {
        Impulse impulse = new Impulse();
        CountingListener counter = new CountingListener(impulse);

        impulse.trigger();
        impulse.trigger();

        assertEquals(2, counter.getAndReset());

        Block b = impulse.block();
        impulse.trigger();
        impulse.trigger();
        b.close();

        assertEquals(1, counter.get());
    }

}
