package reactfx;

import javafx.beans.value.ObservableValue;

/**
 * Observable value that reflects the latest event emitted by an event stream.
 * @param <T>
 */
public interface StreamBoundValue<T> extends ObservableValue<T>, Subscription {

    /**
     * Unsubscribe from the event stream. After unsubscribing, no more value
     * changes happen. This method should be called when this value is not
     * going to be used any longer, to prevent leaks.
     */
    @Override
    void unsubscribe();
}
