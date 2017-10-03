package at.TimoCraft.TimoCloud.bukkit.managers;

import at.TimoCraft.TimoCloud.bukkit.TimoCloudBukkit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Created by Timo on 09.04.17.
 */
public class BukkitMessageManager {
    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', TimoCloudBukkit.getInstance().getPrefix() + message));
    }

    public static void noPermission(CommandSender sender) {
        sendMessage(sender, "&cYou don't have any permission to do that!");
    }
}