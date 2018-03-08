package com.tierconnect.riot.iot.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.apache.commons.lang.StringUtils.join;
import static org.junit.Assert.*;

/**
 * Created by achambi on 7/24/17.
 * Class for test a direct graph.
 */
public class DirectedGraphTest {

    private LinkedHashMap<Long, List<String>> result;
    private DirectedGraph directedGraph;

    @Before
    public void setUp() throws Exception {
        result = new LinkedHashMap<>(10);
        result.put(0L, Arrays.asList("0,4,6,8", "0,4,6,9", "0,4,6,11", "0,4,6,13", "0,4,6,14", "0,4,7,10"));
        result.put(1L, Arrays.asList("1,5,6,8", "1,5,6,9", "1,5,6,11", "1,5,6,13", "1,5,6,14"));
        result.put(2L, Arrays.asList(
                "2,4,6,8",
                "2,4,6,9",
                "2,4,6,11",
                "2,4,6,13",
                "2,4,6,14",
                "2,4,7,10",
                "2,5,6,8",
                "2,5,6,9",
                "2,5,6,11",
                "2,5,6,13",
                "2,5,6,14"));
        result.put(3L, Arrays.asList(
                "3,4,6,8",
                "3,4,6,9",
                "3,4,6,11",
                "3,4,6,13",
                "3,4,6,14",
                "3,4,7,10",
                "3,5,6,8",
                "3,5,6,9",
                "3,5,6,11",
                "3,5,6,13",
                "3,5,6,14"));

        directedGraph = new DirectedGraph(14);
        directedGraph.addEdge(0L, 4L);
        directedGraph.addEdge(1L, 5L);
        directedGraph.addEdge(2L, 4L);
        directedGraph.addEdge(2L, 5L);
        directedGraph.addEdge(3L, 4L);
        directedGraph.addEdge(3L, 5L);
        directedGraph.addEdge(4L, 6L);
        directedGraph.addEdge(4L, 7L);
        directedGraph.addEdge(5L, 6L);

        directedGraph.addEdge(6L, 8L);
        directedGraph.addEdge(6L, 9L);
        directedGraph.addEdge(6L, 11L);
        directedGraph.addEdge(6L, 13L);
        directedGraph.addEdge(6L, 14L);

        directedGraph.addEdge(7L, 10L);
    }

    @After
    public void tearDown() throws Exception {

    }

    /**
     * Find all the paths in an ordered graph.
     *
     * @throws Exception If the test fail.
     */
    @Test
    public void findAllPathsAt() throws Exception {
        List<Long> parentList = new ArrayList<>(Arrays.asList(0L, 1L, 2L, 3L));
        validateResult(parentList);
    }

    /**
     * Find all the paths in a disordered graph.
     *
     * @throws Exception If the test fail.
     */
    @Test
    public void findAllPathsAtCaseDisGraph() throws Exception {
        directedGraph = new DirectedGraph(14);
        directedGraph.addEdge(6L, 9L);
        directedGraph.addEdge(6L, 11L);
        directedGraph.addEdge(2L, 4L);
        directedGraph.addEdge(7L, 10L);
        directedGraph.addEdge(6L, 14L);
        directedGraph.addEdge(1L, 5L);
        directedGraph.addEdge(0L, 4L);
        directedGraph.addEdge(4L, 6L);
        directedGraph.addEdge(6L, 8L);
        directedGraph.addEdge(3L, 4L);
        directedGraph.addEdge(2L, 5L);
        directedGraph.addEdge(6L, 13L);
        directedGraph.addEdge(3L, 5L);
        directedGraph.addEdge(4L, 7L);
        directedGraph.addEdge(5L, 6L);
        List<Long> parentList = new ArrayList<>(Arrays.asList(0L, 1L, 2L, 3L));
        validateResult(parentList);
    }

    @Test
    public void findAllPaths() throws Exception {
        LinkedList<LinkedList<Long>> allPaths = directedGraph.findAllPaths(2L, 1L, 0L, 3L);
        assertNotNull(allPaths);
        assertEquals(33, allPaths.size());
        List<String> expected = new ArrayList<>();
        for (Long key : result.keySet()) {
            expected.addAll(result.get(key));
        }

        for (LinkedList<Long> allPath : allPaths) {
            assertNotEquals(-1, expected.indexOf(join(allPath.toArray(), ",")));
        }
    }

    @Test
    public void verifyOrder() throws Exception {
        DirectedGraph directedGraph = new DirectedGraph(2);
        directedGraph.addEdge(36L, 34L);
        directedGraph.addEdge(35L, 36L);
        directedGraph.addEdge(36L, 33L);

        LinkedList<LinkedList<Long>> allPaths = directedGraph.findAllPaths(35L);
        assertNotNull(allPaths);
        assertEquals(2, allPaths.size());
        assertEquals("[35, 36, 34]", allPaths.get(0).toString());
        assertEquals("[35, 36, 33]", allPaths.get(1).toString());
    }

    @Test
    public void findAllPathsAtErrorCase() throws Exception {
        LinkedList<Long> auxPath = new LinkedList<>(); // work space
        LinkedList<LinkedList<Long>> allPaths = new LinkedList<>();
        try {
            directedGraph.findAllPathsAt(41L, auxPath, allPaths);
        } catch (IndexOutOfBoundsException ex) {
            assertEquals("The vertex: \"41\" does not exists.", ex.getMessage());
        }
    }

    @Test
    public void directGraph() throws Exception {
        DirectedGraph directedGraph = new DirectedGraph(5);
        directedGraph.addEdge(4L, 5L);
        directedGraph.addEdge(4L, 6L);
        directedGraph.addEdge(5L, 6L);
        directedGraph.addEdge(6L, 7L);
        directedGraph.addEdge(7L, 8L);

        //Natural paths.
        assertTrue(directedGraph.isReachable(4L, 5L));
        assertTrue(directedGraph.isReachable(4L, 6L));
        assertTrue(directedGraph.isReachable(5L, 6L));
        assertTrue(directedGraph.isReachable(6L, 7L));
        assertTrue(directedGraph.isReachable(7L, 8L));

        //Transitive paths.
        assertTrue(directedGraph.isReachable(4L, 7L));
        assertTrue(directedGraph.isReachable(4L, 8L));

        assertTrue(directedGraph.isReachable(5L, 7L));
        assertTrue(directedGraph.isReachable(5L, 8L));

        assertTrue(directedGraph.isReachable(6L, 8L));
    }

    @Test
    public void directGraphVerifyIfPathExists() throws Exception {
        DirectedGraph directedGraph = new DirectedGraph(5);
        directedGraph.addEdge(7L, 8L);
        directedGraph.addEdge(4L, 6L);
        directedGraph.addEdge(4L, 5L);
        directedGraph.addEdge(6L, 7L);
        directedGraph.addEdge(5L, 6L);
        assertFalse(directedGraph.isReachable(1L, 5L));
        assertFalse(directedGraph.isReachable(8L, 4L));
        assertFalse(directedGraph.isReachable(4L, 4L));
        assertFalse(directedGraph.isReachable(3L, 3L));
    }

    private void validateResult(List<Long> parentList) {
        for (Long key : parentList) {
            LinkedList<Long> auxPath = new LinkedList<>(); // work space
            LinkedList<LinkedList<Long>> allPaths = new LinkedList<>();
            directedGraph.findAllPathsAt(key, auxPath, allPaths);
            for (LinkedList<Long> allPath : allPaths) {
                assertNotEquals(-1, result.get(key).indexOf(join(allPath.toArray(), ",")));
            }
        }
    }
}