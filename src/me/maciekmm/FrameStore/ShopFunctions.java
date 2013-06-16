package me.maciekmm.FrameStore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.Location;
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
     * Nr 
     */

    /*
     *Loading maps 
     */
    public void loadMaps(FrameStore pl) {
        for (ShopData value : shopl.values()) {
            value.reRender(pl);
        }
    }

    public void unload(Player player) {
        for (ShopData value : shopl.values()) {
            if (player != null) {
                value.setUnCompleted(player.getName());
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
        return inve.containsAtLeast(costStack, costStack.getAmount());
    }

    public void consumeItems(Inventory inve, ItemStack costStack) {
        inve.removeItem(costStack);

    }

    public HashMap<String, ArrayList<Object>> getShopSet() {
        return shopset;
    }
}
