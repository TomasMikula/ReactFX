package org.reactfx;

import org.reactfx.util.Either;

public interface InterceptableEitherEventStream<L, R>
extends InterceptableEventStream<Either<L, R>>, EitherEventStream<L, R> {}
