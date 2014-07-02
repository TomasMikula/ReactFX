package org.reactfx;

import org.reactfx.util.Either;

@Deprecated
public interface InterceptableEitherEventStream<L, R>
extends InterceptableEventStream<Either<L, R>>, EitherEventStream<L, R> {}
