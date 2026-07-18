package net.mcreator.crustychunks.block;

import net.mcreator.crustychunks.compat.WariumSafety;
import net.mcreator.crustychunks.entity.BlockBusterProjectileEntity;
import net.mcreator.crustychunks.entity.BunkerBusterProjectileEntity;
import net.mcreator.crustychunks.entity.IRMissileEntity;
import net.mcreator.crustychunks.entity.LargeBombProjectileEntity;
import net.mcreator.crustychunks.entity.LargeRadarMissileProjectileEntity;
import net.mcreator.crustychunks.entity.LargeTorpedoEntity;
import net.mcreator.crustychunks.entity.NuclearBombProjectileEntity;
import net.mcreator.crustychunks.entity.OrdinanceFusionBombProjectileEntity;
import net.mcreator.crustychunks.entity.SuperLargeBombProjectileEntity;
import net.mcreator.crustychunks.entity.TorpedoEntity;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Placeholder launcher for the aircraft-only munitions that previously required
 * VS Warium (plane-mounted ordinance). Right-click cycles the selected munition
 * (sneak-click cycles backwards); a redstone pulse fires it in the facing
 * direction. The spawned projectiles use their normal flight/guidance/detonation
 * procedures, so radar and heat seekers still track targets designated through
 * the radar and laser systems.
 */
public class MunitionLauncherBlock extends Block {
   public static final DirectionProperty FACING = BlockStateProperties.FACING;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final EnumProperty<Munition> MUNITION = EnumProperty.create("munition", Munition.class);

   public enum Munition implements StringRepresentable {
      LARGE_BOMB("large_bomb", "Large Bomb", 1.2),
      SUPER_LARGE_BOMB("super_large_bomb", "Super Large Bomb", 1.2),
      NUCLEAR_BOMB("nuclear_bomb", "Nuclear Bomb", 1.0),
      FUSION_BOMB("fusion_bomb", "Fusion Ordinance Bomb", 1.0),
      TORPEDO("torpedo", "Torpedo", 2.0),
      LARGE_TORPEDO("large_torpedo", "Large Torpedo", 2.0),
      BUNKER_BUSTER("bunker_buster", "Bunker Buster", 2.5),
      BLOCK_BUSTER("block_buster", "Block Buster", 2.5),
      IR_MISSILE("ir_missile", "IR Missile", 3.5),
      LARGE_RADAR_MISSILE("large_radar_missile", "Large Radar Missile", 3.5);

      private final String id;
      public final String display;
      public final double speed;

      Munition(String id, String display, double speed) {
         this.id = id;
         this.display = display;
         this.speed = speed;
      }

      @Override
      public String getSerializedName() {
         return this.id;
      }

      public Munition next(boolean backwards) {
         Munition[] all = values();
         int i = (this.ordinal() + (backwards ? all.length - 1 : 1)) % all.length;
         return all[i];
      }
   }

   public MunitionLauncherBlock() {
      super(Properties.of().sound(SoundType.METAL).strength(3.0F, 10.0F).requiresCorrectToolForDrops());
      this.registerDefaultState(
         this.stateDefinition.any().setValue(FACING, Direction.UP).setValue(POWERED, false).setValue(MUNITION, Munition.LARGE_BOMB)
      );
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, POWERED, MUNITION);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
   }

   @Override
   public BlockState rotate(BlockState state, Rotation rot) {
      return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
   }

   @Override
   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
   }

   @Override
   public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
      Munition selected = blockstate.getValue(MUNITION).next(entity.isShiftKeyDown());
      if (!world.isClientSide()) {
         world.setBlock(pos, blockstate.setValue(MUNITION, selected), 3);
         entity.displayClientMessage(Component.literal("Munition: " + selected.display), true);
      }
      return InteractionResult.sidedSuccess(world.isClientSide());
   }

   @Override
   public void neighborChanged(BlockState blockstate, Level world, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean moving) {
      super.neighborChanged(blockstate, world, pos, neighborBlock, fromPos, moving);
      if (world.isClientSide())
         return;
      boolean powered = world.hasNeighborSignal(pos);
      boolean wasPowered = blockstate.getValue(POWERED);
      if (powered && !wasPowered)
         launch(blockstate, world, pos);
      if (powered != wasPowered)
         world.setBlock(pos, blockstate.setValue(POWERED, powered), 3);
   }

   private static void launch(BlockState blockstate, Level world, BlockPos pos) {
      try {
         Direction facing = blockstate.getValue(FACING);
         Munition munition = blockstate.getValue(MUNITION);
         Vec3 origin = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(1.1));
         Entity projectile = create(munition, world, origin);
         if (projectile == null)
            return;
         Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal());
         projectile.setDeltaMovement(dir.scale(munition.speed));
         projectile.setYRot((float) (Math.atan2(-dir.x, dir.z) * (180.0 / Math.PI)));
         projectile.setXRot((float) (-Math.asin(dir.y) * (180.0 / Math.PI)));
         world.addFreshEntity(projectile);
         world.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 3.0F, 0.6F);
      } catch (Throwable t) {
         WariumSafety.report("MunitionLauncherBlock.launch", t);
      }
   }

   @SuppressWarnings("unchecked")
   private static Entity create(Munition munition, Level world, Vec3 at) {
      return switch (munition) {
         case LARGE_BOMB -> new LargeBombProjectileEntity(
            (EntityType<? extends LargeBombProjectileEntity>) CrustyChunksModEntities.LARGE_BOMB_PROJECTILE.get(), at.x, at.y, at.z, world);
         case SUPER_LARGE_BOMB -> new SuperLargeBombProjectileEntity(
            (EntityType<? extends SuperLargeBombProjectileEntity>) CrustyChunksModEntities.SUPER_LARGE_BOMB_PROJECTILE.get(), at.x, at.y, at.z, world);
         case NUCLEAR_BOMB -> new NuclearBombProjectileEntity(
            (EntityType<? extends NuclearBombProjectileEntity>) CrustyChunksModEntities.NUCLEAR_BOMB_PROJECTILE.get(), at.x, at.y, at.z, world);
         case FUSION_BOMB -> new OrdinanceFusionBombProjectileEntity(
            (EntityType<? extends OrdinanceFusionBombProjectileEntity>) CrustyChunksModEntities.ORDINANCE_FUSION_BOMB_PROJECTILE.get(), at.x, at.y, at.z, world);
         case TORPEDO -> new TorpedoEntity(
            (EntityType<? extends TorpedoEntity>) CrustyChunksModEntities.TORPEDO.get(), at.x, at.y, at.z, world);
         case LARGE_TORPEDO -> new LargeTorpedoEntity(
            (EntityType<? extends LargeTorpedoEntity>) CrustyChunksModEntities.LARGE_TORPEDO.get(), at.x, at.y, at.z, world);
         case BUNKER_BUSTER -> new BunkerBusterProjectileEntity(
            (EntityType<? extends BunkerBusterProjectileEntity>) CrustyChunksModEntities.BUNKER_BUSTER_PROJECTILE.get(), at.x, at.y, at.z, world);
         case BLOCK_BUSTER -> new BlockBusterProjectileEntity(
            (EntityType<? extends BlockBusterProjectileEntity>) CrustyChunksModEntities.BLOCK_BUSTER_PROJECTILE.get(), at.x, at.y, at.z, world);
         case IR_MISSILE -> new IRMissileEntity(
            (EntityType<? extends IRMissileEntity>) CrustyChunksModEntities.IR_MISSILE.get(), at.x, at.y, at.z, world);
         case LARGE_RADAR_MISSILE -> new LargeRadarMissileProjectileEntity(
            (EntityType<? extends LargeRadarMissileProjectileEntity>) CrustyChunksModEntities.LARGE_RADAR_MISSILE_PROJECTILE.get(), at.x, at.y, at.z, world);
      };
   }
}
