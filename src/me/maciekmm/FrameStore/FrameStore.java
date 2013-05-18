package me.maciekmm.FrameStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

public class FrameStore extends JavaPlugin {

    private BukkitTask saver;
    public final static Logger log = Bukkit.getLogger();
    public static Economy econ = null;
    public static String type;
    private FileConfiguration shopConfig = null;
    private File shopConfigFile = null;
    private List<String> lores = new ArrayList<>();
    private ItemStack is;

    @Override
    public void onEnable() {
        setupEconomy();
        new ShopListeners(this);
        loadConfiguration();
        type = getConfig().getString("database.type");
        if (type.equalsIgnoreCase("mysql")) {
            Database.db = new DatabaseConnector(this, getConfig().getString("database.addr"), getConfig().getString("database.database"), getConfig().getString("database.login"), getConfig().getString("database.password"));
            Database.createTables();
        } else {
            reloadShopConfig();
        }
        is = new ItemStack(Material.ITEM_FRAME, 1);
        ItemMeta im = is.getItemMeta();
        lores.add("Place a shop and trade!");
        im.setDisplayName("Shop");
        im.setLore(lores);
        is.setItemMeta(im);
        ShapelessRecipe r = new ShapelessRecipe(is);
        r.addIngredient(Material.ITEM_FRAME);
        r.addIngredient(Material.CHEST);
        Bukkit.addRecipe(r);
        ShopListeners.functions.loadShops();
        ShopListeners.functions.loadMaps(this);
        saver = this.getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                ShopListeners.functions.dumpToDatabase();
                log.info("Dumping data to database");
            }
        }, 20000L, 40000L);
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
                        URL website = new URL("http://s3.amazonaws.com/MinecraftDownload/minecraft.jar");
                        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                        fos = new FileOutputStream(ShopListeners.frameshop.getDataFolder() + File.separator + "textures" + File.separator + "minecraft.jar");
                        fos.getChannel().transferFrom(rbc, 0, 1 << 24);

                        while (!new File(ShopListeners.frameshop.getDataFolder() + "/textures/minecraft.jar").exists()) {
                            Thread.sleep(5);
                        }
                        ZipInputStream zis = new ZipInputStream(new FileInputStream(ShopListeners.frameshop.getDataFolder() + "/textures/minecraft.jar"));
                        ZipEntry ze = zis.getNextEntry();

                        while (ze != null) {

                            if (ze.getName().startsWith("textures")) {
                                String fileName = ze.getName();
                                File newFile = new File(ShopListeners.frameshop.getDataFolder() + File.separator + fileName);

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
                        Logger.getLogger(FrameStore.class.getName()).log(Level.SEVERE, null, ex);

                    } finally {
                        try {
                            if (fos != null) {
                                fos.close();
                            }

                        } catch (IOException ex) {
                            Logger.getLogger(FrameStore.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });
        }

    }

    @Override
    public void onDisable() {
        saver.cancel();
        this.saveConfig();
        saveShopConfig();
        ShopListeners.functions.clearer();
        Database.db = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("fs")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("framestore.admin")) {
                    this.reloadConfig();
                    sender.sendMessage(ChatColor.DARK_GREEN + "Successfully reloaded config!");
                    return true;
                }
            } else {
                sender.sendMessage("Available arguments: reload");
                return true;
            }
        }
        return false;
    }

    private void loadConfiguration() {
        this.getConfig().addDefault("database.type", "flat");
        this.getConfig().addDefault("database.login", "login");
        this.getConfig().addDefault("database.password", "password");
        this.getConfig().addDefault("database.addr", "localhost");
        this.getConfig().addDefault("database.database", "frameshops");
        String[] pictures = {"260:items/apple.png",
            "322:items/appleGold.png",
            "262:items/arrow.png",
            "355:items/bed.png",
            "364:items/beefCooked.png",
            "363:items/beefRaw.png",
            "377:items/blazePowder.png",
            "369:items/blazeRod.png",
            "333:items/boat.png",
            "352:items/bone.png",
            "340:items/book.png",
            "305:items/bootsChain.png",
            "313:items/bootsDiamond.png",
            "317:items/bootsGold.png",
            "309:items/bootsIron.png",
            "261:items/bow.png",
            "281:items/bowl.png",
            "297:items/bread.png",
            "379:items/brewingStand.png",
            "336:items/brick.png",
            "325:items/bucket.png",
            "327:items/bucketLava.png",
            "326:items/bucketWater.png",
            "354:items/cake.png",
            "396:items/carrotGolden.png",
            "398:items/carrotOnAStick.png",
            "391:items/carrots.png",
            "380:items/cauldron.png",
            "303:items/chestplateChain.png",
            "299:items/chestplateCloth.png",
            "311:items/chestplateDiamond.png",
            "315:items/chestplateGold.png",
            "307:items/chestplateIron.png",
            "366:items/chickenCooked.png",
            "365:items/chickenRaw.png",
            "337:items/clay.png",
            "263:items/coal.png",
            "404:items/comparator.png",
            "357:items/cookie.png",
            "264:items/diamond.png",
            "356:items/diode.png",
            "330:items/doorIron.png",
            "324:items/doorWood.png",
            "351@0:items/dyePowder_black.png",
            "351@4:items/dyePowder_blue.png",
            "351@3:items/dyePowder_brown.png",
            "351@6:items/dyePowder_cyan.png",
            "351@8:items/dyePowder_gray.png",
            "351@2:items/dyePowder_green.png",
            "351@12:items/dyePowder_lightBlue.png",
            "351@10:items/dyePowder_lime.png",
            "351@13:items/dyePowder_magenta.png",
            "351@14:items/dyePowder_orange.png",
            "351@9:items/dyePowder_pink.png",
            "351@5:items/dyePowder_purple.png",
            "351@1:items/dyePowder_red.png",
            "351@7:items/dyePowder_silver.png",
            "351@15:items/dyePowder_white.png",
            "351@11:items/dyePowder_yellow.png",
            "344:items/egg.png",
            "388:items/emerald.png",
            "395:items/emptyMap.png",
            "403:items/enchantedBook.png",
            "368:items/enderPearl.png",
            "384:items/expBottle.png",
            "381:items/eyeOfEnder.png",
            "334:items/feather.png",
            "376:items/fermentedSpiderEye.png",
            "385:items/fireball.png",
            "401:items/fireworks.png",
            "402:items/FireworksCharge.png",
            "350:items/fishCooked.png",
            "346:items/fishingRod.png",
            "349:items/fishRaw.png",
            "318:items/flint.png",
            "259:items/flintAndSteel.png",
            "390:items/flowerPot.png",
            "389:items/frame.png",
            "370:items/ghastTear.png",
            "374:items/glassBottle.png",
            "371:items/goldNugget.png",
            "279:items/hatchetDiamond.png",
            "286:items/hatchetGold.png",
            "258:items/hatchetIron.png",
            "275:items/hatchetStone.png",
            "271:items/hatchetWood.png",
            "302:items/helmetChain.png",
            "298:items/helmetCloth.png",
            "310:items/helmetDiamond.png",
            "314:items/helmetGold.png",
            "306:items/helmetIron.png",
            "293:items/hoeDiamond.png",
            "294:items/hoeGold.png",
            "292:items/hoeIron.png",
            "291:items/hoeStone.png",
            "290:items/hoeWood.png",
            "154:items/hopper.png",
            "266:items/ingotGold.png",
            "265:items/ingotIron.png",
            "334:items/leather.png",
            "303:items/leggingsChain.png",
            "300:items/leggingsCloth.png",
            "312:items/leggingsDiamonds.png",
            "316:items/leggingsGold.png",
            "308:items/leggingsIron.png",
            "378:items/magmaCream.png",
            "358:items/map.png",
            "360:items/melon.png",
            "335:items/milk.png",
            "328:items/minecart.png",
            "342:items/minecartChest.png",
            "343:items/minecartFurnace.png",
            "408:items/minecartHopper.png",
            "407:items/minecartTnt.png",
            "383:items/monsterPlacer.png",
            "282:items/mushroomStew.png",
            "339:items/paper.png",
            "278:items/pickaxeDiamond.png",
            "285:items/pickaxeGold.png",
            "257:items/pickaxeIron.png",
            "274:items/pickaxeStone.png",
            "270:items/pickaxeWood.png",
            "320:items/porkchopCooked.png",
            "319:items/porkchopRaw.png",
            "392:items/potato.png",
            "393:items/potatoBaked.png",
            "394:items/potatoPoisonous.png",
            "374:items/potion.png",
            "374:items/potion_splash.png",
            "400:items/pumpkinPie.png",
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
            "331:items/redstone.png",
            "338:items/reeds.png",
            "367:items/rottenFlesh.png",
            "329:items/saddle.png",
            "295:items/seeds.png",
            "362:items/seeds_melon.png",
            "361:items/seeds_pumpkin.png",
            "359:items/shears.png",
            "277:items/shovelDiamond.png",
            "284:items/shovelGold.png",
            "257:items/shovelIron.png",
            "273:items/shovelStone.png",
            "269:items/shovelWood.png",
            "323:items/sign.png",
            "397@3:items/skull_char.png",
            "397@4:items/skull_creeper.png",
            "397@0:items/skull_skeleton.png",
            "397@1:items/skull_wither.png",
            "397@4:items/skull_zombie.png",
            "341:items/slimeball.png",
            "332:items/snowball.png",
            "382:items/speckledMelon.png",
            "375:items/spiderEye.png",
            "280:items/stick.png",
            "287:items/string.png",
            "353:items/sugar.png",
            "289:items/sulphur.png",
            "276:items/swordDiamond.png",
            "283:items/swordGold.png",
            "267:items/swordIron.png",
            "272:items/swordStone.png",
            "268:items/swordWood.png",
            "269:items/wheat.png",
            "386:items/writingBook.png",
            "387:items/writtenBook.png",
            "348:items/yellowDust.png",
            "157:blocks/activatorRail.png",
            "145:blocks/anvil_top.png",
            "138:blocks/beacon.png",
            "7:blocks/bedrock.png",
            "57:blocks/blockDiamond.png",
            "133:blocks/blockEmerald.png",
            "41:blocks/blockGold.png",
            "42:blocks/blockIron.png",
            "22:blocks/blockLapis.png",
            "152:blocks/blockRedstone.png",
            "47:blocks/bookshelf.png",
            "379:blocks/brewingStand.png",
            "45:blocks/brick.png",
            "81:blocks/cactus_side.png",
            "354:blocks/cake_top.png",
            "82:blocks/clay.png",
            "35@0:blocks/cloth_0.png",
            "35@1:blocks/cloth_1.png",
            "35@2:blocks/cloth_2.png",
            "35@3:blocks/cloth_3.png",
            "35@4:blocks/cloth_4.png",
            "35@5:blocks/cloth_5.png",
            "35@6:blocks/cloth_6.png",
            "35@7:blocks/cloth_7.png",
            "35@8:blocks/cloth_8.png",
            "35@9:blocks/cloth_9.png",
            "35@10:blocks/cloth_10.png",
            "35@11:blocks/cloth_11.png",
            "35@12:blocks/cloth_12.png",
            "35@13:blocks/cloth_13.png",
            "35@14:blocks/cloth_14.png",
            "35@15:blocks/cloth_15.png",
            "137:blocks/commandBlock.png",
            "151:blocks/daylightDetector_top.png",
            "32:blocks/deadbush.png",
            "28:blocks/detectorRail.png",
            "3:blocks/dirt.png",
            "23:blocks/dispenser_front.png",
            "122:blocks/dragonEgg.png",
            "158:blocks/dropper_front.png",
            "116:blocks/enchantment_side.png",
            "120:blocks/endframe_top.png",
            "101:blocks/fenceIron.png",
            "31:blocks/fern.png",
            "37:blocks/flower.png",
            "61:blocks/furnace_front.png",
            "20:blocks/glass.png",
            "102:blocks/glass.png",
            "27:blocks/goldenRail.png",
            "2:blocks/grass_side.png",
            "87:blocks/hellrock.png",
            "88:blocks/hellsand.png",
            "79:blocks/ice.png",
            "84:blocks/jukebox_top.png",
            "65:blocks/ladder.png",
            "18@0:blocks/leaves.png",
            "18@3:blocks/leaves_jungle.png",
            "18@1:blocks/leaves_spruce.png",
            "18@2:blocks/lever.png",
            "89:blocks/lightgem.png",
            "103:blocks/melon_side.png",
            "52:blocks/mobSpawner.png",
            "99:blocks/mushroom_brown.png",
            "100:blocks/mushroom_brown.png",
            "25:blocks/musicBlock.png",
            "110:blocks/mycel_side.png",
            "112:blocks/netherBrick.png",
            "153:blocks/netherquartz.png",
            "49:blocks/obsidian.png",
            "16:blocks/oreCoal.png",
            "56:blocks/oreDiamond.png",
            "129:blocks/oreEmerald.png",
            "14:blocks/oreGold.png",
            "15:blocks/oreIron.png",
            "21:blocks/oreLapis.png",
            "73:blocks/oreRedstone.png",
            "33:blocks/piston_side.png",
            "29:blocks/piston_top_sticky.png",
            "86:blocks/pumpkin_face.png",
            "91:blocks/pumpkin_jack.png",
            "155:blocks/quartzblock_top.png",
            "66:blocks/rail.png",
            "123:blocks/redstoneLight.png",
            "76:blocks/redtorch_lit.png",
            "38:blocks/rose.png",
            "12:blocks/sand.png",
            "24@0:blocks/sandstone_side.png",
            "24@1:blocks/sandstone_carved.png",
            "24@2:blocks/sandstone_smooth.png",
            "6@0:blocks/sapling.png",
            "6@2:blocks/sapling_birch.png",
            "6@3:blocks/sapling_jungle.png",
            "6@1:blocks/sapling_spruce.png",
            "80:blocks/snow.png",
            "19:blocks/sponge.png",
            "98@0:blocks/stonebricksmooth.png",
            "98@3:blocks/stonebricksmooth_carved.png",
            "98@2:blocks/stonebricksmooth_cracked.png",
            "98@1:blocks/stonebricksmooth_mossy.png",
            "1:blocks/stone.png",
            "44:blocks/stoneslab_top.png",
            "46:blocks/tnt_side.png",
            "50:blocks/torch.png",
            "96:blocks/trapdoor.png",
            "17@2:blocks/tree_birch.png",
            "17@3:blocks/tree_jungle.png",
            "17@0:blocks/tree_side.png",
            "17@1:blocks/tree_spruce.png",
            "121:blocks/whiteStone.png",
            "111:blocks/waterlily.png",
            "5@0:blocks/wood.png",
            "5@2:blocks/wood_birch.png",
            "5@3:blocks/wood_jungle.png",
            "5@1:blocks/wood_spruce.png",
            "58:blocks/workbench_front.png"
        };
        String[] mapmessages = {
            "types.buy@§28;Buy (Right click to buy)",
            "types.sell@§16;Sell (Right click to sell)",
            "types.adminshop.buy.glob@§28;AdminShop Buy",
            "types.adminshop.buy.desc@§20;Right click to buy",
            "types.adminshop.sell.glob@§16;AdminShop Sell",
            "types.adminshop.sell.desc@§10;Right click to sell",
            "types.notconf@§16;Not configurated",
            "misc.noitem@Empty Shop",
            "misc.cost@Cost: §28;",
            "misc.amount@Amount: §28;",
            "misc.id@Id: ",
            "misc.enchantments@Enchantments:",
            "misc.owner@Owner: §16;"
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
            "creating.global.settingtype@What type should it be (Shop-1, Sell-2):",
            "creating.global.settingamount@Type what amount you'd like to sell or buy:",
            "creating.global.settingcost@Now set the cost:",
            "creating.global.settingitem@Set the item type.",
            "creating.global.passsettingtype@You have successfully set shop type.",
            "creating.global.passsettingamount@You have successfully set sell amount.",
            "creating.global.passsettingcost@You have successfully set sell cost.",
            "creating.global.passsettingitem@Set the item type.",
            "creating.global.admin.settingtype@Shop is 1, Purchase is 2, AdminShop shop is 3, AdminShop sell is 4",
            "creating.errors.invalidcost@Cost must be greater than 0",
            "creating.errors.othershopoverlaying@You cannot place shop here, other shop is here!",
            "creating.errors.permdenied@You are not permitted to create shops!",
            "destroying.errors.notanowner@You are not a shop owner!",
            "destroying.success@Successfully removed shop."};
        this.getConfig().addDefault("downloadimages", true);
        this.getConfig().addDefault("map.drawowner", true);
        this.getConfig().addDefault("map.drawid", true);
        for (String value : Arrays.asList(mapmessages)) {
            String[] s = value.split("@");
            this.getConfig().addDefault("mapmessages." + s[0], s[1]);
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

    public void reloadShopConfig() {
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
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public ItemStack getFrameItem() {
        return is;
    }

    public String getMessage(String s) {
        if (this.getConfig().getString(s) != null) {
            return this.getConfig().getString(s);
        } else {
            return "Error in lang config " + s;
        }

    }
}
