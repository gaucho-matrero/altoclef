package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.Input;
import javafx.scene.input.KeyCode;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.mixin.client.keybinding.KeyCodeAccessor;
import net.java.games.input.Component;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class FoodChain extends SingleTaskChain {

    private boolean _isTryingToEat = false;
    private boolean _requestFillup = false;

    private static final int RIGHT_CLICK_KEY = 1 - 100;

    int test = 0;

    public FoodChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    public float getPriority(AltoClef mod) {

        /*
        - Eats if:
        - We're hungry and have food that easily fits
            - We're low on health and maybe a little bit hungry
            - We're very low on health and are even slightly hungry
         */

        if (Input.isKeyPressed(GLFW.GLFW_KEY_UP)) {
            test ++;
        } else if (Input.isKeyPressed(GLFW.GLFW_KEY_DOWN)) {
            test --;
        }

        // If we requested a fillup but we're full, stop.
        if (_requestFillup && mod.getPlayer().getHungerManager().getFoodLevel() == 20) {
            _requestFillup = false;
        }

        if (needsToEat() || _requestFillup) {
            Item toUse = getBestItemToEat(mod);
            if (toUse != null) {
                _requestFillup = true;
                Debug.logInternal("EATING " + toUse.getTranslationKey() + " : " + test);
                _isTryingToEat = true;
                _requestFillup = true;
                mod.getInventoryTracker().equipItem(toUse);
                MinecraftClient.getInstance().options.keyUse.setPressed(true);
            } else {
                _isTryingToEat = false;
                _requestFillup = false;
            }
        } else if (_isTryingToEat) {
            MinecraftClient.getInstance().options.keyUse.setPressed(false);
            _isTryingToEat = false;
            _requestFillup = false;
        }

        // Pause interactions when eating.
        mod.getExtraBaritoneSettings()._pauseInteractions = _isTryingToEat;

        // Food eating is handled asynchronously.
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        // Nothing.
    }

    public boolean needsToEat() {
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
            if (player.isOnFire() || health < 6) {
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
        return false;
    }

    private Item getBestItemToEat(AltoClef mod) {
        int foodToFill = 20 - mod.getPlayer().getHungerManager().getFoodLevel();
        Item bestItem = null;
        int bestDifference = Integer.MAX_VALUE;
        for (ItemStack stack : mod.getInventoryTracker().getAvailableFoods()) {
            FoodComponent f = stack.getItem().getFoodComponent();
            if (f != null) {
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
            mod.getExtraBaritoneSettings()._pauseInteractions = false;
        }
        super.onStop(mod);
    }
}
