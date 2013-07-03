package me.maciekmm.FrameStore;

public class Database {

    public static DatabaseConnector db = null;

    public static void createTables() {
        db.query("CREATE TABLE IF NOT EXISTS shops"
                + "(`id` INT(10) AUTO_INCREMENT PRIMARY KEY,"
                + " `loc` MEDIUMTEXT,"
                + " `inv` LONGTEXT NULL,"
                + " `type` INT(2) default '0',"
                + " `amount` INT(3) default '0',"
                + " `owner` VARCHAR(20) default NULL,"
                + " `cost` DOUBLE PRECISION(20,2) default '0.0',"
                + " `sellcost` DOUBLE PRECISION(20,2) default '0.0',"
                + " `item` MEDIUMTEXT,"
                + " `mapid` INT(10) default '0')");
    }
}
