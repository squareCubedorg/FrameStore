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
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
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
        if (en instanceof ItemFrame && p.getItemInHand().getItemMeta().hasLore() && p.hasPermission("frameshop.create")) {
            Iterator<Entity> it = en.getNearbyEntities(0.3, 0.3, 0.3).iterator();
            while (it.hasNext()) {
                if (it.next() instanceof ItemFrame) {
                    e.getPlayer().sendMessage(ChatColor.DARK_RED + "You cannot place shop here, other shop is here!");
                    e.setCancelled(true);
                    return;
                }
            }
            ItemFrame enn = (ItemFrame) en;
            functions.getshopl().put(enn.getLocation(), new ShopData(p.getName(), enn.getLocation()));
            p.sendMessage(ChatColor.DARK_GREEN + "You successfully created empty shop. Now click with item on frame.");

            MapView mv = frameshop.getServer().createMap(frameshop.getServer().getWorlds().get(0));
            ItemStack m = new ItemStack(Material.MAP, 1, mv.getId());
            mv.setCenterX(MAGIC_NUMBER);
            mv.setCenterZ(0);
            for (MapRenderer mr : mv.getRenderers()) {
                mv.removeRenderer(mr);
            }
            mv.addRenderer(new Renderer(frameshop, true, null, 0, 0, e.getPlayer().getName(), 0, 0, 0));
            m.setDurability(mv.getId());
            enn.setItem(m);
        } else if (p.getItemInHand().getItemMeta().hasLore() && !p.hasPermission("frameshop.create")) {
            p.sendMessage(ChatColor.DARK_RED + "You are not permitted to create shops!");
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDetroyShop(HangingBreakByEntityEvent e) {
        if (e.getEntity() instanceof ItemFrame) {
            ItemFrame isf = (ItemFrame) e.getEntity();
            if (isf.getItem().getType().equals(Material.MAP)) {
                e.setCancelled(true);
                if (!functions.getshopl().containsKey(e.getEntity().getLocation())) {
                    e.getEntity().getLocation().getWorld().dropItem(e.getEntity().getLocation(), new ItemStack(Material.ITEM_FRAME, 1));
                    e.getEntity().remove();
                    return;
                }
                ShopData sd = functions.getshopl().get(e.getEntity().getLocation());
                if (e.getRemover() instanceof Player) {
                    Player remo = (Player) e.getRemover();
                    if (!sd.getStringData(0).equalsIgnoreCase(remo.getName()) && !remo.isOp()) {
                        remo.sendMessage(ChatColor.DARK_RED + "You are not a shop owner!");
                        return;
                    }
                }

                sd.removeFD();
                for (ItemStack is : sd.getInv().getContents()) {
                    if (is != null) {
                        isf.getLocation().getWorld().dropItem(isf.getLocation(), is);
                        is.setType(Material.AIR);
                    }
                }
                functions.getshopl().remove(isf.getLocation());
                if (e.getRemover() instanceof Player) {
                    ((Player) e.getRemover()).sendMessage(ChatColor.DARK_GREEN + "Successfully removed shop.");
                    List<String> lores = new ArrayList<>();
                    lores.add("Place a shop and trade!");
                    ItemStack ifr = new ItemStack(Material.ITEM_FRAME, 1);
                    ItemMeta im = ifr.getItemMeta();
                    im.setDisplayName("Shop");
                    im.setLore(lores);
                    ifr.setItemMeta(im);
                    isf.getLocation().getWorld().dropItem(isf.getLocation(), ifr);
                    e.getEntity().remove();

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
                    List<String> lores = new ArrayList<>();
                    lores.add("Place a shop and trade!");
                    ItemStack ifr = new ItemStack(Material.ITEM_FRAME, 1);
                    ItemMeta im = ifr.getItemMeta();
                    im.setDisplayName("Shop");
                    im.setLore(lores);
                    ifr.setItemMeta(im);
                    e.getEntity().getLocation().getWorld().dropItem(e.getEntity().getLocation(), ifr);
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
                            sd.reRender(frameshop);
                        } else if (sd.getIntData(4) == 0 && sd.getStringData(1) != null) {
                            ArrayList<Object> al = new ArrayList<>();
                            al.add(0, 1);
                            al.add(1, Serializer.serializeLoc(enn.getLocation()));
                            p.sendMessage(ChatColor.DARK_RED + "What type should it be (Shop-1, Purchase-2):");
                            functions.getShopSet().put(p.getName(), al);
                        } else if (sd.getIntData(2) == 0 && sd.getStringData(1) != null) {
                            ArrayList<Object> al = new ArrayList<>();
                            al.add(0, 2);
                            al.add(1, Serializer.serializeLoc(enn.getLocation()));
                            p.sendMessage(ChatColor.DARK_RED + "Type what amount you'd like to sell:");
                            functions.getShopSet().put(p.getName(), al);
                        } else if (sd.getDoubleData(0) == 0.0 && sd.getStringData(1) != null) {
                            ArrayList<Object> al = new ArrayList<>();
                            al.add(0, 3);
                            al.add(1, Serializer.serializeLoc(enn.getLocation()));
                            p.sendMessage(ChatColor.DARK_RED + "Now set the cost:");
                            functions.getShopSet().put(p.getName(), al);
                        } else {
                            p.sendMessage(ChatColor.DARK_RED + "Set the item type!");
                        }
                    } else {
                        if (sd.getIntData(4) == 1) {
                            if (sd.getInv() != null && sd.getIntData(4) != 0 && sd.getIntData(1) != 0 && sd.getIntData(2) != 0 && functions.checkItems(sd.getInv(), new ItemStack(sd.getIntData(1), sd.getIntData(2)))) {
                                ItemStack con = new ItemStack(sd.getIntData(1), sd.getIntData(2), (byte) sd.getIntData(3));
                                if (e.getPlayer().getInventory().firstEmpty() == -1) {
                                    p.sendMessage(ChatColor.DARK_RED + "You don't have space in inventory!");
                                    return;
                                }
                                if (FrameStore.econ.getBalance(p.getName()) < sd.getDoubleData(0)) {

                                    p.sendMessage(ChatColor.DARK_RED + "You don't have enough money!");
                                    return;
                                }

                                FrameStore.econ.withdrawPlayer(p.getName(), sd.getDoubleData(0));
                                functions.consumeItems(sd.getInv(), con);
                                p.sendMessage(ChatColor.DARK_GREEN + "You bought " + sd.getIntData(2) + " of " + sd.getStringData(1));
                                p.getInventory().addItem(con);
                            } else {
                                p.sendMessage(ChatColor.DARK_RED + "Shop is not configured or doesn't have enough resources.");
                            }
                        } else if (sd.getIntData(4) == 2) {
                            if (sd.getInv() != null && sd.getIntData(4) != 0 && sd.getIntData(1) != 0 && functions.checkItems(e.getPlayer().getInventory(), new ItemStack(sd.getIntData(1), sd.getIntData(2))) && sd.getInv().firstEmpty() != -1) {
                                ItemStack con = new ItemStack(sd.getIntData(1), sd.getIntData(2), (byte) sd.getIntData(3));
                                if (FrameStore.econ.getBalance(Bukkit.getOfflinePlayer(sd.getStringData(0)).getName()) < sd.getDoubleData(0)) {
                                    p.sendMessage(ChatColor.DARK_RED + "Shop owner doesn't have enough money!");
                                    return;
                                }
                                FrameStore.econ.withdrawPlayer(Bukkit.getOfflinePlayer(sd.getStringData(0)).getName(), sd.getDoubleData(0));
                                FrameStore.econ.depositPlayer(p.getName(), sd.getDoubleData(0));
                                functions.consumeItems(e.getPlayer().getInventory(), con);
                                p.sendMessage(ChatColor.DARK_GREEN + "You sold " + sd.getIntData(2) + " of " + sd.getStringData(1));
                                sd.getInv().addItem(con);
                            } else {
                                p.sendMessage(ChatColor.DARK_RED + "Shop is not configured or doesn't have enough space or you don't have items in inventory!");
                            }
                        } else if (sd.getIntData(4) == 3) {
                            if (sd.getIntData(4) != 0 && sd.getIntData(1) != 0 && sd.getIntData(2) != 0) {
                                ItemStack con = new ItemStack(sd.getIntData(1), sd.getIntData(2), (byte) sd.getIntData(3));
                                if (e.getPlayer().getInventory().firstEmpty() == -1) {
                                    p.sendMessage(ChatColor.DARK_RED + "You don't have space in inventory!");
                                    return;
                                }
                                if (FrameStore.econ.getBalance(p.getName()) < sd.getDoubleData(0)) {

                                    p.sendMessage(ChatColor.DARK_RED + "You don't have enough money!");
                                    return;
                                }
                                p.sendMessage(ChatColor.DARK_GREEN + "You bought " + sd.getIntData(2) + " of " + sd.getStringData(1));
                                p.getInventory().addItem(con);

                            } else {
                                p.sendMessage(ChatColor.DARK_RED + "Shop is not configured or doesn't have enough resources.");
                            }
                        } else if (sd.getIntData(4) == 4) {
                            if (sd.getInv() != null && sd.getIntData(4) != 0 && sd.getIntData(1) != 0 && functions.checkItems(e.getPlayer().getInventory(), new ItemStack(sd.getIntData(1), sd.getIntData(2)))) {
                                ItemStack con = new ItemStack(sd.getIntData(1), sd.getIntData(2), (byte) sd.getIntData(3));
                                FrameStore.econ.depositPlayer(p.getName(), sd.getDoubleData(0));
                                functions.consumeItems(e.getPlayer().getInventory(), con);
                                p.sendMessage(ChatColor.DARK_GREEN + "You sold " + sd.getIntData(2) + " of " + sd.getStringData(1));
                                sd.getInv().addItem(con);
                            } else {
                                p.sendMessage(ChatColor.DARK_RED + "Shop is not configured or you don't have items in inventory!");
                            }
                        } else {
                            p.sendMessage(ChatColor.DARK_RED + "Shop is not configured.");
                        }
                    }
                    p.updateInventory();
                }
            } else {
                if (e.getPlayer().getItemInHand().getType().equals(Material.MAP)) {
                    p.sendMessage(ChatColor.DARK_RED + "You can't put map in itemframe!");
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
                        if (am == 2 || am == 1 || (p.hasPermission("frameshop.admin") && am > 0 && am <= 4)) {
                            frameshop.getServer().getScheduler().runTask(frameshop, new Runnable() {
                                @Override
                                public void run() {
                                    ShopData sd = functions.getshopl().get(Serializer.unserializeLoc(slc));
                                    sd.setData(4, (int) am);
                                    sd.reRender(frameshop);

                                }
                            });
                            //EcoListeners.functions.getShopSet().remove(p.getName());
                            p.sendMessage(ChatColor.DARK_GREEN + "You have successfully set shop type.");
                            functions.getShopSet().get(p.getName()).set(0, 2);
                            p.sendMessage(ChatColor.DARK_RED + "Now set the amount:");

                        } else if (p.hasPermission("frameshop.admin")) {
                            p.sendMessage(ChatColor.DARK_RED + "Shop is 1, Purchase is 2, AdminShop shop is 3, AdminShop purchase is 4");
                        } else {
                            p.sendMessage(ChatColor.DARK_RED + "Shop is 1, Purchase is 2");
                        }
                        break;
                    case 2:
                        if (am < 64 && am > 0) {
                            frameshop.getServer().getScheduler().runTask(frameshop, new Runnable() {
                                @Override
                                public void run() {
                                    ShopData sd = functions.getshopl().get(Serializer.unserializeLoc(slc));
                                    sd.setData(2, (int) am);
                                    sd.reRender(frameshop);

                                }
                            });
                            //EcoListeners.functions.getShopSet().remove(p.getName());
                            p.sendMessage(ChatColor.DARK_GREEN + "You have successfully set sell amount.");
                            functions.getShopSet().get(p.getName()).set(0, 3);
                            p.sendMessage(ChatColor.DARK_RED + "Now set the cost:");
                        } else {
                            p.sendMessage(ChatColor.DARK_RED + "Amount must be between 1-64");

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
                            p.sendMessage(ChatColor.DARK_GREEN + "You have successfully set sell cost.");
                            functions.getShopSet().remove(p.getName());
                        } else {
                            p.sendMessage(ChatColor.DARK_RED + "Cost must be greater than 0");
                        }
                        break;

                }
            } else {
                p.sendMessage(ChatColor.DARK_RED + "Value must be a number.");

            }
        }
    }
}
