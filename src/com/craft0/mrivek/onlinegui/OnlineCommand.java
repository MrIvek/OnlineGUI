package com.craft0.mrivek.onlinegui;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OnlineCommand implements CommandExecutor {

	private final OnlineGUI plugin;

	public OnlineCommand(OnlineGUI plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be used by a player.");
			return true;
		}

		if (args.length != 0) {
			sender.sendMessage(cmd.getUsage());
			return true;
		}

		Player player = (Player) sender;
		plugin.openOnlineList(player);
		return true;
	}

}
