package org.reactfx.collection;

import java.util.Collection;

/**
 * Trait to be mixed into implementations of unmodifiable lists.
 * Provides default implementations of mutating list methods.
 */
public interface UnmodifiableByDefaultList<E> extends AccessorListMethods<E> {

    @Override
    default E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    default E remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean add(E e) {
        add(size(), e);
        return true;
    }

    @Override
    default boolean addAll(Collection<? extends E> c) {
        for(E e: c) add(e);
        return !c.isEmpty();
    }

    @Override
    default boolean addAll(int index, Collection<? extends E> c) {
        for(E e: c) add(index++, e);
        return !c.isEmpty();
    }

    @Override
    default boolean remove(Object o) {
        int i = indexOf(o);
        if(i != -1) {
            remove(i);
            return true;
        } else {
            return false;
        }
    }

    @Override
    default boolean removeAll(Collection<?> c) {
        return c.stream().anyMatch(this::remove);
    }

    @Override
    default boolean retainAll(Collection<?> c) {
        boolean changed = false;
        for(int i = size() - 1; i >= 0; --i) {
            if(!c.contains(get(i))) {
                remove(i);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    default void clear() {
        for(int i = size() - 1; i >= 0; --i) {
            remove(i);
        }
    }
}