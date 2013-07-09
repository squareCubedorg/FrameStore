package me.maciekmm.FrameStore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

public class FrameStore extends JavaPlugin {

    private BukkitTask saver;
    public static Economy econ = null;
    public final static Logger log = Bukkit.getLogger();
    public static String type;
    private List<String> lores = new ArrayList<>();
    private ItemStack is;
    public static boolean update = false;
    public static boolean debug = false;

    @Override
    public void onEnable() {
        setupEconomy();
        new ShopListeners(this);
        loadConfiguration();
        debug = getConfig().getBoolean("debug");
        type = getConfig().getString("database.type");
        if (type.equalsIgnoreCase("mysql")) {
            Database.db = new DatabaseConnector(this, getConfig().getString("database.addr"), getConfig().getString("database.database"), getConfig().getString("database.login"), getConfig().getString("database.password"));
            log.info("[FrameStore] MySQL support enabled.");
        } else {
            Database.db = new DatabaseConnector(this, this.getDataFolder() + File.separator + "shops");
            log.info("[FrameStore] Sqlite support enabled.");
        }
        Database.createTables();
        is = new ItemStack(Material.ITEM_FRAME, 1);
        ItemMeta im = is.getItemMeta();
        lores.add(getConfig().getString("shopitem.lore"));
        im.setDisplayName(getConfig().getString("shopitem.name"));
        im.setLore(lores);
        is.setItemMeta(im);
        ShapelessRecipe r = new ShapelessRecipe(is);
        r.addIngredient(Material.ITEM_FRAME);
        r.addIngredient(Material.CHEST);
        Bukkit.addRecipe(r);
        ShopListeners.functions.loadShops();
        ShopListeners.functions.loadMaps(this);
        long savePeriod = (getConfig().getLong("database.savePeriod") * 60) * 20L;
        long delay = 10000L;
        if (debug) {
            delay = 1200L;
            savePeriod = 1200L;
        }
        saver = this.getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                ShopListeners.functions.dumpToDatabase();
            }
        }, delay, savePeriod);
        if (this.getConfig().getBoolean("updatenotifications")) {

            try {
                FileOutputStream fos;
                URL website = new URL("http://maciekmm.tk/framestore/ver.txt");
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                fos = new FileOutputStream(ShopListeners.frameshop.getDataFolder() + File.separator + "ver.txt");
                fos.getChannel().transferFrom(rbc, 0, 1 << 24);
                try {
                    try (BufferedReader br = new BufferedReader(new FileReader(ShopListeners.frameshop.getDataFolder() + File.separator + "ver.txt"))) {
                        String s;
                        while ((s = br.readLine()) != null && (s = s.trim()).length() > 0) {
                            if (!this.getDescription().getVersion().equals(s)) {
                                update = true;
                            }
                        }
                    }
                    new File(ShopListeners.frameshop.getDataFolder() + File.separator + "ver.txt").deleteOnExit();
                } catch (FileNotFoundException ex) {
                    log.log(Level.SEVERE, null, ex);
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FrameStore.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(FrameStore.class.getName()).log(Level.SEVERE, null, ex);
            }


        }
        if (!new File(this.getDataFolder() + File.separator + "textures" + File.separator + "minecraft.jar").exists() && this.getConfig().getBoolean("downloadimages")) {
            this.getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[1024];
                    FileOutputStream fos = null;
                    try {
                        File folder = new File(ShopListeners.frameshop.getDataFolder() + File.separator + "textures" + File.separator);
                        if (!folder.exists()) {
                            folder.mkdir();
                        }
                        URL website = new URL("https://s3.amazonaws.com/Minecraft.Download/versions/1.6.2/1.6.2.jar");
                        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                        fos = new FileOutputStream(ShopListeners.frameshop.getDataFolder() + File.separator + "textures" + File.separator + "minecraft.jar");
                        fos.getChannel().transferFrom(rbc, 0, 1 << 24);

                        while (!new File(ShopListeners.frameshop.getDataFolder() + "/textures/minecraft.jar").exists()) {
                            Thread.sleep(5);
                        }
                        ZipInputStream zis = new ZipInputStream(new FileInputStream(ShopListeners.frameshop.getDataFolder() + "/textures/minecraft.jar"));
                        ZipEntry ze = zis.getNextEntry();

                        while (ze != null) {

                            if (ze.getName().startsWith("assets/minecraft/textures/items") || ze.getName().startsWith("assets/minecraft/textures/blocks")) {
                                String[] fileName = ze.getName().split("/");
                                File newFile = new File(ShopListeners.frameshop.getDataFolder() + File.separator + fileName[fileName.length - 3] + File.separator + fileName[fileName.length - 2] + File.separator + fileName[fileName.length - 1]);

                                new File(newFile.getParent()).mkdirs();
                                try (FileOutputStream fosf = new FileOutputStream(newFile)) {
                                    int len;
                                    while ((len = zis.read(buffer)) > 0) {
                                        fosf.write(buffer, 0, len);
                                    }
                                }
                                ze = zis.getNextEntry();
                            } else {
                                ze = zis.getNextEntry();


                            }
                        }

                    } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(FrameStore.class
                                .getName()).log(Level.SEVERE, null, ex);

                    } finally {
                        try {
                            if (fos != null) {
                                fos.close();


                            }

                        } catch (IOException ex) {
                            Logger.getLogger(FrameStore.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });
        }
        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (IOException e) {
            log.log(Level.INFO, "Failed to load metrics");
        }

        if (debug) {
            log.log(Level.INFO, "[FrameStore] DEBUG MODE!!!");
        }
    }

    @Override
    public void onDisable() {
        saver.cancel();
        this.saveConfig();
        ShopListeners.functions.clearer();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("fs") || label.equalsIgnoreCase("framestore")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload") && (sender.hasPermission("framestore.admin") || sender.isOp())) {
                    this.reloadConfig();
                    sender.sendMessage(ChatColor.DARK_GREEN + "Successfully reloaded config!");
                    return true;
                } else if (args[0].equalsIgnoreCase("save") && (sender.hasPermission("framestore.admin") || sender.isOp())) {
                    ShopListeners.functions.dumpToDatabase();
                    sender.sendMessage(ChatColor.DARK_GREEN + "Successfully saved shops!");
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.DARK_RED + "Available arguments: reload, save");
                return true;
            }
        }
        return false;
    }

    private void loadConfiguration() {
        this.getConfig().addDefault("database.type", "sqlite");
        this.getConfig().addDefault("database.login", "login");
        this.getConfig().addDefault("database.password", "password");
        this.getConfig().addDefault("database.addr", "localhost");
        this.getConfig().addDefault("database.database", "frameshops");
        this.getConfig().addDefault("database.savePeriod", 30);
        String[] pictures = {
            "260:items/apple.png",
            "322:items/apple_golden.png",
            "262:items/arrow.png",
            "355:items/bed.png",
            "364:items/beef_cooked.png",
            "363:items/beef_raw.png",
            "377:items/blaze_powder.png",
            "369:items/blaze_rod.png",
            "333:items/boat.png",
            "352:items/bone.png",
            "340:items/book_normal.png",
            "305:items/chainmail_boots.png",
            "313:items/diamond_boots.png",
            "317:items/gold_boots.png",
            "309:items/iron_boots.png",
            "261:items/bow_standby.png",
            "281:items/bowl.png",
            "97:items/bread.png",
            "379:blocks/brewing_stand.png",
            "336:items/brick.png",
            "325:items/bucket_normal.png",
            "327:items/bucket_lava.png",
            "326:items/bucket_water.png",
            "354:blocks/cake_top.png",
            "96:items/carrot_golden.png",
            "398:items/carrot_on_a_stick.png",
            "391:items/carrot.png",
            "380:items/cauldron.png",
            "303:items/chainmail_chestplate.png",
            "299:items/leather_chestplate.png",
            "311:items/diamond_chestplate.png",
            "315:items/gold_chestplate.png",
            "307:items/iron_chestplate.png",
            "366:items/chicken_cooked.png",
            "365:items/chicken_raw.png",
            "337:items/clay_ball.png",
            "263:items/coal.png",
            "263@1:items/charcoal.png",
            "404:items/comparator.png",
            "357:items/cookie.png",
            "264:items/diamond.png",
            "356:items/repeater.png",
            "330:items/door_iron.png",
            "324:items/door_wood.png",
            "351@0:items/dye_powder_black.png",
            "351@4:items/dye_powder_blue.png",
            "351@3:items/dye_powder_brown.png",
            "351@6:items/dye_powder_cyan.png",
            "351@8:items/dye_powder_gray.png",
            "351@2:items/dye_powder_green.png",
            "351@12:items/dye_powder_light_blue.png",
            "351@10:items/dye_powder_lime.png",
            "351@13:items/dye_powder_magenta.png",
            "351@14:items/dye_powder_orange.png",
            "351@9:items/dye_powder_pink.png",
            "351@5:items/dye_powder_purple.png",
            "351@1:items/dye_powder_red.png",
            "351@7:items/dye_powder_silver.png",
            "351@15:items/dye_powder_white.png",
            "351@11:items/dye_powder_yellow.png",
            "344:items/egg.png",
            "388:items/emerald.png",
            "395:items/map_empty.png",
            "403:items/book_enchanted.png",
            "368:items/ender_pearl.png",
            "384:items/experience_bottle.png",
            "381:items/ender_eye.png",
            "334:items/leather.png",
            "376:items/spider_eye_fermented.png",
            "385:items/fireball.png",
            "401:items/fireworks.png",
            "402:items/fireworks_charge.png",
            "350:items/fish_cooked.png",
            "346:items/fishing_rod_uncast.png",
            "349:items/fish_raw.png",
            "318:items/flint.png",
            "259:items/flint_and_steel.png",
            "390:items/flower_pot.png",
            "389:items/item_frame.png",
            "370:items/ghast_tear.png",
            "374:items/potion_bottle_empty.png",
            "371:items/gold_nugglet.png",
            "279:items/diamond_axe.png",
            "286:items/gold_axe.png",
            "258:items/iron_axe.png",
            "275:items/stone_axe.png",
            "271:items/wood_axe.png",
            "302:items/chainmail_helmet.png",
            "298:items/leather_helmet.png",
            "310:items/diamond_helmet.png",
            "314:items/gold_helmet.png",
            "306:items/iron_helmet.png",
            "293:items/diamond_hoe.png",
            "294:items/gold_hoe.png",
            "292:items/iron_hoe.png",
            "291:items/stone_hoe.png",
            "290:items/wood_hoe.png",
            "154:items/hopper.png",
            "266:items/gold_ingot.png",
            "265:items/iron_ingot.png",
            "304:items/chainmail_leggings.png",
            "300:items/leather_leggings.png",
            "312:items/diamond_leggings.png",
            "316:items/gold_leggings.png",
            "308:items/iron_leggings.png",
            "378:items/magma_cream.png",
            "358:items/map_filled.png",
            "360:items/melon.png",
            "335:items/bucket_milk.png",
            "328:items/minecart_normal.png",
            "342:items/minecart_chest.png",
            "343:items/minecart_furnace.png",
            "408:items/minecart_hopper.png",
            "407:items/minecart_tnt.png",
            "383:items/spawn_egg.png",
            "282:items/mushroom_stew.png",
            "339:items/paper.png",
            "278:items/diamond_pickaxe.png",
            "285:items/gold_pickaxe.png",
            "257:items/iron_pickaxe.png",
            "274:items/stone_pickaxe.png",
            "270:items/wood_pickaxe.png",
            "320:items/porkchop_cooked.png",
            "319:items/porkchop_raw.png",
            "392:items/potato.png",
            "393:items/potato_baked.png",
            "394:items/potato_poisonous.png",
            "400:items/pumpkin_pie.png",
            "2266:items/record_11.png",
            "2256:items/record_13.png",
            "2258:items/record_blocks.png",
            "2257:items/record_cat.png",
            "2259:items/record_chirp.png",
            "2260:items/record_far.png",
            "2261:items/record_mall.png",
            "2262:items/record_mellohi.png",
            "2263:items/record_stal.png",
            "2264:items/record_strad.png",
            "2267:items/record_wait.png",
            "2265:items/record_ward.png",
            "331:items/redstone_dust.png",
            "338:items/reeds.png",
            "367:items/rotten_flesh.png",
            "329:items/saddle.png",
            "295:items/seeds_wheat.png",
            "362:items/seeds_melon.png",
            "361:items/seeds_pumpkin.png",
            "359:items/shears.png",
            "277:items/diamond_shovel.png",
            "284:items/gold_shovel.png",
            "273:items/stone_shovel.png",
            "269:items/wood_shovel.png",
            "323:items/sign.png",
            "397@3:items/skull_char.png",
            "397@4:items/skull_zombie.png",
            "397@0:items/skull_skeleton.png",
            "397@1:items/skull_wither.png",
            "341:items/slimeball.png",
            "332:items/snowball.png",
            "382:items/melon_speckled.png",
            "375:items/spider_eye.png",
            "280:items/stick.png",
            "287:items/string.png",
            "353:items/sugar.png",
            "289:items/gunpowder.png",
            "276:items/diamond_sword.png",
            "283:items/gold_sword.png",
            "267:items/iron_sword.png",
            "272:items/stone_sword.png",
            "268:items/wood_sword.png",
            "386:items/book_writable.png",
            "387:items/book_written.png",
            "348:items/glowstone_dust.png",
            "417:items/iron_horse_armor.png",
            "418:items/gold_horse_armor.png",
            "419:items/diamond_horse_armor.png",
            "421:items/name_tag.png",
            "420:items/lead.png",
            "157:blocks/rail_activator_powered.png",
            "145:blocks/anvil_top_damaged_0.png",
            "138:blocks/beacon.png",
            "7:blocks/bedrock.png",
            "48:blocks/cobblestone_mossy.png",
            "57:blocks/diamond_block.png",
            "133:blocks/emerald_block.png",
            "41:blocks/gold_block.png",
            "42:blocks/iron_block.png",
            "22:blocks/lapis_block.png",
            "152:blocks/redstone_block.png",
            "4:blocks/cobblestone.png",
            "47:blocks/bookshelf.png",
            "45:blocks/brick.png",
            "81:blocks/cactus_side.png",
            "82:blocks/clay.png",
            "35@0:blocks/wool_colored_white.png",
            "35@1:blocks/wool_colored_orange.png",
            "35@2:blocks/wool_colored_magenta.png",
            "35@3:blocks/wool_colored_light_blue.png",
            "35@4:blocks/wool_colored_yellow.png",
            "35@5:blocks/wool_colored_lime.png",
            "35@6:blocks/wool_colored_pink.png",
            "35@7:blocks/wool_colored_gray.png",
            "35@8:blocks/wool_colored_silver.png",
            "35@9:blocks/wool_colored_cyan.png",
            "35@10:blocks/wool_colored_purple.png",
            "35@11:blocks/wool_colored_blue.png",
            "35@12:blocks/wool_colored_brown.png",
            "35@13:blocks/wool_colored_green.png",
            "35@14:blocks/wool_colored_red.png",
            "35@15:blocks/wool_colored_black.png",
            "171@0:blocks/wool_colored_white.png",
            "171@1:blocks/wool_colored_orange.png",
            "171@2:blocks/wool_colored_magenta.png",
            "171@3:blocks/wool_colored_light_blue.png",
            "171@4:blocks/wool_colored_yellow.png",
            "171@5:blocks/wool_colored_lime.png",
            "171@6:blocks/wool_colored_pink.png",
            "171@7:blocks/wool_colored_gray.png",
            "171@8:blocks/wool_colored_silver.png",
            "171@9:blocks/wool_colored_cyan.png",
            "171@10:blocks/wool_colored_purple.png",
            "171@11:blocks/wool_colored_blue.png",
            "171@12:blocks/wool_colored_brown.png",
            "171@13:blocks/wool_colored_green.png",
            "171@14:blocks/wool_colored_red.png",
            "171@15:blocks/wool_colored_black.png",
            "137:blocks/command_block.png",
            "151:blocks/daylight_detector_top.png",
            "32:blocks/deadbush.png",
            "28:blocks/rail_detector.png",
            "3:blocks/dirt.png",
            "13:blocks/gravel.png",
            "23:blocks/dispenser_front_horizontal.png",
            "122:blocks/dragon_egg.png",
            "158:blocks/dropper_front_horizontal.png",
            "116:blocks/enchanting_table_side.png",
            "120:blocks/endframe_top.png",
            "101:blocks/iron_bars.png",
            "31:blocks/fern.png",
            "37:blocks/flower.png",
            "61:blocks/furnace_front_off.png",
            "20:blocks/glass.png",
            "102:blocks/glass.png",
            "27:blocks/rail_golden_powered.png",
            "2:blocks/grass_side.png",
            "87:blocks/netherrack.png",
            "88:blocks/soul_sand.png",
            "79:blocks/ice.png",
            "84:blocks/jukebox_top.png",
            "65:blocks/ladder.png",
            "18@0:blocks/leaves_oak.png",
            "18@3:blocks/leaves_jungle.png",
            "18@1:blocks/leaves_spruce.png",
            "18@2:blocks/leaves_birch.png",
            "89:blocks/glowstone.png",
            "103:blocks/melon_side.png",
            "52:blocks/mob_spawner.png",
            "99:blocks/mushroom_brown.png",
            "100:blocks/mushroom_brown.png",
            "25:blocks/noteblock.png",
            "110:blocks/mycelium_side.png",
            "112:blocks/nether_brick.png",
            "153:blocks/quartz_ore.png",
            "49:blocks/obsidian.png",
            "16:blocks/coal_ore.png",
            "56:blocks/diamond_ore.png",
            "129:blocks/emerald_ore.png",
            "14:blocks/gold_ore.png",
            "15:blocks/iron_ore.png",
            "21:blocks/lapis_ore.png",
            "73:blocks/redstone_ore.png",
            "33:blocks/piston_side.png",
            "29:blocks/piston_top_sticky.png",
            "86:blocks/pumpkin_face_off.png",
            "91:blocks/pumpkin_face_on.png",
            "155:blocks/quartz_block_top.png",
            "66:blocks/rail_normal.png",
            "123:blocks/redstone_lamp_off.png",
            "76:blocks/redstone_torch_on.png",
            "38:blocks/rose.png",
            "12:blocks/sand.png",
            "24@0:blocks/sandstone_normal.png",
            "24@1:blocks/sandstone_carved.png",
            "24@2:blocks/sandstone_smooth.png",
            "6@0:blocks/sapling_oak.png",
            "6@2:blocks/sapling_birch.png",
            "6@3:blocks/sapling_jungle.png",
            "6@1:blocks/sapling_spruce.png",
            "80:blocks/snow.png",
            "19:blocks/sponge.png",
            "98@0:blocks/stonebrick.png",
            "98@3:blocks/stonebrick_carved.png",
            "98@2:blocks/stonebrick_cracked.png",
            "98@1:blocks/stonebrick_mossy.png",
            "1:blocks/stone.png",
            "44:blocks/stone_slab_top.png",
            "46:blocks/tnt_side.png",
            "50:blocks/torch_on.png",
            "96:blocks/trapdoor.png",
            "17@2:blocks/log_birch.png",
            "17@3:blocks/log_jungle.png",
            "17@0:blocks/log_oak.png",
            "17@1:blocks/log_spruce.png",
            "121:blocks/end_stone.png",
            "111:blocks/waterlily.png",
            "5@0:blocks/planks_oak.png",
            "5@2:blocks/planks_birch.png",
            "5@3:blocks/planks_jungle.png",
            "5@1:blocks/planks_spruce.png",
            "58:blocks/crafting_table_front.png",
            "172:blocks/hardened_clay.png",
            "159@0:blocks/hardened_clay_stained_white.png",
            "159@1:blocks/hardened_clay_stained_orange.png",
            "159@2:blocks/hardened_clay_stained_magenta.png",
            "159@3:blocks/hardened_clay_stained_light_blue.png",
            "159@4:blocks/hardened_clay_stained_yellow.png",
            "159@5:blocks/hardened_clay_stained_lime.png",
            "159@6:blocks/hardened_clay_stained_pink.png",
            "159@7:blocks/hardened_clay_stained_gray.png",
            "159@8:blocks/hardened_clay_stained_silver.png",
            "159@9:blocks/hardened_clay_stained_cyan.png",
            "159@10:blocks/hardened_clay_stained_purple.png",
            "159@11:blocks/hardened_clay_stained_blue.png",
            "159@12:blocks/hardened_clay_stained_brown.png",
            "159@13:blocks/hardened_clay_stained_green.png",
            "159@14:blocks/hardened_clay_stained_red.png",
            "159@15:blocks/hardened_clay_stained_black.png",
            "170:blocks/hay_block_side.png",
            "173:blocks/coal_block.png",
            "301:items/leather_boots.png",
            "256:items/iron_shovel.png",
            "30:blocks/web.png",
            "40:blocks/mushroom_red.png",
            "39:blocks/mushroom_brown.png",
            "38:blocks/flower_rose.png",
            "37:blocks/flower_dandelion.png",
            "78:blocks/snow.png",
            "97:blocks/stone.png",
            "106:blocks/vine.png",
            "321:items/painting.png",
            "69:blocks/lever.png",
            "131:blocks/trip_wire_source.png",
            "296:items/wheat.png",
            "288:items/feather.png",
            "372:items/nether_wart.png",
            "399:items/nether_star.png",
            "405:items/netherbrick.png",
            "406:items/quartz.png"};
        String[] mapmessages = {
            "types.buy@§28;Buy (Right click to buy)",
            "types.sell@§16;Sell (Right click to sell)",
            "types.both.buy@§28;RMB to Buy",
            "types.both.sell@§28;LMB to Sell",
            "types.adminshop.buy.glob@§28;AdminShop Buy",
            "types.adminshop.buy.desc@§20;Right click to buy",
            "types.adminshop.sell.glob@§16;AdminShop Sell",
            "types.adminshop.sell.desc@§10;Right click to sell",
            "types.adminshop.both.buy@§28;(AS)Right click to Buy",
            "types.adminshop.both.sell@§28;(AS)Left click to Sell",
            "types.notconf@§16;Not configurated",
            "misc.noitem@Empty Shop",
            "misc.cost@Cost: §28;",
            "misc.costbuy@Buycost: §28;",
            "misc.costsell@Sellcost: §28;",
            "misc.amount@Amount: §28;",
            "misc.id@Id: ",
            "misc.enchantments@Enchantments:",
            "misc.owner@Owner: §16;",
            "misc.custom@Custom "
        };
        String[] shopitem = {
            "name@Shop",
            "lore@Place a shop and trade!"
        };
        String[] confmessages = {
            "interacting.buying.errors.notenoughspace@You don't have space in inventory.",
            "interacting.buying.errors.notenoughmoney@You don't have enough money.",
            "interacting.buying.errors.noitems@Shop's container doesn't have enough items.",
            "interacting.buying.success@You bought %amount% of %name%",
            "interacting.selling.success@You sold %amount% of %name%",
            "interacting.selling.errors.notenoughmoney@Shop owner doesn't have enough money.",
            "interacting.selling.errors.ownernotenoughspace@Shop's container doesn't have enough space.",
            "interacting.selling.errors.noitems@You don't have items to sell.",
            "interacting.errors.notconfigured@Shop is not configured.",
            "interacting.puttingmapinside@You can't put map in itemframe!",
            "creating.global.created@You successfully created empty shop. Now click with item on frame.",
            "creating.global.settingtype@What type should it be(type in chat) (Buy-1, Sell-2, Sell/Buy-5):",
            "creating.global.settingamount@Type what amount you'd like to sell or buy:",
            "creating.global.settingcost@Now set the cost:",
            "creating.global.settingbuycost@Now set buy cost:",
            "creating.global.settingsellcost@Now set sell cost:",
            "creating.global.settingitem@Set the item type.",
            "creating.global.passsettingtype@You have successfully set shop type.",
            "creating.global.passsettingamount@You have successfully set amount.",
            "creating.global.passsettingbuycost@You have successfully set buy cost.",
            "creating.global.passsettingsellcost@You have successfully set sell cost.",
            "creating.global.passsettingcost@You have successfully set cost.",
            "creating.global.passsettingitem@Set the item type.",
            "creating.global.admin.settingtype@Type in chat what type would you like:\n Shop is 1,\n Sell is 2,\n AdminShop shop is 3,\n AdminShop sell is 4,\n Buy/Sell is 5 \n Buy/Sell AdminShop is 6",
            "creating.errors.invalidcost@Cost must be greater than 0",
            "creating.errors.othershopoverlaying@You cannot place shop here, other shop is here!",
            "creating.errors.permdenied@You are not permitted!",
            "destroying.errors.notanowner@You are not a shop owner!",
            "destroying.success@Successfully removed shop."};
        this.getConfig().addDefault("downloadimages", true);
        this.getConfig().addDefault("updatenotifications", true);
        this.getConfig().addDefault("debug", false);
        this.getConfig().addDefault("map.drawowner", true);
        this.getConfig().addDefault("map.drawid", true);
        for (String value : Arrays.asList(mapmessages)) {
            String[] s = value.split("@");
            this.getConfig().addDefault("mapmessages." + s[0], s[1]);
        }
        for (String value : Arrays.asList(shopitem)) {
            String[] s = value.split("@");
            this.getConfig().addDefault("shopitem." + s[0], s[1]);
        }
        for (String value : Arrays.asList(confmessages)) {
            String[] s = value.split("@");
            this.getConfig().addDefault("confmessages." + s[0], s[1]);
        }
        for (String value : Arrays.asList(pictures)) {
            String[] s = value.split(":");
            this.getConfig().addDefault("pictures." + s[0], s[1]);
        }

        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
    }

    /* public void reloadShopConfig() {
     if (shopConfigFile == null) {
     shopConfigFile = new File(getDataFolder(), "shops.yml");
     }
     shopConfig = YamlConfiguration.loadConfiguration(shopConfigFile);
     }

     public FileConfiguration getShopConfig() {
     if (shopConfig == null) {
     this.reloadShopConfig();
     }
     return shopConfig;
     }

     public void saveShopConfig() {
     if (shopConfig == null || shopConfigFile == null) {
     return;
     }
     try {
     getShopConfig().save(shopConfigFile);
     } catch (IOException ex) {
     this.getLogger().log(Level.SEVERE, "Could not save config to " + shopConfigFile, ex);
     }
     } */
    public ItemStack getFrameItem() {
        return is;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;


        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp
                == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ
                != null;
    }

    public String getMessage(String s) {
        if (this.getConfig().getString(s) != null) {
            return this.getConfig().getString(s);
        } else {
            return "Error in lang config " + s;
        }

    }
}
