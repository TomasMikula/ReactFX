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
interface AccessorListMethods<E> extends List<E> {

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
        return new ListIteratorImpl<>(this);
    }

    @Override
    default ListIterator<E> listIterator(int index) {
        return new ListIteratorImpl<>(this, index);
    }

    @Override
    default List<E> subList(int fromIndex, int toIndex) {
        return new SubList<>(this, fromIndex, toIndex);
    }

    @Override
    default Object[] toArray() {
        return toArray(new Object[0]);
    }

    @Override
    default <T> T[] toArray(T[] a) {
        return new ArrayList<E>(this).toArray(a); // screw it
    }
}

class ListIteratorImpl<E> implements ListIterator<E> {
    private final List<E> list;
    private int position;

    public ListIteratorImpl(List<E> list, int initialPosition) {
        this.list = list;
        this.position = initialPosition;
    }

    public ListIteratorImpl(List<E> list) {
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

class SubList<E> extends AbstractList<E> {
    private final List<E> list;
    private final int from;
    private final int to;

    public SubList(List<E> list, int from, int to) {
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
        checkIndex(index);
        return list.get(from + index);
    }

    @Override
    public E set(int index, E element) {
        checkIndex(index);
        return list.set(from + index, element);
    }

    @Override
    public void add(int index, E element) {
        if(index < 0 || index > size()) {
            throw new IndexOutOfBoundsException(Integer.toString(index));
        }

        list.add(from + index, element);
    }

    @Override
    public E remove(int index) {
        checkIndex(index);
        return list.remove(from + index);
    }

    private void checkIndex(int index) {
        if(index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException(Integer.toString(index));
        }
    }
}