package adris.altoclef.util;


import adris.altoclef.TaskCatalogue;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


public class ItemTarget {
    public static final ItemTarget EMPTY_ITEM = new ItemTarget(new Item[0], 0);
    private final boolean infinite;
    private final Item[] itemMatches;
    public int targetCount; // TODO: 2021-05-22 make not public
    private String catalogueName;
    
    private ItemTarget(Item[] items, int targetCount, boolean infinite) {
        itemMatches = items;
        this.targetCount = targetCount;
        this.infinite = infinite;
    }
    
    public ItemTarget(Item[] items, int targetCount) {
        this(items, targetCount, false);
    }
    
    public ItemTarget(@NotNull String catalogueName, int targetCount) {
        this(Objects.requireNonNull(TaskCatalogue.getItemMatches(catalogueName),
                                    "Invalid catalogue name for item target: \"" + catalogueName + "\". Something isn't robust!"),
             targetCount, false);
    }
    
    public ItemTarget(String catalogueName) {
        this(Objects.requireNonNull(TaskCatalogue.getItemMatches(catalogueName),
                                    "Invalid catalogue name for item target: \"" + catalogueName + "\". Something isn't robust!"),
             99999999, true);
    }
    
    public ItemTarget(Item item, int targetCount) {
        this(new Item[]{ item }, targetCount);
    }
    
    public ItemTarget(Item[] items) {
        this(items, 9999999, true);
    }
    
    public ItemTarget(Item item) {
        this(new Item[]{ item });
    }
    
    public ItemTarget(ItemTarget toCopy) {
        itemMatches = new Item[toCopy.itemMatches.length];
        System.arraycopy(toCopy.itemMatches, 0, itemMatches, 0, toCopy.itemMatches.length);
        catalogueName = toCopy.catalogueName;
        targetCount = toCopy.targetCount;
        infinite = toCopy.infinite;
    }
    
    public static Item[] getMatches(ItemTarget... targets) {
        Set<Item> result = new HashSet<>();
        for (ItemTarget target : targets) {
            result.addAll(Arrays.asList(target.getMatches()));
        }
        return Util.toArray(Item.class, result);
    }
    
    public Item[] getMatches() {
        return itemMatches;
    }
    
    public boolean matches(Item item) {
        for (Item match : itemMatches) {
            if (match == null) continue;
            if (match.equals(item)) return true;
        }
        return false;
    }
    
    public boolean isCatalogueItem() {
        return catalogueName != null;
    }
    
    public String getCatalogueName() {
        return catalogueName;
    }
    
    public boolean isEmpty() {
        return itemMatches == null || itemMatches.length == 0;
    }
    
    @Override
    public int hashCode() {
        int result = targetCount;
        result = 31 * result + Arrays.hashCode(itemMatches);
        result = 31 * result + (catalogueName != null ? catalogueName.hashCode() : 0);
        result = 31 * result + (infinite ? 1 : 0);
        return result;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ItemTarget target = (ItemTarget) o;
        
        if (infinite) {
            if (!target.infinite)
                return false;
        } else {
            if (targetCount != target.targetCount)
                return false;
        }
        if (!Arrays.equals(itemMatches, target.itemMatches)) return false;
        return Objects.equals(catalogueName, target.catalogueName);
    }
    
    @Override
    public String toString() {
        
        StringBuilder result = new StringBuilder();
        if (isEmpty()) {
            result.append("(empty)");
        } else if (isCatalogueItem()) {
            result.append(catalogueName);
        } else {
            result.append("[");
            int counter = 0;
            for (Item item : itemMatches) {
                if (item == null) {
                    result.append("(null??)");
                } else {
                    result.append(ItemUtil.trimItemName(item.getTranslationKey()));
                }
                if (++counter != itemMatches.length) {
                    result.append(",");
                }
            }
            result.append("]");
        }
        if (!infinite && !isEmpty()) {
            result.append(" x ").append(targetCount);
        }
        
        return result.toString();
    }
}
