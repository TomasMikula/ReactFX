package reactfx;

public interface Sink<T> {
    void push(T value);
}
