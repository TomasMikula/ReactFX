package org.reactfx;

import org.reactfx.util.Tuple2;

public interface BiConnectable<A, B> extends Connectable<Tuple2<A, B>> {

    Subscription connectToBi(BiEventStream<? extends A, ? extends B> source);
}
