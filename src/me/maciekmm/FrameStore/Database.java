package me.maciekmm.FrameStore;

public class Database {

    public static DatabaseConnector db = null;

    public static void createTables() {
        if(FrameStore.type.equalsIgnoreCase("mysql"))
        {
            db.query("CREATE TABLE IF NOT EXISTS shops "
                    + "(`id` INT(10) AUTO_INCREMENT PRIMARY KEY,"
                    + "`loc` MEDIUMTEXT,"
                    + " `mat` VARCHAR(30) default NULL, `inv` LONGTEXT NULL,"
                    + "  `owner` VARCHAR(20) default NULL, `cost` DOUBLE PRECISION(7,3) default '0.0', `costs` DOUBLE PRECISION(7,3) default '0.0',"
                    + " `amount` INT(20) default '0',`type` INT(1) default '0', `idd` INT(10) default '0',"
                    + " `data` INT(5) default '0',"
                    + " `name` VARCHAR(50) default NULL,"
                    + " `lore` MEDIUMTEXT,"
                    + " `enchantments` MEDIUMTEXT) CHARSET=latin1;");
        }
           
    }
}
