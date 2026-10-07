package com.velocityspider.crashphysics.client;

import com.velocityspider.crashphysics.CrashPhysics;
import com.velocityspider.crashphysics.config.CrashClientConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Shakes the camera after nearby crashes. Uses a decaying "trauma" value and smooth noise, so a big crash gives a hard
 * jolt that settles over a second or two rather than random jitter.
 */
@EventBusSubscriber(modid = CrashPhysics.MODID, value = Dist.CLIENT)
public final class CameraShake {

    private static final float MAX_YAW = 3.5f;
    private static final float MAX_PITCH = 3.0f;
    private static final float MAX_ROLL = 5.0f;

    private static float trauma;
    private static long ticks;

    private CameraShake() {
    }

    public static void add(final float strength) {
        if (!CrashClientConfig.CAMERA_SHAKE.getAsBoolean()) {
            return;
        }
        trauma = Math.min(1.5f, trauma + strength * (float) CrashClientConfig.CAMERA_SHAKE_STRENGTH.getAsDouble());
    }

    @SubscribeEvent
    static void onClientTick(final ClientTickEvent.Post event) {
        ticks++;
        if (trauma > 0.0f) {
            trauma = Math.max(0.0f, trauma - 0.025f - trauma * 0.06f);
        }
    }

    @SubscribeEvent
    static void onComputeCameraAngles(final ViewportEvent.ComputeCameraAngles event) {
        if (trauma <= 0.0f) {
            return;
        }

        final float amount = Math.min(1.0f, trauma);
        final float shake = amount * amount;
        final double time = (ticks + event.getPartialTick()) * 0.9;

        event.setYaw(event.getYaw() + MAX_YAW * shake * noise(time, 1));
        event.setPitch(event.getPitch() + MAX_PITCH * shake * noise(time, 2));
        event.setRoll(event.getRoll() + MAX_ROLL * shake * noise(time, 3));
    }

    /**
     * Smooth noise in roughly [-1, 1] from a few incommensurate sine waves.
     */
    private static float noise(final double time, final int channel) {
        final double phase = channel * 17.31;
        return (float) ((Math.sin(time * 1.73 + phase) + 0.5 * Math.sin(time * 3.17 + phase * 1.9) + 0.25 * Math.sin(time * 6.29 + phase * 2.7)) / 1.75);
    }
}
