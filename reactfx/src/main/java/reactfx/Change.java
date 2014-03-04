package reactfx;

/**
 * Represents a change of a value.
 * @param <T> type of the value that changed.
 */
public class Change<T> {
    private final T oldValue;
    private final T newValue;

    public Change(T oldValue, T newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public T getOldValue() {
        return oldValue;
    }

    public T getNewValue() {
        return newValue;
    }
}
