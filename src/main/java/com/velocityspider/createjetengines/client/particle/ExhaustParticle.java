package com.velocityspider.createjetengines.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Hot air and soot leaving the nozzle.
 *
 * <p>Behaves like exhaust rather than like smoke: no gravity, no collision, it keeps most of the
 * velocity it was given, and it grows and fades as it mixes with the surrounding air. Because it
 * does not collide it will happily stream past the airframe instead of piling up on it.
 */
public class ExhaustParticle extends TextureSheetParticle {

    private final float growth;

    protected ExhaustParticle(ClientLevel level, double x, double y, double z,
                              double vx, double vy, double vz,
                              SpriteSet sprites, boolean soot) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        this.gravity = 0.0F;
        this.hasPhysics = false;
        // Exhaust slows as it entrains air; soot hangs around a little longer.
        this.friction = soot ? 0.88F : 0.82F;
        this.lifetime = soot ? 22 + this.random.nextInt(14) : 8 + this.random.nextInt(6);
        this.quadSize = soot ? 0.14F + this.random.nextFloat() * 0.10F
                             : 0.20F + this.random.nextFloat() * 0.16F;
        this.growth = soot ? 0.030F : 0.055F;

        if (soot) {
            float v = 0.20F + this.random.nextFloat() * 0.16F;
            this.setColor(v, v * 0.97F, v * 0.95F);
            this.alpha = 0.75F;
        } else {
            this.setColor(1.0F, 0.97F, 0.92F);
            this.alpha = 0.30F;
        }
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
        this.quadSize += this.growth * (1.0F - t);
        // Fade out on a curve so the plume has a soft tail rather than popping.
        this.alpha *= 0.93F;
        if (this.alpha < 0.01F) {
            this.remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class HazeProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public HazeProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new ExhaustParticle(level, x, y, z, vx, vy, vz, sprites, false);
        }
    }

    public static class SootProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SootProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new ExhaustParticle(level, x, y, z, vx, vy, vz, sprites, true);
        }
    }
}
