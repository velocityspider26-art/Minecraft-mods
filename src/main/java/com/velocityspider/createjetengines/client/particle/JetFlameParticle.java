package com.velocityspider.createjetengines.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Afterburner flame and shock diamonds.
 *
 * <p>Unlike a vanilla flame this does not rise, does not stall, and is rendered full-bright so it
 * reads as combustion rather than as a lit texture. Colour is driven by age: a blue-violet core
 * near the nozzle that cools through orange as it travels, which is roughly what reheat actually
 * looks like.
 */
public class JetFlameParticle extends TextureSheetParticle {

    private final boolean shock;

    protected JetFlameParticle(ClientLevel level, double x, double y, double z,
                               double vx, double vy, double vz,
                               SpriteSet sprites, boolean shock) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.shock = shock;

        this.gravity = 0.0F;
        this.hasPhysics = false;
        // Heavy damping: the plume's shape comes from spawning along the axis, not
        // from particles flying downrange. Without this they travel ~15 blocks.
        this.friction = shock ? 0.74F : 0.80F;
        this.lifetime = shock ? 4 + this.random.nextInt(3) : 6 + this.random.nextInt(5);
        this.quadSize = shock ? 0.13F + this.random.nextFloat() * 0.05F
                              : 0.20F + this.random.nextFloat() * 0.12F;
        this.alpha = 1.0F;
        this.setColor(0.55F, 0.70F, 1.0F);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;

        float t = (float) this.age / this.lifetime;
        if (shock) {
            // A shock ring flashes and collapses rather than cooling.
            this.setColor(0.85F, 0.92F, 1.0F);
            this.quadSize *= 0.80F;
            this.alpha = 0.75F * (1.0F - t);
        } else {
            // blue core cooling to orange; blue must fall faster than green or the
            // tail goes pink instead of orange
            float r = 0.55F + 0.45F * t;
            float g = 0.70F - 0.24F * t;
            float b = 1.0F - 0.94F * t * t;
            this.setColor(r, g, b);
            this.quadSize *= 0.90F;
            this.alpha = (1.0F - t) * (1.0F - t);
        }
    }

    /** Combustion emits its own light; never let block lighting darken it. */
    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class FlameProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public FlameProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new JetFlameParticle(level, x, y, z, vx, vy, vz, sprites, false);
        }
    }

    public static class ShockProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public ShockProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new JetFlameParticle(level, x, y, z, vx, vy, vz, sprites, true);
        }
    }
}
