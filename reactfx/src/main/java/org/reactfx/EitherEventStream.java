package org.reactfx;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.application.Platform;

import org.reactfx.util.Either;

/**
 * @deprecated Since 1.2.1. The utility of {@code EitherEventStream<L, R>} over
 * just {@code EventStream<Either<L, R>>} is questionable and probably not worth
 * maintaining a special implementation. Let us know if you use it and your code
 * would get much less readable without it.
 */
@Deprecated
public interface EitherEventStream<L, R> extends EventStream<Either<L, R>> {

    default Subscription subscribe(
            Consumer<? super L> leftSubscriber,
            Consumer<? super R> rightSubscriber) {
        return subscribe(either -> {
            either.ifLeft(leftSubscriber);
            either.ifRight(rightSubscriber);
        });
    }

    default Subscription watch(
            Consumer<? super L> leftSubscriber,
            Consumer<? super R> rightSubscriber,
            Consumer<? super Throwable> monitor) {
        return subscribe(leftSubscriber, rightSubscriber).and(monitor(monitor));
    }

    default EventStream<L> left() {
        return filterMap(Either::isLeft, Either::getLeft);
    }

    default EventStream<R> right() {
        return filterMap(Either::isRight, Either::getRight);
    }

    @Override
    default EitherEventStream<L, R> hook(
            Consumer<? super Either<L, R>> sideEffect) {
        return new SideEffectEitherStream<>(this, sideEffect);
    }

    default EitherEventStream<L, R> hook(
            Consumer<? super L> leftSideEffect,
            Consumer<? super R> rightSideEffect) {
        return hook(either -> either.exec(leftSideEffect, rightSideEffect));
    }

    @Override
    default EitherEventStream<L, R> filter(
            Predicate<? super Either<L, R>> predicate) {
        return new FilterEitherStream<>(this, predicate);
    }

    @Override
    default EitherEventStream<L, R> distinct() {
        return new DistinctEitherStream<>(this);
    }

    default <L1, R1> EitherEventStream<L1, R1> map(
            Function<? super L, ? extends L1> leftMap,
            Function<? super R, ? extends R1> rightMap) {
        return split(either -> either.map(leftMap, rightMap));
    }

    default <L1> EitherEventStream<L1, R> mapLeft(
            Function<? super L, ? extends L1> f) {
        return split(either -> either.mapLeft(f));
    }

    default <R1> EitherEventStream<L, R1> mapRight(
            Function<? super R, ? extends R1> f) {
        return split(either -> either.mapRight(f));
    }

    default <L1, R1> EitherEventStream<L1, R1> split(
            Function<? super L, Either<L1, R1>> leftMap,
            Function<? super R, Either<L1, R1>> rightMap) {
        return split(either -> either.flatMap(leftMap, rightMap));
    }

    default <L1> EitherEventStream<L1, R> splitLeft(
            Function<? super L, Either<L1, R>> leftMap) {
        return split(either -> either.flatMapLeft(leftMap));
    }

    default <R1> EitherEventStream<L, R1> splitRight(
            Function<? super R, Either<L, R1>> rightMap) {
        return split(either -> either.flatMapRight(rightMap));
    }

    default <T> EventStream<T> unify(
            Function<? super L, ? extends T> leftMap,
            Function<? super R, ? extends T> rightMap) {
        return map(either -> either.unify(leftMap, rightMap));
    }

    @Override
    default EitherEventStream<L, R> emitOn(EventStream<?> impulse) {
        return new EmitOnEitherStream<>(this, impulse);
    }

    @Override
    default EitherEventStream<L, R> emitOnEach(EventStream<?> impulse) {
        return new EmitOnEachEitherStream<>(this, impulse);
    }

    @Override
    default EitherEventStream<L, R> repeatOn(EventStream<?> impulse) {
        return new RepeatOnEitherStream<>(this, impulse);
    }

    @Override
    default InterceptableEitherEventStream<L, R> interceptable() {
        if(this instanceof InterceptableEitherEventStream) {
            return (InterceptableEitherEventStream<L, R>) this;
        } else {
            return new InterceptableEitherEventStreamImpl<L, R>(this);
        }
    }

    @Override
    default EitherEventStream<L, R> threadBridge(
            Executor sourceThreadExecutor,
            Executor targetThreadExecutor) {
        return new EitherThreadBridge<L, R>(this, sourceThreadExecutor, targetThreadExecutor);
    }

    @Override
    default EitherEventStream<L, R> threadBridgeFromFx(Executor targetThreadExecutor) {
        return threadBridge(Platform::runLater, targetThreadExecutor);
    }

    @Override
    default EitherEventStream<L, R> threadBridgeToFx(Executor sourceThreadExecutor) {
        return threadBridge(sourceThreadExecutor, Platform::runLater);
    }

    @Override
    default EitherEventStream<L, R> guardedBy(Guardian... guardians) {
        return new GuardedEitherStream<>(this, guardians);
    }
}