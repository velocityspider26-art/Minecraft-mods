package shipwrights.genesis.space.voxel;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.teleportation.CubeFaceFrame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Viewport-wide, hole-free selection for the resident planet voxel pyramid.
 *
 * <p>The selector starts at each coarsest resident root and descends only when
 * every occupied child octant is available. A missing fine brick therefore
 * falls back to real coarser voxel geometry instead of exposing the old flat
 * planet shell or leaving an empty patch. Projected voxel size, not crosshair
 * distance, controls refinement across the whole view.</p>
 */
public final class PlanetVoxelLodSelector {
    private static final double MIN_FOV_RADIANS = Math.toRadians(20.0);
    private static final double MAX_FOV_RADIANS = Math.toRadians(150.0);
    private static final double REFINE_HYSTERESIS = 1.18;
    private static final double COARSEN_HYSTERESIS = 0.82;

    private PlanetVoxelLodSelector() {
    }

    public record OrbitView(Vector3dc camera,
                            Vector3dc look,
                            double cubeHalfExtent,
                            double worldToRendered,
                            int seaLevel,
                            int viewportHeight,
                            double aspectRatio,
                            double verticalFovRadians,
                            double targetVoxelPixels,
                            int maximumLod,
                            int maximumBricks,
                            boolean cullBackFaces) {
        public OrbitView {
            if (camera == null || look == null) {
                throw new IllegalArgumentException("camera and look are required");
            }
            if (!Double.isFinite(cubeHalfExtent) || cubeHalfExtent <= 0.0
                    || !Double.isFinite(worldToRendered) || worldToRendered <= 0.0) {
                throw new IllegalArgumentException("invalid planet render scale");
            }
            if (viewportHeight <= 0 || !Double.isFinite(aspectRatio) || aspectRatio <= 0.0) {
                throw new IllegalArgumentException("invalid viewport");
            }
            if (!Double.isFinite(targetVoxelPixels) || targetVoxelPixels <= 0.0) {
                throw new IllegalArgumentException("targetVoxelPixels must be positive");
            }
            if (maximumLod < 0 || maximumLod > PlanetVoxelBrickKey.MAX_LOD
                    || maximumBricks <= 0) {
                throw new IllegalArgumentException("invalid LOD selection limits");
            }
            verticalFovRadians = clamp(verticalFovRadians,
                    MIN_FOV_RADIANS, MAX_FOV_RADIANS);
            camera = new Vector3d(camera);
            Vector3d normalizedLook = new Vector3d(look);
            if (normalizedLook.lengthSquared() < 1.0E-12) {
                normalizedLook.set(0.0, 0.0, -1.0);
            } else {
                normalizedLook.normalize();
            }
            look = normalizedLook;
        }
    }

    public record Selection(PlanetVoxelBrick brick,
                            double distance,
                            double projectedVoxelPixels) {
    }

    public static List<Selection> selectOrbit(List<PlanetVoxelBrick> resident,
                                               OrbitView view,
                                               Set<PlanetVoxelBrickKey> previousSelection) {
        if (resident == null || resident.isEmpty()) return List.of();
        Map<PlanetVoxelBrickKey, PlanetVoxelBrick> bricks = new HashMap<>(resident.size() * 2);
        for (PlanetVoxelBrick brick : resident) {
            if (brick != null && !brick.isEmpty() && brick.key().lod() <= view.maximumLod()) {
                PlanetVoxelBrick previous = bricks.put(brick.key(), brick);
                if (previous != null && previous.revision() > brick.revision()) {
                    bricks.put(previous.key(), previous);
                }
            }
        }
        if (bricks.isEmpty()) return List.of();

        Set<PlanetVoxelBrickKey> previous = previousSelection == null
                ? Set.of() : Set.copyOf(previousSelection);
        Set<PlanetVoxelBrickKey> previouslyRefined = refinedAncestors(previous, view.maximumLod());
        LinkedHashSet<PlanetVoxelBrickKey> roots = new LinkedHashSet<>();
        for (PlanetVoxelBrickKey key : bricks.keySet()) {
            PlanetVoxelBrickKey root = key;
            PlanetVoxelBrickKey ancestor = key;
            while (ancestor.lod() < view.maximumLod()) {
                ancestor = ancestor.parent();
                if (bricks.containsKey(ancestor)) root = ancestor;
            }
            roots.add(root);
        }

        List<Root> orderedRoots = new ArrayList<>(roots.size());
        for (PlanetVoxelBrickKey key : roots) {
            PlanetVoxelBrick brick = bricks.get(key);
            if (brick == null) continue;
            Metrics metrics = metrics(brick, view);
            if (metrics.visible) orderedRoots.add(new Root(brick, metrics.distance));
        }
        orderedRoots.sort(Comparator.comparingDouble(Root::distance));

        Map<PlanetVoxelBrickKey, Selection> selected = new HashMap<>();
        PriorityQueue<Refinement> refinements = new PriorityQueue<>(
                Comparator.comparingDouble(Refinement::priority).reversed());
        for (Root root : orderedRoots) {
            Selection selection = selection(root.brick, view);
            if (selection == null) continue;
            selected.put(root.brick.key(), selection);
            Refinement refinement = refinement(root.brick, bricks, view,
                    previous, previouslyRefined);
            if (refinement != null) refinements.add(refinement);
        }

        // Coarsest visible roots are coverage, not optional detail. Let that
        // tiny baseline exceed the requested refinement budget rather than
        // opening holes on a partially streamed planet.
        int budget = Math.max(view.maximumBricks(), selected.size());
        while (!refinements.isEmpty()) {
            Refinement refinement = refinements.remove();
            if (!selected.containsKey(refinement.parent().key())) continue;
            int resultingSize = selected.size() - 1 + refinement.children().size();
            if (resultingSize > budget) continue;

            selected.remove(refinement.parent().key());
            for (Selection child : refinement.children()) {
                selected.put(child.brick().key(), child);
                Refinement childRefinement = refinement(child.brick(), bricks, view,
                        previous, previouslyRefined);
                if (childRefinement != null) refinements.add(childRefinement);
            }
        }

        List<Selection> result = new ArrayList<>(selected.values());
        result.sort(Comparator.comparingDouble(Selection::distance)
                .thenComparingInt(selection -> selection.brick().key().lod()));
        return List.copyOf(result);
    }

    private static Refinement refinement(PlanetVoxelBrick brick,
                                         Map<PlanetVoxelBrickKey, PlanetVoxelBrick> bricks,
                                         OrbitView view,
                                         Set<PlanetVoxelBrickKey> previous,
                                         Set<PlanetVoxelBrickKey> previouslyRefined) {
        Metrics metrics = metrics(brick, view);
        if (!metrics.visible) return null;

        PlanetVoxelBrickKey key = brick.key();
        if (key.lod() == 0) return null;
        double threshold = view.targetVoxelPixels();
        if (previous.contains(key)) threshold *= REFINE_HYSTERESIS;
        else if (previouslyRefined.contains(key)) threshold *= COARSEN_HYSTERESIS;
        if (metrics.projectedVoxelPixels <= threshold) return null;

        int occupiedMask = brick.occupiedOctantMask();
        if (occupiedMask == 0) return null;
        List<Selection> children = new ArrayList<>(Integer.bitCount(occupiedMask));
        for (int slot = 0; slot < 8; slot++) {
            if ((occupiedMask & (1 << slot)) == 0) continue;
            PlanetVoxelBrick child = bricks.get(childKey(key, slot));
            if (child == null) return null;
            Selection selection = selection(child, view);
            if (selection != null) children.add(selection);
        }
        return new Refinement(brick, List.copyOf(children),
                metrics.projectedVoxelPixels / threshold);
    }

    private static Selection selection(PlanetVoxelBrick brick, OrbitView view) {
        Metrics metrics = metrics(brick, view);
        return metrics.visible
                ? new Selection(brick, metrics.distance, metrics.projectedVoxelPixels)
                : null;
    }

    private static Metrics metrics(PlanetVoxelBrick brick, OrbitView view) {
        PlanetVoxelBrickKey key = brick.key();
        double span = key.brickSpan();
        double u = key.minU() + span * 0.5;
        double v = key.minV() + span * 0.5;
        double elevation = key.minY() + span * 0.5 - view.seaLevel() + 0.72;
        Vector3d center = CubeFaceFrame.cubePoint(key.face(), u, v,
                        view.cubeHalfExtent(), elevation)
                .mul(view.worldToRendered());
        Vector3d toCenter = center.sub(view.camera(), new Vector3d());
        double distance = Math.max(1.0E-6, toCenter.length());
        double radius = span * view.worldToRendered() * 0.88;

        if (view.cullBackFaces()) {
            Vector3dc normal = CubeFaceFrame.axes(key.face()).up();
            double renderedHalf = view.cubeHalfExtent() * view.worldToRendered();
            double cameraReach = Math.max(Math.abs(view.camera().x()),
                    Math.max(Math.abs(view.camera().y()), Math.abs(view.camera().z())));
            if (cameraReach > renderedHalf * 0.98
                    && view.camera().dot(normal) - renderedHalf < -radius) {
                return new Metrics(false, distance, 0.0);
            }
        }

        double along = toCenter.dot(view.look());
        if (along < -radius) return new Metrics(false, distance, 0.0);
        double tanVertical = Math.tan(view.verticalFovRadians() * 0.5);
        double tanDiagonal = Math.hypot(tanVertical, tanVertical * view.aspectRatio());
        double lateralSquared = Math.max(0.0, distance * distance - along * along);
        double allowedLateral = Math.max(0.0, along) * tanDiagonal + radius;
        if (lateralSquared > allowedLateral * allowedLateral) {
            return new Metrics(false, distance, 0.0);
        }

        double focalPixels = view.viewportHeight() / (2.0 * tanVertical);
        double nearDistance = Math.max(key.cellSize() * view.worldToRendered(), distance - radius);
        double projected = key.cellSize() * view.worldToRendered()
                * focalPixels / nearDistance;
        return new Metrics(true, distance, projected);
    }

    private static PlanetVoxelBrickKey childKey(PlanetVoxelBrickKey parent, int slot) {
        int lod = parent.lod() - 1;
        return new PlanetVoxelBrickKey(parent.face(), lod,
                parent.brickU() * 2 + (slot & 1),
                parent.brickY() * 2 + ((slot >>> 1) & 1),
                parent.brickV() * 2 + ((slot >>> 2) & 1));
    }

    private static Set<PlanetVoxelBrickKey> refinedAncestors(Set<PlanetVoxelBrickKey> selected,
                                                              int maximumLod) {
        Set<PlanetVoxelBrickKey> ancestors = new HashSet<>();
        for (PlanetVoxelBrickKey key : selected) {
            PlanetVoxelBrickKey parent = key;
            while (parent.lod() < maximumLod) {
                parent = parent.parent();
                ancestors.add(parent);
            }
        }
        return ancestors;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record Root(PlanetVoxelBrick brick, double distance) {
    }

    private record Refinement(PlanetVoxelBrick parent,
                              List<Selection> children,
                              double priority) {
    }

    private record Metrics(boolean visible, double distance,
                           double projectedVoxelPixels) {
    }
}
