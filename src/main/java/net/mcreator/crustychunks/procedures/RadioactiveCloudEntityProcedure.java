package net.mcreator.crustychunks.procedures;

import java.util.Comparator;
import net.mcreator.crustychunks.init.CrustyChunksModMobEffects;
import net.mcreator.crustychunks.init.CrustyChunksModParticleTypes;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RadioactiveCloudEntityProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
      try {
      if (immediatesourceentity != null) {
         boolean found = false;
         double particleRadius = 0.0;
         double particleAmount = 0.0;
         double sx = 0.0;
         double sy = 0.0;
         double sz = 0.0;
         double xRadius = 0.0;
         double loop = 0.0;
         double zRadius = 0.0;
         double Groundatspot = 0.0;
         immediatesourceentity.setNoGravity(true);
         immediatesourceentity.noPhysics = true;
         immediatesourceentity.setDeltaMovement(new Vec3(0.0, 0.0, 0.0));
         if (!immediatesourceentity.getPersistentData().getBoolean("Used")) {
            for (int index0 = 0; index0 < 5; index0++) {
               world.addParticle(
                  (SimpleParticleType)CrustyChunksModParticleTypes.RADIOACTIVE_CLOUD.get(),
                  x + Mth.nextDouble(RandomSource.create(), -6.5, 7.5),
                  y + 1.0,
                  z + Mth.nextDouble(RandomSource.create(), -6.5, 7.5),
                  Mth.nextDouble(RandomSource.create(), -0.5, 0.5),
                  Mth.nextDouble(RandomSource.create(), -0.1, 0.3),
                  Mth.nextDouble(RandomSource.create(), -0.5, 0.5)
               );
            }

            immediatesourceentity.getPersistentData().putBoolean("Used", true);
         }

         if (immediatesourceentity.getPersistentData().getDouble("T") <= 200.0) {
            immediatesourceentity.getPersistentData().putDouble("T", immediatesourceentity.getPersistentData().getDouble("T") + 1.0);
            if (1 == Mth.nextInt(RandomSource.create(), 1, 20)) {
               Vec3 _center = new Vec3(x, y, z);

               for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(15.0), e -> true)
                  .stream()
                  .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
                  .toList()) {
                  if (entityiterator instanceof LivingEntity) {
                     immediatesourceentity.lookAt(Anchor.EYES, new Vec3(entityiterator.getX(), entityiterator.getY() + 1.0, entityiterator.getZ()));
                     if (Math.abs(entityiterator.getY() - y)
                              - (
                                 Math.abs(
                                       (double)immediatesourceentity.level()
                                             .clip(
                                                new ClipContext(
                                                   immediatesourceentity.getEyePosition(1.0F),
                                                   immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(8.0)),
                                                   Block.COLLIDER,
                                                   Fluid.ANY,
                                                   immediatesourceentity
                                                )
                                             )
                                             .getBlockPos()
                                             .getY()
                                          - y
                                    )
                                    + 0.5
                              )
                           <= 0.0
                        && Math.abs(entityiterator.getX() - x)
                              - (
                                 Math.abs(
                                       (double)immediatesourceentity.level()
                                             .clip(
                                                new ClipContext(
                                                   immediatesourceentity.getEyePosition(1.0F),
                                                   immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(17.0)),
                                                   Block.COLLIDER,
                                                   Fluid.ANY,
                                                   immediatesourceentity
                                                )
                                             )
                                             .getBlockPos()
                                             .getX()
                                          - x
                                    )
                                    + 0.5
                              )
                           <= 0.0
                        && Math.abs(entityiterator.getZ() - z)
                              - (
                                 Math.abs(
                                       (double)immediatesourceentity.level()
                                             .clip(
                                                new ClipContext(
                                                   immediatesourceentity.getEyePosition(1.0F),
                                                   immediatesourceentity.getEyePosition(1.0F).add(immediatesourceentity.getViewVector(1.0F).scale(17.0)),
                                                   Block.COLLIDER,
                                                   Fluid.ANY,
                                                   immediatesourceentity
                                                )
                                             )
                                             .getBlockPos()
                                             .getZ()
                                          - z
                                    )
                                    + 0.5
                              )
                           <= 0.0
                        && !(entityiterator instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY)
                           .is(ItemTags.create(ResourceLocation.parse("crusty_chunks:gasmask")))
                        && entityiterator instanceof LivingEntity) {
                        LivingEntity _entity = (LivingEntity)entityiterator;
                        if (!_entity.level().isClientSide()) {
                           _entity.addEffect(new MobEffectInstance(CrustyChunksModMobEffects.CONTAMINATED, 2400, 3, false, false));
                        }
                     }
                  }
               }
            }
         } else if (!immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
         }
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("RadioactiveCloudEntityProcedure.execute", _wtSafe);
      }
   }
}
