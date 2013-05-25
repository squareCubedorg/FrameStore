package me.maciekmm.FrameStore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static me.maciekmm.FrameStore.ShopListeners.MAGIC_NUMBER;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.util.Vector;

public class ShopData {

    public ShopData(String shopl) {
        loadData(shopl);
    }

    public ShopData(String ow, Location loc) {
        createData(loc, ow);
    }

    private void loadData(String s) {
        if (FrameStore.type.equalsIgnoreCase("mysql")) {
            try {
                String query = "SELECT * FROM `shops` WHERE loc='" + s + "'";
                rs = Database.db.query(query, true);
                rs.next();
                datas = new String[2];
                datas[0] = rs.getString("owner");
                datas[1] = rs.getString("mat");
                datas[2] = rs.getString("name");
                datai = new int[5];
                datai[1] = rs.getInt("idd");
                datai[2] = rs.getInt("amount");
                datai[3] = rs.getInt("data");
                datai[4] = rs.getInt("type");
                datad = new double[2];
                datad[0] = rs.getDouble("cost");
                datad[1] = rs.getDouble("costs");
                si = Serializer.fromBase64(rs.getString("inv"));
                sl = Serializer.unserializeLoc(rs.getString("loc"));
                imsd = Serializer.toItemMeta(rs.getString("enchantments"));
                String[] ss = rs.getString("lore").split("^$");
                ls = Arrays.asList(ss);

            } catch (SQLException ex) {
                Logger.getLogger(ShopData.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            String sr = s.replaceAll("\\.", "@");
            datas = new String[3];
            datas[0] = ShopListeners.frameshop.getShopConfig().getString(sr + ".owner");
            if (ShopListeners.frameshop.getShopConfig().getString(sr + ".mat") != null && !ShopListeners.frameshop.getShopConfig().getString(sr + ".mat").equalsIgnoreCase("null")) {
                datas[1] = ShopListeners.frameshop.getShopConfig().getString(sr + ".mat");
            } else {
                datas[1] = null;
            }
            datai = new int[5];
            datai[1] = ShopListeners.frameshop.getShopConfig().getInt(sr + ".idd");
            datai[2] = ShopListeners.frameshop.getShopConfig().getInt(sr + ".amount");
            datai[3] = ShopListeners.frameshop.getShopConfig().getInt(sr + ".data");
            datai[4] = ShopListeners.frameshop.getShopConfig().getInt(sr + ".type");
            datad = new double[2];
            datad[0] = ShopListeners.frameshop.getShopConfig().getDouble(sr + ".cost");
            datad[1] = ShopListeners.frameshop.getShopConfig().getDouble(sr + ".costs");
            if (ShopListeners.frameshop.getShopConfig().getString(sr + ".name") != null && !ShopListeners.frameshop.getShopConfig().getString(sr + ".name").equalsIgnoreCase("null")) {
                datas[2] = ShopListeners.frameshop.getShopConfig().getString(sr + ".name");
            } else {
                datas[2] = null;
            }
            if (ShopListeners.frameshop.getShopConfig().getString(sr + ".inv") != null) {
                si = Serializer.fromBase64(ShopListeners.frameshop.getShopConfig().getString(sr + ".inv"));
            } else {
                si = null;
            }
            if (ShopListeners.frameshop.getShopConfig().getString(sr + ".enchantments") != null && !ShopListeners.frameshop.getShopConfig().getString(sr + ".enchantments").equalsIgnoreCase("null")) {
                imsd = Serializer.toItemMeta(ShopListeners.frameshop.getShopConfig().getString(sr + ".enchantments"));
            } else {
                imsd = null;
            }
            sl = Serializer.unserializeLoc(s);
            ls = ShopListeners.frameshop.getShopConfig().getStringList(sr + ".lore");
            ShopListeners.frameshop.saveShopConfig();
        }
    }

    private void createData(Location loc, String owner) {
        Inventory e = Bukkit.createInventory(null, 54);
        if (FrameStore.type.equalsIgnoreCase("mysql")) {
            String queryins = "INSERT INTO `shops` (`loc`, `owner`,`inv`) VALUES ('" + Serializer.serializeLoc(loc) + "','" + owner + "','" + Serializer.toBase64(e) + "')";
            Database.db.query(queryins);
        } else {
            String[] pictures = {"owner:" + owner,
                "inv:" + Serializer.toBase64(e)};
            for (String value : Arrays.asList(pictures)) {
                String[] s = value.split(":");
                ShopListeners.frameshop.getShopConfig().set(Serializer.serializeLoc(loc).replaceAll("\\.", "@") + "." + s[0], s[1]);
            }
        }
        ShopListeners.frameshop.saveShopConfig();
        loadData(Serializer.serializeLoc(loc));
    }

    public void reRender(FrameStore pl) {

        Location nl = sl.clone();
        nl.setY(nl.getY() - 0.3);
        Entity e = nl.getWorld().spawnArrow(nl, new Vector(0, 0, 0), 0, 0);
        Iterator<Entity> it = e.getNearbyEntities(0.3, 0.3, 0.3).iterator();
        e.remove();
        MapView mv = pl.getServer().createMap(pl.getServer().getWorlds().get(0));
        ItemStack m = new ItemStack(Material.MAP, 1, mv.getId());
        while (it.hasNext()) {
            Entity sd = it.next();
            if (sd instanceof ItemFrame) {
                mv.setCenterX(MAGIC_NUMBER);
                mv.setCenterZ(0);
                for (MapRenderer mr : mv.getRenderers()) {
                    mv.removeRenderer(mr);
                }
                mv.addRenderer(new Renderer(pl, true, datas[1], datad[0], datai[2], datas[0], datai[4], datai[1], datai[3], imsd, datas[2], datad[1]));
                m.setDurability(mv.getId());
                ((ItemFrame) sd).setItem(m);
                map = mv;
            }

        }


    }

    public String getStringData(int nr) {
        return datas[nr];
    }

    public int getIntData(int nr) {
        return datai[nr];
    }

    public double getDoubleData(int nr) {
        return datad[nr];
    }

    public Inventory getInv() {
        return si;
    }
    public List<String> getLores() {
        return ls;
    }
    public void setData(int nr, String content) {
        datas[nr] = content;
    }

    public void setData(int nr, int content) {
        datai[nr] = content;
    }

    public void setData(int nr, double content) {
        datad[nr] = content;
    }

    public void setEnch(Map<Enchantment, Integer> m) {
        imsd = m;
    }
    public void setLores(List<String> s)
    {
        ls = s;
    }

    public Map<Enchantment, Integer> getEnch() {
        return imsd;
    }

    public Location getLoc() {
        return sl;
    }

    public void upData() {
        if (FrameStore.type.equalsIgnoreCase("mysql")) {
            String query = "UPDATE `shops` SET "
                    + "`owner`=" + ShopListeners.functions.nullFixer("'" + datas[0] + "'") + ", "
                    + "`mat`=" + ShopListeners.functions.nullFixer("'" + datas[1] + "'") + ", "
                    + "`inv` =" + ShopListeners.functions.nullFixer("'" + Serializer.toBase64(si) + "'") + ", "
                    + "`cost`=" + datad[0] + ", "
                    + "`idd`=" + datai[1] + ", "
                    + "`amount`=" + datai[2] + ", "
                    + "`data`=" + datai[3] + ", "
                    + "`type`=" + datai[4] + ", "
                    + "`enchantments`=" + imsd + ", "
                    + "`name`=" + ShopListeners.functions.nullFixer("'" + datas[2] + "'") + ", "
                    + "`lore`=" + ShopListeners.functions.nullFixer("'" + Serializer.serializeLore(ls) + "'") + ", "
                    + "`costs`=" + datad[1]
                    + " WHERE loc='" + Serializer.serializeLoc(sl) + "'";
            Database.db.query(query, true);
        } else {
            String sr = Serializer.serializeLoc(sl).replaceAll("\\.", "@");
            String[] pictures = {"owner:" + datas[0],
                "mat:" + datas[1],
                "inv:" + Serializer.toBase64(si),
                "name:" + datas[2],
            };
            String[] ints = {
                "idd:" + datai[1],
                "amount:" + datai[2],
                "data:" + datai[3],
                "type:" + datai[4]
            };
            if (imsd != null && !imsd.isEmpty()) {
                pictures[pictures.length - 1] = "enchantments:" + Serializer.serializeEnch(imsd);
            }
            for (String value : Arrays.asList(pictures)) {
                String[] s = value.split(":");
                ShopListeners.frameshop.getShopConfig().set(sr + "." + s[0], s[1]);
            }
            for (String value : Arrays.asList(ints)) {
                String[] s = value.split(":");
                ShopListeners.frameshop.getShopConfig().set(sr + "." + s[0], Integer.parseInt(s[1]));
            }
            ShopListeners.frameshop.getShopConfig().set(sr + ".lore", ls);
            ShopListeners.frameshop.getShopConfig().set(sr + ".cost", datad[0]);
            ShopListeners.frameshop.getShopConfig().set(sr + ".costs", datad[1]);
            ShopListeners.frameshop.saveShopConfig();
        }
    }

    public void removeFD() {
        if (FrameStore.type.equalsIgnoreCase("mysql")) {
            String q = "DELETE FROM `shops` WHERE loc='" + Serializer.serializeLoc(sl) + "';";
            Database.db.query(q, true);
        } else {
            ShopListeners.frameshop.getShopConfig().set(Serializer.serializeLoc(sl).replaceAll("\\.", "@"), null);
        }

    }

    public MapView getMap() {
        return map;
    }
    private ResultSet rs;
    private String[] datas;
    private double[] datad;
    private int[] datai;
    private Inventory si;
    private Location sl;
    private MapView map;
    private List<String> ls;
    private Map<Enchantment, Integer> imsd;
}
