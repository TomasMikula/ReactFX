package org.reactfx.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Immutable singly-linked list.
 */
public abstract class LL<T> implements Iterable<T> {

    private static class Nil<T> extends LL<T> {
        private static final Nil<?> INSTANCE = new Nil<Void>();

        @SuppressWarnings("unchecked")
        static <T> Nil<T> instance() { return (Nil<T>) INSTANCE; }

        @Override public boolean isEmpty() { return true; }
        @Override public int size() { return 0; }
        @Override public T head() { throw new NoSuchElementException(); }
        @Override public LL<T> tail() { throw new NoSuchElementException(); }
        @Override public Iterator<T> iterator() { return Collections.emptyIterator(); }
    }

    public static final class Cons<T> extends LL<T> {
        private final T head;
        private final LL<? extends T> tail;
        private final int size;

        private Cons(T head, LL<? extends T> tail) {
            this.head = head;
            this.tail = tail;
            this.size = 1 + tail.size();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public T head() {
            return head;
        }

        @Override
        public LL<? extends T> tail() {
            return tail;
        }

        @Override
        public final Iterator<T> iterator() {
            return new Iterator<T>() {
                private LL<? extends T> l = Cons.this;

                @Override
                public boolean hasNext() {
                    return !l.isEmpty();
                }

                @Override
                public T next() {
                    T res = l.head();
                    l = l.tail();
                    return res;
                }
            };
        }
    }

    public static <T> LL<T> nil() {
        return Nil.instance();
    }

    public static <T> LL<T> cons(T head, LL<? extends T> tail) {
        return new Cons<>(head, tail);
    }

    @SafeVarargs
    public static <T> LL<T> of(T... elems) {
        return of(elems, elems.length, LL.<T>nil());
    }

    private static <T> LL<T> of(T[] elems, int to, LL<T> tail) {
        if(to == 0) {
            return tail;
        } else {
            return of(elems, to - 1, cons(elems[to-1], tail));
        }
    }

    // private constructor to prevent subclassing
    private LL() {}

    public abstract boolean isEmpty();
    public abstract int size();
    public abstract T head();
    public abstract LL<? extends T> tail();

    public Stream<T> stream() {
        Spliterator<T> spliterator = new Spliterator<T>() {
            private final Iterator<T> iterator = iterator();

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                if(iterator.hasNext()) {
                    action.accept(iterator.next());
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public Spliterator<T> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return size();
            }

            @Override
            public int characteristics() {
                return Spliterator.IMMUTABLE | Spliterator.SIZED;
            }
        };

        return StreamSupport.stream(spliterator, false);
    }
}