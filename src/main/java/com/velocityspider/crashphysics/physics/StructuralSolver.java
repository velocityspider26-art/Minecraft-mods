package com.velocityspider.crashphysics.physics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Finds where a vehicle's structure tears apart under crash loads.
 * <p>
 * When a vehicle hits something, the impact force enters at the contact blocks and has to be carried through the
 * structure to decelerate everything else. A block connection has to carry the inertial load of everything "behind"
 * it (further from the impact). If that load is more than the connection can hold, it snaps and the part behind it
 * flies off on its own, which is how wings, tails and masts come off in real crashes, even if they never touched
 * anything.
 * <p>
 * How a connection is loaded matters. A stack pressing down on its base is in compression and only fails once the
 * material itself crushes; an overhang like a wing loads its root in shear and bending, and fails at the much lower
 * strength of the joint. Each cut's load is split into its component along the cut (push or pull) and across it
 * (shear), and checked with an elliptical interaction rule.
 * <p>
 * The structure is a graph of blocks. A breadth-first search from the impact blocks sorts every block into layers by
 * distance. Every connected part beyond layer {@code d} hangs onto layer {@code d - 1} only through the connections
 * between the two layers, so those connections are a cut that has to carry the load of that whole part. A union-find
 * sweep from the outermost layer inwards builds those parts, and they are then checked from the impact outwards, so a
 * part that breaks off stops loading anything further out.
 */
public final class StructuralSolver {

    /**
     * A connection that failed.
     *
     * @param brokenNodes the nodes that break to free the detached part
     * @param load        the load the cut had to carry [N]
     * @param capacity    the load the cut could have carried in that direction [N]
     */
    public record Failure(int[] brokenNodes, double load, double capacity) {
    }

    /**
     * The block graph to analyse. Positions and accelerations must be in the same frame.
     *
     * @param nodeCount       number of blocks
     * @param neighborStart   compressed adjacency: neighbours of node {@code i} are at
     *                        {@code neighbors[neighborStart[i] .. neighborStart[i + 1])}; length {@code nodeCount + 1}
     * @param neighbors       neighbour node indices
     * @param neighborWeights fraction of a full face each connection represents (1 for faces, less for edges)
     * @param position        position of each block, packed as x, y, z [m]
     * @param mass            mass of each block [kg]
     * @param acceleration    proper acceleration the structure has to give each block, packed as x, y, z [m/s²]
     * @param jointStrength   tension and shear a full-face connection of each block can carry [N]
     * @param crushStrength   compression a full-face connection of each block can carry [N]
     * @param sources         blocks where the impact force enters the structure
     */
    public record Graph(int nodeCount, int[] neighborStart, int[] neighbors, float[] neighborWeights, double[] position,
                        double[] mass, double[] acceleration, double[] jointStrength, double[] crushStrength, int[] sources) {
        public Graph {
            if (neighborStart.length != nodeCount + 1) {
                throw new IllegalArgumentException("neighborStart must have nodeCount + 1 entries");
            }
            if (neighbors.length != neighborWeights.length) {
                throw new IllegalArgumentException("neighbors and neighborWeights must have the same length");
            }
            if (mass.length != nodeCount || jointStrength.length != nodeCount || crushStrength.length != nodeCount
                    || acceleration.length != nodeCount * 3 || position.length != nodeCount * 3) {
                throw new IllegalArgumentException("per-node arrays have the wrong length");
            }
        }
    }

    private StructuralSolver() {
    }

    /**
     * Finds the connections that fail.
     *
     * @param graph       the structure
     * @param maxFailures the maximum amount of failures to report
     */
    public static List<Failure> solve(final Graph graph, final int maxFailures) {
        final int n = graph.nodeCount();
        if (n == 0 || graph.sources().length == 0 || maxFailures <= 0) {
            return List.of();
        }

        // 1. Breadth-first layering from the impact blocks
        final int[] dist = new int[n];
        Arrays.fill(dist, -1);
        final int[] order = new int[n];
        int tail = 0;
        for (final int source : graph.sources()) {
            if (source >= 0 && source < n && dist[source] == -1) {
                dist[source] = 0;
                order[tail++] = source;
            }
        }
        for (int head = 0; head < tail; head++) {
            final int i = order[head];
            for (int e = graph.neighborStart()[i]; e < graph.neighborStart()[i + 1]; e++) {
                final int j = graph.neighbors()[e];
                if (dist[j] == -1) {
                    dist[j] = dist[i] + 1;
                    order[tail++] = j;
                }
            }
        }

        final int reached = tail;
        if (reached == 0) {
            return List.of();
        }
        final int maxDist = dist[order[reached - 1]];
        if (maxDist <= 0) {
            return List.of();
        }

        // order[] is sorted by distance, so every layer is one contiguous range
        final int[] layerStart = new int[maxDist + 2];
        Arrays.fill(layerStart, reached);
        for (int idx = reached - 1; idx >= 0; idx--) {
            layerStart[dist[order[idx]]] = idx;
        }
        layerStart[maxDist + 1] = reached;

        // 2. Sweep from the outermost layer inwards, building the part hanging beyond each cut
        final int[] parent = new int[n];
        final double[] load = new double[n * 3];
        final boolean[] inSet = new boolean[n];
        final IntList[] pendingChildren = new IntList[n];

        final List<Record> records = new ArrayList<>();
        final Cut[] cuts = new Cut[n];
        final IntList touchedRoots = new IntList();

        final double[] position = graph.position();

        for (int d = maxDist; d >= 1; d--) {
            for (int idx = layerStart[d]; idx < layerStart[d + 1]; idx++) {
                final int i = order[idx];
                inSet[i] = true;
                parent[i] = i;
                load[i * 3] = graph.mass()[i] * graph.acceleration()[i * 3];
                load[i * 3 + 1] = graph.mass()[i] * graph.acceleration()[i * 3 + 1];
                load[i * 3 + 2] = graph.mass()[i] * graph.acceleration()[i * 3 + 2];

                for (int e = graph.neighborStart()[i]; e < graph.neighborStart()[i + 1]; e++) {
                    final int j = graph.neighbors()[e];
                    if (inSet[j]) {
                        union(parent, load, pendingChildren, i, j);
                    }
                }
            }

            // The cut between layer d - 1 and everything beyond it
            for (int idx = layerStart[d - 1]; idx < layerStart[d]; idx++) {
                final int k = order[idx];
                for (int e = graph.neighborStart()[k]; e < graph.neighborStart()[k + 1]; e++) {
                    final int j = graph.neighbors()[e];
                    if (!inSet[j]) {
                        continue;
                    }

                    final int root = find(parent, j);
                    Cut cut = cuts[root];
                    if (cut == null) {
                        cut = new Cut();
                        cuts[root] = cut;
                    }
                    if (cut.edges.size == 0) {
                        touchedRoots.add(root);
                    }

                    final double weight = graph.neighborWeights()[e];
                    cut.tension += weight * Math.min(graph.jointStrength()[k], graph.jointStrength()[j]);
                    cut.compression += weight * Math.min(graph.crushStrength()[k], graph.crushStrength()[j]);

                    // Direction from the inner block to the outer one
                    final double dx = position[j * 3] - position[k * 3];
                    final double dy = position[j * 3 + 1] - position[k * 3 + 1];
                    final double dz = position[j * 3 + 2] - position[k * 3 + 2];
                    final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (length > 1.0e-9) {
                        cut.dx += weight * dx / length;
                        cut.dy += weight * dy / length;
                        cut.dz += weight * dz / length;
                    }

                    cut.edges.add(k);
                    cut.edges.add(j);
                }
            }

            for (int t = 0; t < touchedRoots.size; t++) {
                final int root = touchedRoots.data[t];
                final Cut cut = cuts[root];
                final Record record = Record.of(load[root * 3], load[root * 3 + 1], load[root * 3 + 2], cut,
                        pendingChildren[root] != null ? pendingChildren[root].toArray() : new int[0]);

                final int recordId = records.size();
                records.add(record);

                final IntList pending = new IntList();
                pending.add(recordId);
                pendingChildren[root] = pending;

                cut.reset();
            }
            touchedRoots.size = 0;
        }

        // 3. Check the cuts from the impact outwards. A part that breaks off stops loading the cuts further out.
        final List<Failure> failures = new ArrayList<>();
        final IntList stack = new IntList();
        for (int i = 0; i < n; i++) {
            if (inSet[i] && parent[i] == i && pendingChildren[i] != null) {
                for (int c = 0; c < pendingChildren[i].size; c++) {
                    stack.add(pendingChildren[i].data[c]);
                }
            }
        }

        while (stack.size > 0 && failures.size() < maxFailures) {
            final Record record = records.get(stack.data[--stack.size]);

            if (record.utilization > 1.0) {
                failures.add(new Failure(weakerSide(record.cutEdges, graph.jointStrength()), record.load, record.load / Math.sqrt(record.utilization)));
            } else {
                for (final int child : record.children) {
                    stack.add(child);
                }
            }
        }

        return failures;
    }

    /**
     * For each connection in the cut, picks the weaker of the two blocks as the one that breaks.
     */
    private static int[] weakerSide(final int[] cutEdges, final double[] jointStrength) {
        final IntList broken = new IntList();
        for (int e = 0; e + 1 < cutEdges.length; e += 2) {
            final int inner = cutEdges[e];
            final int outer = cutEdges[e + 1];
            final int weaker = jointStrength[inner] < jointStrength[outer] ? inner : outer;

            boolean seen = false;
            for (int b = 0; b < broken.size; b++) {
                if (broken.data[b] == weaker) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                broken.add(weaker);
            }
        }
        return broken.toArray();
    }

    private static int find(final int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(final int[] parent, final double[] load, final IntList[] pendingChildren, final int a, final int b) {
        final int rootA = find(parent, a);
        final int rootB = find(parent, b);
        if (rootA == rootB) {
            return;
        }

        parent[rootB] = rootA;
        load[rootA * 3] += load[rootB * 3];
        load[rootA * 3 + 1] += load[rootB * 3 + 1];
        load[rootA * 3 + 2] += load[rootB * 3 + 2];

        final IntList childrenB = pendingChildren[rootB];
        if (childrenB != null && childrenB.size > 0) {
            if (pendingChildren[rootA] == null) {
                pendingChildren[rootA] = new IntList();
            }
            for (int c = 0; c < childrenB.size; c++) {
                pendingChildren[rootA].add(childrenB.data[c]);
            }
        }
        pendingChildren[rootB] = null;
    }

    /**
     * Connections crossing one cut, accumulated while sweeping.
     */
    private static final class Cut {
        double tension;
        double compression;
        double dx;
        double dy;
        double dz;
        final IntList edges = new IntList();

        void reset() {
            this.tension = 0.0;
            this.compression = 0.0;
            this.dx = 0.0;
            this.dy = 0.0;
            this.dz = 0.0;
            this.edges.size = 0;
        }
    }

    /**
     * A part beyond a cut, and how heavily its cut is loaded (1 = at the limit).
     */
    private record Record(double load, double utilization, int[] cutEdges, int[] children) {
        static Record of(final double lx, final double ly, final double lz, final Cut cut, final int[] children) {
            final double load = Math.sqrt(lx * lx + ly * ly + lz * lz);

            // Split the load into push/pull along the cut and shear across it
            final double nLength = Math.sqrt(cut.dx * cut.dx + cut.dy * cut.dy + cut.dz * cut.dz);
            double along = 0.0;
            double across = load;
            if (nLength > 1.0e-9) {
                along = (lx * cut.dx + ly * cut.dy + lz * cut.dz) / nLength;
                across = Math.sqrt(Math.max(0.0, load * load - along * along));
            }

            // A positive component means the inner side pushes the part: compression
            final double axialCapacity = along > 0.0 ? cut.compression : cut.tension;
            final double shearCapacity = cut.tension;

            double utilization;
            if (!(axialCapacity > 0.0) || !(shearCapacity > 0.0)) {
                utilization = load > 0.0 ? Double.POSITIVE_INFINITY : 0.0;
            } else {
                final double axial = along / axialCapacity;
                final double shear = across / shearCapacity;
                utilization = axial * axial + shear * shear;
            }

            return new Record(load, utilization, cut.edges.toArray(), children);
        }
    }

    private static final class IntList {
        private int[] data = new int[8];
        private int size;

        void add(final int value) {
            if (this.size == this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }
            this.data[this.size++] = value;
        }

        int[] toArray() {
            return Arrays.copyOf(this.data, this.size);
        }
    }
}
