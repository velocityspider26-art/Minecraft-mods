package com.velocityspider.crashphysics.sable;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.material.CrashMaterials;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePostPhysicsTickEvent;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Hooks crash physics into Sable's physics loop and the game tick.
 * <pre>
 * every game tick, for each physics sub-step:
 *     pre-physics   sample every vehicle's velocity and pose
 *     Rapier step   collision callbacks decide which contacts are crushing material ({@link ImpactCollector})
 *     post-physics  apply resisting forces, damage, debris ({@link ImpactResolver})
 * end of tick:      crew g-forces, structural failure, collapses, explosions, sounds
 * </pre>
 */
public final class CrashTickHandler {

    private CrashTickHandler() {
    }

    public static void register(final IEventBus bus) {
        bus.addListener(CrashTickHandler::onPrePhysics);
        bus.addListener(CrashTickHandler::onPostPhysics);
        bus.addListener(CrashTickHandler::onLevelTick);
        bus.addListener(CrashTickHandler::onLevelUnload);
        bus.addListener(CrashTickHandler::onTagsUpdated);
    }

    /**
     * Called from {@link CrashCollisionCallback} inside the physics step.
     */
    static boolean onContact(final BlockState state, final int x, final int y, final int z, final int ox, final int oy, final int oz,
                             final double ix, final double iy, final double iz, final double impactVelocity, final boolean hasOther) {
        final SubLevelPhysicsSystem system = SubLevelPhysicsSystem.getCurrentlySteppingSystem();
        if (system == null) {
            return false;
        }

        final LevelCrashState crashState = LevelCrashState.getIfPresent(system.getLevel());
        if (crashState == null) {
            return false;
        }

        return crashState.collector.onContact(state, x, y, z, ox, oy, oz, ix, iy, iz, impactVelocity, hasOther);
    }

    private static void onPrePhysics(final ForgeSablePrePhysicsTickEvent event) {
        final SubLevelPhysicsSystem system = event.getPhysicsSystem();
        try {
            LevelCrashState.get(system.getLevel()).prePhysics(system, event.getTimeStep());
        } catch (final RuntimeException e) {
            CrashPhysics.LOGGER.error("Crash physics failed before a physics step", e);
        }
    }

    private static void onPostPhysics(final ForgeSablePostPhysicsTickEvent event) {
        final SubLevelPhysicsSystem system = event.getPhysicsSystem();
        final LevelCrashState crashState = LevelCrashState.getIfPresent(system.getLevel());
        if (crashState == null) {
            return;
        }

        try {
            crashState.postPhysics(system);
        } catch (final RuntimeException e) {
            CrashPhysics.LOGGER.error("Crash physics failed after a physics step", e);
        }
    }

    private static void onLevelTick(final LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof final ServerLevel serverLevel)) {
            return;
        }

        final LevelCrashState crashState = LevelCrashState.getIfPresent(serverLevel);
        if (crashState == null) {
            return;
        }

        try {
            crashState.endTick();
        } catch (final RuntimeException e) {
            CrashPhysics.LOGGER.error("Crash physics failed at the end of a tick", e);
        }
    }

    private static void onLevelUnload(final LevelEvent.Unload event) {
        if (event.getLevel() instanceof final ServerLevel serverLevel) {
            LevelCrashState.remove(serverLevel);
        }
    }

    private static void onTagsUpdated(final TagsUpdatedEvent event) {
        CrashMaterials.invalidate();
    }
}
