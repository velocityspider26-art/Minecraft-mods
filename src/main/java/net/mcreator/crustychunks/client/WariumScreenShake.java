package net.mcreator.crustychunks.client;

import net.mcreator.crustychunks.WariumConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-only decaying camera shake for nuke detonations. Never referenced from
 * common/server code except through a Dist.CLIENT guard, so dedicated servers do
 * not load it.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class WariumScreenShake {
	private static final RandomSource RANDOM = RandomSource.create();
	private static float intensity = 0.0F;
	private static float decayPerTick = 0.0F;

	private WariumScreenShake() {
	}

	public static void add(float amount, int durationTicks) {
		if (!WariumConfig.NUKE_SCREEN_SHAKE.get())
			return;
		intensity = Math.max(intensity, Math.min(amount, 8.0F));
		decayPerTick = intensity / Math.max(1, durationTicks);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		if (intensity <= 0.0F)
			return;
		if (Minecraft.getInstance().isPaused())
			return;
		intensity = Math.max(0.0F, intensity - decayPerTick);
	}

	@SubscribeEvent
	public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
		if (intensity <= 0.0F)
			return;
		float mag = intensity;
		event.setYaw((float) event.getYaw() + Mth.randomBetween(RANDOM, -mag, mag));
		event.setPitch((float) event.getPitch() + Mth.randomBetween(RANDOM, -mag, mag));
		event.setRoll((float) event.getRoll() + Mth.randomBetween(RANDOM, -mag * 0.5F, mag * 0.5F));
	}
}
