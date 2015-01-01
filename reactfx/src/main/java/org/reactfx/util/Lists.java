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