package org.reactfx;

import java.util.function.Consumer;

class LimitedInvocationSubscriber<T> implements Consumer<T> {
    private final Consumer<? super T> subscriber;
    private int remainingInvocations;
    private Subscription subscription = null;

    LimitedInvocationSubscriber(int limit, Consumer<? super T> subscriber) {
        if(limit <= 0) {
            throw new IllegalArgumentException(
                    "Number of invocations must be positive. Was " + limit);
        }

        this.subscriber = subscriber;
        this.remainingInvocations = limit;
    }

    Subscription subscribeTo(EventStream<? extends T> stream) {
        assert subscription == null;
        subscription = stream.subscribe(this);
        if(remainingInvocations == 0) {
            // If the stream emitted some events as part of the subscription
            // process, limit might have been reached.
            subscription.unsubscribe();
        }
        return subscription;
    }

    @Override
    public void accept(T t) {
        if(remainingInvocations == 0) {
            // Do nothing.
            // May happen when there were multiple events scheduled for this
            // subscriber before it unsubscribed, e.g. when releasing a paused
            // stream.
        } else {
            --remainingInvocations;
            if(remainingInvocations == 0) {
                if(subscription != null) {
                    subscription.unsubscribe();
                } else {
                    // subscription may be null if this subscriber was notified
                    // while subscribing to the stream. In that case,
                    // do nothing.
                }
            }
            subscriber.accept(t);
        }
    }
}
