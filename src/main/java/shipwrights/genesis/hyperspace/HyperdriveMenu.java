package shipwrights.genesis.hyperspace;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import shipwrights.genesis.content.block.GenesisBlocks;

public class HyperdriveMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private int jumpTicks;
    private int jumpDuration;
    private int destinationIndex = -1;

    public HyperdriveMenu(int windowId, Player player, BlockPos pos) {
        super(GenesisBlocks.HYPERDRIVE_MENU.get(), windowId);
        this.pos = pos;

        if (player.level().getBlockEntity(pos) instanceof HyperdriveBlockEntity hyperdrive) {
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return hyperdrive.jumpTicks();
                }

                @Override
                public void set(int value) {
                    HyperdriveMenu.this.jumpTicks = value;
                }
            });
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return hyperdrive.jumpDuration();
                }

                @Override
                public void set(int value) {
                    HyperdriveMenu.this.jumpDuration = value;
                }
            });
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return hyperdrive.destinationIndex();
                }

                @Override
                public void set(int value) {
                    HyperdriveMenu.this.destinationIndex = value;
                }
            });
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        HyperspaceDestination destination = HyperspaceDestination.byButtonId(id);
        if (destination == null || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        if (player.level().getBlockEntity(pos) instanceof HyperdriveBlockEntity hyperdrive) {
            return hyperdrive.startJump(serverPlayer, destination);
        }
        return false;
    }

    public boolean isJumping() {
        return jumpDuration > 0 && jumpTicks < jumpDuration;
    }

    public float progress() {
        if (jumpDuration <= 0) {
            return 0.0f;
        }
        return Math.min(1.0f, jumpTicks / (float) jumpDuration);
    }

    public int jumpTicks() {
        return jumpTicks;
    }

    public int jumpDuration() {
        return jumpDuration;
    }

    public String destinationName() {
        HyperspaceDestination[] destinations = HyperspaceDestination.values();
        if (destinationIndex < 0 || destinationIndex >= destinations.length) {
            return "Moon";
        }
        return destinations[destinationIndex].displayName();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        // Any hyperdrive class block keeps the menu open — validating against
        // the single legacy block instantly closed the GUI on class variants.
        return player.level().getBlockState(pos).getBlock() instanceof HyperdriveBlock
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
