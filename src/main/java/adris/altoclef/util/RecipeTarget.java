package adris.altoclef.util;

import java.util.Objects;

public class RecipeTarget {

    private final CraftingRecipe _recipe;
    private final ItemTarget _item;

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

    @Override
    public String toString() {
        return "RecipeTarget{" +
                "_recipe=" + _recipe +
                ", _item=" + _item +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeTarget that = (RecipeTarget) o;
        return _recipe.equals(that._recipe) && (_item == null) == (that._item == null) && (_item == null || _item.equals(that._item));
    }

    @Override
    public int hashCode() {
        return Objects.hash(_recipe, _item);
    }
}
