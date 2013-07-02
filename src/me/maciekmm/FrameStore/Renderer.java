package me.maciekmm.FrameStore;

import java.awt.Font;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

/**
 *
 * @author maciekmm
 */
public class Renderer extends MapRenderer {

    Font test;
    FrameStore plg;
    double cost, costs;
    int amount, idd, data, type;
    Image i;
    String name, seller, cname;
    ShopData sd;
    Map<Enchantment, Integer> imsd;
    public Renderer(FrameStore plg, String name, double cost, int amount, String seller, int type, int idd, int data, Map<Enchantment, Integer> imsd, String cname, double costs,ShopData sd) {
        super(true);
        this.plg = plg;

        File sourceimage = new File(plg.getDataFolder() + File.separator + "textures" + File.separator + plg.getConfig().getString("pictures." + idd + "@" + data));
        if (!sourceimage.exists()) {
            sourceimage = new File(plg.getDataFolder() + File.separator + "textures" + File.separator + plg.getConfig().getString("pictures." + idd));
        }
        try {
            i = ImageIO.read(sourceimage);

        } catch (IOException ex) {
        }
        //this.stock = stock;
        if (cname != null && !cname.equalsIgnoreCase("null") && cname.length() < 12) {
            this.name = cname;
        } else if (cname != null && !cname.equalsIgnoreCase("null")) {
            this.name = "Custom " + name;
        } else {
            this.name = name;
        }
        this.seller = seller;
        this.amount = amount;
        this.type = type;
        this.cost = cost;
        this.sd = sd;
        this.idd = idd;
        this.data = data;
        this.imsd = imsd;
        this.costs = costs;

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
                canvas.drawText(6, 4 * fh + 11, MinecraftFont.Font, plg.getMessage("mapmessages.misc.costsell") + costs);
            } else {
                canvas.drawText(6, 3 * fh + 11, MinecraftFont.Font, plg.getMessage("mapmessages.misc.cost") + cost);
            }
            canvas.drawText(6, 5 * fh + 11, MinecraftFont.Font, plg.getMessage("mapmessages.misc.amount") + amount);
            if (plg.getConfig().getBoolean("map.drawid")) {
                canvas.drawText(6, 6 * fh + 11, MinecraftFont.Font, plg.getMessage("mapmessages.misc.id") + idd + ":" + data);
            }
            if (imsd != null && !imsd.isEmpty() && type != 4 && type != 2) {
                canvas.drawText(6, 7 * fh + 11, MinecraftFont.Font, plg.getMessage("mapmessages.misc.enchantments"));
                int line = 1;
                for (Map.Entry thisEntry : imsd.entrySet()) {
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
