package com.velocityspider.crashphysics.physics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuralSolverTest {

    private static final double MASS = 500.0;

    /**
     * A test structure: block positions, face connections and uniform loads.
     */
    private static final class Builder {
        final List<double[]> positions = new ArrayList<>();
        final List<int[]> edges = new ArrayList<>();
        final List<Double> joint = new ArrayList<>();
        final List<Double> crush = new ArrayList<>();

        int block(final double x, final double y, final double z, final double jointStrength, final double crushStrength) {
            this.positions.add(new double[]{x, y, z});
            this.joint.add(jointStrength);
            this.crush.add(crushStrength);
            return this.positions.size() - 1;
        }

        void connect(final int a, final int b) {
            this.edges.add(new int[]{a, b});
        }

        StructuralSolver.Graph build(final double[] accel, final int... sources) {
            final int nodes = this.positions.size();
            final List<List<Integer>> adjacency = new ArrayList<>();
            for (int i = 0; i < nodes; i++) {
                adjacency.add(new ArrayList<>());
            }
            for (final int[] edge : this.edges) {
                adjacency.get(edge[0]).add(edge[1]);
                adjacency.get(edge[1]).add(edge[0]);
            }

            final int[] start = new int[nodes + 1];
            for (int i = 0; i < nodes; i++) {
                start[i + 1] = start[i] + adjacency.get(i).size();
            }
            final int[] neighbors = new int[start[nodes]];
            final float[] weights = new float[start[nodes]];
            for (int i = 0; i < nodes; i++) {
                for (int k = 0; k < adjacency.get(i).size(); k++) {
                    neighbors[start[i] + k] = adjacency.get(i).get(k);
                    weights[start[i] + k] = 1.0f;
                }
            }

            final double[] position = new double[nodes * 3];
            final double[] masses = new double[nodes];
            final double[] accelerations = new double[nodes * 3];
            final double[] joints = new double[nodes];
            final double[] crushes = new double[nodes];
            for (int i = 0; i < nodes; i++) {
                System.arraycopy(this.positions.get(i), 0, position, i * 3, 3);
                System.arraycopy(accel, 0, accelerations, i * 3, 3);
                masses[i] = MASS;
                joints[i] = this.joint.get(i);
                crushes[i] = this.crush.get(i);
            }

            return new StructuralSolver.Graph(nodes, start, neighbors, weights, position, masses, accelerations, joints, crushes, sources);
        }
    }

    /**
     * 10 blocks in a row along x, the impact at block 0.
     */
    private static Builder beam(final double joint, final double crush) {
        final Builder builder = new Builder();
        for (int i = 0; i < 10; i++) {
            builder.block(i, 0, 0, joint, crush);
            if (i > 0) {
                builder.connect(i - 1, i);
            }
        }
        return builder;
    }

    @Test
    void beamBeingPulledSnapsOnceNextToTheImpact() {
        // Every block has to be pulled back towards the impact at 100 m/s²: the first joint carries 9 blocks in tension
        final List<StructuralSolver.Failure> failures = StructuralSolver.solve(beam(1e5, 1e7).build(new double[]{-100.0, 0.0, 0.0}, 0), 16);

        // It fails, and the part beyond it is then free, so nothing further out fails
        assertEquals(1, failures.size());
        assertArrayEquals(new int[]{1}, failures.getFirst().brokenNodes());
        assertEquals(450_000.0, failures.getFirst().load(), 1e-6);
        assertEquals(100_000.0, failures.getFirst().capacity(), 1e-6);
    }

    @Test
    void beamBeingPushedHoldsInCompression() {
        // Same load pushing the beam into the impact: the joints are squeezed, and only the material's crush strength counts
        assertTrue(StructuralSolver.solve(beam(1e5, 1e6).build(new double[]{100.0, 0.0, 0.0}, 0), 16).isEmpty());
    }

    @Test
    void strongBeamHoldsInTension() {
        assertTrue(StructuralSolver.solve(beam(1e6, 1e7).build(new double[]{-100.0, 0.0, 0.0}, 0), 16).isEmpty());
    }

    /**
     * A fuselage along x with a wing sticking out along z from block 5.
     */
    private static Builder plane(final int wingRootWidth, final double wingJoint) {
        final Builder builder = beam(1e7, 1e8);
        final int[][] wing = new int[wingRootWidth][5];
        for (int row = 0; row < wingRootWidth; row++) {
            for (int span = 0; span < 5; span++) {
                wing[row][span] = builder.block(5 + row, 0, span + 1, wingJoint, 1e7);
                if (span == 0) {
                    builder.connect(5 + row, wing[row][span]);
                } else {
                    builder.connect(wing[row][span - 1], wing[row][span]);
                }
                if (row > 0) {
                    builder.connect(wing[row - 1][span], wing[row][span]);
                }
            }
        }
        return builder;
    }

    @Test
    void weakWingTearsOffInShear() {
        // Decelerating along the fuselage loads the wing root sideways: 2.5 t × 100 m/s² = 250 kN of shear on a 100 kN joint
        final List<StructuralSolver.Failure> failures = StructuralSolver.solve(plane(1, 1e5).build(new double[]{100.0, 0.0, 0.0}, 0), 16);

        assertEquals(1, failures.size());
        assertArrayEquals(new int[]{10}, failures.getFirst().brokenNodes());
        assertEquals(250_000.0, failures.getFirst().load(), 1e-6);
    }

    @Test
    void widerWingRootSharesTheLoad() {
        // Two rows of 5 blocks: 500 kN of shear spread over two 300 kN joints holds
        assertTrue(StructuralSolver.solve(plane(2, 3e5).build(new double[]{100.0, 0.0, 0.0}, 0), 16).isEmpty());
    }

    @Test
    void nothingToAnalyseWithoutAnImpact() {
        assertTrue(StructuralSolver.solve(beam(1e3, 1e3).build(new double[]{-100.0, 0.0, 0.0}), 16).isEmpty());
    }
}
