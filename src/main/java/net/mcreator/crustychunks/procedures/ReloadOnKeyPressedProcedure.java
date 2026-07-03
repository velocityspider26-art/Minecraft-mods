package net.mcreator.crustychunks.procedures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.mcreator.crustychunks.init.CrustyChunksModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

public class ReloadOnKeyPressedProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.SMG_ANIMATED.get()) {
            SMGReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
            == CrustyChunksModItems.PUMP_ACTION_SHOTGUN_ANIMATED.get()) {
            ShotgunReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.REVOLVER_ANIMATED.get()) {
            RevolverReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
            == CrustyChunksModItems.SEMI_AUTOMATIC_RIFLE_ANIMATED.get()) {
            RifleReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
            == CrustyChunksModItems.SEMI_AUTOMATIC_PISTOL_ANIMATED.get()) {
            PistolReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.AUTO_PISTOL.get()) {
            PistolReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.STEALTH_PISTOL.get()) {
            PistolReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
            == CrustyChunksModItems.BOLT_ACTION_RIFLE_ANIMATED.get()) {
            BoltReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
            == CrustyChunksModItems.SCOPED_BOLT_ACTION_RIFLE_ANIMATED.get()) {
            BoltReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.SINGLE_SHOT_RIFLE.get()) {
            SingleShotReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.LMG_ANIMATED.get()) {
            LMGReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.BURST_RIFLE.get()) {
            BurstRifleReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.ARMOR_PEELER_UNLOADED.get()) {
            ArmorPeelerReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
            == CrustyChunksModItems.BREAK_ACTION_SHOTGUN_ANIMATED.get()) {
            BreakActionReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.BATTLE_RIFLE.get()) {
            BattleRifleReloadProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.MACHINE_CARBINE.get()) {
            MachineCarbineReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
            == CrustyChunksModItems.FLAME_THROWER_TANK_CHESTPLATE.get()) {
            FlamethrowerReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.HAND_DRILL.get()) {
            DrillReloadProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.FLARE_PISTOL.get()) {
            FlareReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.BREECH_RIFLE.get()) {
            BreechRifleReloadProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.SCOPED_BREECH_RIFLE.get()) {
            BreechRifleReloadProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.ERADICATION.get()) {
            EradicationReloadProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.LEVER_RIFLE.get()) {
            LeverRifleReloadProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.AUTOMATIC_RIFLE.get()) {
            RifleReloadScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.MACHINE_GUN_BOX.get()) {
            MGBoxScriptProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.PAINT_TOOL.get()) {
            PaintToolReloadProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == CrustyChunksModItems.GRENADE_LAUNCHER.get()) {
            GrenadeLauncherReloadProcedure.execute(world, x, y, z, entity);
         }

         if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)
            .is(ItemTags.create(ResourceLocation.parse("crusty_chunks:firearm")))) {
            CustomData.update(DataComponents.CUSTOM_DATA, (entity instanceof LivingEntity _livEntx ? _livEntx.getMainHandItem() : ItemStack.EMPTY), _tagupd -> _tagupd.putBoolean("Reloading", true));
         }
      }
   }
}
