package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public class NoFriendlyFireProcedure {
   public static boolean execute(Entity entity) {
      if (entity == null) {
         return false;
      } else {
         boolean state = false;
         if ((entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null) != null) {
            if ((entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null)
               .getType()
               .is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))) {
               state = false;
            } else {
               state = true;
            }
         }

         return state;
      }
   }
}
