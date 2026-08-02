package shipwrights.genesis.content.item;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import shipwrights.genesis.GenesisMod;

import java.util.List;
import java.util.Map;

public final class SpaceArmourMaterial {
    public static final Holder<ArmorMaterial> MATERIAL = Holder.direct(new ArmorMaterial(
            Map.of(
                    ArmorItem.Type.HELMET, 1,
                    ArmorItem.Type.CHESTPLATE, 1,
                    ArmorItem.Type.LEGGINGS, 1,
                    ArmorItem.Type.BOOTS, 1
            ),
            5,
            Holder.direct(SoundEvents.WOOL_HIT),
            () -> Ingredient.of(Items.PHANTOM_MEMBRANE),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "space_suit"))),
            1.0F,
            0.0F
    ));

    private SpaceArmourMaterial() {
    }
}
