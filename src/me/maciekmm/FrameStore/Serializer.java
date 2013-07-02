package me.maciekmm.FrameStore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.server.v1_6_R1.NBTBase;
import net.minecraft.server.v1_6_R1.NBTTagCompound;
import net.minecraft.server.v1_6_R1.NBTTagList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_6_R1.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_6_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;


import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
/*
 * @author Comphenix||aadnk
 * source https://gist.github.com/aadnk/4102407
 */

public class Serializer {

    public static String toBase64(Inventory inventory) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(outputStream);
        NBTTagList itemList = new NBTTagList();

        // Save every element in the list
        for (int i = 0; i < inventory.getSize(); i++) {
            NBTTagCompound outputObject = new NBTTagCompound();
            CraftItemStack craft = getCraftVersion(inventory.getItem(i));

            // Convert the item stack to a NBT compound
            if (craft != null) {
                CraftItemStack.asNMSCopy(craft).save(outputObject);
            }
            itemList.add(outputObject);
        }

        // Now save the list
        NBTBase.a(itemList, dataOutput);

        // Serialize that array
        return Base64Coder.encodeLines(outputStream.toByteArray());
    }

    public static Inventory fromBase64(String data) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
        NBTTagList itemList = (NBTTagList) NBTBase.a(new DataInputStream(inputStream));
        Inventory inventory = new CraftInventoryCustom(null, itemList.size());

        for (int i = 0; i < itemList.size(); i++) {
            NBTTagCompound inputObject = (NBTTagCompound) itemList.get(i);

            if (!inputObject.isEmpty()) {
                inventory.setItem(i, CraftItemStack.asCraftMirror(
                        net.minecraft.server.v1_6_R1.ItemStack.createStack(inputObject)));
            }
        }

        // Serialize that array
        return inventory;
    }

    public static Inventory getInventoryFromArray(ItemStack[] items) {
        CraftInventoryCustom custom = new CraftInventoryCustom(null, items.length);

        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                custom.setItem(i, items[i]);
            }
        }
        return custom;
    }

    /**
     * @author maciekmm
     * @param l - location
     * @return String
     */
    public static String serializeLoc(Location l) {
        return l.getX() + "," + l.getY() + "," + l.getZ() + "," + l.getYaw() + "," + l.getPitch() + "," + l.getWorld().getName();
    }

    public static Location unserializeLoc(String l) {
        if (l == null) {
            return null;
        }
        String[] array = l.split(",");
        if (array.length < 5) {
            return null;
        }
        return new Location(Bukkit.getWorld(array[5]), Double.valueOf(array[0]), Double.valueOf(array[1]), Double.valueOf(array[2]), Float.valueOf(array[3]),
                Float.valueOf(array[4]));
    }

    /*
     * @author maciekmm
     */
    public static String serializeEnch(Map<Enchantment, Integer> im) {
        if (im != null) {
            StringBuilder sb = new StringBuilder();
            for (Entry thisEntry : im.entrySet()) {
                sb.append(((Enchantment) thisEntry.getKey()).getId()).append("@").append(thisEntry.getValue()).append("!");
            }
            return sb.toString();
        } else {
            return null;
        }
    }
    public static String serializeLore(List<String> ls) {
        if(ls==null||ls.isEmpty())
        {
            return null;
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            for (String s : ls)
            {
                sb.append(s).append("^$");
            }
            return sb.toString();
        }
    }
    public static Map<Enchantment, Integer> toItemMeta(String s) {
        if (s != null && !s.equalsIgnoreCase("null") && !s.equals("")) {
            Map<Enchantment, Integer> em = new HashMap<>();
            String[] eslist = s.split("!");
            for (int i = 0; i <= eslist.length - 1; i++) {
                String[] ese = eslist[i].split("@");
                em.put(Enchantment.getById(Integer.parseInt(ese[0])), Integer.parseInt(ese[1]));
            }
            return em;
        } else {
            return null;
        }
    }

    public static void addEnchantments(ItemStack is, Map<Enchantment, Integer> em) {
        ItemMeta im = is.getItemMeta();
        for (Entry thisEntry : em.entrySet()) {
            im.addEnchant((Enchantment) thisEntry.getKey(), (Integer) thisEntry.getValue(), true);
        }
        is.setItemMeta(im);
    }
    /*
     * @author Comphenix
     */

    private static CraftItemStack getCraftVersion(ItemStack stack) {
        if (stack instanceof CraftItemStack) {
            return (CraftItemStack) stack;
        } else if (stack != null) {
            return CraftItemStack.asCraftCopy(stack);
        } else {
            return null;
        }
    }
}