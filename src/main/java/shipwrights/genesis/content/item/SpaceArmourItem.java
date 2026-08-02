package shipwrights.genesis.content.item;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class SpaceArmourItem extends ArmorItem {
    public SpaceArmourItem(Holder<ArmorMaterial> arg, Type arg2, Properties arg3) {
        super(arg, arg2, arg3);
    }

    public static boolean hasModule(ItemStack armour,String moduleName)
    {
        return armour.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(moduleName);
    }

    public static void addModule(ItemStack armour, String moduleName)
    {
        CustomData.update(DataComponents.CUSTOM_DATA, armour, tag -> tag.putBoolean(moduleName, true));
    }

    @Override
    public void appendHoverText(ItemStack arg, Item.TooltipContext arg2, List<Component> list, TooltipFlag arg3) {
        CustomData customData = arg.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if(!customData.isEmpty())
        {
            CompoundTag tag = customData.copyTag();
            if(tag.contains("has_cooling"))
            {
                list.add(Component.literal("Has Cooling"));
            }
            if(tag.contains("has_heat"))
            {
                list.add(Component.literal("Has heat"));
            }
            if(tag.contains("has_oxygen"))
            {
                list.add(Component.literal("Has Oxygen"));
            }
        }
        super.appendHoverText(arg, arg2, list, arg3);
    }
}
