package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.Input;
import adris.altoclef.util.LookUtil;
import baritone.Baritone;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.RayTraceUtils;
import baritone.api.utils.Rotation;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.mixin.client.keybinding.KeyCodeAccessor;
import net.java.games.input.Component;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class FoodChain extends SingleTaskChain {

    private boolean _isTryingToEat = false;
    private boolean _requestFillup = false;

    private static final int RIGHT_CLICK_KEY = 1 - 100;

    public FoodChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    public float getPriority(AltoClef mod) {

        if (!mod.inGame()) {
            return Float.NEGATIVE_INFINITY;
        }

        if (!mod.getModSettings().isAutoEat()) {
            stopEat(mod);
            return Float.NEGATIVE_INFINITY;
        }

        if (mod.getInventoryTracker().totalFoodScore() <= 0) {
            // Do nothing if we have no food.
            stopEat(mod);
            return Float.NEGATIVE_INFINITY;
        }

        /*
        - Eats if:
        - We're hungry and have food that fits
            - We're low on health and maybe a little bit hungry
            - We're very low on health and are even slightly hungry
        - We're kind of hungry and have food that fits perfectly
         */

        // We're in danger, don't eat now!!
        if (mod.getMobDefenseChain().isDoingAcrobatics()) {
            stopEat(mod);
            return Float.NEGATIVE_INFINITY;
        }

        // If we requested a fillup but we're full, stop.
        if (_requestFillup && mod.getPlayer().getHungerManager().getFoodLevel() == 20) {
            _requestFillup = false;
        }

        if (needsToEat(mod) || _requestFillup) {
            Item toUse = getBestItemToEat(mod);
            if (toUse != null) {
                //Debug.logInternal("EATING " + toUse.getTranslationKey() + " : " + test);
                _isTryingToEat = true;
                _requestFillup = true;

                // Make sure we're not facing a container
                if (!LookUtil.tryAvoidingInteractable(mod)) {
                    return Float.NEGATIVE_INFINITY;
                }

                mod.getInventoryTracker().equipItem(toUse);
                startEat(mod);
            } else {
                stopEat(mod);
            }
        } else if (_isTryingToEat) {
            stopEat(mod);
        }


        // Food eating is handled asynchronously.
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        // Nothing.
    }

    private void startEat(AltoClef mod) {
        MinecraftClient.getInstance().options.keyUse.setPressed(true);
        mod.getExtraBaritoneSettings().setInteractionPaused(true);
    }
    private void stopEat(AltoClef mod) {
        if (_isTryingToEat) {
            MinecraftClient.getInstance().options.keyUse.setPressed(false);
            mod.getExtraBaritoneSettings().setInteractionPaused(false);
            _isTryingToEat = false;
            _requestFillup = false;
        }
    }

    public boolean needsToEat(AltoClef mod) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
        int foodLevel = player.getHungerManager().getFoodLevel();
        float health = player.getHealth();

        //Debug.logMessage("FOOD: " + foodLevel + " -- HEALTH: " + health);

        // TODO: Threshold behaviour preferences
        if (foodLevel >= 20) {
            // We can't eat.
            return false;
        } else {
            // Eat if we're desparate/need to heal ASAP
            if (player.isOnFire() || player.hasStatusEffect(StatusEffects.WITHER) || health < 6) {
                return true;
            } else if (foodLevel > 10) {
                if (health < 14) {
                    return true;
                }
            } else {
                // We have half hunger
                return true;
            }
        }

        // Eat if we're more than 2.5 units hungry and we have a perfect fit.
        if (foodLevel < 20 - 5) {
            int need = 20 - foodLevel;
            Item best = getBestItemToEat(mod);
            int fills = (best != null && best.getFoodComponent() != null)? best.getFoodComponent().getHunger() : 0;
            if (fills == need) return true;
        }

        return false;
    }

    private Item getBestItemToEat(AltoClef mod) {
        int foodToFill = 20 - mod.getPlayer().getHungerManager().getFoodLevel();
        Item bestItem = null;
        int bestDifference = Integer.MAX_VALUE;
        for (ItemStack stack : mod.getInventoryTracker().getAvailableFoods()) {
            FoodComponent f = stack.getItem().getFoodComponent();
            if (f != null) {
                // Ignore spider eyes
                if (stack.getItem() == Items.SPIDER_EYE) {
                    continue;
                }
                int fill = f.getHunger();
                int diff = Math.abs(fill - foodToFill);
                if (diff < bestDifference) {
                    bestDifference = diff;
                    bestItem = stack.getItem();
                }
            }
        }
        return bestItem;
    }

    public boolean isTryingToEat() {
        return _isTryingToEat;
    }

    @Override
    public boolean isActive() {
        // We're always checking for food.
        return true;
    }

    @Override
    public String getName() {
        return "Food";
    }

    @Override
    protected void onStop(AltoClef mod) {
        if (_isTryingToEat) {
            MinecraftClient.getInstance().options.keyUse.setPressed(false);
            _isTryingToEat = false;
            _requestFillup = false;
            mod.getExtraBaritoneSettings().setInteractionPaused(false);
        }
        super.onStop(mod);
    }



    // If we need to eat like, NOW.
    public boolean needsToEatCritical(AltoClef mod) {
        // Don't do this if we have no food
        if (mod.getInventoryTracker().totalFoodScore() <= 0) return false;
        int foodLevel = mod.getPlayer().getHungerManager().getFoodLevel();
        float health = mod.getPlayer().getHealth();
        int armor = mod.getPlayer().getArmor();
        if (health < 3 && foodLevel < 3) return false; // RUN NOT EAT
        if (armor >= 15 && foodLevel < 3) return true; // EAT WE CAN TAKE A FEW HITS
        return false;
    }
}
