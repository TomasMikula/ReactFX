package org.reactfx.util;

import java.util.function.Function;

public interface MapToMonoid<T, U> extends Function<T, U>, Monoid<U> {}