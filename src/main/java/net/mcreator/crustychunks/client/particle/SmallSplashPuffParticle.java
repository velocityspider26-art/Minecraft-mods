package net.mcreator.crustychunks.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SmallSplashPuffParticle extends TextureSheetParticle {
   private final SpriteSet spriteSet;

   public static SmallSplashPuffParticle.SmallSplashPuffParticleProvider provider(SpriteSet spriteSet) {
      return new SmallSplashPuffParticle.SmallSplashPuffParticleProvider(spriteSet);
   }

   protected SmallSplashPuffParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
      super(world, x, y, z);
      this.spriteSet = spriteSet;
      this.setSize(5.0F, 5.0F);
      this.quadSize *= 5.0F;
      this.lifetime = Math.max(1, 90 + (this.random.nextInt(60) - 30));
      this.gravity = 0.0F;
      this.hasPhysics = false;
      this.xd = vx * 0.2;
      this.yd = vy * 0.2;
      this.zd = vz * 0.2;
      this.pickSprite(spriteSet);
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public void tick() {
      super.tick();
   }

   public static class SmallSplashPuffParticleProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet spriteSet;

      public SmallSplashPuffParticleProvider(SpriteSet spriteSet) {
         this.spriteSet = spriteSet;
      }

      public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new SmallSplashPuffParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
      }
   }
}
