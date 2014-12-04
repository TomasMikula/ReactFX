package org.reactfx;

import static org.reactfx.util.Tuples.*;

import java.util.function.Consumer;

import org.reactfx.util.TriConsumer;
import org.reactfx.util.Tuple3;

@FunctionalInterface
public interface TriSubscriber<A, B, C> extends ErrorHandler {
    void onEvent(A a, B b, C c);

    default <A1 extends A, B1 extends B, C1 extends C>
    Subscriber<Tuple3<A1, B1, C1>> toSubscriber() {
        return new Subscriber<Tuple3<A1, B1, C1>>() {

            @Override
            public void onEvent(Tuple3<A1, B1, C1> event) {
                TriSubscriber.this.onEvent(event._1, event._2, event._3);
            }

            @Override
            public void onError(Throwable error) {
                TriSubscriber.this.onError(error);
            }
        };
    }

    static <A, B, C> TriSubscriber<A, B, C> create(
            TriConsumer<? super A, ? super B, ? super C> onEvent,
            Consumer<? super Throwable> onError) {

        return new TriSubscriber<A, B, C>() {

            @Override
            public void onEvent(A a, B b, C c) {
                onEvent.accept(a, b, c);
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        };
    }

    static <A, B, C> TriSubscriber<A, B, C> fromErrorHandler(
            Consumer<? super Throwable> onError) {

        return new TriSubscriber<A, B, C>() {

            @Override
            public void onEvent(A a, B b, C c) {
                // do nothing
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        };
    }

    static <A, B, C> TriSubscriber<A, B, C> fromSubscriber(
            Subscriber<? super Tuple3<A, B, C>> subscriber) {

        return new TriSubscriber<A, B, C>() {

            @Override
            public void onEvent(A a, B b, C c) {
                subscriber.onEvent(t(a, b, c));
            }

            @Override
            public void onError(Throwable error) {
                subscriber.onError(error);
            }
        };
    }
}
