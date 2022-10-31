package adris.altoclef.util.helpers;

import adris.altoclef.AltoClef;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Helper functions to interpret entity state
 */
public class EntityHelper {
    public static final double ENTITY_GRAVITY = 0.08; // per second

    public static boolean isAngryAtPlayer(AltoClef mod, Entity mob) {
        boolean hostile = isGenerallyHostileToPlayer(mod, mob);
        if (mob instanceof LivingEntity entity) {
            return hostile && entity.canSee(mod.getPlayer());
        }
        return hostile;
    }
    public static boolean isGenerallyHostileToPlayer(AltoClef mod, Entity hostile) {
        if (hostile instanceof MobEntity entity) {
            if (entity instanceof HostileEntity entity1) {
                return entity1.isAttacking() || !(entity1 instanceof EndermanEntity || entity1 instanceof PiglinEntity ||
                        entity1 instanceof SpiderEntity || entity1 instanceof ZombifiedPiglinEntity);
            }
            if (entity instanceof SlimeEntity entity1) {
                return entity1.canSee(mod.getPlayer());
            }
            return entity.isAttacking();
        }
        return !isTradingPiglin(hostile);
    }

    public static boolean isTradingPiglin(Entity entity) {
        if (entity instanceof PiglinEntity pig) {
            for (ItemStack stack : pig.getHandItems()) {
                if (stack.getItem().equals(Items.GOLD_INGOT)) {
                    // We're trading with this one, ignore it.
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Calculate the resulting damage dealt to a player as a result of some damage.
     * If this player were to receive this damage, the player's health will be subtracted by the resulting value.
     */
    public static double calculateResultingPlayerDamage(PlayerEntity player, DamageSource source, double damageAmount) {
        // Copied logic from `PlayerEntity.applyDamage`

        if (player.isInvulnerableTo(source))
            return 0;

        // Armor Base
        if (!source.bypassesArmor()) {
            damageAmount = DamageUtil.getDamageLeft((float) damageAmount, (float) player.getArmor(), (float) player.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
        }

        // Enchantments & Potions
        if (!source.isUnblockable()) {
            int k;
            if (player.hasStatusEffect(StatusEffects.RESISTANCE) && source != DamageSource.OUT_OF_WORLD) {
                //noinspection ConstantConditions
                k = (player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1) * 5;
                int j = 25 - k;
                double f = damageAmount * (double)j;
                double g = damageAmount;
                damageAmount = Math.max(f / 25.0F, 0.0F);
            }

            if (damageAmount <= 0.0) {
                damageAmount = 0.0;
            } else {
                k = EnchantmentHelper.getProtectionAmount(player.getArmorItems(), source);
                if (k > 0) {
                    damageAmount = DamageUtil.getInflictedDamage((float)damageAmount, (float)k);
                }
            }
        }

        // Absorption
        damageAmount = Math.max(damageAmount - player.getAbsorptionAmount(), 0.0F);
        return damageAmount;
    }
}
