package org.reactfx.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import org.junit.Test;

public class FingerTreeTest {

    /**
     * Returns a random int, with higher probability for larger numbers.
     */
    private static int progressiveInt(Random rnd, int bound) {
        double d = rnd.nextDouble();
        d = d*d*d;
        int i = (int) Math.floor(d * bound);
        return bound - 1 - i;
    }

    @Test
    public void testSubList() {
        final int n = 50000;

        Integer[] arr = new Integer[n];
        for(int i=0; i<n; ++i) arr[i] = i;

        List<Integer> list = Arrays.asList(arr);
        List<Integer> treeList = FingerTree.mkTree(list).asList();
        assertEquals(list, treeList);

        Random rnd = new Random(12345);
        while(list.size() > 0) {
            int len = progressiveInt(rnd, list.size() + 1);
            int offset = rnd.nextInt(list.size() - len + 1);
            list = list.subList(offset, offset + len);
            treeList = treeList.subList(offset, offset + len);
            assertEquals(list, treeList);
        }
    }

    @Test
    public void testIteration() {
        final int n = 50000;
        final int from = 10000;
        final int to = 40000;

        Integer[] arr = new Integer[n];
        for(int i=0; i<n; ++i) arr[i] = i;

        List<Integer> list = Arrays.asList(arr);
        List<Integer> treeList = FingerTree.mkTree(list).asList();

        list = list.subList(from, to);
        treeList = treeList.subList(from, to);

        ListIterator<Integer> it = treeList.listIterator();

        List<Integer> fwRes = new ArrayList<>(treeList.size());
        while(it.hasNext()) fwRes.add(it.next());
        assertEquals(list, fwRes);

        List<Integer> bwRes = new ArrayList<>(treeList.size());
        while(it.hasPrevious()) bwRes.add(it.previous());
        Collections.reverse(bwRes);
        assertEquals(list, bwRes);
    }

}
