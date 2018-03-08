package com.tierconnect.riot.iot.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by achambi on 8/29/17.
 * Class created to use operations on matrices
 */
public class MatrixUtils {

    /**
     * Get fists elements in a List of lists.
     * @param matrix the {@link List} of {@link LinkedList} instance.
     * @return a List with THe unique Parents
     */
    public static Set<Long> getFirstElements(List<LinkedList<Long>> matrix) {
        Set<Long> fistElements = new TreeSet<>();
        for (List<Long> item : matrix) {
            if (!matrix.isEmpty()) {
                fistElements.add(item.get(0));
            }
        }
        return fistElements;
    }
}
