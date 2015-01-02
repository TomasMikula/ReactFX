package org.reactfx.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public abstract class ListHelper<T> {

    public static <T> T get(ListHelper<T> listHelper, int index) {
        if(listHelper == null) {
            throw new IndexOutOfBoundsException(index + " not in [0, 0)");
        } else {
            return listHelper.get(index);
        }
    }

    public static <T> ListHelper<T> add(ListHelper<T> listHelper, T elem) {
        if(listHelper == null) {
            return new SingleElemHelper<>(elem);
        } else {
            return listHelper.add(elem);
        }
    }

    public static <T> ListHelper<T> remove(ListHelper<T> listHelper, T elem) {
        if(listHelper == null) {
            return listHelper;
        } else {
            return listHelper.remove(elem);
        }
    }

    public static <T> void forEach(ListHelper<T> listHelper, Consumer<T> f) {
        if(listHelper != null) {
            listHelper.forEach(f);
        }
    }

    public static <T> Iterator<T> iterator(ListHelper<T> listHelper) {
        if(listHelper != null) {
            return listHelper.iterator();
        } else {
            return Collections.emptyIterator();
        }
    }

    public static <T> Optional<T> reduce(ListHelper<T> listHelper, BinaryOperator<T> f) {
        if(listHelper == null) {
            return Optional.empty();
        } else {
            return listHelper.reduce(f);
        }
    }

    public static <T, U> U reduce(ListHelper<T> listHelper, U unit, BiFunction<U, T, U> f) {
        if(listHelper == null) {
            return unit;
        } else {
            return listHelper.reduce(unit, f);
        }
    }

    public static <T> T[] toArray(ListHelper<T> listHelper, IntFunction<T[]> allocator) {
        if(listHelper == null) {
            return allocator.apply(0);
        } else {
            return listHelper.toArray(allocator);
        }
    }

    public static <T> boolean isEmpty(ListHelper<T> listHelper) {
        return listHelper == null;
    }

    public static <T> int size(ListHelper<T> listHelper) {
        if(listHelper == null) {
            return 0;
        } else {
            return listHelper.size();
        }
    }

    private ListHelper() {
        // private constructor to prevent subclassing
    };

    abstract T get(int index);
    abstract ListHelper<T> add(T elem);
    abstract ListHelper<T> remove(T elem);
    abstract void forEach(Consumer<T> f);
    abstract Iterator<T> iterator();
    abstract Optional<T> reduce(BinaryOperator<T> f);
    abstract <U> U reduce(U unit, BiFunction<U, T, U> f);
    abstract T[] toArray(IntFunction<T[]> allocator);
    abstract int size();


    private static class SingleElemHelper<T> extends ListHelper<T> {
        private final T elem;

        SingleElemHelper(T elem) {
            this.elem = elem;
        }

        @Override
        T get(int index) {
            if(index == 0) {
                return elem;
            } else {
                throw new IndexOutOfBoundsException(index + " not in [0, 1)");
            }
        }

        @Override
        ListHelper<T> add(T elem) {
            return new MultiElemHelper<>(this.elem, elem);
        }

        @Override
        ListHelper<T> remove(T elem) {
            if(Objects.equals(this.elem, elem)) {
                return null;
            } else {
                return this;
            }
        }

        @Override
        void forEach(Consumer<T> f) {
            f.accept(elem);
        }

        @Override
        Iterator<T> iterator() {
            return new Iterator<T>() {
                boolean hasNext = true;

                @Override
                public boolean hasNext() {
                    return hasNext;
                }

                @Override
                public T next() {
                    if(hasNext) {
                        hasNext = false;
                        return elem;
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            };
        }

        @Override
        Optional<T> reduce(BinaryOperator<T> f) {
            return Optional.of(elem);
        }

        @Override
        <U> U reduce(U unit, BiFunction<U, T, U> f) {
            return f.apply(unit, elem);
        }

        @Override
        T[] toArray(IntFunction<T[]> allocator) {
            T[] res = allocator.apply(1);
            res[0] = elem;
            return res;
        }

        @Override
        int size() {
            return 1;
        }
    }

    private static class MultiElemHelper<T> extends ListHelper<T> {
        private final List<T> elems;

        // when > 0, this ListHelper must be immutable,
        // i.e. use copy-on-write for mutating operations
        private int iterating = 0;

        @SafeVarargs
        MultiElemHelper(T... elems) {
            this(Arrays.asList(elems));
        }

        private MultiElemHelper(List<T> elems) {
            this.elems = new ArrayList<>(elems);
        }

        private MultiElemHelper<T> copy() {
            return new MultiElemHelper<>(elems);
        }

        @Override
        T get(int index) {
            return elems.get(index);
        }

        @Override
        ListHelper<T> add(T elem) {
            if(iterating > 0) {
                return copy().add(elem);
            } else {
                elems.add(elem);
                return this;
            }
        }

        @Override
        ListHelper<T> remove(T elem) {
            int idx = elems.indexOf(elem);
            if(idx == -1) {
                return this;
            } else {
                switch(elems.size()) {
                case 0: // fall through
                case 1: throw new AssertionError();
                case 2: return new SingleElemHelper<>(elems.get(1-idx));
                default:
                    if(iterating > 0) {
                        return copy().remove(elem);
                    } else {
                        elems.remove(elem);
                        return this;
                    }
                }
            }
        }

        @Override
        void forEach(Consumer<T> f) {
            ++iterating;
            try {
                for(T elem: elems) {
                    f.accept(elem);
                }
            } finally {
                --iterating;
            }
        }

        @Override
        Iterator<T> iterator() {
            ++iterating;
            return new Iterator<T>() {
                int next = 0;

                @Override
                public boolean hasNext() {
                    return next < elems.size();
                }

                @Override
                public T next() {
                    if(next < elems.size()) {
                        T res = elems.get(next);
                        ++next;
                        if(next == elems.size()) {
                            --iterating;
                        }
                        return res;
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            };
        }

        @Override
        Optional<T> reduce(BinaryOperator<T> f) {
            return elems.stream().reduce(f);
        }

        @Override
        <U> U reduce(U unit, BiFunction<U, T, U> f) {
            U u = unit;
            for(T elem: elems) {
                u = f.apply(u, elem);
            }
            return u;
        }

        @Override
        T[] toArray(IntFunction<T[]> allocator) {
            return elems.toArray(allocator.apply(size()));
        }

        @Override
        int size() {
            return elems.size();
        }
    }
}
