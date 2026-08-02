package shipwrights.genesis.hyperspace;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Per-player hyperspace state, stored on the player's persistent NBT so it
 * survives relogs mid-jump.
 *
 * A hyperdrive jump moves through three phases:
 * <ol>
 *   <li>{@code PHASE_SPOOL} — the drive charges in the origin dimension while the
 *       star-streak overlay builds up on the client.</li>
 *   <li>{@code PHASE_TRANSIT} — the player (and their ship) are inside the
 *       hyperspace dimension ({@code genesis:subspace}), physically flying toward
 *       the destination's mapped coordinates. No timer: arrival is by distance,
 *       and the crew can walk around the ship the whole way.</li>
 *   <li>{@code PHASE_EXITING} — arrival triggered; the flash rises for a few
 *       ticks, then the travel unit drops out into real space.</li>
 * </ol>
 * The legacy free cruise mode ({@code /hyperspace enter}) uses {@code PHASE_CRUISE}.
 */
public final class HyperspaceData {
    private static final String ACTIVE_KEY = "genesisHyperspaceActive";
    private static final String PHASE_KEY = "genesisHyperspacePhase";
    private static final String SPEED_KEY = "genesisHyperspaceSpeed";
    private static final String TICKS_KEY = "genesisHyperspaceTicks";
    private static final String DURATION_KEY = "genesisHyperspaceDuration";
    private static final String DESTINATION_KEY = "genesisHyperspaceDestination";
    private static final String SHIP_KEY = "genesisHyperspaceShip";
    private static final String SOURCE_X_KEY = "genesisHyperspaceSourceX";
    private static final String SOURCE_Y_KEY = "genesisHyperspaceSourceY";
    private static final String SOURCE_Z_KEY = "genesisHyperspaceSourceZ";

    public static final int PHASE_CRUISE = 0;
    public static final int PHASE_SPOOL = 1;
    public static final int PHASE_TRANSIT = 2;
    public static final int PHASE_EXITING = 3;

    public static final double DEFAULT_SPEED = 2.2;
    public static final int DEFAULT_DURATION_SECONDS = 0;
    /** Charge-up time in the origin dimension before the jump fires. */
    public static final int SPOOL_TICKS = 80;

    private static final String DRIVE_CLASS_KEY = "genesisHyperspaceDriveClass";
    /** Hyperdrive class: 6 is the slowest, 0.5 the fastest standard; 0.1 is the Falken's custom drive. */
    public static final double DEFAULT_DRIVE_CLASS = 2.0;
    public static final double MIN_DRIVE_CLASS = 0.1;
    public static final double MAX_DRIVE_CLASS = 6.0;

    private HyperspaceData() {
    }

    public static void enterCruise(ServerPlayer player, double speed, int durationSeconds) {
        CompoundTag tag = player.getPersistentData();
        clear(tag);
        tag.putBoolean(ACTIVE_KEY, true);
        tag.putInt(PHASE_KEY, PHASE_CRUISE);
        tag.putDouble(SPEED_KEY, speed);
        tag.putInt(TICKS_KEY, 0);
        tag.putInt(DURATION_KEY, Math.max(0, durationSeconds) * 20);
    }

    public static void beginSpool(ServerPlayer player, HyperspaceDestination destination, BlockPos source, int spoolTicks) {
        beginSpool(player, destination, source, spoolTicks, DEFAULT_DRIVE_CLASS);
    }

    public static void beginSpool(ServerPlayer player, HyperspaceDestination destination, BlockPos source, int spoolTicks, double driveClass) {
        CompoundTag tag = player.getPersistentData();
        clear(tag);
        tag.putBoolean(ACTIVE_KEY, true);
        tag.putInt(PHASE_KEY, PHASE_SPOOL);
        tag.putInt(TICKS_KEY, 0);
        tag.putInt(DURATION_KEY, Math.max(1, spoolTicks));
        tag.putString(DESTINATION_KEY, destination.id());
        tag.putDouble(DRIVE_CLASS_KEY, clampDriveClass(driveClass));
        tag.putInt(SOURCE_X_KEY, source.getX());
        tag.putInt(SOURCE_Y_KEY, source.getY());
        tag.putInt(SOURCE_Z_KEY, source.getZ());
    }

    public static double driveClass(ServerPlayer player) {
        double driveClass = player.getPersistentData().getDouble(DRIVE_CLASS_KEY);
        return driveClass > 0.0 ? driveClass : DEFAULT_DRIVE_CLASS;
    }

    public static double clampDriveClass(double driveClass) {
        return Math.max(MIN_DRIVE_CLASS, Math.min(MAX_DRIVE_CLASS, driveClass));
    }

    public static void beginTransit(ServerPlayer player, UUID shipId) {
        CompoundTag tag = player.getPersistentData();
        tag.putInt(PHASE_KEY, PHASE_TRANSIT);
        tag.putInt(TICKS_KEY, 0);
        tag.remove(DURATION_KEY);
        if (shipId != null) {
            tag.putUUID(SHIP_KEY, shipId);
        } else {
            tag.remove(SHIP_KEY);
        }
    }

    /** Arrival reached (or manual exit requested): flash up, then drop out. */
    public static void beginExiting(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        tag.putInt(PHASE_KEY, PHASE_EXITING);
        tag.putInt(TICKS_KEY, 0);
    }

    /** Manual exits drop out at the current position instead of a destination ring. */
    public static void clearDestination(ServerPlayer player) {
        player.getPersistentData().remove(DESTINATION_KEY);
    }

    public static void exit(ServerPlayer player) {
        clear(player.getPersistentData());
    }

    private static void clear(CompoundTag tag) {
        tag.remove(ACTIVE_KEY);
        tag.remove(PHASE_KEY);
        tag.remove(SPEED_KEY);
        tag.remove(TICKS_KEY);
        tag.remove(DURATION_KEY);
        tag.remove(DESTINATION_KEY);
        tag.remove(DRIVE_CLASS_KEY);
        tag.remove(SHIP_KEY);
        tag.remove(SOURCE_X_KEY);
        tag.remove(SOURCE_Y_KEY);
        tag.remove(SOURCE_Z_KEY);
    }

    public static boolean isActive(ServerPlayer player) {
        return player.getPersistentData().getBoolean(ACTIVE_KEY);
    }

    public static int phase(ServerPlayer player) {
        return player.getPersistentData().getInt(PHASE_KEY);
    }

    public static double speed(ServerPlayer player) {
        double speed = player.getPersistentData().getDouble(SPEED_KEY);
        return speed > 0.0 ? speed : DEFAULT_SPEED;
    }

    public static int ticks(ServerPlayer player) {
        return player.getPersistentData().getInt(TICKS_KEY);
    }

    public static int durationTicks(ServerPlayer player) {
        return player.getPersistentData().getInt(DURATION_KEY);
    }

    public static HyperspaceDestination destination(ServerPlayer player) {
        return HyperspaceDestination.byId(player.getPersistentData().getString(DESTINATION_KEY));
    }

    public static UUID shipId(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        return tag.hasUUID(SHIP_KEY) ? tag.getUUID(SHIP_KEY) : null;
    }

    public static BlockPos sourceBlock(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        if (!tag.contains(SOURCE_X_KEY) || !tag.contains(SOURCE_Y_KEY) || !tag.contains(SOURCE_Z_KEY)) {
            return null;
        }
        return new BlockPos(tag.getInt(SOURCE_X_KEY), tag.getInt(SOURCE_Y_KEY), tag.getInt(SOURCE_Z_KEY));
    }

    public static int advance(ServerPlayer player) {
        int ticks = ticks(player) + 1;
        player.getPersistentData().putInt(TICKS_KEY, ticks);
        return ticks;
    }
}
