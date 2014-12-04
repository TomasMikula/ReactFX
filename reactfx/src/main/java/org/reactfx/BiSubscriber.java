package org.reactfx;

import static org.reactfx.util.Tuples.*;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.reactfx.util.Tuple2;

@FunctionalInterface
public interface BiSubscriber<A, B> extends ErrorHandler {
    void onEvent(A a, B b);

    default <A1 extends A, B1 extends B>
    Subscriber<Tuple2<A1, B1>> toSubscriber() {
        return new Subscriber<Tuple2<A1, B1>>() {

            @Override
            public void onEvent(Tuple2<A1, B1> event) {
                BiSubscriber.this.onEvent(event._1, event._2);
            }

            @Override
            public void onError(Throwable error) {
                BiSubscriber.this.onError(error);
            }
        };
    }

    static <A, B> BiSubscriber<A, B> create(
            BiConsumer<? super A, ? super B> onEvent,
            Consumer<? super Throwable> onError) {

        return new BiSubscriber<A, B>() {

            @Override
            public void onEvent(A a, B b) {
                onEvent.accept(a, b);
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        };
    }

    static <A, B> BiSubscriber<A, B> fromErrorHandler(
            Consumer<? super Throwable> onError) {

        return new BiSubscriber<A, B>() {

            @Override
            public void onEvent(A a, B b) {
                // do nothing
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        };
    }

    static <A, B> BiSubscriber<A, B> fromSubscriber(
            Subscriber<? super Tuple2<A, B>> subscriber) {

        return new BiSubscriber<A, B>() {

            @Override
            public void onEvent(A a, B b) {
                subscriber.onEvent(t(a, b));
            }

            @Override
            public void onError(Throwable error) {
                subscriber.onError(error);
            }
        };
    }
}
