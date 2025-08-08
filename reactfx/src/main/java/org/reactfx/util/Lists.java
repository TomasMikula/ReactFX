package org.reactfx.util;

import static org.reactfx.util.Tuples.*;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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

    public static <E> Iterator<E> readOnlyIterator(Collection<? extends E> col) {
        return new Iterator<E>() {
            private final Iterator<? extends E> delegate = col.iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public E next() {
                return delegate.next();
            }
        };
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

    /**
     * Returns a list that is a concatenation of the given lists. The returned
     * list is a view of the underlying lists, without copying the elements.
     * The returned list is unmodifiable. Modifications to underlying lists
     * will be visible through the concatenation view.
     */
    public static <E> List<E> concatView(List<List<? extends E>> lists) {
        if(lists.isEmpty()) {
            return Collections.emptyList();
        } else {
            return ConcatView.create(lists);
        }
    }

    @SafeVarargs
    public static <E> List<E> concat(List<? extends E>... lists) {
        return concat(Arrays.asList(lists));
    }

    /**
     * Returns a list that is a concatenation of the given lists. The returned
     * list is a view of the underlying lists, without copying the elements.
     * As opposed to {@link #concatView(List)}, the underlying lists must not
     * be modified while the returned concatenation view is in use. On the other
     * hand, this method guarantees balanced nesting if some of the underlying
     * lists are already concatenations created by this method.
     */
    public static <E> List<E> concat(List<List<? extends E>> lists) {
        return ListConcatenation.create(lists);
    }

    public static int commonPrefixLength(List<?> l, List<?> m) {
        ListIterator<?> i = l.listIterator();
        ListIterator<?> j = m.listIterator();
        while(i.hasNext() && j.hasNext()) {
            if(!Objects.equals(i.next(), j.next())) {
                return i.nextIndex() - 1;
            }
        }
        return i.nextIndex();
    }

    public static int commonSuffixLength(List<?> l, List<?> m) {
        ListIterator<?> i = l.listIterator(l.size());
        ListIterator<?> j = m.listIterator(m.size());
        while(i.hasPrevious() && j.hasPrevious()) {
            if(!Objects.equals(i.previous(), j.previous())) {
                return l.size() - i.nextIndex() - 1;
            }
        }
        return l.size() - i.nextIndex();
    }

    /**
     * Returns the lengths of common prefix and common suffix of two lists.
     * The total of the two lengths returned is not greater than either of
     * the list sizes. Prefix is prioritized: for lists [a, b, a, b], [a, b]
     * returns (2, 0); although the two lists have a common suffix of length 2,
     * the length of the second list is already included in the length of the
     * common prefix.
     */
    public static Tuple2<Integer, Integer> commonPrefixSuffixLengths(List<?> l1, List<?> l2) {
        int n1 = l1.size();
        int n2 = l2.size();

        if(n1 == 0 || n2 == 0) {
            return t(0, 0);
        }

        int pref = commonPrefixLength(l1, l2);
        if(pref == n1 || pref == n2) {
            return t(pref, 0);
        }

        int suff = commonSuffixLength(l1, l2);

        return t(pref, suff);
    }
}

class ConcatView<E> extends AbstractList<E> {

    static <E> List<E> create(List<List<? extends E>> lists) {
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
            return new ConcatView<>(
                    concatView(lists.subList(0, mid), false),
                    concatView(lists.subList(mid, len), false));
        }
    }

    private final List<? extends E> first;
    private final List<? extends E> second;

    ConcatView(List<? extends E> first, List<? extends E> second) {
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

class ListConcatenation<E> extends AbstractList<E> {

    private static final ToSemigroup<List<?>, Integer> LIST_SIZE_MONOID =
            new ToSemigroup<List<?>, Integer>() {

                @Override
                public Integer apply(List<?> t) {
                    return t.size();
                }

                @Override
                public Integer reduce(Integer left, Integer right) {
                    return left + right;
                }

            };

    static <E> List<E> create(List<List<? extends E>> lists) {
        return lists.stream()
            .filter(l -> !l.isEmpty())
            .map(l -> {
                @SuppressWarnings("unchecked") // cast safe because l is unmodifiable
                List<E> lst = (List<E>) l;
                return lst instanceof ListConcatenation
                    ? ((ListConcatenation<E>) lst).ft
                    : FingerTree.mkTree(Collections.singletonList(lst), LIST_SIZE_MONOID);
            })
            .reduce(FingerTree::join)
            .<List<E>>map(ListConcatenation<E>::new)
            .orElse(Collections.emptyList());
    }

    private final FingerTree<List<E>, Integer> ft;

    ListConcatenation(FingerTree<List<E>, Integer> ft) {
        this.ft = ft;
    }

    @Override
    public E get(int index) {
        return ft.get(Integer::intValue, index, List::get);
    }

    @Override
    public int size() {
        return ft.getSummary(0);
    }

    @Override
    public List<E> subList(int from, int to) {
        Lists.checkRange(from, to, size());
        return trim(to).drop(from);
    }

    private ListConcatenation<E> trim(int limit) {
        return ft.caseEmpty().<ListConcatenation<E>>unify(
                emptyTree -> this,
                neTree -> neTree.split(Integer::intValue, limit).map((l, m, r) -> {
                    FingerTree<List<E>, Integer> t =
                            m.map((lst, i) -> i == 0 ? l : l.append(lst.subList(0, i)));
                    return new ListConcatenation<>(t);
                }));
    }

    private ListConcatenation<E> drop(int n) {
        return ft.caseEmpty().<ListConcatenation<E>>unify(
                emptyTree -> this,
                neTree -> neTree.split(Integer::intValue, n).map((l, m, r) -> {
                    FingerTree<List<E>, Integer> t =
                            m.map((lst, i) -> i == lst.size() ? r : r.prepend(lst.subList(i, lst.size())));
                    return new ListConcatenation<>(t);
                }));
    }
}