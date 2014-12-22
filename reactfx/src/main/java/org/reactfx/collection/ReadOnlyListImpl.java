package org.reactfx.collection;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Plain boilerplate, because java.util.List does not have default methods.
 */
interface ReadOnlyListImpl<E> extends List<E> {

    @Override
    default boolean isEmpty() {
        return size() != 0;
    }

    @Override
    default int indexOf(Object o) {
        for(int i = 0; i < size(); ++i) {
            if(Objects.equals(o, get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    default int lastIndexOf(Object o) {
        for(int i = size() - 1; i >= 0; ++i) {
            if(Objects.equals(o, get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    default boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    @Override
    default boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(this::contains);
    }

    @Override
    default Iterator<E> iterator() {
        return listIterator();
    }

    @Override
    default ListIterator<E> listIterator() {
        return new ReadOnlyListIterator<>(this);
    }

    @Override
    default ListIterator<E> listIterator(int index) {
        return new ReadOnlyListIterator<>(this, index);
    }

    @Override
    default List<E> subList(int fromIndex, int toIndex) {
        return new ReadOnlySubList<>(this, fromIndex, toIndex);
    }

    @Override
    default Object[] toArray() {
        return toArray(new Object[0]);
    }

    @Override
    default <T> T[] toArray(T[] a) {
        return new ArrayList<E>(this).toArray(a); // screw it
    }

    @Override
    default void add(int index, E element) {
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
    default E remove(int index) {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    @Override
    default E set(int index, E element) {
        throw new UnsupportedOperationException();
    }
}

class ReadOnlyListIterator<E> implements ListIterator<E> {
    private final List<E> list;
    private int position;

    public ReadOnlyListIterator(List<E> list, int initialPosition) {
        this.list = list;
        this.position = initialPosition;
    }

    public ReadOnlyListIterator(List<E> list) {
        this(list, 0);
    }

    @Override
    public boolean hasNext() {
        return position < list.size();
    }

    @Override
    public E next() {
        if(position < list.size()) {
            return list.get(position++);
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public boolean hasPrevious() {
        return position > 0;
    }

    @Override
    public E previous() {
        if(position > 0) {
            return list.get(--position);
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public int nextIndex() {
        return position;
    }

    @Override
    public int previousIndex() {
        return position - 1;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(E e) {
        throw new UnsupportedOperationException();
    }
}

class ReadOnlySubList<E> extends AbstractList<E> {
    private final List<E> list;
    private final int from;
    private final int to;

    public ReadOnlySubList(List<E> list, int from, int to) {
        if(from < 0 || from > to || to > list.size()) {
            throw new IndexOutOfBoundsException("0 <= " + from + " <= " + to + " <= " + list.size());
        }

        this.list = list;
        this.from = from;
        this.to = to;
    }

    @Override
    public int size() {
        return to - from;
    }

    @Override
    public E get(int index) {
        if(index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException(Integer.toString(index));
        }

        return list.get(from + index);
    }
}