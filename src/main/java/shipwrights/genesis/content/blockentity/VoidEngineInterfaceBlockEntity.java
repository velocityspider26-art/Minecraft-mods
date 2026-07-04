package shipwrights.genesis.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.compat.aeronautics.AeronauticsContraptionLookup;
import shipwrights.genesis.content.block.VoidCoreBlock;
import shipwrights.genesis.networking.*;
import shipwrights.genesis.teleportation.DimensionTravelTeleporter;
import shipwrights.genesis.teleportation.TravelDirection;

public class VoidEngineInterfaceBlockEntity extends BlockEntity {
    private static final int MAX_ENERGY = 8192;
    private static final int ENERGY_PER_TICK = 512;
    private final EnergyStorage energyStorage = new EnergyStorage(MAX_ENERGY);

    public VoidEngineInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(GenesisBlockEntities.VOID_ENGINE_INTERFACE.get(), pos, state);
    }

    private int chargeUpTicks = 0;
    boolean active = false;
    private ResourceLocation returningDim = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    /** Exposed to the block-entity energy capability (registered in {@link GenesisBlockEntities}). */
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("energy")) {
            energyStorage.deserializeNBT(registries, tag.get("energy"));
        }
        chargeUpTicks = tag.getInt("chargeUpTicks");
        active = tag.getBoolean("active");
        returningDim = ResourceLocation.parse(tag.getString("returningDim"));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energyStorage.serializeNBT(registries));
        tag.putInt("chargeUpTicks", chargeUpTicks);
        tag.putBoolean("active", active);
        tag.putString("returningDim", returningDim.toString());
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        if (blockEntity instanceof VoidEngineInterfaceBlockEntity voidEngineInterface) {
            // Try to receive energy from adjacent blocks
            if (voidEngineInterface.energyStorage.getEnergyStored() < voidEngineInterface.energyStorage.getMaxEnergyStored()) {
                for (Direction direction : Direction.values()) {
                    IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(direction), direction.getOpposite());
                    if (neighbor != null && neighbor.canExtract()) {
                        int toExtract = Math.min(128, voidEngineInterface.energyStorage.getMaxEnergyStored() - voidEngineInterface.energyStorage.getEnergyStored());
                        int extracted = neighbor.extractEnergy(toExtract, false);
                        voidEngineInterface.energyStorage.receiveEnergy(extracted, false);
                    }
                }
            }

            BlockState core = level.getBlockState(pos.offset(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getNormal().multiply(-1)));
            if (core.hasProperty(VoidCoreBlock.DORMANT) && !core.getValue(VoidCoreBlock.DORMANT)) {
                AeronauticsConstruct construct = AeronauticsContraptionLookup.getConstructManaging(level, pos);

                Vec3 center = pos.getCenter();
                boolean isPowered = level.getBlockState(pos).hasProperty(BlockStateProperties.POWERED) && level.getBlockState(pos).getValue(BlockStateProperties.POWERED);
                boolean hasEnergy = voidEngineInterface.energyStorage.getEnergyStored() >= ENERGY_PER_TICK;

                if (isPowered && hasEnergy) {
                    voidEngineInterface.energyStorage.extractEnergy(ENERGY_PER_TICK, false);

                    if (!voidEngineInterface.active && voidEngineInterface.chargeUpTicks >= 0) {
                        voidEngineInterface.active = true;
                        GenesisMod.LOGGER.info("Current dimension id: {}", level.dimension().location());
                        if (!level.dimension().location().equals(GenesisMod.WORMHOLE_DIM)) {
                            GenesisNetworking.sendToAll(StopVoidEngineStartSoundPacket.INSTANCE);
                            GenesisNetworking.sendToAll(new VoidEngineSoundPacket(pos));
                        }
                    }

                    if (!level.dimension().location().equals(GenesisMod.WORMHOLE_DIM) && level.getServer() != null) {
                        voidEngineInterface.chargeUpTicks++;

                        // Check if we should teleport to wormhole dimension
                        if (voidEngineInterface.chargeUpTicks == 244) {
                            if (construct == null || !level.dimension().location().equals(ResourceLocation.fromNamespaceAndPath("genesis", "great_unknown"))) {
                                if (level.getBlockState(pos).hasProperty(BlockStateProperties.POWERED) && level.getBlockState(pos).getValue(BlockStateProperties.POWERED)) {
                                    explode(level, center);
                                }
                            } else {
                                voidEngineInterface.chargeUpTicks = 2;

                                // Save current dimension for return
                                voidEngineInterface.returningDim = level.dimension().location();

                                // Get wormhole level
                                ServerLevel wormholeLevel = level.getServer().getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, GenesisMod.WORMHOLE_DIM));
                                if (wormholeLevel != null) {
                                    GenesisNetworking.sendToChunk(level.getChunkAt(pos), EnteringWarpPacket.INSTANCE);

                                    Vector3dc targetPos = construct.positionInWorld().mul(1 / 32.0, new Vector3d());
                                    DimensionTravelTeleporter.teleportConstruct(construct, TravelDirection.PLANET_TO_SPACE, (ServerLevel) level, wormholeLevel, targetPos, new Quaterniond());

                                    GenesisNetworking.sendToAll(new WormholeTravelSoundPacket(pos));
                                }
                                return;
                            }
                        }
                    } else {
                        voidEngineInterface.chargeUpTicks = 32;
                    }
                } else {
                    GenesisNetworking.sendToAll(StopVoidEngineStartSoundPacket.INSTANCE);
                    if (voidEngineInterface.chargeUpTicks > 0) {
                        voidEngineInterface.chargeUpTicks--;
                    }
                    if (level.dimension().location().equals(GenesisMod.WORMHOLE_DIM) && level.getServer() != null) {
                        if (voidEngineInterface.chargeUpTicks <= 0) {
                            voidEngineInterface.chargeUpTicks = -64;
                            // Auto-return to saved dimension when in wormhole
                            ServerLevel returnLevel = level.getServer().getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, voidEngineInterface.returningDim));
                            returnFromWormhole(level, pos, returnLevel, construct, false);
                        }
                    } else {
                        if (voidEngineInterface.chargeUpTicks > 0) {
                            voidEngineInterface.chargeUpTicks = 0;
                            GenesisNetworking.sendToAll(StopVoidEngineStartSoundPacket.INSTANCE);
                        }
                    }
                }
            }
            if (voidEngineInterface.chargeUpTicks < 0) {
                voidEngineInterface.chargeUpTicks++;
            }
            if (voidEngineInterface.active) {
                 if (voidEngineInterface.chargeUpTicks == 0) {
                    voidEngineInterface.active = false;
                }
            }
        }
    }

    public static void returnFromWormhole(Level level, BlockPos pos, ServerLevel returnLevel, AeronauticsConstruct construct, boolean unstable) {
        if (construct != null && returnLevel != null) {
            // Teleport construct back - scale position up
            Vector3dc targetPos = construct.positionInWorld().mul(32.0, new Vector3d());

            GenesisNetworking.sendToChunk(level.getChunkAt(pos), EnteringWarpPacket.INSTANCE);

            GenesisNetworking.sendToAll(new WormholeTravelSoundPacket(pos));
            DimensionTravelTeleporter.teleportConstruct(construct, TravelDirection.SPACE_TO_PLANET, (ServerLevel) level, returnLevel, targetPos, new Quaterniond());
            GenesisNetworking.sendToAll(new WormholeTravelSoundPacket(pos));
            if (unstable) {
                Vec3 center = new Vec3(targetPos.x(), targetPos.y(), targetPos.z());
                explode(returnLevel, center);
            }
        }
    }

    private static void explode(Level level, Vec3 center) {
        level.explode(null, center.x, center.y, center.z, 16f, Level.ExplosionInteraction.BLOCK);
    }
}
