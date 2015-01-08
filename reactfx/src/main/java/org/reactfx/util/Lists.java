package org.reactfx.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class Lists {

    // private constructor to prevent instantiation
    private Lists() {}

    public static <E> int hashCode(List<E> list) {
        int hashCode = 1;
        for(E e: list) {
            hashCode = 31 * hashCode + Objects.hashCode(e);
        }
        return hashCode;
    }

    public static boolean equals(List<?> list, Object o) {
        if(o == list) return true;

        if(!(o instanceof List)) return false;

        List<?> that = (List<?>) o;

        if(list.size() != that.size()) return false;

        Iterator<?> it1 = list.iterator();
        Iterator<?> it2 = that.iterator();
        while(it1.hasNext()) {
            if(!Objects.equals(it1.next(), it2.next())) {
                return false;
            }
        }

        return true;
    }

    public static String toString(List<?> list) {
        StringBuilder res = new StringBuilder();
        res.append('[');
        Iterator<?> it = list.iterator();
        while(it.hasNext()) {
            res.append(it.next());
            if(it.hasNext()) {
                res.append(", ");
            }
        }
        res.append(']');
        return res.toString();
    }

    public static boolean isValidIndex(int index, int size) {
        return isValidIndex(0, index, size);
    }

    public static boolean isValidIndex(int min, int index, int max) {
        return min <= index && index < max;
    }

    public static void checkIndex(int index, int size) {
        checkIndex(0, index, size);
    }

    public static void checkIndex(int min, int index, int max) {
        if(!isValidIndex(min, index, max)) {
            throw new IndexOutOfBoundsException(index + " not in [" + min + ", " + max + ")");
        }
    }

    public static boolean isValidPosition(int position, int size) {
        return isValidPosition(0, position, size);
    }

    public static boolean isValidPosition(int min, int position, int max) {
        return min <= position && position <= max;
    }

    public static void checkPosition(int position, int size) {
        checkPosition(0, position, size);
    }

    public static void checkPosition(int min, int position, int max) {
        if(!isValidPosition(min, position, max)) {
            throw new IndexOutOfBoundsException(position + " not in [" + min + ", " + max + "]");
        }
    }

    public static boolean isValidRange(int from, int to, int size) {
        return isValidRange(0, from, to, size);
    }

    public static boolean isValidRange(int min, int from, int to, int max) {
        return min <= from && from <= to && to <= max;
    }

    public static void checkRange(int from, int to, int size) {
        checkRange(0, from, to, size);
    }

    public static void checkRange(int min, int from, int to, int max) {
        if(!isValidRange(min, from, to, max)) {
            throw new IndexOutOfBoundsException(
                    "[" + from + ", " + to + ") is not a valid range within " +
                    "[" + min + ", " + max + ")");
        }
    }

    public static boolean isNonEmptyRange(int from, int to, int size) {
        return isNonEmptyRange(0, from, to, size);
    }

    public static boolean isNonEmptyRange(int min, int from, int to, int max) {
        return min <= from && from < to && to <= max;
    }

    public static boolean isProperRange(int from, int to, int size) {
        return isProperRange(0, from, to, size);
    }

    public static boolean isProperRange(int min, int from, int to, int max) {
        return isValidRange(min, from, to, max) && (min < from || to < max);
    }

    public static boolean isProperNonEmptyRange(int from, int to, int size) {
        return isProperNonEmptyRange(0, from, to, size);
    }

    public static boolean isProperNonEmptyRange(int min, int from, int to, int max) {
        return isNonEmptyRange(min, from, to, max) && (min < from || to < max);
    }

    public static boolean isStrictlyInsideRange(int from, int to, int size) {
        return isStrictlyInsideRange(0, from, to, size);
    }

    public static boolean isStrictlyInsideRange(int min, int from, int to, int max) {
        return min < from && from <= to && to < max;
    }

    public static boolean isStrictlyInsideNonEmptyRange(int from, int to, int size) {
        return isStrictlyInsideNonEmptyRange(0, from, to, size);
    }

    public static boolean isStrictlyInsideNonEmptyRange(int min, int from, int to, int max) {
        return min < from && from < to && to < max;
    }

    public static <E, F> List<F> mappedView(
            List<? extends E> source,
            Function<? super E, ? extends F> f) {
        return new AbstractList<F>() {

            @Override
            public F get(int index) {
                return f.apply(source.get(index));
            }

            @Override
            public int size() {
                return source.size();
            }
        };
    }

    @SafeVarargs
    public static <E> List<E> concatView(List<? extends E>... lists) {
        return concatView(Arrays.asList(lists));
    }

    public static <E> List<E> concatView(List<List<? extends E>> lists) {
        List<List<? extends E>> lsts = new ArrayList<>(lists);
        lsts.removeIf(List::isEmpty);

        if(lsts.isEmpty()) {
            return Collections.emptyList();
        } else {
            return ListsHelper.concatView(lsts);
        }
    }
}

class ListsHelper {

    static <E> List<E> concatView(List<List<? extends E>> lists) {
        return concatView(lists, true);
    }

    private static <E> List<E> concatView(
            List<List<? extends E>> lists, boolean makeUnmodifiable) {
        int len = lists.size();
        if(len < 1) {
            throw new AssertionError("Supposedly unreachable code");
        } else if(len == 1) {
            List<? extends E> list = lists.get(0);
            if(makeUnmodifiable) {
                return Collections.unmodifiableList(list);
            } else {
                @SuppressWarnings("unchecked")
                List<E> lst = (List<E>) list;
                return lst;
            }
        } else {
            int mid = len / 2;
            return new ConcatListView<>(
                    concatView(lists.subList(0, mid), false),
                    concatView(lists.subList(mid, len), false));
        }
    }

    private static class ConcatListView<E> extends AbstractList<E> {
        private final List<? extends E> first;
        private final List<? extends E> second;

        ConcatListView(List<? extends E> first, List<? extends E> second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public E get(int index) {
            if(index < first.size()) {
                return first.get(index);
            } else {
                return second.get(index - first.size());
            }
        }

        @Override
        public int size() {
            return first.size() + second.size();
        }

    }
}