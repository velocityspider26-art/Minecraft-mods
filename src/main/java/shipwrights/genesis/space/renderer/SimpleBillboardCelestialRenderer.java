package shipwrights.genesis.space.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Registry;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.mixin.LevelRendererAccessor;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;

/**
 * A simplified, vanilla-pipeline celestial renderer.
 *
 * <p>Genesis originally rendered stars/planets/black-holes with a suite of bespoke GLSL shaders
 * driven through Lodestone's 1.20 shader registry. That subsystem was rewritten from scratch for
 * Minecraft 1.21's core-shader pipeline, so this port draws each celestial body as a camera-facing
 * billboard quad tinted by the body's colour, using only the vanilla {@code POSITION_COLOR} pipeline.
 * The bodies remain visible and correctly positioned in the sky; the high-fidelity procedural
 * surfaces/atmospheres are a documented follow-up (see the porting report).</p>
 */
public class SimpleBillboardCelestialRenderer implements CelestialRenderer {

    private final float r, g, b, a;

    public SimpleBillboardCelestialRenderer(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @NotNull VantagePoint vantagePoint) {
        ClientLevel level = ((LevelRendererAccessor) event.getLevelRenderer()).getLevel();
        if (level == null) return;
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);

        Vector3dc bodyPos = toRender.getPosition(ticks, partialTick, registry);
        Vector3dc vantagePos = vantagePoint.getPosition();

        // Direction from the viewer to the body, placed on a fixed-radius sky sphere.
        Vector3d dir = new Vector3d(bodyPos).sub(vantagePos);
        double dist = dir.length();
        if (dist < 1.0e-6) return;
        dir.div(dist);

        double skyRadius = 100.0;
        Vector3d center = new Vector3d(dir).mul(skyRadius);

        // Angular size -> billboard half-extent on the sky sphere.
        double angularRadius = Math.atan((toRender.getActualSize() * 0.5) / dist);
        float half = (float) Math.max(0.5, Math.tan(angularRadius) * skyRadius);

        // Build a camera-facing basis.
        Vector3d up = Math.abs(dir.y) > 0.99 ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
        Vector3d right = new Vector3d(dir).cross(up).normalize().mul(half);
        Vector3d realUp = new Vector3d(right).cross(dir).normalize().mul(half);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addCorner(buffer, matrix, center, right, realUp, -1, -1);
        addCorner(buffer, matrix, center, right, realUp, -1, 1);
        addCorner(buffer, matrix, center, right, realUp, 1, 1);
        addCorner(buffer, matrix, center, right, realUp, 1, -1);
        com.mojang.blaze3d.vertex.MeshData mesh = buffer.build();
        if (mesh != null) {
            com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(mesh);
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private void addCorner(BufferBuilder buffer, Matrix4f matrix, Vector3d center, Vector3d right, Vector3d up, int sx, int sy) {
        float x = (float) (center.x + right.x * sx + up.x * sy);
        float y = (float) (center.y + right.y * sx + up.y * sy);
        float z = (float) (center.z + right.z * sx + up.z * sy);
        buffer.addVertex(matrix, x, y, z).setColor(r, g, b, a);
    }
}
