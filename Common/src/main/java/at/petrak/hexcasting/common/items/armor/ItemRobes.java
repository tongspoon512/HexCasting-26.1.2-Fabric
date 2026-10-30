package at.petrak.hexcasting.common.items.armor;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * To get the armor model in;
 * On fabric: hook in ClientInit
 * <p>
 * 26.1 has no dedicated {@code ArmorItem}; armor behavior comes from the {@code humanoidArmor}
 * item properties.
 */
public class ItemRobes extends Item {
    public final ArmorType type;

    public ItemRobes(ArmorType type, Properties properties) {
        super(properties.humanoidArmor(ArmorMaterials.LEATHER, type));
        this.type = type;
    }
}
