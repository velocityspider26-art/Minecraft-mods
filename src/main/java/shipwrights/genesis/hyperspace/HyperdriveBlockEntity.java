package shipwrights.genesis.hyperspace;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import shipwrights.genesis.content.blockentity.GenesisBlockEntities;
import shipwrights.genesis.networking.GenesisNetworking;
import shipwrights.genesis.networking.HyperspaceStatePacket;
import shipwrights.genesis.teleportation.SpaceTravelManager;

import java.util.List;
import java.util.UUID;

public class HyperdriveBlockEntity extends BlockEntity {
    private static final String JUMP_TICKS_TAG = "JumpTicks";
    private static final String JUMP_DURATION_TAG = "JumpDuration";
    private static final String DESTINATION_TAG = "Destination";
    private static final String ACTIVE_PLAYER_TAG = "ActivePlayer";
    private static final String ACTIVE_PLAYER_NAME_TAG = "ActivePlayerName";
    private static final String DRIVE_CLASS_TAG = "DriveClass";

    private int jumpTicks;
    private int jumpDuration;
    private HyperspaceDestination destination;
    private UUID activePlayer;
    private String activePlayerName = "";
    /** Hyperdrive class: 6 slowest .. 0.5 fastest; 0.1 is the Falken's custom drive. */
    private double driveClass = HyperspaceData.DEFAULT_DRIVE_CLASS;

    public HyperdriveBlockEntity(BlockPos pos, BlockState state) {
        super(GenesisBlockEntities.HYPERDRIVE.get(), pos, state);
        if (state.getBlock() instanceof HyperdriveBlock hyperdrive) {
            this.driveClass = hyperdrive.driveClass();
        }
    }

    public boolean startJump(ServerPlayer player, HyperspaceDestination destination) {
        if (destination == null || level == null) {
            return false;
        }

        if (isJumping()) {
            player.displayClientMessage(Component.literal("Hyperdrive already spooling for " + destinationName()), true);
            return false;
        }

        if (player.server.getLevel(destination.dimension()) == null) {
            player.displayClientMessage(Component.literal("Destination dimension is not loaded: " + destination.dimension().location()), true);
            return false;
        }

        if (HyperspaceData.isActive(player)) {
            player.displayClientMessage(Component.literal("You are already in hyperspace"), true);
            return false;
        }

        this.destination = destination;
        this.jumpTicks = 0;
        // Transit is real flight now (no timer), so the progress bar covers the
        // spool-up; once in hyperspace the ship flies itself to the target.
        this.jumpDuration = HyperspaceData.SPOOL_TICKS;
        this.activePlayer = player.getUUID();
        this.activePlayerName = player.getName().getString();

        HyperspaceData.beginSpool(player, destination, worldPosition, HyperspaceData.SPOOL_TICKS, driveClass);
        sendToCrew(player, HyperspaceStatePacket.preJump(HyperspaceData.SPOOL_TICKS));
        setPowered(true);
        setChanged();

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0f, 0.75f);
        }

        player.displayClientMessage(Component.literal(
                String.format("Class %.1f hyperdrive locked: %s", driveClass, destination.displayName())), true);
        return true;
    }

    private void sendToCrew(ServerPlayer pilot, HyperspaceStatePacket packet) {
        List<ServerPlayer> crew = List.of();
        if (level instanceof ServerLevel serverLevel) {
            ServerSubLevel ship = SpaceTravelManager.findShipContaining(serverLevel, worldPosition);
            if (ship != null) {
                crew = SpaceTravelManager.crewPlayers(serverLevel, ship);
            }
        }
        for (ServerPlayer member : crew) {
            GenesisNetworking.sendToPlayer(member, packet);
        }
        if (!crew.contains(pilot)) {
            GenesisNetworking.sendToPlayer(pilot, packet);
        }
    }

    public double driveClass() {
        return driveClass;
    }

    public void setDriveClass(double driveClass) {
        this.driveClass = HyperspaceData.clampDriveClass(driveClass);
        setChanged();
    }

    public void tickServer() {
        if (!isJumping()) {
            return;
        }

        jumpTicks++;
        if (jumpTicks >= jumpDuration + 12) {
            clearJump();
        } else {
            setChanged();
        }
    }

    public boolean isJumping() {
        return destination != null && jumpDuration > 0 && jumpTicks < jumpDuration + 12;
    }

    public int jumpTicks() {
        return jumpTicks;
    }

    public int jumpDuration() {
        return jumpDuration;
    }

    public int destinationIndex() {
        return destination == null ? -1 : destination.ordinal();
    }

    public String destinationName() {
        return destination == null ? "None" : destination.displayName();
    }

    public String activePlayerName() {
        return activePlayerName == null || activePlayerName.isBlank() ? "No pilot" : activePlayerName;
    }

    private void clearJump() {
        jumpTicks = 0;
        jumpDuration = 0;
        destination = null;
        activePlayer = null;
        activePlayerName = "";
        setPowered(false);
        setChanged();
    }

    private void setPowered(boolean powered) {
        if (level == null || !getBlockState().hasProperty(BlockStateProperties.POWERED)) {
            return;
        }

        if (getBlockState().getValue(BlockStateProperties.POWERED) != powered) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(BlockStateProperties.POWERED, powered));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(JUMP_TICKS_TAG, jumpTicks);
        tag.putInt(JUMP_DURATION_TAG, jumpDuration);
        if (destination != null) {
            tag.putString(DESTINATION_TAG, destination.id());
        }
        if (activePlayer != null) {
            tag.putUUID(ACTIVE_PLAYER_TAG, activePlayer);
        }
        tag.putString(ACTIVE_PLAYER_NAME_TAG, activePlayerName);
        tag.putDouble(DRIVE_CLASS_TAG, driveClass);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        jumpTicks = tag.getInt(JUMP_TICKS_TAG);
        jumpDuration = tag.getInt(JUMP_DURATION_TAG);
        destination = HyperspaceDestination.byId(tag.getString(DESTINATION_TAG));
        activePlayer = tag.hasUUID(ACTIVE_PLAYER_TAG) ? tag.getUUID(ACTIVE_PLAYER_TAG) : null;
        activePlayerName = tag.getString(ACTIVE_PLAYER_NAME_TAG);
        // Saved NBT (e.g. a command override) wins; otherwise keep the class
        // the block variant assigned in the constructor.
        double savedClass = tag.getDouble(DRIVE_CLASS_TAG);
        if (savedClass > 0.0) {
            driveClass = HyperspaceData.clampDriveClass(savedClass);
        }
    }
}
