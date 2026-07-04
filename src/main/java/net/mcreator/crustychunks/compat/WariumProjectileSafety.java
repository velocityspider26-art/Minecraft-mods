package net.mcreator.crustychunks.compat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.mcreator.crustychunks.WariumConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Global safety net for all Warium projectiles:
 * lifetime cap, velocity clamp, NaN/position sanity, per-level projectile cap,
 * and velocity inheritance from vehicles / physics constructs.
 */
@EventBusSubscriber
public final class WariumProjectileSafety {
	private static final Map<EntityType<?>, Boolean> WARIUM_ARROW = new ConcurrentHashMap<>();
	private static final Map<ResourceKey<Level>, AtomicInteger> ACTIVE = new ConcurrentHashMap<>();

	private WariumProjectileSafety() {
	}

	private static boolean isWariumProjectile(Entity entity) {
		if (!(entity instanceof AbstractArrow))
			return false;
		return WARIUM_ARROW.computeIfAbsent(entity.getType(), type -> {
			ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
			return key != null && CrustyChunksMod.MODID.equals(key.getNamespace());
		});
	}

	@SubscribeEvent
	public static void onJoin(EntityJoinLevelEvent event) {
		Entity entity = event.getEntity();
		if (event.getLevel().isClientSide() || !isWariumProjectile(entity))
			return;
		AtomicInteger counter = ACTIVE.computeIfAbsent(event.getLevel().dimension(), d -> new AtomicInteger());
		if (counter.get() >= WariumConfig.MAX_ACTIVE_PROJECTILES.get()) {
			event.setCanceled(true);
			return;
		}
		counter.incrementAndGet();
		// Inherit shooter's vehicle / physics-construct velocity so shots from
		// moving craft do not spawn behind the vehicle or hit the shooter.
		AbstractArrow arrow = (AbstractArrow) entity;
		Entity owner = arrow.getOwner();
		if (owner != null) {
			Vec3 inherited = AeronauticsCompat.inheritedShooterVelocity(owner);
			if (inherited.lengthSqr() > 1.0E-4)
				arrow.setDeltaMovement(arrow.getDeltaMovement().add(inherited));
		}
		sanitize(arrow);
	}

	@SubscribeEvent
	public static void onLeave(EntityLeaveLevelEvent event) {
		if (event.getLevel().isClientSide() || !isWariumProjectile(event.getEntity()))
			return;
		AtomicInteger counter = ACTIVE.get(event.getLevel().dimension());
		if (counter != null)
			counter.updateAndGet(v -> Math.max(0, v - 1));
	}

	@SubscribeEvent
	public static void onTick(EntityTickEvent.Post event) {
		Entity entity = event.getEntity();
		if (entity.level().isClientSide() || !isWariumProjectile(entity))
			return;
		if (entity.tickCount > WariumConfig.PROJECTILE_LIFETIME_TICKS.get()) {
			entity.discard();
			return;
		}
		sanitize((AbstractArrow) entity);
	}

	private static void sanitize(AbstractArrow arrow) {
		Vec3 velocity = arrow.getDeltaMovement();
		if (!isFinite(velocity) || !isFinite(arrow.position()) || Math.abs(arrow.getY()) > 20000.0) {
			arrow.discard();
			return;
		}
		double max = WariumConfig.MAX_PROJECTILE_SPEED.get();
		double speedSqr = velocity.lengthSqr();
		if (speedSqr > max * max)
			arrow.setDeltaMovement(velocity.scale(max / Math.sqrt(speedSqr)));
	}

	private static boolean isFinite(Vec3 v) {
		return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
	}
}
