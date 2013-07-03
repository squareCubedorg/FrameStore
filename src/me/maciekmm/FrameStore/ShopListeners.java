package me.maciekmm.FrameStore;

import java.util.ArrayList;
import java.util.Iterator;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class ShopListeners implements Listener {

    public static ShopFunctions functions;
    public static FrameStore frameshop;
    public static final int MAGIC_NUMBER = Integer.MAX_VALUE - 395742;

    public ShopListeners(FrameStore plugin) {
        frameshop = plugin;
        functions = new ShopFunctions();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCreateShop(HangingPlaceEvent e) {
        if (e.isCancelled()) {
            return;
        }
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
            final ShopData sd = new ShopData(p.getName(), enn.getLocation());
            functions.getshopl().put(enn.getLocation(), sd);
            p.sendMessage(ChatColor.DARK_GREEN + frameshop.getMessage("confmessages.creating.global.created"));
            frameshop.getServer().getScheduler().runTaskLater(frameshop, new Runnable() {
                @Override
                public void run() {
                    sd.reRender(frameshop);
                }
            }, 5);

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
                        if (!sd.getOwner().equalsIgnoreCase(remo.getName()) && !remo.hasPermission("framestore.admin")) {
                            if (sd.getType() == 5) {
                                sellingToShop(sd, remo, false, false);
                            } else if (sd.getType() == 6) {
                                sellingToShop(sd, remo, true, false);
                            } else {
                                remo.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.destroying.errors.notanowner"));
                            }
                        } else if (sd.getOwner().equalsIgnoreCase(remo.getName()) || remo.isOp()) {
                            sd.removeFD();
                            for (ItemStack is : sd.getInv().getContents()) {
                                if (is != null) {
                                    isf.getLocation().getWorld().dropItem(isf.getLocation(), is);
                                    is.setType(Material.AIR);
                                }
                            }
                            functions.getshopl().remove(isf.getLocation());
                            if (e.getRemover() instanceof Player && (((Player) e.getRemover()).getName().equalsIgnoreCase(sd.getOwner()) || ((Player) e.getRemover()).isOp())) {
                                ((Player) e.getRemover()).sendMessage(ChatColor.DARK_GREEN + frameshop.getMessage("confmessages.destroying.success"));
                                isf.getLocation().getWorld().dropItem(isf.getLocation(), frameshop.getFrameItem());
                                e.getEntity().remove();

                            }
                        }
                    }
                } else {
                    e.getEntity().remove();
                    frameshop.getLogger().log(Level.WARNING, "Destroyed unknown shop(ItemFrame) on {0} by" + e.getCause(), e.getEntity().getLocation().toString());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSomethingDestroyShop(HangingBreakEvent e) {
        if (e.isCancelled()) {
            return;
        }
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
                    if (sd.getOwner().equals(p.getName())) {
                        if (sd.getItem() != null && sd.getCost() != 0 && sd.getAmount() != 0) {
                            p.openInventory(sd.getInv());
                        } else if (p.getItemInHand().getType() != Material.AIR && sd.getItem() == null) {
                            ItemStack iih = p.getItemInHand().clone();
                            sd.setItem(iih);
                            sd.reRender(frameshop);
                        } else if (sd.getType() == 0 && sd.getItem() != null) {
                            ArrayList<Object> al = new ArrayList<>();
                            al.add(0, 1);
                            al.add(1, Serializer.serializeLoc(enn.getLocation()));
                            if (p.hasPermission("framestore.admin")) {
                                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.admin.settingtype"));
                            } else {
                                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.settingtype"));
                            }
                            functions.getShopSet().put(p.getName(), al);
                        } else if (sd.getItem() != null && sd.getAmount() == 0) {
                            ArrayList<Object> al = new ArrayList<>();
                            al.add(0, 2);
                            al.add(1, Serializer.serializeLoc(enn.getLocation()));
                            p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.settingamount"));
                            functions.getShopSet().put(p.getName(), al);
                        } else if (sd.getCost() == 0.0 && sd.getItem() != null) {
                            ArrayList<Object> al = new ArrayList<>();
                            al.add(0, 3);
                            al.add(1, Serializer.serializeLoc(enn.getLocation()));
                            p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.settingcost"));
                            functions.getShopSet().put(p.getName(), al);
                        } else {
                            p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.global.settingitem"));
                        }
                    } else {
                        if (sd.getType() == 1 || sd.getType() == 5) {
                            buyingFromShop(sd, p, false);
                        } else if (sd.getType() == 2) {
                            sellingToShop(sd, p, false, true);
                        } else if (sd.getType() == 3 || sd.getType() == 6) {
                            buyingFromShop(sd, p, true);
                        } else if (sd.getType() == 4) {
                            sellingToShop(sd, p, true, true);

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
                                    sd.setType((int) am);
                                    sd.reRender(frameshop);
                                }
                            });
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
                        int mss = sd.getItem().getMaxStackSize();
                        if (am <= mss && am > 0) {
                            frameshop.getServer().getScheduler().runTask(frameshop, new Runnable() {
                                @Override
                                public void run() {
                                    ShopData sd = functions.getshopl().get(Serializer.unserializeLoc(slc));
                                    sd.setAmount((int) am);
                                    sd.reRender(frameshop);

                                }
                            });
                            p.sendMessage(ChatColor.DARK_GREEN + frameshop.getMessage("confmessages.creating.global.passsettingamount"));
                            functions.getShopSet().get(p.getName()).set(0, 3);
                            if (functions.getshopl().get(Serializer.unserializeLoc(slc)).getType() == 5 || functions.getshopl().get(Serializer.unserializeLoc(slc)).getType() == 6) {
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
                                    sd.setCost(am);
                                    sd.reRender(frameshop);
                                }
                            });

                            if (functions.getshopl().get(Serializer.unserializeLoc(slc)).getType() == 5 || functions.getshopl().get(Serializer.unserializeLoc(slc)).getType() == 6) {
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
                                    sd.setSellCost(am);
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
        if (e.getPlayer().hasPermission("framestore.admin") && FrameStore.update == true) {
            e.getPlayer().sendMessage(ChatColor.DARK_RED + "New update of framestore is available, check http://dev.bukkit.org/server-mods/framestore/");
        }
    }

    @EventHandler
    public void onPlayerLeft(PlayerQuitEvent e) {
        ShopListeners.functions.unload(e.getPlayer());
        functions.getShopSet().remove(e.getPlayer().getName());
    }

    private void sellingToShop(ShopData sd, Player seller, boolean adminshop, boolean singlemode) {
        if (sd.getInv() != null && sd.getType() != 0 && sd.getItem() != null) {
            if (!(adminshop && seller.hasPermission("framestore.use.sell.adminshop")) || (!adminshop && seller.hasPermission("framestore.use.sell.normal"))) {
                seller.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.errors.permdenied"));
                return;
            }
            double cost;
            if (singlemode) {
                cost = sd.getCost();
            } else {
                cost = sd.getSellCost();
            }
            if (!functions.checkItems(seller.getInventory(), sd.getItem())) {
                seller.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.buying.errors.noitems"));
                return;
            }
            if (sd.getInv().firstEmpty() == -1) {
                seller.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.selling.errors.notenoughspace"));
                return;
            }
            if (!FrameStore.econ.withdrawPlayer(Bukkit.getOfflinePlayer(sd.getOwner()).getName(), cost).transactionSuccess() && !adminshop) {
                seller.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.selling.errors.notenoughmoney"));
                return;
            }
            FrameStore.econ.depositPlayer(seller.getName(), cost);
            if (!adminshop) {
                sd.getInv().addItem(sd.getItem());
            }
            functions.consumeItems(seller.getInventory(), sd.getItem());
            String success = frameshop.getMessage("confmessages.interacting.selling.success").replaceAll("%amount%", String.valueOf(sd.getItem().getAmount())).replaceAll("%name%", sd.getItem().getType().toString().toLowerCase());
            seller.sendMessage(ChatColor.DARK_GREEN + success);
        } else {
            seller.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.errors.notconfigured"));
        }
    }

    private void buyingFromShop(ShopData sd, Player p, boolean adminshop) {
        if (sd.getInv() != null && sd.getType() != 0 && sd.getItem() != null && sd.getAmount() != 0) {
            if (!(adminshop && p.hasPermission("framestore.use.buy.adminshop")) || (!adminshop && p.hasPermission("framestore.use.buy.normal"))) {
                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.creating.errors.permdenied"));
                return;
            }
            if (!functions.checkItems(sd.getInv(), sd.getItem()) && !adminshop) {
                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.buying.errors.noitems"));
                return;
            }
            if (p.getInventory().firstEmpty() == -1) {
                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.buying.errors.notenoughspace"));
                return;
            }
            if (!FrameStore.econ.withdrawPlayer(p.getName(), sd.getCost()).transactionSuccess()) {
                p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.buying.errors.notenoughmoney"));
                return;
            }
            if (!adminshop) {
                FrameStore.econ.depositPlayer(sd.getOwner(), sd.getCost());
                functions.consumeItems(sd.getInv(), sd.getItem());
            }
            p.getInventory().addItem(sd.getItem());
            String success = frameshop.getMessage("confmessages.interacting.buying.success").replaceAll("%amount%", String.valueOf(sd.getItem().getAmount())).replaceAll("%name%", sd.getItem().getType().toString().toLowerCase());
            p.sendMessage(ChatColor.DARK_GREEN + success);
        } else {
            p.sendMessage(ChatColor.DARK_RED + frameshop.getMessage("confmessages.interacting.errors.notconfigured"));
        }
    }
}
