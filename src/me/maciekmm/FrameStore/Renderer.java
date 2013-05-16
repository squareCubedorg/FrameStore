package me.maciekmm.FrameStore;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import me.maciekmm.FrameStore.FrameStore;
import org.bukkit.Bukkit;
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

    FrameStore plg;
    boolean redrawneeded, shop;
    double cost;
    int amount, idd, data, type;
    Image i;
    String name, seller;
    Map<Enchantment, Integer> imsd;
    public Renderer(FrameStore plg, boolean rn, String name, double cost, int amount, String seller, int type, int idd, int data, Map<Enchantment, Integer> imsd) {
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
        redrawneeded = rn;
        //this.stock = stock;
        this.seller = seller;
        this.name = name;
        this.amount = amount;
        this.type = type;
        this.cost = cost;
        this.idd = idd;
        this.data = data;
        this.imsd = imsd;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player p) {

        int fh = MinecraftFont.Font.getHeight();
                if (i != null) {
            canvas.drawImage(78, fh + 10, i.getScaledInstance(50, 50, Image.SCALE_DEFAULT));
        }
        if (type == 1) {
            canvas.drawText(10, fh, MinecraftFont.Font, "§16;Buy (Right click to buy)");
        } else if (type == 2) {
            canvas.drawText(10, fh, MinecraftFont.Font, "§16;Sell (Right click to sell)");
        } else if (type == 3) {
            canvas.drawText(10, fh, MinecraftFont.Font, "§16;AdminShop Buy ");
            canvas.drawText(11, fh + 10, MinecraftFont.Font, "§16; Right click to buy");

        } else if (type == 4) {
            canvas.drawText(10, fh, MinecraftFont.Font, "§16;AdminShop Sell");
            canvas.drawText(11, fh + 10, MinecraftFont.Font, "§16; Right click to sell");
        } else {
            canvas.drawText(10, fh, MinecraftFont.Font, "§16;Not configurated");
        }


        if (name == null) {
            name = "Empty Shop";
        }
        canvas.drawText(6, 2 * fh + 11, MinecraftFont.Font, name);

        canvas.drawText(6, 3 * fh + 11, MinecraftFont.Font, "Cost: §28;" + cost);
        canvas.drawText(6, 4 * fh + 11, MinecraftFont.Font, "Amount: §28;" + amount);
        canvas.drawText(6, 5 * fh + 11, MinecraftFont.Font, "Id: " + idd + ":" + data);
        if(imsd != null && !imsd.isEmpty() && type!=4 && type!=2)
        {
        canvas.drawText(6, 6 * fh + 11, MinecraftFont.Font, "Enchantments: ");
        int line = 1;
        for (Map.Entry thisEntry : imsd.entrySet()) {

              canvas.drawText(6, (6+line) * fh + 11 , MinecraftFont.Font, ((Enchantment)thisEntry.getKey()).getName()+": "+thisEntry.getValue());
 
          line++;
        }
        }
        if (seller != null) {
            canvas.drawText(6, 125 - fh, MinecraftFont.Font, "Owner: §16;" + seller);
        }

        //canvas.drawText(127 - MinecraftFont.Font.getWidth("     EconomyLife"), 125 - fh, MinecraftFont.Font, "§12;EconomyLife" + cost);
        if (redrawneeded) {
            redrawneeded = false;
            for (Player pm : Bukkit.getOnlinePlayers()) {
                pm.sendMap(map);
            }


        }
    }
}
