package org.reactfx.util;

import java.util.function.Function;

public interface ToSemigroup<T, U> extends Function<T, U>, Semigroup<U> {}