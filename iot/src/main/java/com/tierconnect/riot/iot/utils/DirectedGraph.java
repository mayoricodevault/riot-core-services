package com.tierconnect.riot.iot.utils;


import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Created by achambi on 7/24/17.
 * Class for implement a direction graph.
 */
public class DirectedGraph {

    private int vertexNumber;
    private LinkedHashMap<Long, LinkedList<Long>> adj;
    private List<Long> vList;
    private static Logger logger = Logger.getLogger(DirectedGraph.class);

    /**
     * Default constructor
     *
     * @param vertexNumber number of nodes.
     */
    public DirectedGraph(int vertexNumber) {
        this.vertexNumber = vertexNumber;
        this.adj = new LinkedHashMap<>(vertexNumber);
        this.vList = new ArrayList<>();
    }

    //Function to add an edge into the graph
    @SuppressWarnings("unchecked")
    public void addEdge(Long v, Long w) {
        if (adj.get(v) == null) {
            adj.put(v, new LinkedList(newArrayList(w)));
        } else {
            adj.get(v).add(w);
        }

        adj.computeIfAbsent(w, k -> new LinkedList<>());
        addVertexList(v);
        addVertexList(w);
    }

    /**
     * add vertex list.
     *
     * @param v vertex id.
     */
    private void addVertexList(Long v) {
        if (!vList.contains(v)) {
            vList.add(v);
        }
    }

    /**
     * Method For verify if exists a path between two vertex.
     *
     * @param s A {@link Long} that contains the first vertex.
     * @param d A {@link Long} that contains the second vertex.
     * @return A instance of {@link Boolean} true if it exists a path or false if not exists.
     */
    Boolean isReachable(Long s, Long d) {

        // Mark all the vertices as not visited(By default set
        // as false)
        LinkedHashMap<Long, Boolean> visited = new LinkedHashMap<>(this.vertexNumber);

        // Create a queue for BFS
        LinkedList<Long> queue = new LinkedList<>();

        // Mark the current node as visited and enqueue it
        visited.put(s, true);
        queue.add(s);

        // 'i' will be used to get all adjacent vertices of a vertex
        Iterator<Long> i;
        while (queue.size() != 0) {
            // Dequeue a vertex from queue and print it
            s = queue.poll();
            Long n;
            if (adj.get(s) == null) {
                return false;
            }
            i = adj.get(s).listIterator();

            // Get all adjacent vertices of the dequeued vertex s
            // If a adjacent has not been visited, then mark it
            // visited and enqueue it
            while (i.hasNext()) {
                n = i.next();

                // If this adjacent node is the destination node,
                // then return true
                if (n.equals(d)) {
                    return true;
                }

                // Else, continue to do BFS
                if (visited.get(n) == null) {
                    visited.put(n, true);
                    queue.add(n);
                }
            }
        }
        // If BFS is complete without visited d
        return false;
    }

    /**
     * Find all paths from the begin of graph to end that passing per a vertex.
     *
     * @param v        a {@link Long} that contains the vertex to verify.
     * @param auxPath  a instance of {@link LinkedList}<{@link Long}> to use with auxiliary container.
     * @param allPaths a output {@link LinkedList}<{@link LinkedList}<{@link Long}>> that contains all paths.
     */

    void findAllPathsAt(Long v, LinkedList<Long> auxPath, LinkedList<LinkedList<Long>> allPaths) {
        if (this.vList.indexOf(v) == -1) {
            logger.debug("The vertex: \"" + v + "\" does not exists.");
            return;
        }
        auxPath.add(v);
        /*Path end or path not exists*/
        if (adj.get(v).isEmpty()) {
            //noinspection unchecked
            allPaths.add((LinkedList<Long>) auxPath.clone());
            return;
        }
        for (int i = 0; i < adj.get(v).size(); i++) {
            //noinspection unchecked
            LinkedList<Long> auxPathTwo = (LinkedList<Long>) auxPath.clone();
            findAllPathsAt(adj.get(v).get(i), auxPathTwo, allPaths);
        }
    }

    /**
     * Find all paths from all parent vertex of a graph to end.
     *
     * @param parents a instance of {@link LinkedList}<{@link Long}> to use with auxiliary container.
     * @return a {@link LinkedHashMap}<{@link Long}, {@link LinkedList}<{@link LinkedList}<{@link Long}>>> that contains
     * all paths per parent vertex.
     */
    public LinkedList<LinkedList<Long>> findAllPaths(Long... parents) {
        LinkedList<LinkedList<Long>> fullPaths = new LinkedList<>();
        // Generate an iterator. Start just after the last element.
        for (Long pathItem : parents) {
            LinkedList<Long> auxPath = new LinkedList<>(); // work space
            LinkedList<LinkedList<Long>> paths = new LinkedList<>();
            this.findAllPathsAt(pathItem, auxPath, paths);
            fullPaths.addAll(paths);
        }
        return fullPaths;
    }

    /**
     * Method to get all ways of a vertex
     *
     * @param v       A {@link Long} vertex number
     * @param parents A instance of {@link Long}[] that contains the parent numbers.
     * @return a {@link List}<{@link LinkedList}<{@link Long}>> that contains all ways through v or empty list.
     */
    public List<LinkedList<Long>> findAllPathsThrough(Long v, Long parents[]) {
        LinkedList<LinkedList<Long>> allPaths = findAllPaths(parents);
        List<LinkedList<Long>> thingTypePathsList;
        if (v != null) {
            thingTypePathsList = allPaths.stream().filter(p -> p.indexOf(v) != -1).collect(Collectors.toList());
            return thingTypePathsList;
        } else {
            return allPaths;
        }
    }

    /**
     * Method to get all vertex in a list of a way.
     *
     * @param v       A {@link Long} vertex number
     * @param parents A instance of {@link Long}[] that contains the parent numbers.
     * @return a {@link List}<{@link LinkedList}<{@link Long}>> that contains all ways through v or empty list.
     */
    public List<Long> findAllPathsThroughMerged(Long v, Long parents[]) {
        List<LinkedList<Long>> allPathsThrough = findAllPathsThrough(v, parents);
        return allPathsThrough.stream().flatMap(List::stream).distinct().collect(Collectors.toList());
    }

    /**
     * Get the vertex adjacency matrix
     *
     * @param v A instance of {@link Long} that contains the vertex id.
     * @return a adjacency matrix: {@link List}<{@link Long}>.
     */
    public LinkedList<Long> getAdjByVertex(Long v) {
        return adj.get(v);
    }
}
