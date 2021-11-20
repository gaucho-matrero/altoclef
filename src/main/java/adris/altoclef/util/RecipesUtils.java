package adris.altoclef.util;

import adris.altoclef.AltoClef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * RecipesUtils supplies several methods for easing the process of removing existing recipes,
 * as well as a helper method for making a 9x9 grid of the same item (nuggets to ingots, etc)
 * https://github.com/Draco18s/ReasonableRealism/blob/1.12.1/src/main/java/com/draco18s/hardlib/util/RecipesUtils.java#L164
 *
 * @author Draco18s
 *
 * Modified by Meloweh
 *
 */
public class RecipesUtils {
    @Nullable
    public static Recipe getRecipeWithOutput(ItemStack resultStack) {
        ItemStack recipeResult;
        List<CraftingRecipe> recipes = MinecraftClient.getInstance().world.getRecipeManager().listAllOfType(RecipeType.CRAFTING);
        Iterator<CraftingRecipe> iterator = recipes.iterator();
        while(iterator.hasNext()) {
            Recipe tmpRecipe = iterator.next();
            recipeResult = tmpRecipe.getOutput();
            if (ItemStack.areItemsEqual(resultStack, recipeResult)) {
                return tmpRecipe;
            }
        }
        return null;
    }
}