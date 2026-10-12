package com.velocityspider.crashphysics.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.velocityspider.crashphysics.CrashPhysics;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The mod's own particles: white-hot sparks that streak, bounce and cool through yellow and orange to dull red;
 * billowing smoke and dust that grow, drift with the wind and thin out; spray droplets that fall back and ring the
 * water where they land; and fine mist. They live only on the client and use textures from the particle atlas, so
 * nothing has to be registered and players without the mod can still join.
 */
public final class CrashParticles {

    /**
     * Gravity [blocks per tick²]: 9.81 m/s²
     */
    private static final float GRAVITY = 9.81f / 400.0f;

    private static final RandomSource RANDOM = RandomSource.create();

    private CrashParticles() {
    }

    private static TextureAtlasSprite sprite(final String name) {
        final TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
        return atlas.getSprite(CrashPhysics.id(name));
    }

    private static TextureAtlasSprite smokeSprite() {
        return sprite("smoke_" + RANDOM.nextInt(4));
    }

    private static void add(final TextureSheetParticle particle) {
        Minecraft.getInstance().particleEngine.add(particle);
    }

    /**
     * A spark thrown off metal [velocity in blocks per tick].
     */
    public static void spark(final ClientLevel level, final double x, final double y, final double z, final double vx, final double vy, final double vz) {
        add(new Spark(level, x, y, z, vx, vy, vz));
    }

    /**
     * A puff of smoke.
     *
     * @param size     starting and final size [blocks]
     * @param lifetime how long it lasts [ticks]
     * @param lift     how strongly it rises (hot smoke) or sinks (dust) [blocks per tick²]
     */
    public static void smoke(final ClientLevel level, final double x, final double y, final double z, final double vx, final double vy, final double vz,
                             final float startSize, final float endSize, final int lifetime, final float lift, final float alpha,
                             final float red, final float green, final float blue, final float endRed, final float endGreen, final float endBlue) {
        add(new Smoke(level, x, y, z, vx, vy, vz, smokeSprite(), startSize, endSize, lifetime, lift, alpha, red, green, blue, endRed, endGreen, endBlue, 0.96f));
    }

    /**
     * Dust thrown up from the ground: rolls out fast, slows, hangs low and settles.
     */
    public static void dust(final ClientLevel level, final double x, final double y, final double z, final double vx, final double vy, final double vz,
                            final float size, final int lifetime, final float red, final float green, final float blue) {
        add(new Smoke(level, x, y, z, vx, vy, vz, smokeSprite(), size * 0.5f, size * 2.2f, lifetime, -0.0006f, 0.55f,
                red, green, blue, red * 1.08f, green * 1.08f, blue * 1.08f, 0.9f));
    }

    /**
     * Fine spray hanging in the air.
     */
    public static void mist(final ClientLevel level, final double x, final double y, final double z, final double vx, final double vy, final double vz,
                            final float size, final int lifetime) {
        add(new Smoke(level, x, y, z, vx, vy, vz, sprite("mist"), size * 0.6f, size * 1.6f, lifetime, 0.0004f, 0.16f,
                0.93f, 0.96f, 1.0f, 0.97f, 0.98f, 1.0f, 0.95f));
    }

    /**
     * A drop of water flying through the air [velocity in blocks per tick].
     */
    public static void droplet(final ClientLevel level, final double x, final double y, final double z, final double vx, final double vy, final double vz,
                               final float size) {
        add(new Droplet(level, x, y, z, vx, vy, vz, size));
    }

    /**
     * Draws a particle as a streak along its motion, a little longer than the distance it covers in a tick: how
     * anything fast and bright looks to the eye.
     */
    private static void streak(final VertexConsumer buffer, final Camera camera,
                               final double x, final double y, final double z, final double vx, final double vy, final double vz,
                               final float width, final float length, final float r, final float g, final float b, final float a, final int light,
                               final float u0, final float u1, final float v0, final float v1) {
        final Vec3 view = camera.getPosition();
        final Vector3f head = new Vector3f((float) (x - view.x), (float) (y - view.y), (float) (z - view.z));
        Vector3f motion = new Vector3f((float) vx, (float) vy, (float) vz);
        if (motion.lengthSquared() < 1.0e-8f) {
            motion = new Vector3f(0.0f, -1.0e-3f, 0.0f);
        }
        final float speed = motion.length();
        final Vector3f tail = new Vector3f(head).sub(new Vector3f(motion).mul(Math.max(length, width * 1.5f / speed)));
        final Vector3f toCamera = new Vector3f(head).negate();
        Vector3f side = new Vector3f(motion).cross(toCamera);
        if (side.lengthSquared() < 1.0e-10f) {
            side = new Vector3f(1.0f, 0.0f, 0.0f);
        }
        side.normalize(width);

        final Vector3f a1 = new Vector3f(head).add(side);
        final Vector3f a2 = new Vector3f(tail).add(side);
        final Vector3f a3 = new Vector3f(tail).sub(side);
        final Vector3f a4 = new Vector3f(head).sub(side);
        // Wound to face the camera, which drawn quads have to
        final Vector3f normal = new Vector3f(a2).sub(a1).cross(new Vector3f(a3).sub(a1));
        if (normal.dot(toCamera) >= 0.0f) {
            quadVertex(buffer, a1, u1, v0, r, g, b, a, light);
            quadVertex(buffer, a2, u1, v1, r, g, b, a * 0.6f, light);
            quadVertex(buffer, a3, u0, v1, r, g, b, a * 0.6f, light);
            quadVertex(buffer, a4, u0, v0, r, g, b, a, light);
        } else {
            quadVertex(buffer, a4, u0, v0, r, g, b, a, light);
            quadVertex(buffer, a3, u0, v1, r, g, b, a * 0.6f, light);
            quadVertex(buffer, a2, u1, v1, r, g, b, a * 0.6f, light);
            quadVertex(buffer, a1, u1, v0, r, g, b, a, light);
        }
    }

    private static void quadVertex(final VertexConsumer buffer, final Vector3f position, final float u, final float v, final float r, final float g,
                                   final float b, final float a, final int light) {
        buffer.addVertex(position.x, position.y, position.z).setUv(u, v).setColor(r, g, b, a).setLight(light);
    }

    /**
     * A white-hot fleck of metal: streaks, bounces off what it hits, and cools as it flies.
     */
    static final class Spark extends TextureSheetParticle {
        private final float heat;

        Spark(final ClientLevel level, final double x, final double y, final double z, final double vx, final double vy, final double vz) {
            super(level, x, y, z);
            this.setSprite(sprite("spark"));
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.gravity = 0.0f;
            this.friction = 1.0f;
            this.hasPhysics = true;
            this.lifetime = 8 + RANDOM.nextInt(18);
            this.quadSize = 0.06f + RANDOM.nextFloat() * 0.06f;
            this.heat = 0.8f + RANDOM.nextFloat() * 0.2f;
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
            this.yd -= GRAVITY;
            this.xd *= 0.985;
            this.yd *= 0.985;
            this.zd *= 0.985;
            final double before = this.yd;
            this.move(this.xd, this.yd, this.zd);
            if (this.onGround && before < -0.02) {
                // Bounces, losing most of its speed, and skitters along
                this.yd = -before * 0.35;
                this.xd *= 0.6;
                this.zd *= 0.6;
                this.onGround = false;
            }
            if (this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).is(FluidTags.WATER)) {
                // Quenched
                this.remove();
            }
        }

        @Override
        public void render(final VertexConsumer buffer, final Camera camera, final float partialTicks) {
            // Cooling: white, yellow, orange, then a dull red glow
            final float life = Mth.clamp((this.age + partialTicks) / this.lifetime, 0.0f, 1.0f) / this.heat;
            final float r = life < 0.7f ? 1.0f : Mth.lerp((life - 0.7f) / 0.3f, 1.0f, 0.55f);
            final float g = life < 0.15f ? 0.95f : life < 0.5f ? Mth.lerp((life - 0.15f) / 0.35f, 0.95f, 0.55f) : Mth.lerp(Math.min(1.0f, (life - 0.5f) / 0.5f), 0.55f, 0.12f);
            final float b = life < 0.15f ? 0.8f : Mth.lerp(Math.min(1.0f, (life - 0.15f) / 0.4f), 0.8f, 0.05f);
            final float a = life < 0.8f ? 1.0f : Mth.lerp((life - 0.8f) / 0.2f, 1.0f, 0.0f);
            streak(buffer, camera, Mth.lerp(partialTicks, this.xo, this.x), Mth.lerp(partialTicks, this.yo, this.y),
                    Mth.lerp(partialTicks, this.zo, this.z), this.xd, this.yd, this.zd, this.quadSize, 2.5f, r, g, b, Mth.clamp(a, 0.0f, 1.0f),
                    LightTexture.FULL_BRIGHT, this.getU0(), this.getU1(), this.getV0(), this.getV1());
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        @Override
        protected int getLightColor(final float partialTick) {
            return LightTexture.FULL_BRIGHT;
        }
    }

    /**
     * A puff of smoke, dust or mist: it grows as it mixes with the air, rises if it is hot (or settles if it is dust),
     * drifts with the wind, turns slowly and thins out.
     */
    static final class Smoke extends TextureSheetParticle {
        private final float startSize;
        private final float endSize;
        private final float lift;
        private final float peakAlpha;
        private final float startR, startG, startB, endR, endG, endB;
        private final float drag;
        private final float spin;

        Smoke(final ClientLevel level, final double x, final double y, final double z, final double vx, final double vy, final double vz,
              final TextureAtlasSprite sprite, final float startSize, final float endSize, final int lifetime, final float lift, final float alpha,
              final float r, final float g, final float b, final float endR, final float endG, final float endB, final float drag) {
            super(level, x, y, z);
            this.setSprite(sprite);
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.gravity = 0.0f;
            this.friction = 1.0f;
            this.hasPhysics = true;
            this.lifetime = (int) (lifetime * (0.8f + RANDOM.nextFloat() * 0.4f));
            this.startSize = startSize;
            this.endSize = endSize;
            this.quadSize = startSize;
            this.lift = lift;
            this.peakAlpha = alpha;
            this.alpha = 0.0f;
            this.startR = r;
            this.startG = g;
            this.startB = b;
            this.endR = endR;
            this.endG = endG;
            this.endB = endB;
            this.drag = drag;
            this.spin = (RANDOM.nextFloat() - 0.5f) * 0.04f;
            this.roll = RANDOM.nextFloat() * Mth.TWO_PI;
            this.oRoll = this.roll;
            this.setColor(r, g, b);
        }

        @Override
        public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            this.oRoll = this.roll;
            if (this.age++ >= this.lifetime) {
                this.remove();
                return;
            }
            final float life = (float) this.age / this.lifetime;

            // A light breeze that wanders slowly
            final double time = this.level.getGameTime() * 0.004;
            final double windX = 0.012 * Math.sin(time) + 0.006;
            final double windZ = 0.010 * Math.cos(time * 0.7);
            this.xd = this.xd * this.drag + windX * (1.0 - this.drag) * life;
            this.zd = this.zd * this.drag + windZ * (1.0 - this.drag) * life;
            this.yd = this.yd * this.drag + this.lift;
            this.move(this.xd, this.yd, this.zd);
            if (this.onGround && this.lift < 0.0f) {
                this.yd = 0.0;
            }

            this.roll += this.spin;
            // Grows quickly at first as it mixes with the air, then more slowly
            this.quadSize = this.startSize + (this.endSize - this.startSize) * (1.0f - (float) Math.exp(-life * 3.0));
            this.alpha = this.peakAlpha * Math.min(1.0f, this.age / 4.0f) * (life < 0.45f ? 1.0f : 1.0f - (life - 0.45f) / 0.55f);
            this.setColor(Mth.lerp(life, this.startR, this.endR), Mth.lerp(life, this.startG, this.endG), Mth.lerp(life, this.startB, this.endB));
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /**
     * A drop of spray: falls under gravity with a little air drag, and rings the water where it lands.
     */
    static final class Droplet extends TextureSheetParticle {
        Droplet(final ClientLevel level, final double x, final double y, final double z, final double vx, final double vy, final double vz, final float size) {
            super(level, x, y, z);
            this.setSprite(sprite("droplet"));
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.gravity = 0.0f;
            this.friction = 1.0f;
            this.hasPhysics = true;
            this.lifetime = 60 + RANDOM.nextInt(40);
            this.quadSize = size;
            this.alpha = 0.85f;
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
            this.yd -= GRAVITY;
            this.xd *= 0.99;
            this.yd *= 0.99;
            this.zd *= 0.99;
            this.move(this.xd, this.yd, this.zd);
            if (this.onGround) {
                this.remove();
                return;
            }
            if (this.yd < 0.0 && this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).is(FluidTags.WATER)) {
                WaterWaves.ripple(this.x, this.y, this.z, Math.min(0.04f, 0.006f + this.quadSize * 0.15f));
                this.remove();
            }
        }

        @Override
        public void render(final VertexConsumer buffer, final Camera camera, final float partialTicks) {
            final int light = this.getLightColor(partialTicks);
            streak(buffer, camera, Mth.lerp(partialTicks, this.xo, this.x), Mth.lerp(partialTicks, this.yo, this.y),
                    Mth.lerp(partialTicks, this.zo, this.z), this.xd, this.yd, this.zd, this.quadSize, 0.8f, 0.85f, 0.92f, 1.0f, this.alpha, light,
                    this.getU0(), this.getU1(), this.getV0(), this.getV1());
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}
