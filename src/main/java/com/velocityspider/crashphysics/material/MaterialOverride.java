package com.velocityspider.crashphysics.material;

import com.velocityspider.crashphysics.physics.MaterialProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * One material definition from a datapack. Every field is optional and only replaces what it specifies.
 */
public record MaterialOverride(ResourceLocation source, List<Selector> selectors, int priority,
                               OptionalDouble density, OptionalDouble strength, OptionalDouble jointStrength,
                               Optional<FractureMode> fracture, Optional<Boolean> unbreakable,
                               Optional<StateReference> ejecta, Optional<StateReference> scar, Optional<StateReference> skid,
                               OptionalDouble volatility, Optional<Boolean> ignites) {

    public boolean matches(final BlockState state) {
        for (final Selector selector : this.selectors) {
            if (selector.matches(state)) {
                return true;
            }
        }
        return false;
    }

    public CrashMaterial applyTo(final BlockState state, final CrashMaterial base) {
        final MaterialProfile baseProfile = base.profile();
        final boolean isUnbreakable = this.unbreakable.orElse(baseProfile.unbreakable());

        final MaterialProfile profile;
        if (isUnbreakable) {
            profile = MaterialProfile.UNBREAKABLE;
        } else {
            final double baseStrength = baseProfile.unbreakable() ? 1.0e9 : baseProfile.strength();
            final double baseJoint = baseProfile.unbreakable() ? 1.0e8 : baseProfile.jointStrength();
            profile = new MaterialProfile(
                    this.density.orElse(baseProfile.density()),
                    this.strength.orElse(baseStrength),
                    this.jointStrength.orElse(this.strength.isPresent() ? this.strength.getAsDouble() * 0.1 : baseJoint),
                    false
            );
        }

        return new CrashMaterial(
                profile,
                this.fracture.orElse(base.fracture()),
                this.ejecta.map(reference -> reference.resolve(state)).orElse(base.ejecta()),
                this.scar.map(reference -> reference.resolve(state)).orElse(base.scar()),
                this.skid.map(reference -> reference.resolve(state)).orElse(base.skid()),
                (float) this.volatility.orElse(base.volatility()),
                this.ignites.orElse(base.ignites())
        );
    }

    /**
     * Matches a block by ID or tag. Blocks and tags from mods that are not installed simply never match.
     */
    public sealed interface Selector {
        boolean matches(BlockState state);

        static Selector parse(final String text) {
            if (text.startsWith("#")) {
                return new TagSelector(TagKey.create(net.minecraft.core.registries.Registries.BLOCK, ResourceLocation.parse(text.substring(1))));
            }
            return new BlockSelector(ResourceLocation.parse(text));
        }
    }

    public record TagSelector(TagKey<Block> tag) implements Selector {
        @Override
        public boolean matches(final BlockState state) {
            return state.is(this.tag);
        }
    }

    public record BlockSelector(ResourceLocation id) implements Selector {
        @Override
        public boolean matches(final BlockState state) {
            return this.id.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        }
    }

    /**
     * A block state written in a datapack: {@code "none"}, {@code "self"} or a block ID.
     */
    public sealed interface StateReference {
        @Nullable
        BlockState resolve(BlockState self);

        static StateReference parse(final String text) {
            return switch (text) {
                case "none" -> new NoState();
                case "self" -> new SelfState();
                default -> new NamedState(ResourceLocation.parse(text));
            };
        }
    }

    public record NoState() implements StateReference {
        @Override
        public @Nullable BlockState resolve(final BlockState self) {
            return null;
        }
    }

    public record SelfState() implements StateReference {
        @Override
        public BlockState resolve(final BlockState self) {
            return self.getBlock().defaultBlockState();
        }
    }

    public record NamedState(ResourceLocation id) implements StateReference {
        @Override
        public @Nullable BlockState resolve(final BlockState self) {
            return BuiltInRegistries.BLOCK.getOptional(this.id).map(Block::defaultBlockState).orElse(null);
        }
    }
}
