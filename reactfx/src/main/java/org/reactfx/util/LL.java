package org.reactfx.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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
        @Override public <U> LL<U> map(Function<? super T, ? extends U> f) { return instance(); }
        @Override public Iterator<T> iterator() { return Collections.emptyIterator(); }

        @Override
        public <R> R fold(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction) {
            return acc;
        }

        @Override
        public <R> Optional<R> mapReduce(
                Function<? super T, ? extends R> map,
                BinaryOperator<R> reduce) {
            return Optional.empty();
        }
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
        public <U> Cons<U> map(Function<? super T, ? extends U> f) {
            return cons(f.apply(head), tail.map(f));
        }

        @Override
        public <R> R fold(
                R acc,
                BiFunction<? super R, ? super T, ? extends R> reduction) {
            return tail.fold(reduction.apply(acc, head), reduction);
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

        @Override
        public <R> Optional<R> mapReduce(
                Function<? super T, ? extends R> map,
                BinaryOperator<R> reduce) {
            return Optional.of(mapReduce1(map, reduce));
        }

        public <R> R mapReduce1(
                Function<? super T, ? extends R> map,
                BinaryOperator<R> reduce) {
            R acc = map.apply(head);
            return tail.fold(acc, (r, t) -> reduce.apply(r, map.apply(t)));
        }
    }

    public static <T> LL<T> nil() {
        return Nil.instance();
    }

    public static <T> Cons<T> cons(T head, LL<? extends T> tail) {
        return new Cons<>(head, tail);
    }

    @SafeVarargs
    public static <T> Cons<T> of(T head, T... tail) {
        return cons(head, of(tail, tail.length, LL.<T>nil()));
    }

    private static <T> LL<T> of(T[] elems, int to, LL<T> tail) {
        if(to == 0) {
            return tail;
        } else {
            return of(elems, to - 1, cons(elems[to-1], tail));
        }
    }

    public static <T> LL<? extends T> concat(LL<? extends T> l1, LL<? extends T> l2) {
        if(l1.isEmpty()) {
            return l2;
        } else {
            return cons(l1.head(), concat(l1.tail(), l2));
        }
    }

    // private constructor to prevent subclassing
    private LL() {}

    public abstract boolean isEmpty();
    public abstract int size();
    public abstract T head();
    public abstract LL<? extends T> tail();
    public abstract <U> LL<U> map(Function<? super T, ? extends U> f);
    public abstract <R> R fold(R acc, BiFunction<? super R, ? super T, ? extends R> reduction);
    public abstract <R> Optional<R> mapReduce(
            Function<? super T, ? extends R> map,
            BinaryOperator<R> reduce);

    public boolean all(Predicate<T> cond) {
        return fold(true, (b, t) -> b && cond.test(t));
    }

    public <U> U with2(BiFunction<? super T, ? super T, ? extends U> f) {
        return f.apply(head(), tail().head());
    }

    public <U> U with3(TriFunction<? super T, ? super T, ? super T, ? extends U> f) {
        return tail().with2(f.pApply(head()));
    }

    public <U> U with4(TetraFunction<? super T, ? super T, ? super T, ? super T, ? extends U> f) {
        return tail().with3(f.pApply(head()));
    }

    public <U> U with5(PentaFunction<? super T, ? super T, ? super T, ? super T, ? super T, ? extends U> f) {
        return tail().with4(f.pApply(head()));
    }

    public <U> U with6(HexaFunction<? super T, ? super T, ? super T, ? super T, ? super T, ? super T, ? extends U> f) {
        return tail().with5(f.pApply(head()));
    }

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