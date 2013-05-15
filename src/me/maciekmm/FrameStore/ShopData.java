package me.maciekmm.FrameStore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import static me.maciekmm.FrameStore.ShopListeners.MAGIC_NUMBER;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
                datai = new int[5];
                datai[1] = rs.getInt("idd");
                datai[2] = rs.getInt("amount");
                datai[3] = rs.getInt("data");
                datai[4] = rs.getInt("type");
                datad = new double[1];
                datad[0] = rs.getDouble("cost");
                si = Serializer.fromBase64(rs.getString("inv"));
                sl = Serializer.unserializeLoc(rs.getString("loc"));

            } catch (SQLException ex) {
                Logger.getLogger(ShopData.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            String sr = s.replaceAll("\\.", "@");
            datas = new String[2];
            datas[0] = ShopListeners.frameshop.getShopConfig().getString(sr + ".owner");
            datas[1] = ShopListeners.frameshop.getShopConfig().getString(sr + ".mat");
            datai = new int[5];
            if (ShopListeners.frameshop.getShopConfig().getString(sr + ".idd") != null) {
                datai[1] = Integer.valueOf(ShopListeners.frameshop.getShopConfig().getString(sr + ".idd"));
            } else {
                datai[1] = 0;
            }
            if (ShopListeners.frameshop.getShopConfig().getString(sr + ".amount") != null) {
                datai[2] = Integer.valueOf(ShopListeners.frameshop.getShopConfig().getString(sr + ".amount"));
            } else {
                datai[2] = 0;
            }
            if (ShopListeners.frameshop.getShopConfig().getString(sr + ".data") != null) {
                datai[3] = Integer.valueOf(ShopListeners.frameshop.getShopConfig().getString(sr + ".data"));
            } else {
                datai[3] = 0;
            }
            if (ShopListeners.frameshop.getShopConfig().getString(sr + ".type") != null) {
                datai[4] = Integer.valueOf(ShopListeners.frameshop.getShopConfig().getString(sr + ".type"));
            } else {
                datai[4] = 0;
            }
            datad = new double[1];
            if (ShopListeners.frameshop.getShopConfig().getString(sr + ".cost") != null) {
                datad[0] = Double.valueOf(ShopListeners.frameshop.getShopConfig().getString(sr + ".cost"));
            } else {
                datad[0] = 0;
            }
            if (ShopListeners.frameshop.getShopConfig().getString(sr + ".inv") != null) {
                si = Serializer.fromBase64(ShopListeners.frameshop.getShopConfig().getString(sr + ".inv"));
            } else {
                si = null;
            }
            sl = Serializer.unserializeLoc(s);
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
        Entity e = Bukkit.getWorlds().get(0).spawnArrow(nl, new Vector(0, 0, 0), 0, 0);
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
                mv.addRenderer(new Renderer(pl, true, datas[1], datad[0], datai[2], datas[0], datai[4],datai[1],datai[3]));
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

    public void setData(int nr, String content) {
        datas[nr] = content;
    }

    public void setData(int nr, int content) {
        datai[nr] = content;
    }

    public void setData(int nr, double content) {
        datad[nr] = content;
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
                    + "`type`=" + datai[4]
                    + " WHERE loc='" + Serializer.serializeLoc(sl) + "'";
            Database.db.query(query, true);
        } else {
            String sr = Serializer.serializeLoc(sl).replaceAll("\\.", "@");
            String[] pictures = {"owner:" + datas[0],
                "mat:" + datas[1],
                "cost:" + datad[0],
                "idd:" + datai[1],
                "amount:" + datai[2],
                "data:" + datai[3],
                "type:" + datai[4],
                "inv:" + Serializer.toBase64(si),};
            for (String value : Arrays.asList(pictures)) {
                String[] s = value.split(":");
                ShopListeners.frameshop.getShopConfig().set(sr + "." + s[0], s[1]);
            }
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
}
