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
        EventCounter counter = new EventCounter();
        EventSource<Boolean> stream = new EventSource<Boolean>();

        stream.prepend(true).subscribe(counter::accept);

        assertEquals(1, counter.get());
    }
}
