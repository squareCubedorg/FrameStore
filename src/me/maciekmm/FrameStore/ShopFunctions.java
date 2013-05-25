package me.maciekmm.FrameStore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_5_R3.inventory.CraftInventory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ShopFunctions {

    private HashMap<Location, ShopData> shopl = new HashMap<>();
    private HashMap<String, ArrayList<Object>> shopset = new HashMap<>();

    public void dumpToDatabase() {
        for (ShopData value : shopl.values()) {
            value.upData();
        }
    }

    /**
     * Loading shops
     */
    public void loadShops() {
        if (FrameStore.type.equalsIgnoreCase("flat")) {
            for (String s : ShopListeners.frameshop.getShopConfig().getKeys(false)) {
                shopl.put(Serializer.unserializeLoc(s.replaceAll("@", ".")), new ShopData(s.replaceAll("@", ".")));
            }
        } else if (FrameStore.type.equalsIgnoreCase("mysql")) {
            try {
                String query = "SELECT loc FROM `shops`";
                ResultSet rs = Database.db.query(query, true);
                while (rs.next()) {
                    shopl.put(Serializer.unserializeLoc(rs.getString("loc")), new ShopData(rs.getString("loc")));
                }
            } catch (SQLException e) {
                FrameStore.log.severe("[Mysql-EcoCraft] Error while fetching companies!");

            }
        }
    }

    /*
     *Loading maps 
     */
    public void loadMaps(FrameStore pl) {
        for (ShopData value : shopl.values()) {
            value.reRender(pl);
        }
    }
    /*
     * RefreshForPlayer
     */

    public void sendMaps(Player player) {
        for (ShopData value : shopl.values()) {
            if (value.getMap() != null && player != null) {
                player.sendMap(value.getMap());
            }
        }
    }

    public void clearer() {
        dumpToDatabase();
        shopl.clear();
        shopset.clear();
    }

    public HashMap<Location, ShopData> getshopl() {
        return shopl;
    }

    public String nullFixer(String s) {
        if (s.equalsIgnoreCase("'null'")) {
            String sf = s.replace("'null'", "NULL");
            return sf;
        } else {
            return s;
        }
    }

    public boolean checkItems(Inventory inve, ItemStack costStack) {
        int cost = costStack.getAmount();
        boolean hasEnough = false;
        for (ItemStack invStack : inve.getContents()) {
            if (invStack == null) {
                continue;
            }
            if (invStack.getTypeId() == costStack.getTypeId()) {

                int inv = invStack.getAmount();
                if (cost - inv >= 1) {
                    cost = cost - inv;
                } else {
                    hasEnough = true;
                    break;
                }
            }
        }
        return hasEnough;
    }

    public void consumeItems(Inventory inve, ItemStack costStack) {
        inve.removeItem(costStack);

    }
    public HashMap<String, ArrayList<Object>> getShopSet() {
        return shopset;
    }
}
