package com.craft0.mrivek.onlinegui;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class OnlineCommand implements CommandExecutor {

	private OnlineGUI plugin;

	public OnlineCommand(OnlineGUI plugin) {
		this.plugin = plugin;
	}

	@Override
	@EventHandler(priority = EventPriority.HIGHEST)
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return true;
		}

		if (args.length != 0) {
			sender.sendMessage(cmd.getUsage());
			return true;
		}

		Player player = (Player) sender;
		plugin.open(player);
		return true;
	}

}
