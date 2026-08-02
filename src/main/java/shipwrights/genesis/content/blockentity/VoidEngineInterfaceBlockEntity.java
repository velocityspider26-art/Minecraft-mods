package shipwrights.genesis.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.block.VoidCoreBlock;
import shipwrights.genesis.networking.*;

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

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.deserializeNBT(registries, tag.get("energy"));
        chargeUpTicks = tag.getInt("chargeUpTicks");
        active = tag.getBoolean("active");
        returningDim = ResourceLocation.parse(tag.getString("returningDim"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
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
                    BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
                    if (neighbor != null) {
                        IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(direction), direction.getOpposite());
                        if (energy != null) {
                            int toExtract = Math.min(128, voidEngineInterface.energyStorage.getMaxEnergyStored() - voidEngineInterface.energyStorage.getEnergyStored());
                            int extracted = energy.extractEnergy(toExtract, false);
                            voidEngineInterface.energyStorage.receiveEnergy(extracted, false);
                        }
                    }
                }
            }

            //GenesisEvents.message = Component.literal("chargeUpTicks = " + voidEngineInterface.chargeUpTicks + " active = " + voidEngineInterface.active);

            BlockState core = level.getBlockState(pos.offset(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getNormal().multiply(-1)));
            if (core.hasProperty(VoidCoreBlock.DORMANT) && !core.getValue(VoidCoreBlock.DORMANT)) {
                Vec3 center = pos.getCenter();
                boolean isPowered = level.getBlockState(pos).hasProperty(BlockStateProperties.POWERED) && level.getBlockState(pos).getValue(BlockStateProperties.POWERED);
                boolean hasEnergy = voidEngineInterface.energyStorage.getEnergyStored() >= ENERGY_PER_TICK;

                if (isPowered && hasEnergy) {
                    voidEngineInterface.energyStorage.extractEnergy(ENERGY_PER_TICK, false);

                    if (!voidEngineInterface.active && voidEngineInterface.chargeUpTicks >= 0) {
                        voidEngineInterface.active = true;
                        GenesisMod.LOGGER.info("Current dimension id: {}", level.dimension().location());
                        if (!level.dimension().location().equals(GenesisMod.WORMHOLE_DIM)) {
                            GenesisNetworking.sendToAll(new StopVoidEngineStartSoundPacket());
                            GenesisNetworking.sendToAll(new VoidEngineSoundPacket(pos));
                        }
                    }

                    if (!level.dimension().location().equals(GenesisMod.WORMHOLE_DIM) && level.getServer() != null) {
                        voidEngineInterface.chargeUpTicks++;

                        // Check if we should teleport to wormhole dimension
                        if (voidEngineInterface.chargeUpTicks == 244) {
                            if (!level.dimension().location().equals(ResourceLocation.fromNamespaceAndPath("genesis", "great_unknown"))) {
                                if (level.getBlockState(pos).hasProperty(BlockStateProperties.POWERED) && level.getBlockState(pos).getValue(BlockStateProperties.POWERED)) {
                                    explode(level, center);
                                }
                            } else {
                                voidEngineInterface.chargeUpTicks = 2;
                                voidEngineInterface.returningDim = level.dimension().location();
                                GenesisMod.LOGGER.warn("Void Engine ship warp is disabled until Genesis is rebuilt on Sable sub-level teleport APIs.");
                                return;
                            }
                        }
                    } else {
                        voidEngineInterface.chargeUpTicks = 32;
                    }
                } else {
                    GenesisNetworking.sendToAll(new StopVoidEngineStartSoundPacket());
                    if (voidEngineInterface.chargeUpTicks > 0) {
                        voidEngineInterface.chargeUpTicks--;
                    }
                    if (level.dimension().location().equals(GenesisMod.WORMHOLE_DIM) && level.getServer() != null) {
                        if (voidEngineInterface.chargeUpTicks <= 0) {
                            voidEngineInterface.chargeUpTicks = -64;
                            GenesisMod.LOGGER.warn("Void Engine wormhole return is disabled until Genesis is rebuilt on Sable sub-level teleport APIs.");
                        }
                    } else {
                        if (voidEngineInterface.chargeUpTicks > 0) {
                            voidEngineInterface.chargeUpTicks = 0;
                            GenesisNetworking.sendToAll(new StopVoidEngineStartSoundPacket());
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

    private static void explode(Level level, Vec3 center) {
        level.explode(null, center.x, center.y, center.z, 16f, Level.ExplosionInteraction.BLOCK);
    }
}
