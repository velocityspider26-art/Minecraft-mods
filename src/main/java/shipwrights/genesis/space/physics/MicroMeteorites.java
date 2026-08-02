package shipwrights.genesis.space.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import shipwrights.genesis.GenesisMod;

/**
 * Micro-meteorite strikes on airless worlds. With no atmosphere to burn them up,
 * debris reaches the ground intact — so impacts land near players as small,
 * shallow craters rather than the fireballs an atmosphere would produce.
 *
 * <p>Deliberately conservative: strikes are small, land away from the player,
 * and never set fire (there is no oxygen), so the effect reads as a hazard
 * without being a griefing machine.</p>
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class MicroMeteorites {
    private static final ResourceLocation MERCURY =
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "mercury");
    private static final ResourceLocation MOON =
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "moon");

    /** Average ticks between strike attempts per player. */
    private static final int INTERVAL = 200;
    /** Chance a given attempt actually produces a strike. */
    private static final float STRIKE_CHANCE = 0.35f;
    /** Horizontal spread around the player, in blocks. */
    private static final int SPREAD = 48;
    /** Kept clear of the player so a strike is a spectacle, not an ambush. */
    private static final int MIN_DISTANCE = 12;

    private MicroMeteorites() {
    }

    private static boolean isAirless(Level level) {
        ResourceLocation id = level.dimension().location();
        return id.equals(MERCURY) || id.equals(MOON);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isAirless(level)) {
            return;
        }
        if (level.getGameTime() % INTERVAL != 0) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            if (level.random.nextFloat() > STRIKE_CHANCE) {
                continue;
            }

            int dx = level.random.nextInt(SPREAD * 2) - SPREAD;
            int dz = level.random.nextInt(SPREAD * 2) - SPREAD;
            if (Math.abs(dx) < MIN_DISTANCE && Math.abs(dz) < MIN_DISTANCE) {
                continue;
            }

            BlockPos impact = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                    player.blockPosition().offset(dx, 0, dz));
            if (!level.isLoaded(impact)) {
                continue;
            }

            // No atmosphere means no fire — just the impact itself.
            level.explode(null, impact.getX(), impact.getY(), impact.getZ(),
                    1.2f, false, Level.ExplosionInteraction.MOB);
            level.sendParticles(ParticleTypes.EXPLOSION,
                    impact.getX() + 0.5, impact.getY() + 0.5, impact.getZ() + 0.5,
                    3, 0.4, 0.2, 0.4, 0.0);
            // Sound cannot travel through vacuum: play it faintly, as the thud
            // that reaches you through the ground rather than through the air.
            level.playSound(null, impact, SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.BLOCKS, 0.35f, 0.6f);
        }
    }
}
