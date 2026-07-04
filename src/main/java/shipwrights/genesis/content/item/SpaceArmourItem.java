package shipwrights.genesis.content.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Space suit armour. Module flags (cooling / heat / oxygen) are stored in the item's
 * {@link DataComponents#CUSTOM_DATA} component (replacing the old raw-NBT tags from 1.20.1).
 */
public class SpaceArmourItem extends ArmorItem {
    public SpaceArmourItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public static boolean hasModule(ItemStack armour, String moduleName) {
        CustomData data = armour.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(moduleName);
    }

    public static void addModule(ItemStack armour, String moduleName) {
        CustomData.update(DataComponents.CUSTOM_DATA, armour, tag -> tag.putBoolean(moduleName, true));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag tag = data.copyTag();
            if (tag.contains("has_cooling")) tooltip.add(Component.literal("Has Cooling"));
            if (tag.contains("has_heat")) tooltip.add(Component.literal("Has heat"));
            if (tag.contains("has_oxygen")) tooltip.add(Component.literal("Has Oxygen"));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
