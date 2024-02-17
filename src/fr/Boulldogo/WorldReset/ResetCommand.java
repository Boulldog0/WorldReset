package fr.Boulldogo.WorldReset;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ResetCommand implements CommandExecutor {

    private final Main plugin;

    public ResetCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] arg3) {

        if (!(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sender.sendMessage(ChatColor.RED + "This command can be only executed by console.");
            return true;
        }

        startResetTimer();
        return true;
    }

    private void startResetTimer() {
        new BukkitRunnable() {
            boolean usePrefix = plugin.getConfig().getBoolean("use_prefix");
            String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
            int seconds = plugin.getConfig().getInt("reset_time");
            int secondsRemaining = seconds;

            @Override
            public void run() {
            	
                if (secondsRemaining == seconds * 1 || secondsRemaining == seconds / 2 || secondsRemaining == seconds / 4) {
                	String message = prefix + plugin.getConfig().getString("reset_message").replace("%s", String.valueOf((int)secondsRemaining));
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
                    plugin.getLogger().info("A reset is in progress. If you want to stop it, quickly type /cancelreset!");
                }
                
                if (secondsRemaining > 0) {
            		if(Main.cancelled) {
            			cancel();
            			plugin.getLogger().info("The current reset of the map has been correctly stopped !");
            			Main.cancelled = false;
            		}
                }

                if (secondsRemaining <= 0) {
                    kickAllPlayers();

                    backupWorld();

                    deleteWorld();

                    restartTimer();
                    cancel();
                }

                secondsRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void backupWorld() {
        World world = Bukkit.getWorlds().get(0);
        if (world != null) {
            File sourceFolder = world.getWorldFolder();
            File backupFolder = new File(plugin.getDataFolder(), "backup-world");
            int maxBackups = plugin.getConfig().getInt("max_backups");

            List<File> existingBackups = Arrays.asList(backupFolder.listFiles());

            if (maxBackups == 0) {
                maxBackups += 1;
            }

            if(existingBackups.size() > maxBackups) {
                Collections.sort(existingBackups, Comparator.comparingLong(File::lastModified));
                File oldestBackup = existingBackups.get(0);

                if (oldestBackup.isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(oldestBackup);
                        plugin.getLogger().info("Old backup " + oldestBackup.getName() + " deleted because max backups allowed are exceeded");
                    } catch (IOException e) {
                        e.printStackTrace();
                        plugin.getLogger().warning("Failed to delete old backup: " + oldestBackup.getName());
                    }
                } else {
                    if (oldestBackup.delete()) {
                        plugin.getLogger().info("Old backup (file) deleted: " + oldestBackup.getName() + " because max backups allowed are exceeded");
                    } else {
                        plugin.getLogger().warning("Failed to delete old backup (file): " + oldestBackup.getName());
                    }
                }

                existingBackups = new ArrayList<>(Arrays.asList(backupFolder.listFiles()));
            }

            String deletedWorldName = "deletedworld_" + new SimpleDateFormat("yyyy_MM_dd_HH:mm").format(new Date());
            File renamedBackup = new File(backupFolder, deletedWorldName);

            try {
                FileUtils.copyDirectory(sourceFolder, renamedBackup);
                plugin.getLogger().info("World saved in plugin folders as: " + renamedBackup.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void restartTimer() {
        new BukkitRunnable() {
            int seconds = plugin.getConfig().getInt("time_to_restart");
            int secondsRemaining = seconds;

            @Override
            public void run() {
            	
            	if(secondsRemaining > 0) {
            		kickAllPlayers();
                    plugin.getLogger().info("A reset is in progress. If you want to stop it, quickly type /cancelreset!");
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("restart_message")).replace("%s", String.valueOf(seconds)));
            		if(Main.cancelled) {
            			cancel();
            			plugin.getLogger().info("The current reset of the map has been correctly stopped !");
            			Main.cancelled = false;
            		}
            	}

                if (secondsRemaining == 0) {
                    plugin.getLogger().info("Server is shutting down...");
                    Bukkit.getServer().shutdown();
                    cancel();
                }

                secondsRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }


    private void kickAllPlayers() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player player : players) {
            if (!(player.hasPermission("worldreset.kick.exclude"))) {
                boolean usePrefix = plugin.getConfig().getBoolean("use_prefix");
                String prefix = usePrefix ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix")) : "";
                String kickMessage = plugin.getConfig().getString("kick_message");
                player.kickPlayer(prefix + ChatColor.translateAlternateColorCodes('&', kickMessage));
            }
        }
    }

    private void deleteWorld() {
        World world = Bukkit.getWorlds().get(0);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
            deleteWorldFolder(world.getWorldFolder());
        }
    }

    private void deleteWorldFolder(File worldFolder) {
        if (worldFolder.exists()) {
            try {
                FileUtils.deleteDirectory(worldFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
