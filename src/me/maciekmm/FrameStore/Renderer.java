package me.maciekmm.FrameStore;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import me.maciekmm.FrameStore.FrameStore;
import org.bukkit.Bukkit;
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
    boolean redrawneeded,shop;
    double cost;
    int amount,idd,data,type;
    Image i;
    String name,seller;
    public Renderer(FrameStore plg, boolean rn, String name, double cost, int amount, String seller, int type, int idd, int data) {
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
        this.data= data;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player p) {

        int fh = MinecraftFont.Font.getHeight();
        if (type == 1) {
            canvas.drawText(10, fh, MinecraftFont.Font, "§16;Sell");
        } else if (type == 2) {
            canvas.drawText(10, fh, MinecraftFont.Font, "§16;Purchase");
        } else if (type == 3) {
            canvas.drawText(10, fh, MinecraftFont.Font, "§16;AdminShop Sell");
            
        } else if(type==4) 
        {   canvas.drawText(10, fh, MinecraftFont.Font, "§16;AdminShop Purchase");
        } else {
            canvas.drawText(10, fh, MinecraftFont.Font, "§16;Not configurated");
        }
        if (i != null) {
            canvas.drawImage(78, fh + 10, i.getScaledInstance(50, 50, Image.SCALE_DEFAULT));
        }

        if (name == null) {
            name = "Empty Shop";
        }
        canvas.drawText(6, 2 * fh + 10, MinecraftFont.Font, name);

        canvas.drawText(6, 3 * fh + 10, MinecraftFont.Font, "Cost: §28;" + cost);
        canvas.drawText(6, 4 * fh + 10, MinecraftFont.Font, "Amount: §28;" + amount);
        canvas.drawText(6, 5 * fh + 10, MinecraftFont.Font, "Id: "+idd+":"+data);
        if (seller != null) {
            canvas.drawText(6, 8 * fh + 10, MinecraftFont.Font, "Seller: §16;" + seller);
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
