package org.reactfx;

import static org.junit.Assert.*;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.Event;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.shape.Rectangle;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by jordan on 11/23/15.
 */
public class PrependEventStreamTest {

    @Before
    public void setUp() {
        new JFXPanel();
    }

    @Test
    public void test() {
        Platform.runLater(() -> {
            EventCounter counter = new EventCounter();
            final Rectangle rectangle = new Rectangle();
            EventStream<Boolean> controlPressed = EventStreams
                    .eventsOf(rectangle, KeyEvent.KEY_PRESSED)
                    .filter(key -> key.getCode().equals(KeyCode.CONTROL))
                    .map(KeyEvent::isControlDown)
                    .prepend(false);
            controlPressed.subscribe(counter::accept);

            assertEquals(1, counter.get());
        });
    }
}
