package shipwrights.genesis.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.stream.Stream;

/**
 * Only lets a feature generate near a world's poles — far enough north or south
 * (high |Z|) that it stands in for the polar regions. Mercury's water ice only
 * survives in the permanently-shadowed craters at its poles, so its ice patches
 * are gated on this.
 */
public class PolarPlacement extends PlacementModifier {
    public static final PolarPlacement INSTANCE = new PolarPlacement();
    public static final MapCodec<PolarPlacement> CODEC = MapCodec.unit(INSTANCE);

    /** How far from the equator (Z=0) counts as polar, in blocks. */
    private static final int POLE_DISTANCE = 1500;

    private PolarPlacement() {
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        return Math.abs(pos.getZ()) >= POLE_DISTANCE ? Stream.of(pos) : Stream.empty();
    }

    @Override
    public PlacementModifierType<?> type() {
        return GenesisPlacementModifiers.POLAR.get();
    }
}
