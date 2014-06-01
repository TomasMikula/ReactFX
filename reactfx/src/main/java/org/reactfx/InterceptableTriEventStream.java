package org.reactfx;

import org.reactfx.util.Tuple3;

public interface InterceptableTriEventStream<A, B, C>
extends InterceptableEventStream<Tuple3<A, B, C>>, TriEventStream<A, B, C> {}
