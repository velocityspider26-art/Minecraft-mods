package shipwrights.genesis.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.networking.GenesisNetworking;
import shipwrights.genesis.networking.SyncTimeOffsetPacket;
import shipwrights.genesis.time.GenesisTimeData;

@Mixin(TimeCommand.class)
public class TimeCommandMixin {

    private static final long DAY_LENGTH = 24_000L;

    /**
     * Makes {@code /time set} move the sun the player can actually see.
     *
     * <p>Genesis does not light the sky from {@code dayTime}. It computes the
     * sun's apparent angle from the celestial's real orbit, sampled at
     * {@link GenesisMod#getTicks}, which is game time plus a saved offset.
     * Vanilla's {@code setTime} writes {@code dayTime} and nothing else, so
     * {@code /time set day} left the orbit exactly where it was: the command
     * reported success while the world stayed lit for whatever time the orbit
     * was really at, which is why noon could look like midnight.</p>
     *
     * <p>Moving the offset instead puts the orbit at the requested time. The
     * shift is always forwards, matching how a day cycle behaves and avoiding a
     * backwards jump in a value other systems treat as monotonic.</p>
     */
    @WrapMethod(method = "setTime")
    private static int setTimeWrap(CommandSourceStack source, int time,
                                   Operation<Integer> original) {
        MinecraftServer server = source.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld != null) {
            GenesisTimeData data = GenesisTimeData.getOrCreate(server);
            long current = overworld.getGameTime() + data.getTimeOffset();
            long delta = Math.floorMod((long) time - current, DAY_LENGTH);
            if (delta != 0L) {
                data.setOffset(data.getTimeOffset() + delta);
                // The offset only reaches clients on login, so a mid-session
                // change has to be pushed or the sky stays where it was for
                // everyone already connected.
                long offset = data.getTimeOffset();
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    GenesisNetworking.sendToPlayer(player, new SyncTimeOffsetPacket(offset));
                }
            }
        }
        return original.call(source, time);
    }

    @WrapMethod(method = "getDayTime")
    private static int getDayTimeWrap(ServerLevel level, Operation<Integer> original) {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, GenesisMod.getTicks(level)));
    }

    @WrapOperation(method = "addTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getDayTime()J"))
    private static long fixAddTime(ServerLevel instance, Operation<Long> original) {
        return GenesisMod.getTicks(instance);
    }

    /** Same reasoning as {@link #setTimeWrap}: advance the orbit, not just {@code dayTime}. */
    @WrapMethod(method = "addTime")
    private static int addTimeWrap(CommandSourceStack source, int amount,
                                   Operation<Integer> original) {
        if (amount != 0) shiftCelestialTime(source.getServer(), amount);
        return original.call(source, amount);
    }

    private static void shiftCelestialTime(MinecraftServer server, long delta) {
        GenesisTimeData data = GenesisTimeData.getOrCreate(server);
        data.setOffset(data.getTimeOffset() + delta);
        long offset = data.getTimeOffset();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            GenesisNetworking.sendToPlayer(player, new SyncTimeOffsetPacket(offset));
        }
    }
}
