package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Settings;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.LookUtil;
import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class FoodChain extends SingleTaskChain {

    private static final int RIGHT_CLICK_KEY = 1 - 100;
    private boolean _isTryingToEat = false;
    private boolean _requestFillup = false;
    private boolean _needsFood = false;

    public FoodChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    public float getPriority(AltoClef mod) {

        if (!AltoClef.inGame()) {
            return Float.NEGATIVE_INFINITY;
        }

        if (!mod.getModSettings().isAutoEat()) {
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
        if (mod.getMobDefenseChain().isDoingAcrobatics() || mod.getMLGBucketChain().isFallingOhNo(mod)) {
            stopEat(mod);
            return Float.NEGATIVE_INFINITY;
        }

        boolean hasFood = mod.getInventoryTracker().totalFoodScore() != 0;

        // If we requested a fillup but we're full, stop.
        if (_requestFillup && mod.getPlayer().getHungerManager().getFoodLevel() == 20) {
            _requestFillup = false;
        }
        // If we no longer have food, we no longer can eat.
        if (!hasFood) {
            _requestFillup = false;
        }

        if (hasFood && (needsToEat(mod) || _requestFillup)) {
            Item toUse = getBestItemToEat(mod);
            if (toUse != null) {

                // Make sure we're not facing a container
                if (!LookUtil.tryAvoidingInteractable(mod)) {
                    return Float.NEGATIVE_INFINITY;
                }

                startEat(mod, toUse);
            } else {
                stopEat(mod);
            }
        } else if (_isTryingToEat) {
            stopEat(mod);
        }

        Settings settings = mod.getModSettings();

        int foodScore = mod.getInventoryTracker().totalFoodScore();

        if (_needsFood || foodScore < settings.getMinimumFoodAllowed()) {
            _needsFood = foodScore < settings.getFoodUnitsToCollect();

            // Only collect if we don't have enough food.
            // If the user inputs invalid settings, the bot would get stuck here.
            if (foodScore < settings.getFoodUnitsToCollect()) {
                setTask(new CollectFoodTask(settings.getFoodUnitsToCollect()));
                return 55f;
            }
        }


        // Food eating is handled asynchronously.
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        // Nothing.
    }

    private void startEat(AltoClef mod, Item food) {
        //Debug.logInternal("EATING " + toUse.getTranslationKey() + " : " + test);
        _isTryingToEat = true;
        _requestFillup = true;
        mod.getInventoryTracker().equipItem(food);
        mod.getInputControls().hold(Input.CLICK_RIGHT);
        mod.getExtraBaritoneSettings().setInteractionPaused(true);
    }

    private void stopEat(AltoClef mod) {
        if (_isTryingToEat) {
            mod.getInputControls().release(Input.CLICK_RIGHT);
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
            int fills = (best != null && best.getFoodComponent() != null) ? best.getFoodComponent().getHunger() : 0;
            return fills == need;
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
                if (stack.getItem() == Items.ROTTEN_FLESH) {
                    // Eat rotten flesh only if it's the only thing we have.
                    diff = 99999; // hmm feels kinda bad but it should work
                }
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
            mod.getInputControls().release(Input.CLICK_RIGHT);
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
        return armor >= 15 && foodLevel < 3; // EAT WE CAN TAKE A FEW HITS
    }
}
