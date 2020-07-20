package me.ashenguard.agmenchants.enchants;

import me.ashenguard.agmenchants.AGMEnchants;
import me.ashenguard.agmenchants.api.Messenger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class CustomEnchantment {
    protected final boolean treasure;
    protected final boolean cursed;
    protected final int maxLevel;

    protected File configFile;
    protected YamlConfiguration config;

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException exception) {
            Messenger.ExceptionHandler(exception);
        }
    }

    protected BukkitScheduler scheduler = Bukkit.getScheduler();

    protected String name;
    protected String description;

    protected List<String> applicable;

    public CustomEnchantment(String name) {
        File configFolder = new File(AGMEnchants.getEnchantsFolder(), "configs");
        if (!configFolder.exists() && configFolder.mkdirs())
            Messenger.Debug("General", "Config folder wasn't found, A new one created");

        configFile = new File(configFolder, name + ".yml");
        if (!configFile.exists()) {
            config = new YamlConfiguration();
            HashMap<String, Object> defaults = getDefaultConfig();
            for (String path : defaults.keySet())
                config.set(path, defaults.get(path));

            saveConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        this.name = name;
        this.description = config.getString("Description", "");
        this.applicable = config.getStringList("Applicable");
        this.maxLevel = config.getInt("MaxLevel", 1);
        this.treasure = config.getBoolean("Treasure", false);
        this.cursed = config.getBoolean("Cursed", false);

        EnchantmentManager.save(this);
    }

    public boolean isApplicable(Material material) {
        if (material == null) return false;
        if (applicable.contains(material.name())) return true;
        for (String applicableName : applicable) {
            List<String> applicable = AGMEnchants.config.getStringList("ItemsList." + applicableName);
            if (applicable.contains(material.name())) return true;
        }
        return false;
    }
    public boolean canEnchantItem(ItemStack item) {
        if (item == null) return false;
        if (item.getType().equals(Material.ENCHANTED_BOOK)) return true;
        return isApplicable(item.getType());
    }
    public ItemStack getBook(int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentManager.addEnchantment(book, this, level);
        return book;
    }
    public ItemStack getInfoBook() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta itemMeta = book.getItemMeta();
        itemMeta.setDisplayName(EnchantmentManager.getColoredName(this));

        List<String> lore = new ArrayList<>();
        lore.add(getDescription());
        lore.add("§m----------------------");
        lore.add("Levels: " + getMaxLevel());
        itemMeta.setLore(lore);
        book.setItemMeta(itemMeta);

        return book;
    }
    public ItemStack getInfoBook(int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta itemMeta = book.getItemMeta();
        itemMeta.setDisplayName(EnchantmentManager.getColoredName(this));

        List<String> lore = new ArrayList<>();
        lore.add(getDescription());
        lore.add("§m----------------------");
        lore.addAll(getLevelDetails(level));
        itemMeta.setLore(lore);
        book.setItemMeta(itemMeta);

        return book;
    }

    protected abstract HashMap<String, Object> getDefaultConfig();
    protected abstract List<String> getLevelDetails(int level);

    public String getName() {
        return name;
    }
    public String getDescription() { return description; }

    public boolean isCursed() {
        return cursed;
    }
    public boolean isTreasure() {
        return treasure;
    }
    public int getMaxLevel() {
        return maxLevel;
    }
}
