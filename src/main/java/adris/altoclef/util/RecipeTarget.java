package adris.altoclef.util;


import java.util.Objects;


public class RecipeTarget {
    private final CraftingRecipe recipe;
    private final ItemTarget targetItem;
    
    public RecipeTarget(ItemTarget targetItem, CraftingRecipe recipe) {
        this.targetItem = targetItem;
        this.recipe = recipe;
    }
    
    public CraftingRecipe getRecipe() {
        return recipe;
    }
    
    public ItemTarget getTargetItem() {
        return targetItem;
    }
    
    @Override
    public int hashCode() {
        int result = recipe != null ? recipe.hashCode() : 0;
        result = 31 * result + (targetItem != null ? targetItem.hashCode() : 0);
        return result;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        RecipeTarget recipeTarget = (RecipeTarget) o;
        
        return Objects.equals(recipe, recipeTarget.recipe) &&
               Objects.equals(targetItem, recipeTarget.targetItem);
    }
    
    @Override
    public String toString() {
        return "RecipeTarget{" + "_recipe=" + recipe + ", _item=" + targetItem + '}';
    }
}
