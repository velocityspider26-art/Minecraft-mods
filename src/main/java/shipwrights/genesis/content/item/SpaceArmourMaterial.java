package shipwrights.genesis.content.item;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import shipwrights.genesis.GenesisMod;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The Space suit armour material. In 1.21.1 {@link ArmorMaterial} is a record rather than an
 * interface, so this class simply builds a directly-held material instance.
 */
public final class SpaceArmourMaterial {

    public static final Holder<ArmorMaterial> HOLDER = Holder.direct(create());

    private SpaceArmourMaterial() {}

    private static ArmorMaterial create() {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            defense.put(type, 1);
        }
        return new ArmorMaterial(
                defense,
                5,
                SoundEvents.ARMOR_EQUIP_LEATHER,
                () -> Ingredient.of(Items.PHANTOM_MEMBRANE),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "space"))),
                1.0f,
                0.0f);
    }
}
