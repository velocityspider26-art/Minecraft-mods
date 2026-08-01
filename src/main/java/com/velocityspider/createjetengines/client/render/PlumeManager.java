package com.velocityspider.createjetengines.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.velocityspider.createjetengines.blockentity.CombustionCoreBlockEntity;
import com.velocityspider.createjetengines.blockentity.JetModuleBlockEntity;
import com.velocityspider.createjetengines.client.SubLevelClientUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Owns every live exhaust plume on the client and draws them in world space.
 *
 * <p>The plumes deliberately do <em>not</em> render from the block entity renderer. A block entity
 * inside a Sable sublevel is drawn with the sublevel's transform already applied, so anything drawn
 * there moves rigidly with the aircraft. Exhaust gas does not: once it has left the nozzle it
 * belongs to the world. Rendering from {@code RenderLevelStageEvent} in world coordinates is what
 * lets the plume stay where it was emitted while the aircraft flies on.
 */
public final class PlumeManager {

    /** Furthest a plume can reach; also the raycast length for terrain impingement. */
    private static final double MAX_REACH = 7.0D;

    private static final Map<BlockPos, PlumeTrail> TRAILS = new HashMap<>();

    private PlumeManager() {
    }

    /**
     * Feeds one gas parcel into the nozzle's trail. Called once per client tick per nozzle.
     */
    public static void emit(JetModuleBlockEntity nozzle, CombustionCoreBlockEntity core) {
        Level level = nozzle.getLevel();
        if (level == null) {
            return;
        }
        float spool = core.getSpool();
        if (spool <= 0.02F) {
            return;
        }

        Direction exhaust = nozzle.getFacing();
        BlockPos pos = nozzle.getBlockPos();

        Vec3 localDir = new Vec3(exhaust.getStepX(), exhaust.getStepY(), exhaust.getStepZ());
        Vec3 dir = SubLevelClientUtil.dirToWorld(level, pos, localDir).normalize();
        Vec3 mouth = SubLevelClientUtil.toWorld(level, pos.getCenter().add(localDir.scale(0.52D)));

        TRAILS.computeIfAbsent(pos.immutable(), p -> new PlumeTrail())
                .emit(mouth, dir, core.getThrottle(), core.isAfterburnerActive(),
                        clearanceAhead(level, mouth, dir));
    }

    /**
     * Distance to the first solid block ahead of the nozzle.
     *
     * <p>Measured in world space at the projected nozzle position, so it tests real terrain. That
     * is what makes the plume flatten against the ground when the aircraft is sitting on a runway
     * instead of shooting through it.
     */
    private static double clearanceAhead(Level level, Vec3 from, Vec3 dir) {
        Vec3 to = from.add(dir.scale(MAX_REACH));
        try {
            HitResult hit = level.clip(new ClipContext(
                    from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    CollisionContext.empty()));
            if (hit.getType() == HitResult.Type.MISS) {
                return MAX_REACH;
            }
            return Math.max(0.15D, hit.getLocation().distanceTo(from));
        } catch (Throwable t) {
            return MAX_REACH;
        }
    }

    /** Ages every trail. Runs on the client tick so plumes keep evolving while off screen. */
    public static void tick() {
        Iterator<Map.Entry<BlockPos, PlumeTrail>> it = TRAILS.entrySet().iterator();
        while (it.hasNext()) {
            PlumeTrail trail = it.next().getValue();
            trail.tick();
            if (trail.isDead()) {
                it.remove();
            }
        }
    }

    /** Draws every trail. Called from {@code RenderLevelStageEvent} in world space. */
    public static void render(PoseStack pose, Camera camera, float partialTick) {
        if (TRAILS.isEmpty()) {
            return;
        }
        Vec3 cam = camera.getPosition();
        MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();
        var consumer = buffers.getBuffer(JetRenderTypes.PLUME);

        pose.pushPose();
        // The level pose stack is still camera-relative at this stage, so shift world
        // coordinates into it once and draw everything in absolute positions.
        pose.translate(-cam.x, -cam.y, -cam.z);
        for (PlumeTrail trail : TRAILS.values()) {
            if (!trail.isEmpty()) {
                trail.build(consumer, pose, partialTick);
            }
        }
        pose.popPose();

        buffers.endBatch(JetRenderTypes.PLUME);
    }

    /** Drops everything on disconnect so no plume survives into the next world. */
    public static void reset() {
        TRAILS.clear();
    }
}
