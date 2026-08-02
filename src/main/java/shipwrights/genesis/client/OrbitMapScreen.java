package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.physics.CelestialGravityWells;
import shipwrights.genesis.space.physics.CraftMotion;
import shipwrights.genesis.space.transformProvider.OrbitingTransformProvider;
import shipwrights.genesis.space.type.BuiltinCelestialTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * A KSP-style orbit map. Shows the solar system from a rotatable, zoomable
 * vantage: every body with its orbit, the player's craft and its predicted
 * path, and — once a body is picked — the speed it takes to leave the current
 * orbit and reach that target.
 *
 * <p>The view opens centred on you. Drag to rotate around whatever is centred,
 * scroll to zoom, left-click a body to target it, and right-click a body to
 * centre the view on it — so you can turn the camera around a planet and look
 * at it as an object rather than a dot. The chosen target is remembered in
 * {@link OrbitMapState} so the in-world orbit lines highlight it too.</p>
 */
public final class OrbitMapScreen extends Screen {
    /** Points sampled around a body's orbit ellipse. */
    private static final int ORBIT_SAMPLES = 160;
    /** Smallest a body may appear on screen, so far ones stay findable. */
    private static final double MIN_BODY_PIXELS = 3.0;
    private static final double MIN_PITCH = 0.02;
    private static final double MAX_PITCH = Math.PI / 2.0 - 0.02;
    private static final double MIN_ZOOM = 1.0e-4;
    private static final double MAX_ZOOM = 4.0;

    private final Level level;
    private final long ticks;

    private double yaw = 0.0;
    private double pitch = 0.45;
    private double zoom = 0.02;
    private Vector3d focus = new Vector3d();
    private boolean initialisedView = false;

    private long lastClickMs = 0;
    private ResourceLocation lastClicked = null;

    public OrbitMapScreen() {
        super(Component.literal("Orbit Map"));
        this.level = Minecraft.getInstance().level;
        this.ticks = this.level != null ? GenesisMod.getTicks(this.level) : 0L;
    }

    @Override
    public boolean isPauseScreen() {
        return false; // keep the world (and the craft's motion) live behind the map
    }

    @Override
    protected void init() {
        if (this.level == null || this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        if (this.initialisedView) {
            return;
        }
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(this.level);
        CraftMotion craft = CraftMotion.of(this.level, this.minecraft.player, 1.0f);
        CelestialGravityWells.OrbitalState state =
                CelestialGravityWells.orbitalState(this.level, craft.position(), craft.velocity());

        // Centre on the body the craft is bound to, sized to show its orbit;
        // otherwise centre on the star and frame the whole system.
        // Centre on YOU by default — the map opens showing where you are, and
        // right-clicking a body re-centres on that instead.
        this.focus = new Vector3d(craft.position());
        if (state != null) {
            double span = Double.isNaN(state.apoapsis()) ? state.distance() * 2.0 : state.apoapsis();
            this.zoom = fitZoom(Math.max(span, state.distance()) * 1.3);
        } else {
            this.zoom = fitZoom(systemRadius(registry) * 1.15);
        }
        this.initialisedView = true;
    }

    private double fitZoom(double worldRadius) {
        double halfScreen = Math.min(this.width, this.height) * 0.42;
        double z = worldRadius <= 1.0 ? MAX_ZOOM : halfScreen / worldRadius;
        return Mth.clamp(z, MIN_ZOOM, MAX_ZOOM);
    }

    private double systemRadius(Registry<Celestial> registry) {
        double max = 1000.0;
        for (Celestial celestial : registry) {
            double d = celestial.getPosition(this.ticks, registry).length();
            if (d > max) {
                max = d;
            }
        }
        return max;
    }

    // --- projection -------------------------------------------------------

    private Vector2d project(Vector3dc worldCelestial) {
        Vector3d v = new Vector3d(worldCelestial).sub(this.focus);
        v.rotateY(this.yaw);
        v.rotateX(this.pitch);
        double cx = this.width / 2.0;
        double cy = this.height / 2.0;
        return new Vector2d(cx + v.x * this.zoom, cy - v.z * this.zoom);
    }

    // --- rendering --------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xE6060810);
        if (this.level == null || this.minecraft == null || this.minecraft.player == null) {
            graphics.drawCenteredString(this.font, "No sky here", this.width / 2, this.height / 2, 0xFFAAAAAA);
            return;
        }

        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(this.level);
        LocalPlayer player = this.minecraft.player;
        CraftMotion craft = CraftMotion.of(this.level, player, partialTick);
        CelestialGravityWells.OrbitalState state =
                CelestialGravityWells.orbitalState(this.level, craft.position(), craft.velocity());
        ResourceLocation target = OrbitMapState.getSelectedTarget();

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Orbits and bodies.
        for (Celestial celestial : registry) {
            ResourceLocation id = registry.getKey(celestial);
            if (celestial.transformProvider() instanceof OrbitingTransformProvider orbit) {
                Celestial parent = registry.get(orbit.parentId());
                if (parent != null) {
                    boolean isTarget = id != null && id.equals(target);
                    drawOrbit(builder, matrix, celestial, parent, orbit, registry, isTarget);
                }
            }
        }

        // The craft's own predicted path.
        CelestialGravityWells.PredictedPath path =
                CelestialGravityWells.predictedPath(this.level, craft.position(), craft.velocity(), ORBIT_SAMPLES);
        if (path != null) {
            drawCraftPath(builder, matrix, path, registry);
        }

        // Body dots, drawn over the lines.
        List<BodyHit> hits = new ArrayList<>();
        for (Celestial celestial : registry) {
            ResourceLocation id = registry.getKey(celestial);
            Vector3dc worldPos = celestial.getPosition(this.ticks, registry);
            Vector2d screen = project(worldPos);
            int colour = bodyColour(celestial);
            double size = bodySize(celestial);
            boolean isTarget = id != null && id.equals(target);
            if (isTarget) {
                ring(builder, matrix, screen.x, screen.y, size + 5.0, 0xFFFF3B3B);
            }
            hits.add(new BodyHit(id, celestial, screen.x, screen.y, size));
        }

        // The craft marker.
        Vector2d craftScreen = project(craft.position());
        diamond(builder, matrix, craftScreen.x, craftScreen.y, 4.0, 0xFF63E6FF);

        MeshData mesh = builder.build();
        if (mesh != null) {
            // Flush the batched background fill first, or it would be composited
            // on top of this immediate-mode geometry and hide the whole map.
            graphics.flush();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            BufferUploader.drawWithShader(mesh);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }

        // Bodies themselves, each with its own planet texture, so a world in the
        // map looks like the world you fly to rather than a coloured marker.
        for (Celestial celestial : registry) {
            ResourceLocation id = registry.getKey(celestial);
            if (id == null) {
                continue;
            }
            drawTexturedBody(graphics, celestial, id,
                    celestial.getPosition(this.ticks, registry), bodySize(celestial));
        }

        // Labels next to each body dot.
        for (BodyHit hit : hits) {
            String name = hit.id != null ? prettyName(hit.id) : "?";
            graphics.drawString(this.font, name, (int) (hit.x + hit.size + 2), (int) (hit.y - 4),
                    0xFFCFE0FF, false);
        }

        drawInfoPanel(graphics, state, target, registry, craft);
        drawHelp(graphics);
    }

    private void drawInfoPanel(GuiGraphics graphics, CelestialGravityWells.OrbitalState state,
                               ResourceLocation target, Registry<Celestial> registry, CraftMotion craft) {
        int x = 8;
        int y = 8;
        graphics.drawString(this.font, "§lORBIT MAP", x, y, 0xFFFFFFFF, false);
        y += 14;

        if (state == null) {
            graphics.drawString(this.font, "Deep space — no dominant body", x, y, 0xFFAAAAAA, false);
        } else {
            int col = trajectoryColour(state.trajectory());
            graphics.drawString(this.font, "Around: §f" + prettyName(registry.getKey(state.body())), x, y, 0xFFB0C4FF, false);
            y += 11;
            graphics.drawString(this.font, state.trajectory().name(), x, y, col, false);
            y += 11;
            graphics.drawString(this.font, String.format("speed   %.0f m/s", state.speed()), x, y, 0xFFDDDDDD, false);
            y += 10;
            graphics.drawString(this.font, String.format("circular %.0f m/s", state.circularSpeed()), x, y, 0xFFDDDDDD, false);
            y += 10;
            graphics.drawString(this.font, String.format("escape  §e%.0f m/s", state.escapeSpeed()), x, y, 0xFFDDDDDD, false);
            y += 10;
            double dvEscape = Math.max(0.0, state.escapeSpeed() - state.speed());
            graphics.drawString(this.font, String.format("Δv to escape §e%.0f m/s", dvEscape), x, y, 0xFFDDDDDD, false);
            y += 10;
            if (!Double.isNaN(state.apoapsis())) {
                graphics.drawString(this.font, String.format("ap %.0f  pe %.0f  e %.2f",
                        state.apoapsis(), state.periapsis(), state.eccentricity()), x, y, 0xFF9FB0C0, false);
                y += 10;
            }
        }

        y += 6;
        if (target != null) {
            Celestial targetBody = registry.get(target);
            graphics.drawString(this.font, "Target: §c" + prettyName(target), x, y, 0xFFFF8080, false);
            y += 11;
            if (targetBody != null && state != null) {
                // The move begins by breaking the current bond, so escape speed
                // here is the honest "required speed to make it out".
                graphics.drawString(this.font,
                        String.format("need §e%.0f m/s§r to leave this orbit", state.escapeSpeed()),
                        x, y, 0xFFDDDDDD, false);
                y += 11;
                double targetR = targetBody.getPosition(this.ticks, registry).length();
                double craftR = new Vector3d(craft.position()).length();
                graphics.drawString(this.font,
                        String.format("target orbit r %.0f (you %.0f)", targetR, craftR),
                        x, y, 0xFF9FB0C0, false);
            }
        } else {
            graphics.drawString(this.font, "Click a body to set a target", x, y, 0xFF808080, false);
        }
    }

    private void drawHelp(GuiGraphics graphics) {
        String help = "drag: rotate   scroll: zoom   left-click: target   right-click: centre   M/Esc: close";
        graphics.drawString(this.font, help, 8, this.height - 12, 0xFF6C7A90, false);
    }

    // --- geometry emitters ------------------------------------------------

    private void drawOrbit(BufferBuilder builder, Matrix4f matrix, Celestial celestial, Celestial parent,
                           OrbitingTransformProvider orbit, Registry<Celestial> registry, boolean highlighted) {
        double period = orbit.orbitTicks();
        int colour = highlighted ? 0xFFFF5B5B : (bodyColour(celestial) & 0x00FFFFFF) | 0x88000000;
        double widthPx = highlighted ? 2.0 : 1.0;

        Vector2d prev = null;
        Vector2d first = null;
        for (int i = 0; i <= ORBIT_SAMPLES; i++) {
            long sampleTick = this.ticks + (long) (period * i / ORBIT_SAMPLES);
            Vector3d p = new Vector3d(celestial.getPosition(sampleTick, 0f, registry));
            // Relative to the parent NOW, so a moon's ring sits on its planet
            // rather than smearing along the planet's own orbit.
            p.sub(parent.getPosition(sampleTick, 0f, registry));
            p.add(parent.getPosition(this.ticks, 0f, registry));
            Vector2d screen = project(p);
            if (prev != null) {
                line(builder, matrix, prev.x, prev.y, screen.x, screen.y, widthPx, colour);
            } else {
                first = new Vector2d(screen);
            }
            prev = screen;
        }
        if (first != null && prev != null) {
            line(builder, matrix, prev.x, prev.y, first.x, first.y, widthPx, colour);
        }
    }

    private void drawCraftPath(BufferBuilder builder, Matrix4f matrix,
                               CelestialGravityWells.PredictedPath path, Registry<Celestial> registry) {
        Vector3dc bodyPos = path.body().getPosition(this.ticks, registry);
        int colour = 0xCC63E6FF;
        Vector2d prev = null;
        Vector2d first = null;
        for (Vector3dc rel : path.points()) {
            Vector3d world = new Vector3d(rel).add(bodyPos.x(), bodyPos.y(), bodyPos.z());
            Vector2d screen = project(world);
            if (prev != null) {
                line(builder, matrix, prev.x, prev.y, screen.x, screen.y, 1.5, colour);
            } else {
                first = new Vector2d(screen);
            }
            prev = screen;
        }
        if (path.closed() && first != null && prev != null) {
            line(builder, matrix, prev.x, prev.y, first.x, first.y, 1.5, colour);
        }
        // Mark periapsis and apoapsis.
        markPoint(builder, matrix, path.periapsis(), bodyPos, 0xFFFF9F40);
        if (path.apoapsis() != null) {
            markPoint(builder, matrix, path.apoapsis(), bodyPos, 0xFF6FA0FF);
        }
    }

    private void markPoint(BufferBuilder builder, Matrix4f matrix, Vector3dc rel, Vector3dc bodyPos, int colour) {
        Vector2d s = project(new Vector3d(rel).add(bodyPos.x(), bodyPos.y(), bodyPos.z()));
        square(builder, matrix, s.x, s.y, 2.5, colour);
    }

    // --- 2D primitives (thin quads) --------------------------------------

    private static void line(BufferBuilder b, Matrix4f m, double x1, double y1, double x2, double y2,
                             double width, int argb) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1.0e-6) {
            return;
        }
        double px = -dy / len * width * 0.5;
        double py = dx / len * width * 0.5;
        vertex(b, m, x1 + px, y1 + py, argb);
        vertex(b, m, x2 + px, y2 + py, argb);
        vertex(b, m, x2 - px, y2 - py, argb);
        vertex(b, m, x1 - px, y1 - py, argb);
    }

    private static void square(BufferBuilder b, Matrix4f m, double cx, double cy, double half, int argb) {
        vertex(b, m, cx - half, cy - half, argb);
        vertex(b, m, cx - half, cy + half, argb);
        vertex(b, m, cx + half, cy + half, argb);
        vertex(b, m, cx + half, cy - half, argb);
    }

    private static void diamond(BufferBuilder b, Matrix4f m, double cx, double cy, double r, int argb) {
        vertex(b, m, cx, cy - r, argb);
        vertex(b, m, cx - r, cy, argb);
        vertex(b, m, cx, cy + r, argb);
        vertex(b, m, cx + r, cy, argb);
    }

    private static void ring(BufferBuilder b, Matrix4f m, double cx, double cy, double radius, int argb) {
        int segments = 24;
        Vector2d prev = null;
        for (int i = 0; i <= segments; i++) {
            double a = i / (double) segments * Math.PI * 2.0;
            Vector2d p = new Vector2d(cx + Math.cos(a) * radius, cy + Math.sin(a) * radius);
            if (prev != null) {
                line(b, m, prev.x, prev.y, p.x, p.y, 1.5, argb);
            }
            prev = p;
        }
    }

    private static void vertex(BufferBuilder b, Matrix4f m, double x, double y, int argb) {
        float a = (argb >>> 24) / 255.0f;
        float r = (argb >> 16 & 0xFF) / 255.0f;
        float g = (argb >> 8 & 0xFF) / 255.0f;
        float bl = (argb & 0xFF) / 255.0f;
        b.addVertex(m, (float) x, (float) y, 0.0f).setColor(r, g, bl, a);
    }

    // --- helpers ----------------------------------------------------------

    private static int bodyColour(Celestial celestial) {
        if (BuiltinCelestialTypes.STAR.equals(celestial.type())) {
            return 0xFFFFE070;
        }
        if (BuiltinCelestialTypes.BLACKHOLE.equals(celestial.type())) {
            return 0xFFB060FF;
        }
        return 0xFFCCD6E6;
    }

    private static double bodySize(Celestial celestial) {
        if (BuiltinCelestialTypes.STAR.equals(celestial.type())) {
            return 5.0;
        }
        return 3.0;
    }

    private static int trajectoryColour(CelestialGravityWells.TrajectoryClass t) {
        return switch (t) {
            case CIRCULAR -> 0xFF63E6FF;
            case ELLIPTICAL -> 0xFF63E67A;
            case SUBORBITAL -> 0xFFFF6060;
            case ESCAPE -> 0xFFD37DFF;
        };
    }

    private static String prettyName(ResourceLocation id) {
        if (id == null) {
            return "?";
        }
        String path = id.getPath();
        return Character.toUpperCase(path.charAt(0)) + path.substring(1).replace('_', ' ');
    }

    private record BodyHit(ResourceLocation id, Celestial celestial, double x, double y, double size) {
    }

    /** The body under the cursor, or null. */
    private Celestial pickBody(Registry<Celestial> registry, double mouseX, double mouseY) {
        Celestial best = null;
        double bestDistance = 14.0 * 14.0;
        for (Celestial celestial : registry) {
            Vector2d screen = project(celestial.getPosition(this.ticks, registry));
            double d = (screen.x - mouseX) * (screen.x - mouseX) + (screen.y - mouseY) * (screen.y - mouseY);
            if (d < bestDistance) {
                bestDistance = d;
                best = celestial;
            }
        }
        return best;
    }

    /** Where the viewer is, in celestial coordinates. */
    private Vector3d craftPosition() {
        if (this.level == null || this.minecraft == null || this.minecraft.player == null) {
            return new Vector3d();
        }
        return new Vector3d(CraftMotion.of(this.level, this.minecraft.player, 1.0f).position());
    }

    /**
     * Draws a body as a solid three-dimensional cube rather than a flat dot, so
     * rotating the map reads as looking around real objects. Faces are shaded by
     * orientation and the far ones are skipped, which is what makes the shape
     * legible without a depth buffer to sort against.
     */
    private void drawBodyCube(BufferBuilder builder, Matrix4f matrix, Celestial celestial,
                              Vector3dc centre, double screenRadius, int colour) {
        // Real size, with a floor so a distant body does not vanish.
        double half = celestial == null
                ? screenRadius / Math.max(this.zoom, 1.0E-9)
                : Math.max(celestial.getActualSize() * 0.5,
                        MIN_BODY_PIXELS / Math.max(this.zoom, 1.0E-9));

        // Face order: -X, +X, -Y, +Y, -Z, +Z, each with a shade so the solid reads.
        double[][] normals = {{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}};
        float[] shades = {0.68f, 0.86f, 0.55f, 1.0f, 0.62f, 0.78f};

        for (int face = 0; face < 6; face++) {
            double nx = normals[face][0], ny = normals[face][1], nz = normals[face][2];

            // Skip faces pointing away from the viewer, in view space.
            Vector3d viewNormal = new Vector3d(nx, ny, nz).rotateY(this.yaw).rotateX(this.pitch);
            if (viewNormal.y > 0.0) {
                continue;
            }

            // Two in-plane axes for this face.
            Vector3d axisA = Math.abs(ny) > 0.5 ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
            Vector3d axisB = new Vector3d(nx, ny, nz).cross(axisA, new Vector3d());
            axisA = new Vector3d(axisB).cross(new Vector3d(nx, ny, nz)).normalize();
            axisB.normalize();

            Vector3d faceCentre = new Vector3d(centre).fma(half, new Vector3d(nx, ny, nz));
            int shaded = shade(colour, shades[face]);
            Vector2d p00 = project(new Vector3d(faceCentre).fma(-half, axisA).fma(-half, axisB));
            Vector2d p10 = project(new Vector3d(faceCentre).fma(half, axisA).fma(-half, axisB));
            Vector2d p11 = project(new Vector3d(faceCentre).fma(half, axisA).fma(half, axisB));
            Vector2d p01 = project(new Vector3d(faceCentre).fma(-half, axisA).fma(half, axisB));
            vertex(builder, matrix, p00.x, p00.y, shaded);
            vertex(builder, matrix, p01.x, p01.y, shaded);
            vertex(builder, matrix, p11.x, p11.y, shaded);
            vertex(builder, matrix, p10.x, p10.y, shaded);
        }
    }

    /**
     * Draws a body as a textured cube using the same planet texture the world
     * renderer uses, so a planet on the map is recognisably that planet rather
     * than a coloured dot. The texture's cube-face atlas is a 3x2 grid, matching
     * the layout the celestial renderer expects.
     */
    private void drawTexturedBody(GuiGraphics graphics, Celestial celestial, ResourceLocation id,
                                  Vector3dc centre, double screenRadius) {
        // Planet textures all live under THIS mod's assets, keyed by the body's
        // own namespace and path — minecraft:overworld is genesis-owned art at
        // planets/minecraft/overworld. Using the body's namespace as the asset
        // namespace looked for it under minecraft: and drew the missing-texture
        // magenta instead.
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID,
                "textures/planets/" + id.getNamespace() + "/" + id.getPath() + ".png");
        // A star has no surface texture; its own bright colour reads better.
        boolean star = BuiltinCelestialTypes.STAR.equals(celestial.type());
        if (star) {
            drawStarCube(graphics, celestial, centre, screenRadius);
            return;
        }

        // Draw the body at its REAL size, so zooming in makes it grow the way
        // approaching an object does. Sizing in screen pixels pinned it to a
        // constant on-screen size, which made zooming look like the planet was
        // being dragged along with the camera.
        double half = Math.max(celestial.getActualSize() * 0.5,
                MIN_BODY_PIXELS / Math.max(this.zoom, 1.0E-9));
        double third = 1.0 / 3.0;
        double twoThirds = 2.0 / 3.0;

        // Face order matches the atlas: north, west, south / east, down, up.
        double[][] normals = {{0,0,-1},{-1,0,0},{0,0,1},{1,0,0},{0,-1,0},{0,1,0}};
        double[][] uv = {
                {0.0, 0.0, third, 0.5}, {third, 0.0, twoThirds, 0.5}, {twoThirds, 0.0, 1.0, 0.5},
                {0.0, 0.5, third, 1.0}, {third, 0.5, twoThirds, 1.0}, {twoThirds, 0.5, 1.0, 1.0}};
        float[] shades = {0.72f, 0.66f, 0.82f, 0.90f, 0.58f, 1.0f};

        graphics.flush();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        BufferBuilder builder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();

        for (int face = 0; face < 6; face++) {
            Vector3d normal = new Vector3d(normals[face][0], normals[face][1], normals[face][2]);
            // Skip faces turned away from the viewer, so the solid reads without depth.
            if (new Vector3d(normal).rotateY(this.yaw).rotateX(this.pitch).y > 0.0) {
                continue;
            }

            Vector3d axisA = Math.abs(normal.y) > 0.5 ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
            Vector3d axisB = new Vector3d(normal).cross(axisA, new Vector3d()).normalize();
            axisA = new Vector3d(axisB).cross(normal).normalize();

            Vector3d faceCentre = new Vector3d(centre).fma(half, normal);
            Vector2d p00 = project(new Vector3d(faceCentre).fma(-half, axisA).fma(-half, axisB));
            Vector2d p10 = project(new Vector3d(faceCentre).fma(half, axisA).fma(-half, axisB));
            Vector2d p11 = project(new Vector3d(faceCentre).fma(half, axisA).fma(half, axisB));
            Vector2d p01 = project(new Vector3d(faceCentre).fma(-half, axisA).fma(half, axisB));

            float shade = star ? 1.0f : shades[face];
            float r = star ? 1.0f : shade;
            float g = star ? 0.95f : shade;
            float b = star ? 0.75f : shade;
            double[] t = uv[face];
            texVertex(builder, matrix, p00, t[0], t[1], r, g, b);
            texVertex(builder, matrix, p01, t[0], t[3], r, g, b);
            texVertex(builder, matrix, p11, t[2], t[3], r, g, b);
            texVertex(builder, matrix, p10, t[2], t[1], r, g, b);
        }

        MeshData mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** A star: no surface texture, so draw it as its own bright solid. */
    private void drawStarCube(GuiGraphics graphics, Celestial celestial, Vector3dc centre, double screenRadius) {
        graphics.flush();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        BufferBuilder builder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        drawBodyCube(builder, graphics.pose().last().pose(), celestial, centre, screenRadius, 0xFFFFE070);
        MeshData mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void texVertex(BufferBuilder builder, Matrix4f matrix, Vector2d at,
                                  double u, double v, float r, float g, float b) {
        builder.addVertex(matrix, (float) at.x, (float) at.y, 0.0f)
                .setUv((float) u, (float) v)
                .setColor(r, g, b, 1.0f);
    }

    private static int shade(int argb, float factor) {
        int a = argb >>> 24;
        int r = (int) Math.min(255, (argb >> 16 & 0xFF) * factor);
        int g = (int) Math.min(255, (argb >> 8 & 0xFF) * factor);
        int b = (int) Math.min(255, (argb & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // --- input ------------------------------------------------------------

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            this.yaw += dragX * 0.01;
            this.pitch = Mth.clamp(this.pitch - dragY * 0.01, MIN_PITCH, MAX_PITCH);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double factor = Math.pow(1.2, scrollY);
        this.zoom = Mth.clamp(this.zoom * factor, MIN_ZOOM, MAX_ZOOM);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Right-click re-centres the view on a body, so you can orbit the camera
        // around it and look at it as a model rather than a dot on a chart.
        if (button == 1 && this.level != null) {
            Registry<Celestial> registry = GenesisMod.getCelestialRegistry(this.level);
            Celestial picked = pickBody(registry, mouseX, mouseY);
            this.focus = picked != null
                    ? new Vector3d(picked.getPosition(this.ticks, registry))
                    : new Vector3d(craftPosition());
            return true;
        }
        if (button == 0 && this.level != null) {
            Registry<Celestial> registry = GenesisMod.getCelestialRegistry(this.level);
            ResourceLocation nearest = null;
            Celestial nearestBody = null;
            double best = 12.0 * 12.0;
            for (Celestial celestial : registry) {
                Vector2d s = project(celestial.getPosition(this.ticks, registry));
                double d = (s.x - mouseX) * (s.x - mouseX) + (s.y - mouseY) * (s.y - mouseY);
                if (d < best) {
                    best = d;
                    nearest = registry.getKey(celestial);
                    nearestBody = celestial;
                }
            }
            if (nearest != null) {
                OrbitMapState.setSelectedTarget(nearest);
                return true;
            }
            // Clicked empty space: clear the target.
            OrbitMapState.setSelectedTarget(null);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Close on the same key that opens it, as well as Escape.
        if (GenesisKeyMappings.ORBIT_MAP != null && GenesisKeyMappings.ORBIT_MAP.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
