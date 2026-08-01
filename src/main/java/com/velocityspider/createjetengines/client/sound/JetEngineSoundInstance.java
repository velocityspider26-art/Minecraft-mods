package com.velocityspider.createjetengines.client.sound;

import com.velocityspider.createjetengines.blockentity.CombustionCoreBlockEntity;
import com.velocityspider.createjetengines.client.SubLevelClientUtil;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * A looping engine sound bound to one combustion core.
 *
 * <p>The position is re-projected out of the sublevel every tick, so the sound tracks an aircraft
 * that is translating and rotating instead of staying at the pre-assembly block position. The
 * instance stops itself when the engine shuts down or the block entity goes away, which is what
 * keeps loops from lingering after a shutdown or chunk unload.
 */
public class JetEngineSoundInstance extends AbstractTickableSoundInstance {

    private final CombustionCoreBlockEntity core;
    private final Function<CombustionCoreBlockEntity, Float> volumeCurve;
    private final Function<CombustionCoreBlockEntity, Float> pitchCurve;
    private final boolean requiresAfterburner;

    private float fade;

    public JetEngineSoundInstance(SoundEvent event, CombustionCoreBlockEntity core,
                                  Function<CombustionCoreBlockEntity, Float> volumeCurve,
                                  Function<CombustionCoreBlockEntity, Float> pitchCurve,
                                  boolean requiresAfterburner) {
        super(event, SoundSource.BLOCKS, RandomSource.create());
        this.core = core;
        this.volumeCurve = volumeCurve;
        this.pitchCurve = pitchCurve;
        this.requiresAfterburner = requiresAfterburner;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        updatePosition();
    }

    public CombustionCoreBlockEntity getCore() {
        return core;
    }

    @Override
    public void tick() {
        if (core.isRemoved() || core.getLevel() == null) {
            stop();
            return;
        }
        boolean wanted = requiresAfterburner ? core.isAfterburnerActive() : core.getSpool() > 0.005F;
        float target = wanted ? volumeCurve.apply(core) : 0.0F;

        // Smooth fade so loops never click in or out.
        fade += (target - fade) * 0.12F;
        volume = fade;
        pitch = pitchCurve.apply(core);

        if (!wanted && fade < 0.004F) {
            stop();
            return;
        }
        updatePosition();
    }

    private void updatePosition() {
        Vec3 world = SubLevelClientUtil.toWorld(core.getLevel(), core.getBlockPos().getCenter());
        this.x = world.x;
        this.y = world.y;
        this.z = world.z;
    }
}
