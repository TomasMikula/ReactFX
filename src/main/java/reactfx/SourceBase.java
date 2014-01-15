package reactfx;

import java.util.function.Consumer;

public abstract class SourceBase<T> implements Source<T> {

    private ListHelper<Consumer<T>> valueSubscribers = null;

    protected void emitValue(T value) {
        ListHelper.forEach(valueSubscribers, s -> s.accept(value));
    }

    protected void firstSubscriber() {
        // default implementation is empty
    }

    protected void noSubscribers() {
        // default implementation is empty
    }

    @Override
    public Subscription subscribe(Consumer<T> consumer) {
        valueSubscribers = ListHelper.add(valueSubscribers, consumer);
        if(ListHelper.size(valueSubscribers) == 1) {
            firstSubscriber();
        }

        return () -> {
            valueSubscribers = ListHelper.remove(valueSubscribers, consumer);
            if(ListHelper.isEmpty(valueSubscribers)) {
                noSubscribers();
            }
        };
    }
}
