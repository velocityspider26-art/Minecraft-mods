package shipwrights.genesis.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Registry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.item.GenesisItems;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.SpaceLevel;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.physics.CelestialGravityWells;
import shipwrights.genesis.space.renderer.CelestialRenderCoordinates;
import shipwrights.genesis.space.transformProvider.OrbitingTransformProvider;

/**
 * Draws each orbiting body's actual path while the orbit goggles are held, so
 * orbital mechanics can be seen instead of inferred.
 *
 * <p>The path is sampled straight from {@link Celestial#getPosition} across one
 * full period, which means whatever the orbit really does shows up: the ellipse
 * from eccentricity, the tilt from inclination, the offset from the argument of
 * periapsis, and the uneven spacing of the tick marks that is Kepler's second
 * law (they bunch up at apoapsis where the body moves slowly).</p>
 *
 * <p>Each ring is drawn around its parent at the parent's own render scale, so
 * a moon's orbit appears around the rendered planet. The scale is uniform per
 * orbit, which preserves the shape — the celestial renderer's per-body distance
 * clamping would otherwise flatten every orbit into a sphere.</p>
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID, value = Dist.CLIENT)
public final class OrbitVisualizerRenderer {
    /** Samples around one orbit. Enough that an ellipse reads as smooth. */
    private static final int ORBIT_SAMPLES = 180;
    /** Every Nth sample gets a tick mark, which visualises orbital speed. */
    private static final int TICK_MARK_INTERVAL = 15;
    /** Samples along the wearer's own predicted path. */
    private static final int PATH_SAMPLES = 160;
    /** How near the wearer must be to a craft's pose to count as aboard it. */
    private static final double ABOARD_RADIUS = 32.0;
    private static final double TICK_SECONDS = 0.05;

    private OrbitVisualizerRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Drawn after terrain, entities and particles rather than with the sky,
        // so the depth buffer already holds the world. That is what lets the
        // lines be hidden by blocks and planets instead of painting over them.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || !holdingGoggles(player)) {
            return;
        }

        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);
        Vec3 cameraPos = event.getCamera().getPosition();

        VantagePoint vantagePoint = VantagePoint.get(level,
                new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z), ticks, partialTick);
        if (vantagePoint == null) {
            return;
        }

        Vector3dc observer = CelestialRenderCoordinates.getObserverCelestialPosition(vantagePoint, cameraPos);
        Quaterniond inverseObserver =
                new Quaterniond(CelestialRenderCoordinates.getObserverCelestialRotation(vantagePoint)).conjugate();

        // At this stage the pose stack already carries the camera orientation
        // (unlike AFTER_SKY, where it arrives as identity), and vertices are
        // camera-relative — which is what the render positions below already are.
        Matrix4f matrix = new Matrix4f(event.getPoseStack().last().pose());

        BufferBuilder builder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        boolean drewAnything = false;

        for (Celestial celestial : registry) {
            if (!(celestial.transformProvider() instanceof OrbitingTransformProvider orbit)) {
                continue; // static bodies have no path to draw
            }
            Celestial parent = registry.get(orbit.parentId());
            if (parent == null) {
                continue;
            }
            drewAnything |= drawOrbit(builder, matrix, celestial, parent, orbit,
                    registry, ticks, partialTick, observer, inverseObserver);
        }

        // The wearer's own path, solved from where they are and how fast they
        // are going. This is the one that actually matters while flying.
        drewAnything |= drawCraftPath(builder, matrix, level, player, registry,
                ticks, partialTick, observer, inverseObserver);

        MeshData mesh = builder.build();
        if (mesh == null || !drewAnything) {
            if (mesh != null) {
                mesh.close();
            }
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        // Occlude the lines against the world that has already been drawn.
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            BufferUploader.drawWithShader(mesh);
        } finally {
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    /** Worn on the head, or held in either hand. */
    public static boolean holdingGoggles(LocalPlayer player) {
        return isGoggles(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD))
                || isGoggles(player.getMainHandItem())
                || isGoggles(player.getOffhandItem());
    }

    private static boolean isGoggles(ItemStack stack) {
        return !stack.isEmpty() && stack.is(GenesisItems.ORBIT_GOGGLES.get());
    }

    /** @return true if the orbit was large enough on screen to be worth drawing. */
    private static boolean drawOrbit(BufferBuilder builder, Matrix4f matrix, Celestial celestial,
                                     Celestial parent, OrbitingTransformProvider orbit,
                                     Registry<Celestial> registry, long ticks, float partialTick,
                                     Vector3dc observer, Quaterniond inverseObserver) {
        // Anchor the ring on the parent, at the scale the parent renders at, so
        // it lines up with the body it belongs to.
        Vector3d parentRender = new Vector3d(parent.getPosition(ticks, partialTick, registry)).sub(observer);
        inverseObserver.transform(parentRender);
        double scale = CelestialRenderCoordinates.getPlanetRenderDistanceScale(parentRender);
        Vector3d parentAnchor = new Vector3d(parentRender).mul(scale);

        double period = orbit.orbitTicks();
        // A body targeted in the orbit map is drawn bright red here too, so the
        // selection reads the same in the map and in the world.
        net.minecraft.resources.ResourceLocation id = registry.getKey(celestial);
        boolean targeted = id != null && id.equals(OrbitMapState.getSelectedTarget());
        int[] colour = targeted ? new int[]{255, 91, 91} : orbitColour(celestial);

        Vector3d previous = null;
        Vector3d first = null;
        for (int sample = 0; sample <= ORBIT_SAMPLES; sample++) {
            long sampleTick = ticks + (long) (period * sample / ORBIT_SAMPLES);

            // Sample the path RELATIVE to the parent, so a moon's ring follows
            // its planet instead of smearing along the planet's own orbit.
            Vector3d relative = new Vector3d(celestial.getPosition(sampleTick, partialTick, registry))
                    .sub(parent.getPosition(sampleTick, partialTick, registry));
            inverseObserver.transform(relative);
            Vector3d point = relative.mul(scale).add(parentAnchor);

            if (previous != null) {
                line(builder, matrix, previous, point, colour);
                // Tick marks bunch together where the body moves slowly, which
                // makes Kepler's second law directly visible.
                if (sample % TICK_MARK_INTERVAL == 0) {
                    marker(builder, matrix, point, scale, colour);
                }
            } else {
                first = new Vector3d(point);
            }
            previous = point;
        }

        if (first != null && previous != null) {
            line(builder, matrix, previous, first, colour);
        }

        // A brighter cross marks where the body is right now.
        Vector3d current = new Vector3d(celestial.getPosition(ticks, partialTick, registry))
                .sub(parent.getPosition(ticks, partialTick, registry));
        inverseObserver.transform(current);
        marker(builder, matrix, current.mul(scale).add(parentAnchor), scale * 3.0,
                new int[]{255, 255, 255});
        return true;
    }

    /**
     * Draws the wearer's own predicted trajectory, colour-coded by what kind of
     * path it is, with its low and high points marked.
     *
     * @return true if a path was drawn
     */
    private static boolean drawCraftPath(BufferBuilder builder, Matrix4f matrix, Level level,
                                         LocalPlayer player, Registry<Celestial> registry,
                                         long ticks, float partialTick,
                                         Vector3dc observer, Quaterniond inverseObserver) {
        if (!GenesisMod.isSpaceDimension(level)) {
            return false; // wells and free flight only exist out here
        }

        CraftState craft = craftState(level, player, partialTick);
        if (craft == null) {
            return false;
        }

        CelestialGravityWells.PredictedPath path =
                CelestialGravityWells.predictedPath(level, craft.position(), craft.velocity(), PATH_SAMPLES);
        if (path == null) {
            return false;
        }
        CelestialGravityWells.OrbitalState state =
                CelestialGravityWells.orbitalState(level, craft.position(), craft.velocity());

        // Anchored on the body it orbits, at that body's render scale — the same
        // treatment the celestial orbits get, so the two read on one scale.
        Vector3d bodyRender = new Vector3d(path.body().getPosition(ticks, partialTick, registry)).sub(observer);
        inverseObserver.transform(bodyRender);
        double scale = CelestialRenderCoordinates.getPlanetRenderDistanceScale(bodyRender);
        Vector3d bodyAnchor = new Vector3d(bodyRender).mul(scale);

        int[] colour = pathColour(state);

        Vector3d previous = null;
        Vector3d first = null;
        for (Vector3dc relative : path.points()) {
            Vector3d point = toRender(relative, inverseObserver, scale, bodyAnchor);
            if (previous != null) {
                line(builder, matrix, previous, point, colour);
            } else {
                first = new Vector3d(point);
            }
            previous = point;
        }
        if (path.closed() && first != null && previous != null) {
            line(builder, matrix, previous, first, colour);
        }

        // Periapsis in the path's own colour, apoapsis dimmed, so which end of
        // the orbit you are looking at is never ambiguous.
        marker(builder, matrix, toRender(path.periapsis(), inverseObserver, scale, bodyAnchor),
                scale * 2.0, colour);
        if (path.apoapsis() != null) {
            marker(builder, matrix, toRender(path.apoapsis(), inverseObserver, scale, bodyAnchor),
                    scale * 2.0, new int[]{colour[0] / 2, colour[1] / 2, colour[2] / 2});
        }
        return true;
    }

    /** Body-relative celestial coordinates to camera-relative render coordinates. */
    private static Vector3d toRender(Vector3dc relative, Quaterniond inverseObserver,
                                     double scale, Vector3dc anchor) {
        Vector3d point = new Vector3d(relative);
        inverseObserver.transform(point);
        return point.mul(scale).add(anchor);
    }

    /** Where the wearer is and how fast, in celestial coordinates and m/s. */
    private record CraftState(Vector3d position, Vector3d velocity) {
    }

    /**
     * Reads the craft's motion when the wearer is aboard one, otherwise their
     * own. Ship velocity comes from the pose delta because the rigid body
     * itself is server-side.
     */
    private static CraftState craftState(Level level, LocalPlayer player, float partialTick) {
        SubLevel ship = shipCarrying(level, player);
        if (ship != null) {
            Vector3dc now = ship.logicalPose().position();
            Vector3dc last = ship.lastPose().position();
            return new CraftState(
                    SpaceLevel.toCelestialSpace(level, now),
                    new Vector3d(now.x() - last.x(), 0.0, now.z() - last.z()).mul(1.0 / TICK_SECONDS));
        }

        Vec3 motion = player.getDeltaMovement();
        return new CraftState(
                SpaceLevel.toCelestialSpace(level, player.getPosition(partialTick)),
                // Entity motion is blocks per tick; orbital maths works in m/s.
                new Vector3d(motion.x * 20.0, 0.0, motion.z * 20.0));
    }

    /** Client-side counterpart of the travel manager's aboard check. */
    private static SubLevel shipCarrying(Level level, LocalPlayer player) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }
        Vec3 position = player.position();
        for (SubLevel subLevel : container.getAllSubLevels()) {
            Vector3dc pose = subLevel.logicalPose().position();
            if (position.distanceToSqr(pose.x(), pose.y(), pose.z()) < ABOARD_RADIUS * ABOARD_RADIUS) {
                return subLevel;
            }
        }
        return null;
    }

    /** Reuses the readout's colour language so the line matches the text. */
    private static int[] pathColour(CelestialGravityWells.OrbitalState state) {
        if (state == null) {
            return new int[]{200, 200, 200};
        }
        return switch (state.trajectory()) {
            case CIRCULAR -> new int[]{80, 235, 255};
            case ELLIPTICAL -> new int[]{90, 240, 120};
            case SUBORBITAL -> new int[]{255, 80, 80};
            case ESCAPE -> new int[]{225, 130, 255};
        };
    }

    /** Distinct colour per body so overlapping orbits stay readable. */
    private static int[] orbitColour(Celestial celestial) {
        int hash = celestial.hashCode();
        return new int[]{
                140 + Math.floorMod(hash, 116),
                140 + Math.floorMod(hash >> 8, 116),
                140 + Math.floorMod(hash >> 16, 116)
        };
    }

    private static void line(BufferBuilder builder, Matrix4f matrix, Vector3d from, Vector3d to, int[] colour) {
        builder.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z)
                .setColor(colour[0], colour[1], colour[2], 210);
        builder.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z)
                .setColor(colour[0], colour[1], colour[2], 210);
    }

    /** Small three-axis cross, sized so it stays visible at any orbit scale. */
    private static void marker(BufferBuilder builder, Matrix4f matrix, Vector3d at, double scale, int[] colour) {
        double size = Math.max(0.6, 2.5 * scale);
        for (int axis = 0; axis < 3; axis++) {
            Vector3d offset = new Vector3d(
                    axis == 0 ? size : 0.0,
                    axis == 1 ? size : 0.0,
                    axis == 2 ? size : 0.0);
            line(builder, matrix, new Vector3d(at).sub(offset), new Vector3d(at).add(offset), colour);
        }
    }
}
