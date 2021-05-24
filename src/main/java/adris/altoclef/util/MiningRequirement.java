package adris.altoclef.util;


import adris.altoclef.Debug;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;


public enum MiningRequirement {
    HAND(Items.AIR),
    WOOD(Items.WOODEN_PICKAXE),
    STONE(Items.STONE_PICKAXE),
    IRON(Items.IRON_PICKAXE),
    DIAMOND(Items.DIAMOND_PICKAXE);
    
    private final Item minPickaxe;
    
    MiningRequirement(Item minPickaxe) {
        this.minPickaxe = minPickaxe;
    }
    
    public static MiningRequirement getMinimumRequirementForBlock(Block block) {
        if (block.getDefaultState().isToolRequired()) {
            for (MiningRequirement req : values()) {
                if (req == HAND) continue;
                if (req.minPickaxe.isEffectiveOn(block.getDefaultState())) {
                    return req;
                }
            }
            Debug.logWarning("Failed to find ANY effective tool against: " + block +
                             ". I assume netherite is not required anywhere, so something else probably went wrong.");
            return DIAMOND;
        }
        return HAND;
    }
    
    public Item getMinimumPickaxe() {
        return minPickaxe;
    }
    
}
