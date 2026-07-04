package shipwrights.genesis.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.compat.aeronautics.AeronauticsContraptionLookup;
import shipwrights.genesis.content.block.RadarDisplayBlock;
import shipwrights.genesis.content.radar.RadarDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
            AeronauticsConstruct construct = AeronauticsContraptionLookup.getConstructManaging(level, getBlockPos());
            Vec3i normalShip = state.getValue(RadarDisplayBlock.FACING).getOpposite().getNormal();

            Vector3d pos = new Vector3d(getBlockPos().getCenter().x, getBlockPos().getCenter().y, getBlockPos().getCenter().z);
            Vector3d dir = new Vector3d(normalShip.getX(), normalShip.getY(), normalShip.getZ());
            Vector3d up = getUpVectorForFacing(state.getValue(RadarDisplayBlock.FACING));
            List<UUID> excludedConstructs = new ArrayList<>(1);

            if (construct != null) {
                construct.toWorld(new Vector3d(pos), pos);
                construct.dirToWorld(new Vector3d(dir), dir);
                construct.dirToWorld(new Vector3d(up), up);
                excludedConstructs.add(construct.id());
            }

            display.scan(level, pos, dir, up, excludedConstructs);
        }
    }

    private Vector3d getUpVectorForFacing(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case NORTH, SOUTH, EAST, WEST -> new Vector3d(0, 1, 0);  // Horizontal facings use world up
            case UP -> new Vector3d(0, 0, -1);    // When facing up, north is "up" on screen
            case DOWN -> new Vector3d(0, 0, 1);   // When facing down, south is "up" on screen
        };
    }
}
