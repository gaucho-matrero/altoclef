package adris.altoclef.util;

public class RecipeTarget {

    private CraftingRecipe _recipe;
    private ItemTarget _item;

    public RecipeTarget(ItemTarget item, CraftingRecipe recipe) {
        _item = item;
        _recipe = recipe;
    }

    public CraftingRecipe getRecipe() {
        return _recipe;
    }
    public ItemTarget getItem() {
        return _item;
    }
}
