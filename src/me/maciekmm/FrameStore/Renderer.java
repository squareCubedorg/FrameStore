package me.maciekmm.FrameStore;

import java.awt.Font;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

/**
 *
 * @author maciekmm
 */
public class Renderer extends MapRenderer {

    FrameStore plg;
    double cost, sellcost;
    int type;
    Image i;
    String seller,name;
    ShopData sd;
    ItemStack item;
    public Renderer(FrameStore plg,ShopData sd, ItemStack item, double cost,double costs, String seller, int type) {
        super(true);
        this.plg = plg;
        if(item!=null)
        {
            File sourceimage = new File(plg.getDataFolder() + File.separator + "textures" + File.separator + plg.getConfig().getString("pictures." + item.getTypeId() + "@" + item.getData().getData()));
            if (!sourceimage.exists()) {
                sourceimage = new File(plg.getDataFolder() + File.separator + "textures" + File.separator + plg.getConfig().getString("pictures." + item.getTypeId()));
            }
            try {
                i = ImageIO.read(sourceimage);

            } catch (IOException ex) {
            }
        
        if (item.getItemMeta().getDisplayName() != null && item.getItemMeta().getDisplayName().length() < 12) {
            this.name = item.getItemMeta().getDisplayName();
        } else if (item.getItemMeta().getDisplayName() != null) {
            this.name = plg.getMessage("mapmessages.misc.custom") + item.getType().toString().toLowerCase();
        } else {
            this.name = item.getType().toString().toLowerCase();
        }
        }
        this.seller = seller;
        this.type = type;
        this.cost = cost;
        this.sd = sd;
        this.sellcost = costs;
        this.item = item;

        
    }
    @Override
    public void render(MapView map, MapCanvas canvas, Player p) {
        if (!sd.isCompleted(p.getName())) {
            int fh = MinecraftFont.Font.getHeight();
            if (i != null) {
                canvas.drawImage(78, fh + 10, i.getScaledInstance(50, 50, Image.SCALE_DEFAULT));
            }
            if (type == 1) {
                canvas.drawText(7, fh, MinecraftFont.Font, plg.getMessage("mapmessages.types.buy"));
            } else if (type == 2) {
                canvas.drawText(7, fh, MinecraftFont.Font, plg.getMessage("mapmessages.types.sell"));
            } else if (type == 3) {
                canvas.drawText(7, fh, MinecraftFont.Font, plg.getMessage("mapmessages.types.adminshop.buy.glob"));
                canvas.drawText(8, fh + 10, MinecraftFont.Font, plg.getMessage("mapmessages.types.adminshop.buy.desc"));

            } else if (type == 4) {
                canvas.drawText(7, fh, MinecraftFont.Font, plg.getMessage("mapmessages.types.adminshop.sell.glob"));
                canvas.drawText(8, fh + 10, MinecraftFont.Font, plg.getMessage("mapmessages.types.adminshop.sell.desc"));
            } else if (type == 5) {
                canvas.drawText(7, fh, MinecraftFont.Font, plg.getMessage("mapmessages.types.both.buy"));
                canvas.drawText(7, fh + 10, MinecraftFont.Font, plg.getMessage("mapmessages.types.both.sell"));
            } else if (type == 6) {
                canvas.drawText(7, fh, MinecraftFont.Font, plg.getMessage("mapmessages.types.adminshop.both.buy"));
                canvas.drawText(7, fh + 10, MinecraftFont.Font, plg.getMessage("mapmessages.types.adminshop.both.sell"));
            } else {
                canvas.drawText(7, fh, MinecraftFont.Font, plg.getMessage("mapmessages.types.notconf"));
            }
            if (name != null) {
                String nn = name.replaceAll("§([1-9])", " ");
                canvas.drawText(6, 2 * fh + 11, MinecraftFont.Font, nn);
            }

            if (type == 5 || type == 6) {
                canvas.drawText(6, 3 * fh + 11, MinecraftFont.Font, plg.getMessage("mapmessages.misc.costbuy") + cost);
                canvas.drawText(6, 4 * fh + 11, MinecraftFont.Font, plg.getMessage("mapmessages.misc.costsell") + sellcost);
            } else {
                canvas.drawText(6, 3 * fh + 11, MinecraftFont.Font, plg.getMessage("mapmessages.misc.cost") + cost);
            }
            canvas.drawText(6, 5 * fh + 11, MinecraftFont.Font, plg.getMessage("mapmessages.misc.amount") + sd.getAmount());
            if (plg.getConfig().getBoolean("map.drawid")&&item!=null) {
                canvas.drawText(6, 6 * fh + 11, MinecraftFont.Font, plg.getMessage("mapmessages.misc.id") + item.getTypeId() + ":" + item.getData().getData());
            }
            if (item!=null && this.item.getEnchantments() != null && !this.item.getEnchantments().isEmpty() && type != 4 && type != 2) {
                canvas.drawText(6, 7 * fh + 11, MinecraftFont.Font, plg.getMessage("mapmessages.misc.enchantments"));
                int line = 1;
                for (Map.Entry thisEntry : this.item.getEnchantments().entrySet()) {
                    canvas.drawText(6, (7 + line) * fh + 11, MinecraftFont.Font, ((Enchantment) thisEntry.getKey()).getName() + ": " + thisEntry.getValue());
                    line++;
                }
            }
            if (item!=null && item.hasItemMeta() && item.getType() == Material.ENCHANTED_BOOK) {
                canvas.drawText(6, 7 * fh + 11, MinecraftFont.Font, plg.getMessage("mapmessages.misc.enchantments"));
                EnchantmentStorageMeta itemmeta  = (EnchantmentStorageMeta)item.getItemMeta();
                int line = 1;
                for(Map.Entry thisEntry : itemmeta.getStoredEnchants().entrySet()) {
                    canvas.drawText(6, (7 + line) * fh + 11, MinecraftFont.Font, ((Enchantment) thisEntry.getKey()).getName() + ": " + thisEntry.getValue());
                    line++;
                }
            }
            if (seller != null && (type == 1 || type == 2 || type == 5) && plg.getConfig().getBoolean("map.drawowner")) {
                canvas.drawText(6, 125 - fh, MinecraftFont.Font, plg.getMessage("mapmessages.misc.owner") + seller);
            }
                sd.setCompleted(p.getName());
                p.sendMap(map);
        }
    }
}
