package me.maciekmm.FrameStore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class ShopListeners implements Listener {

    public static ShopFunctions functions;
    public static FrameStore frameshop;
    public static final int MAGIC_NUMBER = Integer.MAX_VALUE - 395742;

    public ShopListeners(FrameStore plugin) {
        frameshop = plugin;
        functions = new ShopFunctions();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerCreateShop(HangingPlaceEvent e) {
        Entity en = e.getEntity();
        Player p = e.getPlayer();
        if (en instanceof ItemFrame && p.getItemInHand().getItemMeta().hasLore() && p.hasPermission("framestore.create")) {
            Iterator<Entity> it = en.getNearbyEntities(0.3, 0.3, 0.3).iterator();
            while (it.hasNext()) {
                if (it.next() instanceof ItemFrame) {
                    e.getPlayer().sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.errors.othershopoverlaying"));
                    e.setCancelled(true);
                    return;
                }
            }
            ItemFrame enn = (ItemFrame) en;
            functions.getshopl().put(enn.getLocation(), new ShopData(p.getName(), enn.getLocation()));
            p.sendMessage(ChatColor.DARK_GREEN + frameshop.getMessage("confmessages.creating.global.created"));

            MapView mv = frameshop.getServer().createMap(frameshop.getServer().getWorlds().get(0));
            ItemStack m = new ItemStack(Material.MAP, 1, mv.getId());
            mv.setCenterX(MAGIC_NUMBER);
            mv.setCenterZ(0);
            for (MapRenderer mr : mv.getRenderers()) {
                mv.removeRenderer(mr);
            }
            mv.addRenderer(new Renderer(frameshop, true, null, 0, 0, e.getPlayer().getName(), 0, 0, 0, null, null, 0));
            m.setDurability(mv.getId());
            enn.setItem(m);
        } else if (p.getItemInHand().getItemMeta().hasLore() && !p.hasPermission("frameshop.create")) {
            p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.errors.permdenied"));
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDetroyShop(HangingBreakByEntityEvent e) {
        if (e.getEntity() instanceof ItemFrame) {
            ItemFrame isf = (ItemFrame) e.getEntity();
            if (isf.getItem().getType().equals(Material.MAP)) {
                e.setCancelled(true);
                if (functions.getshopl().containsKey(e.getEntity().getLocation())) {
                    ShopData sd = functions.getshopl().get(e.getEntity().getLocation());
                    if (e.getRemover() instanceof Player) {
                        Player remo = (Player) e.getRemover();
                        if (!sd.getStringData(0).equalsIgnoreCase(remo.getName()) && !remo.isOp()) {
                            if (sd.getIntData(4) == 5) {
                                if (sd.getInv() != null && sd.getIntData(4) != 0 && sd.getIntData(1) != 0 && functions.checkItems(remo.getInventory(), new ItemStack(sd.getIntData(1), sd.getIntData(2))) && sd.getInv().firstEmpty() != -1) {
                                    ItemStack con = new ItemStack(sd.getIntData(1), sd.getIntData(2), (byte) sd.getIntData(3));
                                    if (!FrameStore.econ.withdrawPlayer(Bukkit.getOfflinePlayer(sd.getStringData(0)).getName(), sd.getDoubleData(0)).transactionSuccess()) {
                                        remo.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.selling.errors.notenoughmoney"));
                                        return;
                                    }

                                    FrameStore.econ.depositPlayer(remo.getName(), sd.getDoubleData(0));
                                    functions.consumeItems(remo.getInventory(), con);
                                    sd.getInv().addItem(con);
                                    String success = frameshop.getMessage("confmessages.interacting.selling.success").replaceAll("%amount%", String.valueOf(sd.getIntData(2))).replaceAll("%name%", sd.getStringData(1));
                                    remo.sendMessage(ChatColor.DARK_GREEN + success);
                                } else {
                                    remo.sendMessage(ChatColor.DARK_RED + "Shop is not configured or doesn't have enough space or you don't have items in inventory!");
                                }
                            } else if (sd.getIntData(4) == 6) {
                                if (sd.getInv() != null && sd.getIntData(4) != 0 && sd.getIntData(1) != 0 && functions.checkItems(remo.getInventory(), new ItemStack(sd.getIntData(1), sd.getIntData(2)))) {
                                    ItemStack con = new ItemStack(sd.getIntData(1), sd.getIntData(2), (byte) sd.getIntData(3));
                                    if (sd.getEnch() != null && !sd.getEnch().isEmpty()) {
                                        Serializer.addEnchantments(con, sd.getEnch());
                                    }
                                    FrameStore.econ.depositPlayer(remo.getName(), sd.getDoubleData(0));
                                    functions.consumeItems(remo.getInventory(), con);
                                    String success = frameshop.getMessage("confmessages.interacting.selling.success").replaceAll("%amount%", String.valueOf(sd.getIntData(2))).replaceAll("%name%", sd.getStringData(1));
                                    remo.sendMessage(ChatColor.DARK_GREEN + success);
                                    //sd.getInv().addItem(con);
                                } else if (!functions.checkItems(remo.getInventory(), new ItemStack(sd.getIntData(1), sd.getIntData(2)))) {
                                    remo.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.selling.errors.noitems"));
                                } else {
                                    remo.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.errors.notconfigured"));
                                }

                            } else {
                                remo.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.destroying.errors.notanowner"));
                            }

                        } else if (sd.getStringData(0).equalsIgnoreCase(remo.getName()) || remo.isOp()) {
                            sd.removeFD();
                            for (ItemStack is : sd.getInv().getContents()) {
                                if (is != null) {
                                    isf.getLocation().getWorld().dropItem(isf.getLocation(), is);
                                    is.setType(Material.AIR);
                                }
                            }
                            functions.getshopl().remove(isf.getLocation());
                            if (e.getRemover() instanceof Player && (((Player) e.getRemover()).getName().equalsIgnoreCase(sd.getStringData(0))||((Player)e.getRemover()).isOp())) {
                                ((Player) e.getRemover()).sendMessage(ChatColor.DARK_GREEN + frameshop.getMessage("confmessages.destroying.success"));
                                isf.getLocation().getWorld().dropItem(isf.getLocation(), frameshop.getFrameItem());
                                e.getEntity().remove();

                            }
                        }
                    }


                }
            }
        }
    }

    @EventHandler
    public void onSomethingDestroyShop(HangingBreakEvent e) {
        if (e.getEntity() instanceof ItemFrame && e.getCause() != RemoveCause.ENTITY) {
            ItemFrame isf = (ItemFrame) e.getEntity();
            if (isf.getItem().getType().equals(Material.MAP)) {
                Location sl = ((ItemFrame) e.getEntity()).getLocation();
                if (functions.getshopl().containsKey(sl)) {
                    ShopData sd = functions.getshopl().get(sl);
                    sd.removeFD();
                    for (ItemStack is : sd.getInv().getContents()) {
                        if (is != null) {
                            isf.getLocation().getWorld().dropItem(isf.getLocation(), is);
                            is.setType(Material.AIR);
                        }
                    }
                    functions.getshopl().remove(sl);
                    e.setCancelled(true);
                    e.getEntity().getLocation().getWorld().dropItem(e.getEntity().getLocation(), frameshop.getFrameItem());
                    e.getEntity().remove();
                } else {
                    e.getEntity().remove();
                    frameshop.getLogger().log(Level.WARNING, "Destroyed unknown shop(ItemFrame) on {0} by" + e.getCause(), sl.toString());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent e) {
        Entity en = e.getRightClicked();
        Player p = e.getPlayer();

        if (en instanceof ItemFrame) {
            ItemFrame enn = (ItemFrame) en;
            if (enn.getItem().getType().equals(Material.MAP)) {
                e.setCancelled(true);
                if (functions.getshopl().containsKey(enn.getLocation())) {

                    ShopData sd = functions.getshopl().get(enn.getLocation());
                    if (sd.getStringData(0).equals(p.getName())) {
                        if (sd.getStringData(1) != null && sd.getDoubleData(0) != 0 && sd.getIntData(2) != 0) {
                            p.openInventory(sd.getInv());
                        } else if (p.getItemInHand().getType() != Material.AIR && sd.getStringData(1) == null) {
                            ItemStack iih = p.getItemInHand();
                            sd.setData(1, iih.getType().name().toLowerCase());
                            sd.setData(1, (int) iih.getTypeId());
                            sd.setData(3, (int) iih.getData().getData());
                            if (iih.getItemMeta().getDisplayName() != null) {
                                sd.setData(2, iih.getItemMeta().getDisplayName());
                            }
                            sd.setEnch(iih.getItemMeta().getEnchants());
                            sd.reRender(frameshop);
                        } else if (sd.getIntData(4) == 0 && sd.getStringData(1) != null) {
                            ArrayList<Object> al = new ArrayList<>();
                            al.add(0, 1);
                            al.add(1, Serializer.serializeLoc(enn.getLocation()));
                            if (p.hasPermission("framestore.admin")) {
                                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.admin.settingtype"));
                            } else {
                                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.settingtype"));
                            }
                            functions.getShopSet().put(p.getName(), al);
                        } else if (sd.getIntData(2) == 0 && sd.getStringData(1) != null) {
                            ArrayList<Object> al = new ArrayList<>();
                            al.add(0, 2);
                            al.add(1, Serializer.serializeLoc(enn.getLocation()));
                            p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.settingamount"));
                            functions.getShopSet().put(p.getName(), al);
                        } else if (sd.getDoubleData(0) == 0.0 && sd.getStringData(1) != null) {
                            ArrayList<Object> al = new ArrayList<>();
                            al.add(0, 3);
                            al.add(1, Serializer.serializeLoc(enn.getLocation()));
                            p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.settingcost"));
                            functions.getShopSet().put(p.getName(), al);
                        } else {
                            p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.settingitem"));
                        }
                    } else {
                        if (sd.getIntData(4) == 1 || sd.getIntData(4) == 5) {
                            if (sd.getInv() != null && sd.getIntData(4) != 0 && sd.getIntData(1) != 0 && sd.getIntData(2) != 0 && functions.checkItems(sd.getInv(), new ItemStack(sd.getIntData(1), sd.getIntData(2)))) {
                                ItemStack con = new ItemStack(sd.getIntData(1), sd.getIntData(2), (byte) sd.getIntData(3));
                                if (sd.getEnch() != null && !sd.getEnch().isEmpty()) {
                                    if (sd.getInv().getItem(sd.getInv().first(new ItemStack(sd.getIntData(1), sd.getIntData(2)))).getEnchantments() != sd.getEnch()) {
                                        p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.buying.errors.noitems"));
                                        return;
                                    }
                                    Serializer.addEnchantments(con, sd.getEnch());
                                }
                                con.setItemMeta(sd.getInv().getItem(sd.getInv().first(con.getType())).getItemMeta());
                                if (p.getInventory().firstEmpty() == -1) {
                                    p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.buying.errors.notenoughspace"));
                                    return;
                                }
                                if (!FrameStore.econ.withdrawPlayer(p.getName(), sd.getDoubleData(0)).transactionSuccess()) {

                                    p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.buying.errors.notenoughmoney"));
                                    return;
                                }
                                FrameStore.econ.depositPlayer(sd.getStringData(0), sd.getDoubleData(0));
                                functions.consumeItems(sd.getInv(), con);
                                p.getInventory().addItem(con);
                                String success = frameshop.getMessage("confmessages.interacting.buying.success").replaceAll("%amount%", String.valueOf(sd.getIntData(2))).replaceAll("%name%", sd.getStringData(1));
                                p.sendMessage(ChatColor.DARK_GREEN + success);
                            } else if (!functions.checkItems(sd.getInv(), new ItemStack(sd.getIntData(1), sd.getIntData(2)))) {
                                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.buying.errors.noitems"));
                            } else {
                                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.errors.notconfigured"));
                            }
                        } else if (sd.getIntData(4) == 2) {
                            if (sd.getInv() != null && sd.getIntData(4) != 0 && sd.getIntData(1) != 0 && functions.checkItems(p.getInventory(), new ItemStack(sd.getIntData(1), sd.getIntData(2))) && sd.getInv().firstEmpty() != -1) {
                                ItemStack con = new ItemStack(sd.getIntData(1), sd.getIntData(2), (byte) sd.getIntData(3));
                                if (!FrameStore.econ.withdrawPlayer(Bukkit.getOfflinePlayer(sd.getStringData(0)).getName(), sd.getDoubleData(0)).transactionSuccess()) {
                                    p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.selling.errors.notenoughmoney"));
                                    return;
                                }

                                FrameStore.econ.depositPlayer(p.getName(), sd.getDoubleData(0));
                                functions.consumeItems(p.getInventory(), con);
                                sd.getInv().addItem(con);
                                String success = frameshop.getMessage("confmessages.interacting.selling.success").replaceAll("%amount%", String.valueOf(sd.getIntData(2))).replaceAll("%name%", sd.getStringData(1));
                                p.sendMessage(ChatColor.DARK_GREEN + success);
                            } else {
                                p.sendMessage(ChatColor.DARK_RED + "Shop is not configured or doesn't have enough space or you don't have items in inventory!");
                            }
                        } else if (sd.getIntData(4) == 3 || sd.getIntData(4) == 6) {
                            if (sd.getIntData(4) != 0 && sd.getIntData(1) != 0 && sd.getIntData(2) != 0) {
                                ItemStack con = new ItemStack(sd.getIntData(1), sd.getIntData(2), (byte) sd.getIntData(3));
                                if (sd.getEnch() != null && !sd.getEnch().isEmpty()) {

                                    Serializer.addEnchantments(con, sd.getEnch());
                                }
                                if(sd.getStringData(2)!=null&&!sd.getStringData(2).equalsIgnoreCase("null"))
                                {
                                    ItemMeta im = con.getItemMeta();
                                    im.setDisplayName(sd.getStringData(2));
                                    con.setItemMeta(im);
                                }
                                if (p.getInventory().firstEmpty() == -1) {
                                    p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.buying.errors.notenoughspace"));
                                    return;
                                }
                                if (!FrameStore.econ.withdrawPlayer(p.getName(), sd.getDoubleData(0)).transactionSuccess()) {

                                    p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.buying.errors.notenoughmoney"));
                                    return;
                                }
                                String success = frameshop.getMessage("confmessages.interacting.buying.success").replaceAll("%amount%", String.valueOf(sd.getIntData(2))).replaceAll("%name%", sd.getStringData(1));
                                p.sendMessage(ChatColor.DARK_GREEN + success);
                                p.getInventory().addItem(con);

                            } else {
                                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.errors.notconfigured"));
                            }
                        } else if (sd.getIntData(4) == 4) {
                            if (sd.getInv() != null && sd.getIntData(4) != 0 && sd.getIntData(1) != 0 && functions.checkItems(p.getInventory(), new ItemStack(sd.getIntData(1), sd.getIntData(2)))) {
                                ItemStack con = new ItemStack(sd.getIntData(1), sd.getIntData(2), (byte) sd.getIntData(3));
                                if (sd.getEnch() != null && !sd.getEnch().isEmpty()) {
                                    Serializer.addEnchantments(con, sd.getEnch());
                                }
                                FrameStore.econ.depositPlayer(p.getName(), sd.getDoubleData(0));
                                functions.consumeItems(p.getInventory(), con);
                                String success = frameshop.getMessage("confmessages.interacting.selling.success").replaceAll("%amount%", String.valueOf(sd.getIntData(2))).replaceAll("%name%", sd.getStringData(1));
                                p.sendMessage(ChatColor.DARK_GREEN + success);
                                //sd.getInv().addItem(con);
                            } else if (!functions.checkItems(e.getPlayer().getInventory(), new ItemStack(sd.getIntData(1), sd.getIntData(2)))) {
                                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.selling.errors.noitems"));
                            } else {
                                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.errors.notconfigured"));
                            }

                        } else {
                            p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.errors.notconfigured"));
                        }
                    }
                    p.updateInventory();
                }
            } else {
                if (e.getPlayer().getItemInHand().getType().equals(Material.MAP)) {
                    p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.puttingmapinside"));
                    e.setCancelled(true);
                }
            }
        }

    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        String om = e.getMessage();
        Player p = e.getPlayer();
        if (functions.getShopSet().containsKey(p.getName())) {
            e.setCancelled(true);
            ArrayList<Object> mode = functions.getShopSet().get(p.getName());
            if (mode.get(0) instanceof Integer && mode.get(1) instanceof String && (om.matches("(\\d+\\.\\d+)") || om.matches("([0-9]+)"))) {

                final double am = Double.parseDouble(om);
                final String slc = String.valueOf(mode.get(1));
                switch ((int) mode.get(0)) {
                    case 1:
                        if (am == 2 || am == 1 || am == 5 || (p.hasPermission("framestore.admin") && am > 0 && am <= 6)) {
                            frameshop.getServer().getScheduler().runTask(frameshop, new Runnable() {
                                @Override
                                public void run() {
                                    ShopData sd = functions.getshopl().get(Serializer.unserializeLoc(slc));
                                    sd.setData(4, (int) am);
                                    sd.reRender(frameshop);

                                }
                            });
                            //EcoListeners.functions.getShopSet().remove(p.getName());
                            p.sendMessage(ChatColor.DARK_GREEN + frameshop.getMessage("confmessages.creating.global.passsettingtype"));
                            functions.getShopSet().get(p.getName()).set(0, 2);
                            p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.settingamount"));

                        } else if (p.hasPermission("framestore.admin")) {
                            p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.admin.passsettingtype"));
                        } else {
                            p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.settingtype"));
                        }
                        break;
                    case 2:
                        ShopData sd = functions.getshopl().get(Serializer.unserializeLoc(slc));
                        int mss = new ItemStack(sd.getIntData(1), 1).getMaxStackSize();
                        if (am <= mss && am > 0) {
                            frameshop.getServer().getScheduler().runTask(frameshop, new Runnable() {
                                @Override
                                public void run() {
                                    ShopData sd = functions.getshopl().get(Serializer.unserializeLoc(slc));
                                    sd.setData(2, (int) am);
                                    sd.reRender(frameshop);

                                }
                            });
                            p.sendMessage(ChatColor.DARK_GREEN + frameshop.getMessage("confmessages.creating.global.passsettingamount"));
                            functions.getShopSet().get(p.getName()).set(0, 3);
                            if (functions.getshopl().get(Serializer.unserializeLoc(slc)).getIntData(4) == 5 || functions.getshopl().get(Serializer.unserializeLoc(slc)).getIntData(4) == 6) {
                                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.settingbuycost"));
                            } else {
                                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.settingcost"));
                            }

                        } else {
                            p.sendMessage(ChatColor.DARK_RED + "Amount must be between 1-" + mss);

                        }
                        break;
                    case 3:
                        if (am > 0) {
                            frameshop.getServer().getScheduler().runTask(frameshop, new Runnable() {
                                @Override
                                public void run() {
                                    ShopData sd = functions.getshopl().get(Serializer.unserializeLoc(slc));
                                    sd.setData(0, am);
                                    sd.reRender(frameshop);
                                }
                            });

                            if (functions.getshopl().get(Serializer.unserializeLoc(slc)).getIntData(4) == 5 || functions.getshopl().get(Serializer.unserializeLoc(slc)).getIntData(4) == 6) {
                                p.sendMessage(ChatColor.DARK_GREEN + frameshop.getMessage("confmessages.creating.global.passsettingbuycost"));
                                functions.getShopSet().get(p.getName()).set(0, 4);
                                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.settingsellcost"));
                            } else {
                                p.sendMessage(ChatColor.DARK_GREEN + frameshop.getMessage("confmessages.creating.global.passsettingcost"));
                                functions.getShopSet().remove(p.getName());
                            }

                        } else {
                            p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.errors.invalidcost"));
                        }
                        break;
                    case 4:
                        if (am > 0) {
                            frameshop.getServer().getScheduler().runTask(frameshop, new Runnable() {
                                @Override
                                public void run() {
                                    ShopData sd = functions.getshopl().get(Serializer.unserializeLoc(slc));
                                    sd.setData(1, am);
                                    sd.reRender(frameshop);
                                }
                            });
                            p.sendMessage(ChatColor.DARK_GREEN + frameshop.getMessage("confmessages.creating.global.passsettingsellcost"));
                            functions.getShopSet().remove(p.getName());
                        } else {
                            p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.errors.invalidcost"));
                        }
                        break;


                }
            } else {
                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.errors.invalidcost"));

            }
        }
    }

    @EventHandler
    public void onPlayerSpawn(final PlayerJoinEvent e) { //Post Login Event, when sending maps in login event player(entity) was not spawned
        frameshop.getServer().getScheduler().runTaskAsynchronously(frameshop, new Runnable() {
            @Override
            public void run() {
                ShopListeners.functions.sendMaps(e.getPlayer());
            }
        });
        if (e.getPlayer().hasPermission("framestore.admin") && FrameStore.update == true) {
            e.getPlayer().sendMessage(ChatColor.DARK_RED + "New update of framestore is available, check http://dev.bukkit.org/server-mods/framestore/");
        }


    }
}
