package shipwrights.genesis.space.physics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;

/**
 * Scales entity gravity to the celestial the dimension belongs to, using the
 * per-body {@code gravity} value from the celestials datapack (moon: 0.1622 of
 * Earth). Lower gravity means higher jumps, slower falls, and floatier
 * landings — Sable ships get the matching treatment from
 * {@code data/genesis/dimension_physics/}.
 *
 * Applied on entity join (which also fires on dimension change), so it follows
 * the entity between worlds. Space and subspace are untouched: entities there
 * are handled by the no-gravity scaling logic in {@link GenesisMod} and the
 * gravity wells in {@link CelestialGravityWells}.
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class CelestialEntityGravity {
    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "celestial_gravity");

    private CelestialEntityGravity() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        AttributeInstance gravity = living.getAttribute(Attributes.GRAVITY);
        if (gravity == null) {
            return;
        }

        gravity.removeModifier(MODIFIER_ID);

        if (GenesisMod.isSpaceDimension(level) || GenesisMod.isSubSpaceDimension(level)) {
            return;
        }

        Celestial celestial = GenesisMod.getCelestialForLevel(level);
        if (celestial == null) {
            return;
        }

        double scale = celestial.gravity();
        if (scale > 0.0 && Math.abs(scale - 1.0) > 0.01) {
            gravity.addTransientModifier(new AttributeModifier(
                    MODIFIER_ID, scale - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
