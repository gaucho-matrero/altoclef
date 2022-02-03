package adris.altoclef.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Settings;
import adris.altoclef.chains.FoodChain.FoodChainConfig;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.helpers.ConfigHelper;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Pair;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class FoodChain extends SingleTaskChain {

    private static FoodChainConfig _config;
    static {
        ConfigHelper.loadConfig("configs/food_chain_settings.json", FoodChainConfig::new, FoodChainConfig.class, newConfig -> _config = newConfig);
    }

    private boolean _isTryingToEat = false;
    private boolean _requestFillup = false;
    private boolean _needsFood = false;

    private int _cachedFoodScore;
    private Optional<Item> _cachedPerfectFood = Optional.empty();

    public FoodChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    public float getPriority(AltoClef mod) {

        if (!AltoClef.inGame()) {
            stopEat(mod);
            return Float.NEGATIVE_INFINITY;
        }

        if (!mod.getModSettings().isAutoEat()) {
            stopEat(mod);
            return Float.NEGATIVE_INFINITY;
        }

        // do NOT eat while in lava if we are escaping it (spaghetti code dependencies go brrrr)
        if (mod.getPlayer().isInLava()) {
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

        Pair<Integer, Optional<Item>> calculation = calculateFood(mod);
        _cachedFoodScore = calculation.getLeft();
        _cachedPerfectFood = calculation.getRight();

        boolean hasFood = _cachedFoodScore > 0;

        // If we requested a fillup but we're full, stop.
        if (_requestFillup && mod.getPlayer().getHungerManager().getFoodLevel() == 20) {
            _requestFillup = false;
        }
        // If we no longer have food, we no longer can eat.
        if (!hasFood) {
            _requestFillup = false;
        }

        if (hasFood && (needsToEat(mod) || _requestFillup) && _cachedPerfectFood.isPresent() && !mod.getMLGBucketChain().isChorusFruiting()) {
            Item toUse = _cachedPerfectFood.get();
            // Make sure we're not facing a container
            if (!LookHelper.tryAvoidingInteractable(mod)) {
                return Float.NEGATIVE_INFINITY;
            }
            startEat(mod, toUse);
        } else {
            stopEat(mod);
        }

        Settings settings = mod.getModSettings();

        if (_needsFood || _cachedFoodScore < settings.getMinimumFoodAllowed()) {
            _needsFood = _cachedFoodScore < settings.getFoodUnitsToCollect();

            // Only collect if we don't have enough food.
            // If the user inputs invalid settings, the bot would get stuck here.
            if (_cachedFoodScore < settings.getFoodUnitsToCollect()) {
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
        mod.getSlotHandler().forceEquipItem(new Item[]{food}, true); //"true" because it's food
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
        if (foodLevel >= 20) {
            // We can't eat.
            return false;
        } else {
            // Eat if we're desparate/need to heal ASAP
            if (player.isOnFire() || player.hasStatusEffect(StatusEffects.WITHER) || health < _config.alwaysEatWhenWitherOrFireAndHealthBelow) {
                return true;
            } else if (foodLevel > _config.alwaysEatWhenBelowHunger) {
                if (health < _config.alwaysEatWhenBelowHealth) {
                    return true;
                }
            } else {
                // We have half hunger
                return true;
            }
        }

        // Eat if we're  units hungry and we have a perfect fit.
        if (foodLevel < _config.alwaysEatWhenBelowHungerAndPerfectFit && _cachedPerfectFood.isPresent()) {
            int need = 20 - foodLevel;
            Item best = _cachedPerfectFood.get();
            int fills = (best.getFoodComponent() != null) ? best.getFoodComponent().getHunger() : -1;
            return fills == need;
        }

        return false;
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
        stopEat(mod);
        super.onStop(mod);
    }

    // If we need to eat like, NOW.
    public boolean needsToEatCritical(AltoClef mod) {
        int foodLevel = mod.getPlayer().getHungerManager().getFoodLevel();
        float health = mod.getPlayer().getHealth();
        int armor = mod.getPlayer().getArmor();
        if (health < _config.runDontEatMaxHealth && foodLevel < _config.runDontEatMaxHunger) return false; // RUN NOT EAT
        return armor >= _config.canTankHitsAndEatArmor && foodLevel < _config.canTankHitsAndEatMaxHunger; // EAT WE CAN TAKE A FEW HITS
    }

    private Pair<Integer, Optional<Item>> calculateFood(AltoClef mod) {
        Item bestFood = null;
        double bestFoodScore = Double.NEGATIVE_INFINITY;
        int foodTotal = 0;
        ClientPlayerEntity player = mod.getPlayer();
        float health = player != null? player.getHealth() : 20;
        //float toHeal = player != null? 20 - player.getHealth() : 0;
        float hunger = player != null? player.getHungerManager().getFoodLevel() : 20;
        float saturation = player != null? player.getHungerManager().getSaturationLevel() : 20;
        // Get best food item + calculate food total
        for (ItemStack stack : mod.getItemStorage().getItemStacksPlayerInventory(true)) {
            if (stack.isFood()) {
                // Ignore protected items
                if (!ItemHelper.canThrowAwayStack(mod, stack)) continue;

                // Ignore spider eyes
                if (stack.getItem() == Items.SPIDER_EYE) {
                    continue;
                }

                FoodComponent food = stack.getItem().getFoodComponent();

                assert food != null;
                float hungerIfEaten = Math.min(hunger + food.getHunger(), 20);
                float saturationIfEaten = Math.min(hungerIfEaten, saturation + food.getSaturationModifier());
                float gainedSaturation = (saturationIfEaten - saturation);
                float gainedHunger = (hungerIfEaten - hunger);
                float hungerNotFilled = 20 - hungerIfEaten;

                float saturationWasted = food.getSaturationModifier() - gainedSaturation;
                float hungerWasted = food.getHunger() - gainedHunger;

                boolean prioritizeSaturation = health < _config.prioritizeSaturationWhenBelowHealth;
                float saturationGoodScore = prioritizeSaturation ? gainedSaturation * _config.foodPickPrioritizeSaturationSaturationMultiplier : gainedSaturation;
                float saturationLossPenalty = prioritizeSaturation ? 0 : saturationWasted * _config.foodPickSaturationWastePenaltyMultiplier;
                float hungerLossPenalty = hungerWasted * _config.foodPickHungerWastePenaltyMultiplier;
                float hungerNotFilledPenalty = hungerNotFilled * _config.foodPickHungerNotFilledPenaltyMultiplier;

                float score = saturationGoodScore - saturationLossPenalty - hungerLossPenalty - hungerNotFilledPenalty;

                if (stack.getItem() == Items.ROTTEN_FLESH) {
                    score -= _config.foodPickRottenFleshPenalty;
                }
                if (score > bestFoodScore) {
                    bestFoodScore = score;
                    bestFood = stack.getItem();
                }

                foodTotal += Objects.requireNonNull(stack.getItem().getFoodComponent()).getHunger() * stack.getCount();
            }
        }

        return new Pair<>(foodTotal, Optional.ofNullable(bestFood));
    }

    static class FoodChainConfig {
        public int alwaysEatWhenWitherOrFireAndHealthBelow = 6;
        public int alwaysEatWhenBelowHunger = 10;
        public int alwaysEatWhenBelowHealth = 14;
        public int alwaysEatWhenBelowHungerAndPerfectFit = 20 - 5;
        public int prioritizeSaturationWhenBelowHealth = 8;
        public float foodPickPrioritizeSaturationSaturationMultiplier = 8;
        public float foodPickSaturationWastePenaltyMultiplier = 1;
        public float foodPickHungerWastePenaltyMultiplier = 2;
        public float foodPickHungerNotFilledPenaltyMultiplier = 1;
        public float foodPickRottenFleshPenalty = 100;
        public float runDontEatMaxHealth = 3;
        public int runDontEatMaxHunger = 3;
        public int canTankHitsAndEatArmor = 15;
        public int canTankHitsAndEatMaxHunger = 3;
    }
}
