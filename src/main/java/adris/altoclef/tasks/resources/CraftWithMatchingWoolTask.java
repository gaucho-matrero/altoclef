package adris.altoclef.tasks.resources;

import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;

// This is literally identical to its parent, but we give it a name for psychological reasons
public abstract class CraftWithMatchingWoolTask extends CraftWithMatchingMaterialsTask {
    public CraftWithMatchingWoolTask(ItemTarget target, CraftingRecipe recipe, boolean[] sameMask) {
        super(target, recipe, sameMask);
    }
}
