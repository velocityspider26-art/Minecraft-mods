package net.mcreator.crustychunks.procedures;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class AlivecheckProcedure {
   public static void execute(Entity entity) {
      if (entity != null) {
         if ((entity instanceof Mob _mobEntxx ? _mobEntxx.getTarget() : null) != null
            && (
               (entity instanceof Mob _mobEntx ? _mobEntx.getTarget() : null)
                     .getType()
                     .is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("crusty_chunks:robot")))
                  || 0.0F >= ((entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null) instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1.0F)
            )
            && entity instanceof Mob _entity
            && entity instanceof LivingEntity _ent) {
            _entity.setTarget(_ent);
         }
      }
   }
}
