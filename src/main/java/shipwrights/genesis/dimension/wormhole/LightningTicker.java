package shipwrights.genesis.dimension.wormhole;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import shipwrights.genesis.GenesisMod;

import java.util.List;

@EventBusSubscriber
public class LightningTicker {

    private static int ticks = 0;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLevelTick(final LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {

            if (!(serverLevel.getPlayers(u -> true, 1).isEmpty()) && GenesisMod.WORMHOLE_DIM.equals(serverLevel.dimension().location())) {
                wormholeLightningTick(serverLevel);
            }
        }
    }

    private static void wormholeLightningTick(ServerLevel wormholeLevel) {
        ticks++;

        if (ticks >= 20) {
            ticks = 0;

            if (wormholeLevel.random.nextFloat() < 0.05f) {
                List<ServerPlayer> players = wormholeLevel.getPlayers(player -> true);
                if (!players.isEmpty()) {
                    ServerPlayer randomPlayer = players.get(wormholeLevel.random.nextInt(players.size()));

                    double distance = 30 + wormholeLevel.random.nextDouble() * 50;
                    double angle = wormholeLevel.random.nextDouble() * Math.PI * 2;

                    Vec3 playerPos = randomPlayer.position();
                    double offsetX = Math.cos(angle) * distance;
                    double offsetZ = Math.sin(angle) * distance;

                    BlockPos lightningPos = new BlockPos(
                            (int) (playerPos.x + offsetX),
                            (int) playerPos.y + wormholeLevel.random.nextInt(-25, 25),
                            (int) (playerPos.z + offsetZ)
                    );

                    LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(wormholeLevel);
                    if (lightning != null) {
                        lightning.moveTo(Vec3.atBottomCenterOf(lightningPos));
                        lightning.setVisualOnly(true);
                        wormholeLevel.addFreshEntity(lightning);
                    }
                }
            }
        }
    }
}
