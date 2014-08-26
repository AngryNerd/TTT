package net.amigocraft.ttt.listeners;

import net.amigocraft.ttt.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

public class EntityListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onHangingBreak(HangingBreakByEntityEvent e){
		if (e.getRemover() instanceof Player){
			if (Main.mg.isPlayer(((Player) e.getRemover()).getName())){
				e.setCancelled(true);
			}
		}
	}

}
