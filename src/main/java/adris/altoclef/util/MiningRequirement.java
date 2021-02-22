package adris.altoclef.util;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

public enum MiningRequirement implements Comparable<MiningRequirement> {
    HAND(Items.AIR), WOOD(Items.WOODEN_PICKAXE), STONE(Items.STONE_PICKAXE), IRON(Items.IRON_PICKAXE), DIAMOND(Items.DIAMOND_PICKAXE);

    private Item _minPickaxe;

    MiningRequirement(Item minPickaxe) {
        _minPickaxe = minPickaxe;
    }

    public Item getMinimumPickaxe() {
        return _minPickaxe;
    }

}
