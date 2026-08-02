package shipwrights.genesis.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import shipwrights.genesis.content.block.RadarDisplayBlock;
import shipwrights.genesis.content.radar.RadarDisplay;

import java.util.List;

public class RadarDisplayBlockEntity extends BlockEntity {

    public RadarDisplay display = new RadarDisplay(64);

    public RadarDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(GenesisBlockEntities.RADAR_DISPLAY.get(), pos, state);
    }

    public double[][] getDisplayableData() {
        return display.data;
    }

    public void clientTick() {
        if (level == null) return;
        BlockState state = level.getBlockState(getBlockPos());
        if (state.getBlock() instanceof RadarDisplayBlock) {
            Vec3i normalShip = state.getValue(RadarDisplayBlock.FACING).getOpposite().getNormal();

            Vector3d pos = new Vector3d(getBlockPos().getCenter().x, getBlockPos().getCenter().y, getBlockPos().getCenter().z);
            Vector3d dir = new Vector3d(normalShip.getX(), normalShip.getY(), normalShip.getZ());
            Vector3d up = getUpVectorForFacing(state.getValue(RadarDisplayBlock.FACING));

            display.scan(level, pos, dir, up, List.of());
        }
    }

    private Vector3d getUpVectorForFacing(net.minecraft.core.Direction facing) {
        // Return the up vector for the radar display based on facing direction
        // This matches the rotation logic in the renderer
        return switch (facing) {
            case NORTH, SOUTH, EAST, WEST -> new Vector3d(0, 1, 0);  // Horizontal facings use world up
            case UP -> new Vector3d(0, 0, -1);    // When facing up, north is "up" on screen
            case DOWN -> new Vector3d(0, 0, 1);   // When facing down, south is "up" on screen
        };
    }
}
