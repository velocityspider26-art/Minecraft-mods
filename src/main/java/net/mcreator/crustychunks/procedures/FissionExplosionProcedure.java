package net.mcreator.crustychunks.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.mcreator.crustychunks.CrustyChunksMod;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class FissionExplosionProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      try {
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;
      double xRadius = 0.0;
      double loop = 0.0;
      double zRadius = 0.0;
      double particleAmount = 0.0;
      boolean found = false;
      boolean space = false;
      if (("D:"
               + (
                  world instanceof Level _lvlxxx
                     ? _lvlxxx.dimension()
                     : (world instanceof WorldGenLevel _wglxxx ? _wglxxx.getLevel().dimension() : Level.OVERWORLD)
               ))
            .contains("orbit")
         || ("D:"
               + (world instanceof Level _lvlxx ? _lvlxx.dimension() : (world instanceof WorldGenLevel _wglxx ? _wglxx.getLevel().dimension() : Level.OVERWORLD)))
            .contains("solar_system")
         || ("D:" + (world instanceof Level _lvlx ? _lvlx.dimension() : (world instanceof WorldGenLevel _wglx ? _wglx.getLevel().dimension() : Level.OVERWORLD)))
            .contains("orbit")
         || ("D:" + (world instanceof Level _lvl ? _lvl.dimension() : (world instanceof WorldGenLevel _wgl ? _wgl.getLevel().dimension() : Level.OVERWORLD)))
            .contains("alpha_system")
         || y - 1000.0 > (double)world.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)x, (int)z)
         || y > 2048.0) {
         space = true;
      }

      if (space) {
         CrustyChunksMod.queueServerWork(1, () -> SpaceFissionExplosionProcedure.execute(world, x, y, z));
      } else {
         CrustyChunksMod.queueServerWork(1, () -> NuclearExplosionExampleProcedure.execute(world, x, y, z, 50.0));
      }
   
      } catch (Throwable _wtSafe) {
         net.mcreator.crustychunks.compat.WariumSafety.report("FissionExplosionProcedure.execute", _wtSafe);
      }
   }
}
