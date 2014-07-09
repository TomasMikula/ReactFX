package org.reactfx;

import org.reactfx.util.Tuple2;

@Deprecated
public interface InterceptableBiEventStream<A, B>
extends InterceptableEventStream<Tuple2<A, B>>, BiEventStream<A, B> {}
