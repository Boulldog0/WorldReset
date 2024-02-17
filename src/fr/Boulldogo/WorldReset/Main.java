package fr.Boulldogo.WorldReset;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	
	public static boolean cancelled = false;
	
	public void onEnable() {
	    saveDefaultConfig();
	    cancelled = false;

	    File backupFolder = new File(getDataFolder(), "backup-world");
	    if (!backupFolder.exists()) {
	        if (backupFolder.mkdir()) {
	            getLogger().info("Folder 'backup-world' create with success.");
	        } else {
	            getLogger().warning("Impossible to create folder 'backup-world'. Please retry anytime.");
	        }
	    }

	    String version = getConfig().getString("version");
	    getLogger().info("WorldReset version " + version + " a été chargé avec succès !");

	    getCommand("reset").setExecutor(new ResetCommand(this));
	    getCommand("cancelreset").setExecutor(new CancelReset(this));
	}

	
	public void onDisable() {
		this.getLogger().info("WorldReset a été désactivé avec succès !");
	}

}
