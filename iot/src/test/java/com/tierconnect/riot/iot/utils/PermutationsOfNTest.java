package com.tierconnect.riot.iot.utils;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by achambi on 7/25/17.
 * Test case for permutation test.
 */
public class PermutationsOfNTest {
    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void permutationsTest() throws Exception {
        List<String> f = Lists.newArrayList("A", "B", "C", "D");
        PermutationsOfN<String> g = new PermutationsOfN<>();
        assertEquals("n=1 subsets [[A], [B], [C], [D]]",
                String.format("n=1 subsets %s", g.processSubsets(f, 1)));
        assertEquals("n=1 permutations [[A], [B], [C], [D]]",
                String.format("n=1 permutations %s", g.permutations(f, 1)));
        assertEquals("n=2 subsets [[A, B], [A, C], [A, D], [B, C], [B, D], [C, D]]",
                String.format("n=2 subsets %s", g.processSubsets(f, 2)));
        assertEquals("n=2 permutations [[A, B], [B, A], [A, C], [C, A], [A, D], [D, A], [B, C], [C, B], [B, D], [D, B], [C, D], [D, C]]",
                String.format("n=2 permutations %s", g.permutations(f, 2)));
        assertEquals("n=3 subsets [[A, B, C], [A, B, D], [A, C, D], [B, C, D]]",
                String.format("n=3 subsets %s", g.processSubsets(f, 3)));
        assertEquals("n=3 permutations [[A, B, C], [A, C, B], [C, A, B], [C, B, A], [B, C, A], [B, A, C], [A, B, D], [A, D, B], [D, A, B], [D, B, A], [B, D, A], [B, A, D], [A, C, D], [A, D, C], [D, A, C], [D, C, A], [C, D, A], [C, A, D], [B, C, D], [B, D, C], [D, B, C], [D, C, B], [C, D, B], [C, B, D]]",
                String.format("n=3 permutations %s", g.permutations(f, 3)));
        assertEquals("n=4 subsets [[A, B, C, D]]", String.format("n=4 subsets %s", g.processSubsets(f, 4)));
        assertEquals("n=4 permutations [[A, B, C, D], [A, B, D, C], [A, D, B, C], [D, A, B, C], [D, A, C, B], [A, D, C, B], [A, C, D, B], [A, C, B, D], [C, A, B, D], [C, A, D, B], [C, D, A, B], [D, C, A, B], [D, C, B, A], [C, D, B, A], [C, B, D, A], [C, B, A, D], [B, C, A, D], [B, C, D, A], [B, D, C, A], [D, B, C, A], [D, B, A, C], [B, D, A, C], [B, A, D, C], [B, A, C, D]]", String.format("n=4 permutations %s", g.permutations(f, 4)));
        assertEquals("n=5 subsets [[A, B, C, D]]", String.format("n=5 subsets %s",
                g.processSubsets(f, 5)));
        assertEquals("n=5 permutations [[A, B, C, D], [A, B, D, C], [A, D, B, C], [D, A, B, C], [D, A, C, B], [A, D, C, B], [A, C, D, B], [A, C, B, D], [C, A, B, D], [C, A, D, B], [C, D, A, B], [D, C, A, B], [D, C, B, A], [C, D, B, A], [C, B, D, A], [C, B, A, D], [B, C, A, D], [B, C, D, A], [B, D, C, A], [D, B, C, A], [D, B, A, C], [B, D, A, C], [B, A, D, C], [B, A, C, D]]",
                String.format("n=5 permutations %s", g.permutations(f, 5)));
    }
}