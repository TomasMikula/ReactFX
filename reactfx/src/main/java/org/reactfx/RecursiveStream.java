package org.reactfx;


class RecursiveStream<T> extends LazilyBoundStream<T> {
    private final EventStream<T> input;

    public RecursiveStream(
            EventStream<T> input,
            EmptyPendingNotifications<Subscriber<? super T>, T> pn) {
        super(pn);
        this.input = input;
    }

    @Override
    protected Subscription subscribeToInputs() {
        return subscribeTo(input, this::emit);
    }
}