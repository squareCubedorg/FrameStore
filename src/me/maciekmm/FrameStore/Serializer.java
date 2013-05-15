package me.maciekmm.FrameStore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.server.v1_5_R3.NBTBase;
import net.minecraft.server.v1_5_R3.NBTTagCompound;
import net.minecraft.server.v1_5_R3.NBTTagList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_5_R3.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_5_R3.inventory.CraftItemStack;

 
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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
            if (craft != null) 
                CraftItemStack.asNMSCopy(craft).save(outputObject);
            itemList.add(outputObject);
        }
 
        // Now save the list
        NBTBase.a(itemList, dataOutput);
 
        // Serialize that array
        return Base64Coder.encodeLines(outputStream.toByteArray());
    }
    
    public static Inventory fromBase64(String data) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
        NBTTagList itemList = (NBTTagList) NBTBase.b(new DataInputStream(inputStream));
        Inventory inventory = new CraftInventoryCustom(null, itemList.size());

 
        for (int i = 0; i < itemList.size(); i++) {
            NBTTagCompound inputObject = (NBTTagCompound) itemList.get(i);
            
            if (!inputObject.isEmpty()) {
                inventory.setItem(i, CraftItemStack.asCraftMirror(
                    net.minecraft.server.v1_5_R3.ItemStack.createStack(inputObject)));
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
     * @author Comphenix
     */
    private static CraftItemStack getCraftVersion(ItemStack stack) {
        if (stack instanceof CraftItemStack)
            return (CraftItemStack) stack;
        else if (stack != null)
            return CraftItemStack.asCraftCopy(stack);
        else
            return null;
    }


}