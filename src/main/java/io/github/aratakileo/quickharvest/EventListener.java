package io.github.aratakileo.quickharvest;

import io.github.aratakileo.quickharvest.util.HoeUtil;
import io.papermc.paper.event.block.BlockPreDispenseEvent;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;
import io.github.aratakileo.quickharvest.util.DropItemUtil;
import io.github.aratakileo.quickharvest.util.SoundUtil;

public class EventListener implements Listener {
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        @NotNull FileConfiguration config = QuickHarvest.getInstance().getConfig();

        if ("no false disabled".contains(
                config.getString("feature.player")
        ) || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = e.getClickedBlock();
        String blockKey = block.getType().getKey().asString();

        if (!config.getStringList("targets").contains(blockKey)) return;

        ItemStack itemInHandStack = e.getItem();
        if (itemInHandStack == null) return;

        String itemInHandKey = e.getMaterial().getKey().asString();

        if (!itemInHandKey.endsWith("_hoe")) return;

        Ageable ageable = (Ageable) block.getBlockData();

        if (ageable.getAge() != ageable.getMaximumAge()) return;

        if (config.getString("feature.player").equals("vanilla")) {
            block.getDrops().forEach(itemStack -> DropItemUtil.drop(block, itemStack));

            HoeUtil.damage(e.getPlayer(), itemInHandStack, 1);

            SoundUtil.playSound(block, config.getString("sound"));
            ageable.setAge(0);
            block.setBlockData(ageable);

            return;
        }

        Inventory pInv = e.getPlayer().getInventory();

        for (ItemStack dropItemStack : block.getDrops()) {
            if (itemInHandStack != null && itemInHandStack.getType() == dropItemStack.getType())
                dropItemStack.setAmount(dropItemStack.getAmount() - 1);

            if (pInv.firstEmpty() == -1) {
                int allInvAmount = 0;

                for (ItemStack invItemStack : pInv.getContents()) {
                    if (invItemStack != null && invItemStack.getType() == dropItemStack.getType())
                        allInvAmount += invItemStack.getAmount();
                }

                int freeAmount = dropItemStack.getMaxStackSize();
                freeAmount = freeAmount - (allInvAmount % freeAmount);
                freeAmount = freeAmount == dropItemStack.getMaxStackSize() ? 0 : freeAmount;

                if (dropItemStack.getAmount() <= freeAmount) {
                    pInv.addItem(dropItemStack);
                    continue;
                }

                int dropItemDropAmount = dropItemStack.getAmount() - freeAmount;

                dropItemStack.setAmount(freeAmount);
                pInv.addItem(dropItemStack.clone());

                dropItemStack.setAmount(dropItemDropAmount);
                DropItemUtil.drop(block, dropItemStack);

                continue;
            }

            pInv.addItem(dropItemStack);
        }

        SoundUtil.playSound(block, config.getString("sound"));
        ageable.setAge(0);
        block.setBlockData(ageable);
    }

    @EventHandler
    public void onDispenserEvent(BlockPreDispenseEvent e) {
        Block dispenser = e.getBlock();
        @NotNull FileConfiguration config = QuickHarvest.getInstance().getConfig();

        if (!config.getBoolean("feature.dispenser") || dispenser.getType() != Material.DISPENSER) return;

        ItemStack itemInHandStack = e.getItemStack();
        String itemInHandKey = itemInHandStack.getType().getKey().asString();
        if (!itemInHandKey.endsWith("_hoe")) return;

        Block cropBlock = dispenser.getRelative(((Directional) dispenser.getBlockData()).getFacing());

        if (
                !config.getStringList("targets").contains(
                        cropBlock
                                .getType()
                                .getKey()
                                .asString()
                )
        ) return;

        Ageable ageable = (Ageable) cropBlock.getBlockData();

        if (ageable.getMaximumAge() != ageable.getAge()) return;

        cropBlock.getDrops().forEach(itemStack -> DropItemUtil.drop(cropBlock, itemStack));

        HoeUtil.damage(null, itemInHandStack, 1);

        dispenser.getWorld().spawnParticle(Particle.SMOKE_NORMAL, cropBlock.getLocation(), 100);

        SoundUtil.playSound(cropBlock, Sound.BLOCK_DISPENSER_FAIL);
        SoundUtil.playSound(cropBlock, config.getString("sound"));

        ageable.setAge(0);
        cropBlock.setBlockData(ageable);
        e.setCancelled(true);
    }
}
