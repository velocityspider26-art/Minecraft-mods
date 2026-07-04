package net.mcreator.crustychunks.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.mcreator.crustychunks.init.CrustyChunksModEntities;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.mcreator.crustychunks.procedures.AIArmorBypassProcedure;
import net.mcreator.crustychunks.procedures.BulletHitProcedure;
import net.mcreator.crustychunks.procedures.DespawningBulletProcedure;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(
   value = Dist.CLIENT,
   _interface = ItemSupplier.class
)
public class MediumAIBulletEntity extends AbstractArrow implements ItemSupplier {
   public static final ItemStack PROJECTILE_ITEM = new ItemStack((ItemLike)CrustyChunksModItems.TINYPROJECTILE_ITEM.get());

   public MediumAIBulletEntity(EntityType<? extends MediumAIBulletEntity> type, Level world) {
      super(type, world);
   }

   public MediumAIBulletEntity(EntityType<? extends MediumAIBulletEntity> type, double x, double y, double z, Level world) {
      super(type, x, y, z, world, PROJECTILE_ITEM.copy(), null);
   }

   public MediumAIBulletEntity(EntityType<? extends MediumAIBulletEntity> type, LivingEntity entity, Level world) {
      super(type, entity, world, PROJECTILE_ITEM.copy(), null);
   }

   @OnlyIn(Dist.CLIENT)
   public ItemStack getItem() {
      return PROJECTILE_ITEM;
   }

   protected ItemStack getDefaultPickupItem() {
      return PROJECTILE_ITEM;
   }

   protected void doPostHurtEffects(LivingEntity entity) {
      super.doPostHurtEffects(entity);
      entity.setArrowCount(entity.getArrowCount() - 1);
   }

   public void onHitEntity(EntityHitResult entityHitResult) {
      super.onHitEntity(entityHitResult);
      AIArmorBypassProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), entityHitResult.getEntity(), this);
   }

   public void onHitBlock(BlockHitResult blockHitResult) {
      super.onHitBlock(blockHitResult);
      BulletHitProcedure.execute(
         this.level(),
         (double)blockHitResult.getBlockPos().getX(),
         (double)blockHitResult.getBlockPos().getY(),
         (double)blockHitResult.getBlockPos().getZ(),
         this
      );
   }

   public void tick() {
      super.tick();
      DespawningBulletProcedure.execute(this.level(), this);
      if (this.inGround) {
         this.discard();
      }
   }

   public static MediumAIBulletEntity shoot(Level world, LivingEntity entity, RandomSource source) {
      return shoot(world, entity, source, 7.0F, 1.5, 0);
   }

   public static MediumAIBulletEntity shoot(Level world, LivingEntity entity, RandomSource source, float pullingPower) {
      return shoot(world, entity, source, pullingPower * 7.0F, 1.5, 0);
   }

   public static MediumAIBulletEntity shoot(Level world, LivingEntity entity, RandomSource random, float power, double damage, int knockback) {
      MediumAIBulletEntity entityarrow = new MediumAIBulletEntity(
         (EntityType<? extends MediumAIBulletEntity>)CrustyChunksModEntities.MEDIUM_AI_BULLET.get(), entity, world
      );
      entityarrow.shoot(entity.getViewVector(1.0F).x, entity.getViewVector(1.0F).y, entity.getViewVector(1.0F).z, power * 2.0F, 0.0F);
      entityarrow.setSilent(true);
      entityarrow.setCritArrow(true);
      entityarrow.setBaseDamage(damage);
      entityarrow.setKnockback(knockback);
      world.addFreshEntity(entityarrow);
      return entityarrow;
   }

   public static MediumAIBulletEntity shoot(LivingEntity entity, LivingEntity target) {
      MediumAIBulletEntity entityarrow = new MediumAIBulletEntity(
         (EntityType<? extends MediumAIBulletEntity>)CrustyChunksModEntities.MEDIUM_AI_BULLET.get(), entity, entity.level()
      );
      double dx = target.getX() - entity.getX();
      double dy = target.getY() + (double)target.getEyeHeight() - 1.1;
      double dz = target.getZ() - entity.getZ();
      entityarrow.shoot(dx, dy - entityarrow.getY() + Math.hypot(dx, dz) * 0.2F, dz, 14.0F, 12.0F);
      entityarrow.setSilent(true);
      entityarrow.setBaseDamage(1.5);
      entityarrow.setKnockback(0);
      entityarrow.setCritArrow(true);
      entity.level().addFreshEntity(entityarrow);
      return entityarrow;
   }

   private int knockbackStrengthCC = 0;

   public void setKnockback(int knockback) {
      this.knockbackStrengthCC = knockback;
   }

   @Override
   protected void doKnockback(LivingEntity livingEntity, DamageSource damageSource) {
      if (this.knockbackStrengthCC > 0) {
         double d1 = Math.max(0.0, 1.0 - livingEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
         Vec3 vec3 = this.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(this.knockbackStrengthCC * 0.6 * d1);
         if (vec3.lengthSqr() > 0.0) {
            livingEntity.push(vec3.x, 0.1, vec3.z);
         }
      }
   }

   @Override
   public boolean shouldBeSaved() {
      return false;
   }
}
