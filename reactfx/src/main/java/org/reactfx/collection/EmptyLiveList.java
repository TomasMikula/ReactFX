package org.reactfx.collection;

public class EmptyLiveList<E>
implements LiveList<E>, ReadOnlyLiveListImpl<E> {

    private static final EmptyLiveList<Object> INSTANCE = new EmptyLiveList<>();

    @SuppressWarnings("unchecked")
    public static <E> LiveList<E> getInstance() {
        return (LiveList<E>) INSTANCE;
    }

    public EmptyLiveList() {
    }

    @Override
    public void addObserver(
        Observer<? super E, ?> observer) {
        // no-op
    }

    @Override
    public void removeObserver(
        Observer<? super E, ?> observer) {
        // no-op
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public E get(
        int index) {
        throw new IndexOutOfBoundsException("Index: " + index);
    }
}
