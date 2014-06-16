package org.reactfx;

import org.reactfx.util.Tuple3;

public interface TriConnectable<A, B, C> extends Connectable<Tuple3<A, B, C>> {

    Subscription connectToTri(TriEventStream<? extends A, ? extends B, ? extends C> source);
}
