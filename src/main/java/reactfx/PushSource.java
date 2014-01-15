package reactfx;


public class PushSource<T> extends SourceBase<T> implements Sink<T> {

    @Override
    public void push(T value) {
        emitValue(value);
    }
}
