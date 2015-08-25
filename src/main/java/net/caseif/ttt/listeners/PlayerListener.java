/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2015, Maxim Roncacé <mproncace@lapis.blue>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.caseif.ttt.listeners;

import net.caseif.ttt.Body;
import net.caseif.ttt.Config;
import net.caseif.ttt.TTTCore;
import net.caseif.ttt.managers.KarmaManager;
import net.caseif.ttt.managers.ScoreManager;
import net.caseif.ttt.util.Constants;
import net.caseif.ttt.util.Constants.Color;
import net.caseif.ttt.util.Constants.Role;
import net.caseif.ttt.util.InventoryUtil;

import com.google.common.base.Optional;
import net.caseif.flint.challenger.Challenger;
import net.caseif.flint.util.physical.Location3D;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

public class PlayerListener implements Listener {

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (TTTCore.mg.getChallenger(event.getPlayer().getUniqueId()).isPresent()) { // check if player is in TTT round
            Challenger ch = TTTCore.mg.getChallenger(event.getPlayer().getUniqueId()).get();
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // disallow cheating/bed setting
                if (event.getClickedBlock().getType() == Material.ENDER_CHEST
                        || event.getClickedBlock().getType() == Material.BED_BLOCK) {
                    event.setCancelled(true);
                    return;
                }
                // handle body checking
                Location3D clicked = new Location3D(
                        event.getClickedBlock().getX(),
                        event.getClickedBlock().getY(),
                        event.getClickedBlock().getZ());
                if (event.getClickedBlock().getType() == Material.CHEST) {
                    if (ch.isSpectating()) {
                        event.setCancelled(true);
                        for (Body b : TTTCore.bodies) {
                            if (b.getLocation().equals(clicked)) {
                                Inventory chestInv = ((Chest) event.getClickedBlock().getState()).getInventory();
                                Inventory inv = TTTCore.getInstance().getServer().createInventory(chestInv.getHolder(),
                                        chestInv.getSize());
                                inv.setContents(chestInv.getContents());
                                event.getPlayer().openInventory(inv);
                                TTTCore.locale.getLocalizable("info.personal.status.discreet-search")
                                        .withPrefix(Color.INFO.toString()).sendTo(event.getPlayer());
                                break;
                            }
                        }
                    } else {
                        int index = -1;
                        for (int i = 0; i < TTTCore.bodies.size(); i++) {
                            if (TTTCore.bodies.get(i).getLocation()
                                    .equals(clicked)) {
                                index = i;
                                break;
                            }
                        }
                        if (index != -1) {
                            boolean found = false;
                            for (Body b : TTTCore.foundBodies) {
                                if (b.getLocation().equals(clicked)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) { // it's a new body
                                Body b = TTTCore.bodies.get(index);
                                Optional<Challenger> bodyPlayer
                                        = TTTCore.mg.getChallenger(TTTCore.bodies.get(index).getPlayer());
                                //TODO: make this DRYer
                                switch (b.getRole()) {
                                    case Role.INNOCENT: {
                                        for (Challenger c : b.getRound().getChallengers()) {
                                            Player pl = Bukkit.getPlayer(c.getUniqueId());
                                            pl.sendMessage(Color.INNOCENT
                                                    + TTTCore.locale
                                                    .getLocalizable("info.global.round.event.body-find")
                                                    .withReplacements(event.getPlayer().getName(),
                                                            b.getPlayer().toString()).localizeFor(pl) + " "
                                                    + TTTCore.locale
                                                    .getLocalizable("info.global.round.event.body-find.innocent")
                                                    .localizeFor(pl));
                                        }
                                        break;
                                    }
                                    case Role.TRAITOR: {
                                        for (Challenger c : b.getRound().getChallengers()) {
                                            Player pl = Bukkit.getPlayer(c.getUniqueId());
                                            pl.sendMessage(Color.TRAITOR
                                                    + TTTCore.locale
                                                    .getLocalizable("info.global.round.event.body-find")
                                                    .withReplacements(event.getPlayer().getName(),
                                                            b.getPlayer().toString()).localizeFor(pl) + " "
                                                    + TTTCore.locale
                                                    .getLocalizable("info.global.round.event.body-find.traitor")
                                                    .localizeFor(pl));
                                        }
                                        break;
                                    }
                                    case Role.DETECTIVE: {
                                        for (Challenger c : b.getRound().getChallengers()) {
                                            Player pl = Bukkit.getPlayer(c.getUniqueId());
                                            pl.sendMessage(Color.DETECTIVE
                                                    + TTTCore.locale
                                                    .getLocalizable("info.global.round.event.body-find")
                                                    .withReplacements(event.getPlayer().getName(),
                                                            b.getPlayer().toString()).localizeFor(pl) + " "
                                                    + TTTCore.locale
                                                    .getLocalizable("info.global.round.event.body-find.detective")
                                                    .localizeFor(pl));
                                        }
                                        break;
                                    }
                                    default: {
                                        event.getPlayer().sendMessage("Something's gone terribly wrong inside the TTT "
                                                + "plugin. Please notify an admin."); // eh, may as well tell the player
                                        throw new AssertionError("Failed to determine role of found body. "
                                                + "Report this immediately.");
                                    }
                                }
                                TTTCore.foundBodies.add(TTTCore.bodies.get(index));
                                if (bodyPlayer.isPresent()
                                        && bodyPlayer.get().getRound().equals(TTTCore.bodies.get(index).getRound())) {
                                    //TODO: no Flint equivalent for this
                                    //bodyPlayer.setPrefix(Config.SB_ALIVE_PREFIX);
                                    bodyPlayer.get().getMetadata().set("bodyFound", true);
                                    if (ScoreManager.sbManagers.containsKey(bodyPlayer.get().getRound().getArena()
                                            .getId())) {
                                        ScoreManager.sbManagers.get(bodyPlayer.get().getRound().getArena().getId())
                                                .update(bodyPlayer.get());
                                    }
                                }
                            }
                            if (ch.getMetadata().has(Role.DETECTIVE)) { // handle DNA scanning
                                if (event.getPlayer().getItemInHand() != null
                                        && event.getPlayer().getItemInHand().getType() == Material.COMPASS
                                        && event.getPlayer().getItemInHand().getItemMeta() != null
                                        && event.getPlayer().getItemInHand().getItemMeta().getDisplayName() != null
                                        && event.getPlayer().getItemInHand().getItemMeta().getDisplayName().endsWith(
                                        TTTCore.locale.getLocalizable("item.dna-scanner.name").localize())) {
                                    event.setCancelled(true);
                                    //TODO: killer should be stored with body
                                    /*Player killer = Main.plugin.getServer().getPlayer(
                                            (String) Main.mg.getMGPlayer(Main.bodies.get(index).getPlayer())
                                                    .getMetadata("killer")
                                    );*/
                                    Body body = TTTCore.bodies.get(index);
                                    if (body.getKiller().isPresent()) {
                                        Player killer = Bukkit.getPlayer(body.getKiller().get());
                                        if (killer != null) {
                                            if (TTTCore.mg.getChallenger(killer.getUniqueId()).isPresent()) {
                                                if (!TTTCore.mg.getChallenger(killer.getUniqueId()).get()
                                                        .isSpectating()) {
                                                    ch.getMetadata().set("tracking", killer.getName());
                                                }
                                                TTTCore.locale.getLocalizable("info.personal.status.collect-dna")
                                                        .withPrefix(Color.INFO.toString())
                                                        .withReplacements(Bukkit.getPlayer(TTTCore.bodies.get(index)
                                                                .getPlayer()).getName())
                                                        .sendTo(event.getPlayer());
                                            } else {
                                                TTTCore.locale.getLocalizable("error.round.killer-left")
                                                        .withPrefix(Color.ERROR.toString()).sendTo(event.getPlayer());
                                            }
                                        } else {
                                            TTTCore.locale.getLocalizable("error.round.killer-left")
                                                    .withPrefix(Color.ERROR.toString()).sendTo(event.getPlayer());
                                        }
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
        // guns
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (event.getPlayer().getItemInHand() != null) {
                if (event.getPlayer().getItemInHand().getItemMeta() != null) {
                    if (event.getPlayer().getItemInHand().getItemMeta().getDisplayName() != null) {
                        if (event.getPlayer().getItemInHand().getItemMeta().getDisplayName()
                                .endsWith(TTTCore.locale.getLocalizable("item.gun.name").localize())) {
                            if ((TTTCore.mg.getChallenger(event.getPlayer().getUniqueId()).isPresent()
                                    && !TTTCore.mg.getChallenger(event.getPlayer().getUniqueId()).get().isSpectating()
                                    && (TTTCore.mg.getChallenger(event.getPlayer().getUniqueId()).get().getRound()
                                    .getLifecycleStage() == Constants.Stage.PLAYING) || Config.GUNS_OUTSIDE_ARENAS)) {
                                event.setCancelled(true);
                                if (event.getPlayer().getInventory().contains(Material.ARROW)
                                        || !Config.REQUIRE_AMMO_FOR_GUNS) {
                                    if (Config.REQUIRE_AMMO_FOR_GUNS) {
                                        InventoryUtil.removeArrow(event.getPlayer().getInventory());
                                        event.getPlayer().updateInventory();
                                    }
                                    event.getPlayer().launchProjectile(Arrow.class);
                                } else {
                                    TTTCore.locale.getLocalizable("info.personal.status.no-ammo")
                                            .withPrefix(Color.ERROR.toString()).sendTo(event.getPlayer());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            Optional<Challenger> victim = TTTCore.mg.getChallenger(event.getEntity().getUniqueId());
            if (victim.isPresent() && victim.get().getRound().getLifecycleStage() != Constants.Stage.PLAYING) {
                if (event.getCause() == DamageCause.VOID) {
                    Bukkit.getPlayer(victim.get().getUniqueId());
                } else {
                    event.setCancelled(true);
                }
            }
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent ed = (EntityDamageByEntityEvent) event;
                if (ed.getDamager().getType() == EntityType.PLAYER
                        || (ed.getDamager() instanceof Projectile
                        && ((Projectile) ed.getDamager()).getShooter() instanceof Player)) {
                    Player damager = ed.getDamager().getType() == EntityType.PLAYER
                            ? (Player) ed.getDamager()
                            : (Player) ((Projectile) ed.getDamager()).getShooter();
                    if (TTTCore.mg.getChallenger(damager.getUniqueId()).isPresent()) {
                        Challenger mgDamager = TTTCore.mg.getChallenger(damager.getUniqueId()).get();
                        if (mgDamager.getRound().getLifecycleStage() != Constants.Stage.PLAYING
                                || !victim.isPresent()) {
                            event.setCancelled(true);
                            return;
                        }
                        if (damager.getItemInHand() != null) {
                            if (damager.getItemInHand().getItemMeta() != null) {
                                if (damager.getItemInHand().getItemMeta().getDisplayName() != null) {
                                    if (damager.getItemInHand().getItemMeta().getDisplayName()
                                            .endsWith(TTTCore.locale.getLocalizable("item.crowbar.name")
                                                    .localize())) {
                                        event.setDamage(Config.CROWBAR_DAMAGE);
                                    }
                                }
                            }
                        }
                        Optional<Double> reduc = mgDamager.getMetadata().<Double>get("damageRed");
                        if (reduc.isPresent()) {
                            event.setDamage((int) (event.getDamage() * reduc.get()));
                        }
                        KarmaManager.handleDamageKarma(mgDamager, victim.get(), event.getDamage());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (TTTCore.mg.getChallenger(event.getPlayer().getUniqueId()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (TTTCore.mg.getChallenger(event.getPlayer().getUniqueId()).isPresent()) {
            event.setCancelled(true);
            TTTCore.locale.getLocalizable("info.personal.status.no-drop").withPrefix(Color.ERROR.toString())
                    .sendTo(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (Config.KARMA_PERSISTENCE) {
            KarmaManager.loadKarma(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!Config.KARMA_PERSISTENCE) {
            KarmaManager.playerKarma.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onHealthRegenerate(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            if (TTTCore.mg.getChallenger(p.getUniqueId()).isPresent()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        for (HumanEntity he : event.getViewers()) {
            Player p = (Player) he;
            if (TTTCore.mg.getChallenger(p.getUniqueId()).isPresent()) {
                if (event.getInventory().getType() == InventoryType.CHEST) {
                    Block block;
                    Block block2 = null;
                    if (event.getInventory().getHolder() instanceof Chest) {
                        block = ((Chest) event.getInventory().getHolder()).getBlock();
                    } else if (event.getInventory().getHolder() instanceof DoubleChest) {
                        block = ((Chest) ((DoubleChest) event.getInventory().getHolder()).getLeftSide()).getBlock();
                        block2 = ((Chest) ((DoubleChest) event.getInventory().getHolder()).getRightSide()).getBlock();
                    } else {
                        return;
                    }
                    Location3D l1 = new Location3D(block.getX(), block.getY(), block.getZ());
                    Location3D l2 = block2 != null ? new Location3D(block2.getX(), block2.getY(), block2.getZ()) : null;
                    for (Body b : TTTCore.bodies) {
                        if (b.getLocation().equals(l1) || b.getLocation().equals(l2)) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }
    }
}
