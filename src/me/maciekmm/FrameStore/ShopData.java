package me.maciekmm.FrameStore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
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
import org.bukkit.inventory.meta.ItemMeta;
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
        try {
            String query = "SELECT * FROM `shops` WHERE loc='" + s + "'";
            rs = Database.db.query(query);
            rs.next();
            owner = rs.getString("owner");
            mapid = rs.getInt("mapid");
            amount = rs.getInt("amount");
            datad = new double[2];
            datad[0] = rs.getDouble("cost");
            datad[1] = rs.getDouble("sellcost");
            type = rs.getInt("type");
            si = Serializer.fromBase64(rs.getString("inv"));
            sl = Serializer.unserializeLoc(rs.getString("loc"));
            item = Serializer.getItem(rs.getString("item"));
            rs.close();
        } catch (SQLException ex) {
            Logger.getLogger(ShopData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createData(Location loc, String owner) {
        Inventory e = Bukkit.createInventory(null, 54);
        String queryins = "INSERT INTO `shops` (`loc`, `owner`,`inv`) VALUES ('" + Serializer.serializeLoc(loc) + "','" + owner + "','" + Serializer.toBase64(e) + "')";
        Database.db.query(queryins);
        loadData(Serializer.serializeLoc(loc));
    }

    public void reRender(FrameStore pl) {
        re.clear();
        Location nl = sl.clone();
        nl.setY(nl.getY() - 0.3);
        Entity e = nl.getWorld().spawnArrow(nl, new Vector(0, 0, 0), 0, 0);
        Iterator<Entity> it = e.getNearbyEntities(0.3, 0.3, 0.3).iterator();
        e.remove();
        MapView mv;
        if (mapid == 0) {
            mv = pl.getServer().createMap(pl.getServer().getWorlds().get(0));
            mapid = mv.getId();
        } else {
            mv = pl.getServer().getMap((short) mapid);
        }
        ItemStack m = new ItemStack(Material.MAP, 1);

        while (it.hasNext()) {
            Entity sd = it.next();
            if (sd instanceof ItemFrame) {

                mv.setCenterX(MAGIC_NUMBER);
                mv.setCenterZ(0);
                for (MapRenderer mr : mv.getRenderers()) {
                    mv.removeRenderer(mr);
                }
                if(item!=null)
                {
                    ItemMeta im = item.getItemMeta();
                    
                    mv.addRenderer(
                            new Renderer(pl,
                            item.getType().toString().toLowerCase(),
                            datad[0],
                            amount, 
                            owner, 
                            type,
                            item.getTypeId(),
                            item.getData().getData(),
                            im.getEnchants(),
                            im.getDisplayName(),
                            datad[1],
                            this));
                }
                else
                {
                    mv.addRenderer(new Renderer(pl, null, datad[0],0, owner, type, 0, 0, null, null, datad[1], this));
                }
                
                m.setDurability(mv.getId());
                ((ItemFrame) sd).setItem(m);
                map = mv;
            }
        }
    }

    public String getOwner() {
        return owner;
    }

    public double getCost() {
        return datad[0];
    }

    public double getSellCost() {
        return datad[1];
    }
    public int getType() {
        return type;
    }
    public ItemStack getItem() {
        if(item!=null&&amount!=0)
        item.setAmount(amount);
        
        return item;
    }
    public int getAmount() {
            return amount;}
    public void setAmount(int amount) {
        this.amount = amount;
        changed=true;
    }
    public void setItem(ItemStack is)  {
        changed = true;
        item = is.clone();
    }
    public Inventory getInv() {
        changed = true;
        return si;
    }

    public void setCost(double content) {
        datad[0] = content;
        changed = true;
    }

    public void setSellCost(double sellCost) {
        datad[1] = sellCost;
        changed = true;
    }
    public void setType(int type) {
        this.type = type;
        changed = true;
    } 
    public Location getLoc() {
        return sl;
    }

    public void upData() {
        if (changed) {
            String query = "UPDATE `shops` SET "
                    + "`owner`=" + ShopListeners.functions.nullFixer("'" + owner + "'") + ", "
                    + "`inv` =" + ShopListeners.functions.nullFixer("'" + Serializer.toBase64(si) + "'") + ", "
                    + "`cost`=" + datad[0] + ", "
                    + "`sellcost`=" + datad[1] + ", "
                    + "`item`=" + ShopListeners.functions.nullFixer("'" + Serializer.getStringFromItem(item) + "'") + ", "
                    + "`type`=" + type + ", "
                    + "`amount`="+amount+", "
                    + "`mapid`=" + mapid
                    + " WHERE `loc`='" + Serializer.serializeLoc(sl) + "'";
            Database.db.query(query);
            changed=false;
        }
    }

    public void removeFD() {
        String q = "DELETE FROM `shops` WHERE loc='" + Serializer.serializeLoc(sl) + "'";
        Database.db.query(q);
    }

    public MapView getMap() {
        return map;
    }

    public boolean isCompleted(String p) {
        return re.contains(p);
    }

    public void setCompleted(String p) {
        re.add(p);
    }

    public void setUnCompleted(String p) {
        re.remove(p);
    }
    private ResultSet rs;
    private ItemStack item;
    private String owner;
    private double[] datad;
    private Inventory si;
    private int mapid,amount,type;
    private Location sl;
    private MapView map;
    private boolean changed = false;
    HashSet<String> re = new HashSet<>();
}
