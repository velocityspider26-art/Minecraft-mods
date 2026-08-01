package com.velocityspider.createjetengines.client.sound;

import com.velocityspider.createjetengines.blockentity.CombustionCoreBlockEntity;
import com.velocityspider.createjetengines.client.SubLevelClientUtil;
import com.velocityspider.createjetengines.registry.JetSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client-side loop bookkeeping for engine audio.
 *
 * <p>One set of loops per core, tracked by position, so a loop is never started twice for the same
 * engine. Instances stop themselves when the engine winds down; this map just drops the dead ones.
 */
public final class JetSoundHandler {

    private record SoundSet(JetEngineSoundInstance idle,
                            JetEngineSoundInstance power,
                            JetEngineSoundInstance afterburner) {
        boolean allStopped() {
            return idle.isStopped() && power.isStopped() && afterburner.isStopped();
        }
    }

    private static final Map<BlockPos, SoundSet> ACTIVE = new HashMap<>();
    private static final Map<BlockPos, Boolean> WAS_RUNNING = new HashMap<>();
    private static final Map<BlockPos, Boolean> WAS_AFTERBURNING = new HashMap<>();

    private JetSoundHandler() {
    }

    /** Called from the core's renderer each frame; cheap and idempotent. */
    public static void ensurePlaying(CombustionCoreBlockEntity core) {
        prune();
        BlockPos pos = core.getBlockPos();
        boolean running = core.getSpool() > 0.005F;
        boolean was = WAS_RUNNING.getOrDefault(pos, false);

        if (running && !was) {
            playOneShot(core, JetSounds.ENGINE_START.get(), 0.9F, 1.0F);
        } else if (!running && was) {
            playOneShot(core, JetSounds.ENGINE_STOP.get(), 0.8F, 1.0F);
        }
        WAS_RUNNING.put(pos, running);

        // reheat ignition transient, fired once on the lighting edge
        boolean lit = core.isAfterburnerActive();
        if (lit && !WAS_AFTERBURNING.getOrDefault(pos, false)) {
            afterburnerIgnite(core);
        }
        WAS_AFTERBURNING.put(pos, lit);

        if (!running) {
            return;
        }
        SoundSet existing = ACTIVE.get(pos);
        if (existing != null && !existing.allStopped()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        JetEngineSoundInstance idle = new JetEngineSoundInstance(
                JetSounds.ENGINE_IDLE.get(), core,
                c -> 0.55F * (1.0F - Math.min(1.0F, c.getSpool() * 1.1F)) + 0.20F,
                c -> 0.80F + 0.35F * c.getSpool(),
                false);
        JetEngineSoundInstance power = new JetEngineSoundInstance(
                JetSounds.ENGINE_POWER.get(), core,
                c -> 0.85F * smoothstep(0.25F, 1.0F, c.getSpool()),
                c -> 0.85F + 0.40F * c.getSpool(),
                false);
        JetEngineSoundInstance ab = new JetEngineSoundInstance(
                JetSounds.AFTERBURNER_LOOP.get(), core,
                c -> 1.0F,
                c -> 0.90F + 0.20F * c.getSpool(),
                true);

        mc.getSoundManager().play(idle);
        mc.getSoundManager().play(power);
        mc.getSoundManager().play(ab);
        ACTIVE.put(pos, new SoundSet(idle, power, ab));
    }

    /** One-shot reheat ignition, fired by the afterburner renderer on the lighting edge. */
    public static void afterburnerIgnite(CombustionCoreBlockEntity core) {
        playOneShot(core, JetSounds.AFTERBURNER_IGNITE.get(), 1.0F, 1.0F);
    }

    private static void playOneShot(CombustionCoreBlockEntity core,
                                    net.minecraft.sounds.SoundEvent event, float volume, float pitch) {
        if (core.getLevel() == null) {
            return;
        }
        Vec3 at = SubLevelClientUtil.toWorld(core.getLevel(), core.getBlockPos().getCenter());
        Minecraft.getInstance().getSoundManager().play(
                new SimpleSoundInstance(event, SoundSource.BLOCKS, volume, pitch,
                        core.getLevel().getRandom(), at.x, at.y, at.z));
    }

    private static void prune() {
        Iterator<Map.Entry<BlockPos, SoundSet>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, SoundSet> e = it.next();
            if (e.getValue().allStopped()) {
                it.remove();
            }
        }
    }

    /** Drops all state on disconnect so nothing survives into the next world. */
    public static void reset() {
        ACTIVE.clear();
        WAS_RUNNING.clear();
        WAS_AFTERBURNING.clear();
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0.0F, Math.min(1.0F, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0F - 2.0F * t);
    }
}
