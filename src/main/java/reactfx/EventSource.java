package reactfx;

/**
 * EventSource is a Sink that serves also as an EventStream - every value
 * pushed to EventSource is immediately emitted by it.
 * @param <T> type of values this EventSource accepts and emits.
 */
public class EventSource<T> extends EventStreamBase<T> implements Sink<T> {

    /**
     * Make this event stream immediately emit the given value.
     */
    @Override
    public void push(T value) {
        emit(value);
    }
}
