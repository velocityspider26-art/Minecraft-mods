package net.mcreator.crustychunks.client.particle;

import net.mcreator.crustychunks.procedures.PhosphorusTrailParticleVisualScaleProcedure;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PhosphorusTrailParticle extends TextureSheetParticle {
   private final SpriteSet spriteSet;

   public static PhosphorusTrailParticle.PhosphorusTrailParticleProvider provider(SpriteSet spriteSet) {
      return new PhosphorusTrailParticle.PhosphorusTrailParticleProvider(spriteSet);
   }

   protected PhosphorusTrailParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
      super(world, x, y, z);
      this.spriteSet = spriteSet;
      this.setSize(0.0F, 0.0F);
      this.lifetime = Math.max(1, 210 + (this.random.nextInt(180) - 90));
      this.gravity = 0.02F;
      this.hasPhysics = false;
      this.xd = vx * 0.5;
      this.yd = vy * 0.5;
      this.zd = vz * 0.5;
      this.pickSprite(spriteSet);
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public float getQuadSize(float scale) {
      Level world = this.level;
      return super.getQuadSize(scale) * (float)PhosphorusTrailParticleVisualScaleProcedure.execute((double)this.age);
   }

   public void tick() {
      super.tick();
   }

   public static class PhosphorusTrailParticleProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet spriteSet;

      public PhosphorusTrailParticleProvider(SpriteSet spriteSet) {
         this.spriteSet = spriteSet;
      }

      public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new PhosphorusTrailParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
      }
   }
}
