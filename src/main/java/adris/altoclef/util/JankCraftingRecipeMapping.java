package adris.altoclef.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * For crafting table/inventory recipe book crafting, we need to figure out identifiers given a recipe.
 */
public class JankCraftingRecipeMapping {
    private static final HashMap<Item, List<Recipe>> _recipeMapping = new HashMap<>();

    private static void reloadRecipeMapping() {
        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            RecipeManager recipes = MinecraftClient.getInstance().getNetworkHandler().getRecipeManager();
            for (Recipe recipe : recipes.values()) {
                Item output = recipe.getOutput().getItem();
                if (!_recipeMapping.containsKey(output)) {
                    _recipeMapping.put(output, new ArrayList<>());
                }
                _recipeMapping.get(output).add(recipe);
            }
        }
    }

    public static Optional<Recipe> getMinecraftMappedRecipe(CraftingRecipe recipe, Item output) {
        if (_recipeMapping.isEmpty()) {
            reloadRecipeMapping();
        }
        if (_recipeMapping.containsKey(output)) {
            for (Recipe checkRecipe : _recipeMapping.get(output)) {
                // Check for item count/satisfiability and not shape satisfiability (that would be annoying)
                // Assumes there are no 2 recipes with the same output and same inputs in a different order.
                List<ItemTarget> toSatisfy = Arrays.stream(recipe.getSlots()).filter(itemTarget -> itemTarget != null && !itemTarget.isEmpty()).collect(Collectors.toList());
                for (Object ingredientObj : checkRecipe.getIngredients()) {
                    Ingredient checkIngredient = (Ingredient) ingredientObj;
                    if (checkIngredient.isEmpty())
                        continue;
                    // Remove from "toSatisfy" if we find something that fits
                    outer:
                    for (int i = 0; i < toSatisfy.size(); ++i) {
                        ItemTarget check = toSatisfy.get(i);
                        for (ItemStack match : checkIngredient.getMatchingStacks()) {
                            if (check.matches(match.getItem())) {
                                toSatisfy.remove(i);
                                break outer;
                            }
                        }
                    }
                }
                /*int i = -1; // ++i first
                boolean found = true;
                for (Object ingredientObj : checkRecipe.getIngredients()) {
                    ++i;
                    // Out of range
                    if (i >= recipe.getSlotCount()) {
                        found = false;
                        break;
                    }
                    ItemTarget ourIngredient = recipe.getSlot(i);
                    Ingredient checkIngredient = (Ingredient) ingredientObj;
                    // If our ingredient is null, our check ingredient MUST be empty (Empty = null)
                    if (ourIngredient == null || ourIngredient.isEmpty()) {
                        if (checkIngredient.isEmpty())
                            continue;
                        found = false;
                        break;
                    }
                    // At least one item must satisfy to move on.
                    if (Arrays.stream(checkIngredient.getMatchingStacks()).noneMatch(itemStack -> ourIngredient.matches(itemStack.getItem()))) {
                        found = false;
                        break;
                    }
                }
                if (found)
                    return Optional.of(checkRecipe);
                 */
                // We satisfied every material, so assume it's the right recipe.
                if (toSatisfy.isEmpty()) {
                    return Optional.of(checkRecipe);
                }
            }
        }
        return Optional.empty();
    }
}
