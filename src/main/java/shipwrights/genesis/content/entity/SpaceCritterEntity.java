package shipwrights.genesis.content.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * A small alien crawler — the wildlife of the barren worlds. It roams the
 * surface, keeps to itself, and bolts when hurt. One class backs both the
 * moon's and Mercury's creatures; the two are told apart by their entity type
 * (and so by their texture).
 */
public class SpaceCritterEntity extends PathfinderMob {
    public SpaceCritterEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    /**
     * Permissive placement: alien fauna is not bound by torch-light rules, so it
     * populates the surface by day as well as night. It only needs solid footing
     * and headroom.
     */
    public static boolean canSpawnHere(EntityType<? extends PathfinderMob> type, ServerLevelAccessor level,
                                       MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).blocksMotion()
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir();
    }
}
