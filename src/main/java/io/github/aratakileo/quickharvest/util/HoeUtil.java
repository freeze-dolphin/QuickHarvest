package io.github.aratakileo.quickharvest.util;

import io.github.aratakileo.quickharvest.QuickHarvest;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class HoeUtil {

    private static final @NotNull FileConfiguration config = QuickHarvest.getInstance().getConfig();
    private static final @NotNull Random rnd = new Random(114514L);

    public static void damage(@Nullable Player plr, @NotNull ItemStack item, int originalAmount) {
        if (config.contains("feature.hoe-dura-cost") && config.getBoolean("feature.hoe-dura-cost")) {
            if (item.getType() == Material.AIR || !item.getType().isItem()) {
                return;
            }

            int amount = originalAmount;

            if (item.containsEnchantment(Enchantment.DURABILITY)) {
                int unbreakingLevel = item.getEnchantmentLevel(Enchantment.DURABILITY);
                double chance = 1.0 / (unbreakingLevel + 1);
                for (int i = 0; i < amount; i++) {
                    if (rnd.nextDouble() > chance) {
                        amount--;
                    }
                }
            }

            int currentDamage = item.getDurability();
            int maxDurab = item.getType().getMaxDurability();
            int newDamage = currentDamage + amount;

            if (newDamage >= maxDurab) {

                if (plr != null) {
                    plr.getWorld().playSound(plr.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    plr.getWorld().spawnParticle(Particle.ITEM_CRACK, plr.getLocation(), 1, item);
                    PlayerItemBreakEvent breakEvent = new PlayerItemBreakEvent(plr, item);
                    plr.incrementStatistic(Statistic.BREAK_ITEM, item.getType());
                    breakEvent.callEvent();
                }

                item.setAmount(0);
            } else {
                item.setDurability((short) newDamage);

                if (plr != null) {
                    PlayerItemDamageEvent evt = new PlayerItemDamageEvent(plr, item, amount, originalAmount);
                    plr.incrementStatistic(Statistic.USE_ITEM, item.getType());
                    evt.callEvent();
                }
            }
        }
    }
}
