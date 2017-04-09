package org.reactfx.util;

import java.util.function.Function;

/**
 * Its {@link #apply(Object)} is used to get the summary of a leaf while its {@link #reduce(Object, Object)}
 * is used to reduce two {@link org.reactfx.util.FingerTree.NonEmptyFingerTree}'s summaries into one.
 */
public interface ToSemigroup<T, U> extends Function<T, U>, Semigroup<U> {}