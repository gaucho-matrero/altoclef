package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.Input;
import baritone.Baritone;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.RayTraceUtils;
import baritone.api.utils.Rotation;
import javafx.scene.input.KeyCode;
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

        if (!mod.getModSettings().isAutoEat()) {
            return Float.NEGATIVE_INFINITY;
        }

        /*
        - Eats if:
        - We're hungry and have food that easily fits
            - We're low on health and maybe a little bit hungry
            - We're very low on health and are even slightly hungry
         */


        // We're in danger, don't eat now!!
        if (mod.getMobDefenseChain().isDoingAcrobatics()) {
            return Float.NEGATIVE_INFINITY;
        }

        // If we requested a fillup but we're full, stop.
        if (_requestFillup && mod.getPlayer().getHungerManager().getFoodLevel() == 20) {
            _requestFillup = false;
        }

        if (needsToEat() || _requestFillup) {
            Item toUse = getBestItemToEat(mod);
            if (toUse != null) {
                //Debug.logInternal("EATING " + toUse.getTranslationKey() + " : " + test);
                _isTryingToEat = true;
                _requestFillup = true;

                // Make sure we're not facing a container
                if (isCollidingContainer(mod)) {
                    randomOrientation(mod);
                    return Float.NEGATIVE_INFINITY;
                }

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
        mod.getExtraBaritoneSettings().setInteractionPaused(_isTryingToEat);

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
            mod.getExtraBaritoneSettings().setInteractionPaused(false);
        }
        super.onStop(mod);
    }

    private boolean isCollidingContainer(AltoClef mod) {

        if (!(mod.getPlayer().currentScreenHandler instanceof PlayerScreenHandler)) {
            mod.getPlayer().closeHandledScreen();
            return true;
        }

        IPlayerContext ctx = mod.getClientBaritone().getPlayerContext();
        HitResult result = MinecraftClient.getInstance().crosshairTarget;
        if (result == null) return false;
        if (result.getType() == HitResult.Type.BLOCK) {
            Block block = mod.getWorld().getBlockState(new BlockPos(result.getPos())).getBlock();
            if (block instanceof ChestBlock
                    || block instanceof EnderChestBlock
                    || block instanceof CraftingTableBlock
                    || block instanceof AbstractFurnaceBlock
                    || block instanceof LoomBlock
                    || block instanceof CartographyTableBlock
                    || block instanceof EnchantingTableBlock
            ) {
                return true;
            }
        } else if (result.getType() == HitResult.Type.ENTITY) {
            if (result instanceof EntityHitResult) {
                Entity entity = ((EntityHitResult) result).getEntity();
                if (entity instanceof MerchantEntity) {
                    return true;
                }
            }
        }
        return false;
    }

    private void randomOrientation(AltoClef mod) {
        Rotation r = new Rotation((float)Math.random() * 360f, (float)Math.random() * 360f);
        mod.getClientBaritone().getLookBehavior().updateTarget(r, true);
    }
}
