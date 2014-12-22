package org.reactfx.util;

import java.util.AbstractList;
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
}
