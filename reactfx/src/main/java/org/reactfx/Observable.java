package org.reactfx;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;

import org.reactfx.value.Val;

/**
 * Anything that can be <em>observed</em>, that is have an <em>observer</em>
 * added and removed. Each {@linkplain Observable} has one canonical observer
 * type, which can be added by the {@link #addObserver(Object)} method and
 * removed by the {@link #removeObserver(Object)} method, or added by the
 * {@link #observe(Object)} method and removed by the returned
 * {@linkplain Subscription}.
 *
 * In specific subtypes, methods for adding/removing canonical observers may
 * have aliases that are more descriptive for the specific type, for example,
 * in {@link EventStream}, {@linkplain #observe(Object)} is aliased as
 * {@linkplain EventStream#subscribe(java.util.function.Consumer)}, or in
 * {@link Val}, {@linkplain #addObserver(Object)} is aliased as
 * {@linkplain Val#addInvalidationObserver(java.util.function.Consumer)}.
 *
 * In addition to the canonical observer, subtypes may support adding other
 * observer types. These other observers will be wrapped to "look like" a
 * canonical observer. For example, the canonical observer for
 * {@code Val<T>} is an <em>invalidation observer</em>, which is of type
 * {@code Consumer<? super T>}, which accepts the invalidated value.
 * In addition to this canonical observer, {@linkplain Val} also supports
 * {@link InvalidationListener}s and {@link ChangeListener}s.
 *
 * @param <O> observer type accepted by this {@linkplain Observable}
 */
public interface Observable<O> {
    void addObserver(O observer);
    void removeObserver(O observer);

    default Subscription observe(O observer) {
        addObserver(observer);
        return () -> removeObserver(observer);
    }
}