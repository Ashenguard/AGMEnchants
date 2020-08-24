package me.ashenguard.agmenchants.listeners;

import me.ashenguard.agmenchants.AGMEnchants;
import me.ashenguard.agmenchants.enchants.CustomEnchantment;
import me.ashenguard.agmenchants.enchants.EnchantmentManager;
import me.ashenguard.agmenchants.enchants.EnchantmentMultiplier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

import java.util.HashMap;
import java.util.Map;

import static org.bukkit.Bukkit.getServer;

public class Anvil implements Listener {
    public Anvil() {
        getServer().getPluginManager().registerEvents(this, AGMEnchants.getInstance());

        AGMEnchants.Messenger.Debug("Listeners", "Anvil has been implemented");
    }

    @EventHandler
    public void Event(PrepareAnvilEvent event) {
        ItemStack item = event.getInventory().getItem(0);
        ItemStack sacrifice = event.getInventory().getItem(1);

        if (item == null || item.getType().equals(Material.AIR)) return;
        if (sacrifice == null || !(sacrifice.getType().equals(Material.ENCHANTED_BOOK) || item.getType().equals(sacrifice.getType()))) return;

        // ---- Get all required things ---- //
        Map<Enchantment, Integer> oldEnchants = item.getEnchantments();
        Map<Enchantment, Integer> newEnchants;
        if (sacrifice.getType().equals(Material.ENCHANTED_BOOK)) newEnchants = ((EnchantmentStorageMeta) sacrifice.getItemMeta()).getStoredEnchants();
        else newEnchants = sacrifice.getEnchantments();

        Map<CustomEnchantment, Integer> oldCustomEnchants = EnchantmentManager.extractEnchantments(item);
        Map<CustomEnchantment, Integer> newCustomEnchants = EnchantmentManager.extractEnchantments(sacrifice);

        // ---- Prior Penalty ---- //
        int initCost = 0;
        if (item.getItemMeta() instanceof Repairable)
            initCost = ((Repairable) item.getItemMeta()).getRepairCost();

        int repairCost = initCost;

        // ---- Create Result ---- //
        ItemStack result = item.clone();
        ItemMeta resultMeta = result.getItemMeta();

        // ---- Naming and durability ---- //
        String name = event.getInventory().getRenameText();
        if (name != null && !name.equals("")) {
            resultMeta.setDisplayName(name);
            result.setItemMeta(resultMeta);
            repairCost += 1;
        }
        if (item.getType().equals(sacrifice.getType())) {
            if (result.getDurability() < result.getType().getMaxDurability()) {
                short durability = (short) (item.getDurability() + sacrifice.getDurability() + Math.floor(item.getType().getMaxDurability() / 20.0));
                result.setDurability((short) Math.max(item.getType().getMaxDurability(), durability));
                repairCost += 2;
            }
        }

        // ---- Vanilla enchantments ---- //
        Map<Enchantment, Integer> enchants = new HashMap<>(oldEnchants);
        for (Map.Entry<Enchantment, Integer> entry : newEnchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (!EnchantmentManager.canEnchantItem(enchantment, result)) {
                repairCost += 1;
                continue;
            }
            int level = entry.getValue();
            int oldLevel = enchants.getOrDefault(enchantment, 0);

            if (level > oldLevel) {
                enchants.put(enchantment, level);
                repairCost += level * EnchantmentMultiplier.getMultiplier(enchantment).get(sacrifice.getType().equals(Material.ENCHANTED_BOOK));
            } else if (level == oldLevel) {
                enchants.put(enchantment, Math.min(level + 1, enchantment.getMaxLevel()));
                repairCost += Math.min(level + 1, enchantment.getMaxLevel())  * EnchantmentMultiplier.getMultiplier(enchantment).get(sacrifice.getType().equals(Material.ENCHANTED_BOOK));
            } else {
                enchants.put(enchantment, oldLevel);
                repairCost += oldLevel * EnchantmentMultiplier.getMultiplier(enchantment).get(sacrifice.getType().equals(Material.ENCHANTED_BOOK));
            }
        }
        result.addUnsafeEnchantments(enchants);

        // ---- Custom enchantments ---- //
        Map<CustomEnchantment, Integer> customEnchants = new HashMap<>(oldCustomEnchants);
        for (Map.Entry<CustomEnchantment, Integer> entry : newCustomEnchants.entrySet()) {
            CustomEnchantment enchantment = entry.getKey();
            if (!EnchantmentManager.canEnchantItem(enchantment, result)) {
                repairCost += 1;
                continue;
            }
            int level = entry.getValue();
            int oldLevel = customEnchants.getOrDefault(enchantment, 0);

            if (level > oldLevel) {
                customEnchants.put(enchantment, level);
                repairCost += level * EnchantmentMultiplier.getMultiplier(enchantment).get(sacrifice.getType().equals(Material.ENCHANTED_BOOK));
            } else if (level == oldLevel) {
                customEnchants.put(enchantment, Math.min(level + 1, enchantment.getMaxLevel()));
                repairCost += Math.min(level + 1, enchantment.getMaxLevel()) * EnchantmentMultiplier.getMultiplier(enchantment).get(sacrifice.getType().equals(Material.ENCHANTED_BOOK));
            } else {
                customEnchants.put(enchantment, oldLevel);
                repairCost += oldLevel * EnchantmentMultiplier.getMultiplier(enchantment).get(sacrifice.getType().equals(Material.ENCHANTED_BOOK));
            }
        }
        EnchantmentManager.addEnchantments(result, customEnchants);

        // ---- Set result ---- //
        if (initCost < repairCost) {
            if (!(name != null && !name.equals("") && repairCost == initCost + 1)) {
                if (result.getItemMeta() instanceof Repairable) {
                    ItemMeta itemMeta = result.getItemMeta();
                    ((Repairable) itemMeta).setRepairCost((initCost * 2) + 1);
                    result.setItemMeta(itemMeta);
                }
            }

            event.setResult(result);

            int finalCost = repairCost;
            getServer().getScheduler().runTask(AGMEnchants.getInstance(), () -> event.getInventory().setRepairCost(finalCost));
        }
    }
}
