package org.reactfx.value;

import java.util.function.Consumer;

import org.reactfx.ProperObservable;
import org.reactfx.util.NotificationAccumulator;

public interface ProperVal<T>
extends Val<T>, ProperObservable<Consumer<? super T>, T> {

    @Override
    default NotificationAccumulator<Consumer<? super T>, T, ?> defaultNotificationAccumulator() {
        return NotificationAccumulator.retainOldestValNotifications();
    }
}
