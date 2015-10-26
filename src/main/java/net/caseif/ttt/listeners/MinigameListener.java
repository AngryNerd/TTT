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

import net.caseif.ttt.TTTCore;
import net.caseif.ttt.command.handler.use.JoinCommand;
import net.caseif.ttt.scoreboard.ScoreboardManager;
import net.caseif.ttt.util.Constants.Color;
import net.caseif.ttt.util.Constants.MetadataTag;
import net.caseif.ttt.util.Constants.Role;
import net.caseif.ttt.util.Constants.Stage;
import net.caseif.ttt.util.helper.gamemode.KarmaHelper;
import net.caseif.ttt.util.helper.gamemode.RoundHelper;
import net.caseif.ttt.util.helper.misc.MiscHelper;
import net.caseif.ttt.util.helper.platform.LocationHelper;
import net.caseif.ttt.util.helper.platform.TitleHelper;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import net.caseif.flint.challenger.Challenger;
import net.caseif.flint.challenger.Team;
import net.caseif.flint.event.lobby.PlayerClickLobbySignEvent;
import net.caseif.flint.event.round.RoundChangeLifecycleStageEvent;
import net.caseif.flint.event.round.RoundEndEvent;
import net.caseif.flint.event.round.RoundTimerTickEvent;
import net.caseif.flint.event.round.challenger.ChallengerJoinRoundEvent;
import net.caseif.flint.event.round.challenger.ChallengerLeaveRoundEvent;
import net.caseif.flint.round.Round;
import net.caseif.rosetta.Localizable;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;

public class MinigameListener {

    @Subscribe
    public void onChallengerJoinRound(ChallengerJoinRoundEvent event) {
        if (event.getRound().getLifecycleStage() == Stage.PLAYING) {
            event.getChallenger().setSpectating(true);
            event.getChallenger().getMetadata().set(MetadataTag.PURE_SPECTATOR, true);
        }

        Bukkit.getPlayer(event.getChallenger().getUniqueId())
                .setHealth(Bukkit.getPlayer(event.getChallenger().getUniqueId()).getMaxHealth());

        Bukkit.getPlayer(event.getChallenger().getUniqueId())
                .setCompassTarget(Bukkit.getWorlds().get(1).getSpawnLocation());

        if (ScoreboardManager.get(event.getRound()).isPresent()) {
            ScoreboardManager.get(event.getRound()).get().assignScoreboard(event.getChallenger());
        }

        if (!event.getChallenger().getMetadata().has(MetadataTag.PURE_SPECTATOR)) {
            if (ScoreboardManager.get(event.getRound()).isPresent()) {
                ScoreboardManager.get(event.getRound()).get().update(event.getChallenger());
            }

            Player pl = Bukkit.getPlayer(event.getChallenger().getUniqueId());
            pl.setGameMode(GameMode.SURVIVAL);
            KarmaHelper.applyKarma(event.getChallenger());

            MiscHelper.broadcast(event.getRound(),
                    TTTCore.locale.getLocalizable("info.global.arena.event.join").withPrefix(Color.INFO)
                            .withReplacements(event.getChallenger().getName() + TTTCore.clh.getContributorString(pl)));

            if (event.getRound().getLifecycleStage() == Stage.WAITING
                    && event.getRound().getChallengers().size() >= TTTCore.config.MINIMUM_PLAYERS) {
                event.getRound().nextLifecycleStage();
            }
        }
    }

    @Subscribe
    public void onChallengerLeaveRound(ChallengerLeaveRoundEvent event) {
        Player pl = Bukkit.getPlayer(event.getChallenger().getUniqueId());
        pl.setScoreboard(
                TTTCore.getPlugin().getServer().getScoreboardManager().getNewScoreboard()
        );

        if (event.getChallenger().getMetadata().has(MetadataTag.SEARCHING_BODY)) {
            pl.closeInventory();
        }

        pl.setDisplayName(event.getChallenger().getName());
        pl.setCompassTarget(LocationHelper.convert(event.getReturnLocation()).getWorld().getSpawnLocation());
        pl.setHealth(pl.getMaxHealth());

        if (!event.getRound().getMetadata().has("ending")) { //TODO: temp fix
            if (!event.getChallenger().getMetadata().has(MetadataTag.PURE_SPECTATOR)) {
                KarmaHelper.saveKarma(event.getChallenger());
                MiscHelper.broadcast(event.getRound(), TTTCore.locale.getLocalizable("info.global.arena.event.leave")
                        .withPrefix(Color.INFO).withReplacements(event.getChallenger().getName(),
                                Color.ARENA + event.getChallenger().getRound().getArena().getName() + Color.INFO));

                if (event.getRound().getLifecycleStage() == Stage.PREPARING
                        && event.getRound().getChallengers().size() <= 1) {
                    event.getRound().setLifecycleStage(Stage.WAITING);
                    MiscHelper.broadcast(event.getRound(),
                            TTTCore.locale.getLocalizable("info.global.round.status.starting.stopped")
                                    .withPrefix(Color.ERROR));
                }
            }
        }
    }

    @Subscribe
    public void onRoundPrepare(RoundChangeLifecycleStageEvent event) {
        if (event.getStageAfter() == Stage.PREPARING) {
            MiscHelper.broadcast(event.getRound(), TTTCore.locale.getLocalizable("info.global.round.event.starting")
                    .withPrefix(Color.INFO));
            ScoreboardManager.getOrCreate(event.getRound());
        } else if (event.getStageAfter() == Stage.PLAYING) {
            RoundHelper.startRound(event.getRound());
        }
    }

    @SuppressWarnings({"deprecation"})
    @Subscribe
    public void onRoundTick(RoundTimerTickEvent event) {
        Round r = event.getRound();
        if (r.getLifecycleStage() != Stage.WAITING) {
            long rTime = r.getRemainingTime();
            Localizable loc;
            Localizable time = null;
            if (rTime >= 60 && rTime % 60 == 0) {
                time = TTTCore.locale.getLocalizable("fragment.minutes" + (rTime / 60 == 1 ? ".singular" : ""))
                        .withReplacements(Long.toString(rTime / 60));
                ;
            } else if (rTime > 0 && rTime <= 30 && rTime % 10 == 0) {
                time = TTTCore.locale.getLocalizable("fragment.seconds" + (rTime == 1 ? ".singular" : ""))
                        .withReplacements(Long.toString(rTime));
            }
            if (time != null) {
                loc = TTTCore.locale.getLocalizable(
                        r.getLifecycleStage() == Stage.PREPARING
                                ? "info.global.round.status.starting.time"
                                : "info.global.round.status.time.remaining"
                ).withPrefix(Color.INFO);

                for (Challenger ch : r.getChallengers()) {
                    Player pl = Bukkit.getPlayer(ch.getUniqueId());
                    loc.withReplacements(time.localizeFor(pl)).sendTo(pl);
                }
            }

            if (event.getRound().getLifecycleStage() == Stage.PLAYING) {
                // check if game is over
                boolean iLeft = false;
                boolean tLeft = false;
                for (Challenger ch : event.getRound().getChallengers()) {
                    if (!(tLeft && iLeft)) {
                        if (!ch.isSpectating()) {
                            if (MiscHelper.isTraitor(ch)) {
                                tLeft = true;
                            } else {
                                iLeft = true;
                            }
                        }
                    } else {
                        break;
                    }

                    // manage DNA Scanners every n seconds
                    if (ch.getMetadata().has(Role.DETECTIVE)
                            && ch.getRound().getTime() % TTTCore.config.SCANNER_CHARGE_TIME == 0) {
                        Player tracker = TTTCore.getPlugin().getServer().getPlayer(ch.getName());
                        if (ch.getMetadata().has("tracking")) {
                            Player killer = TTTCore.getPlugin().getServer()
                                    .getPlayer(ch.getMetadata().<UUID>get("tracking").get());
                            if (killer != null
                                    && TTTCore.mg.getChallenger(killer.getUniqueId()).isPresent()) {
                                tracker.setCompassTarget(killer.getLocation());
                            } else {
                                TTTCore.locale.getLocalizable("error.round.trackee-left")
                                        .withPrefix(Color.ERROR).sendTo(tracker);
                                ch.getMetadata().remove("tracking");
                                tracker.setCompassTarget(Bukkit.getWorlds().get(1).getSpawnLocation());
                            }
                        } else {
                            Random rand = new Random();
                            tracker.setCompassTarget(
                                    new Location(
                                            tracker.getWorld(),
                                            tracker.getLocation().getX() + rand.nextInt(10) - 5,
                                            tracker.getLocation().getY(),
                                            tracker.getLocation().getZ() + rand.nextInt(10) - 5
                                    )
                            );
                        }
                    }
                }
                if (!(tLeft && iLeft)) {
                    if (tLeft) {
                        event.getRound().getMetadata().set("t-victory", true);
                    }
                    event.getRound().getMetadata().set("ending", true); //TODO: temp fix
                    event.getRound().end();
                    return;
                }

                ScoreboardManager.getOrCreate(event.getRound());
            }
        }
    }

    @Subscribe
    public void onRoundEnd(RoundEndEvent event) {
        KarmaHelper.allocateKarma(event.getRound());
        KarmaHelper.saveKarma(event.getRound());

        if (event.getRound().getLifecycleStage() == Stage.PLAYING) {
            boolean tVic = event.getRound().getMetadata().has("t-victory");

            String color = (tVic ? Color.TRAITOR : Color.INNOCENT);
            TTTCore.locale.getLocalizable("info.global.round.event.end." + (tVic ? Role.TRAITOR : Role.INNOCENT))
                    .withPrefix(color)
                    .withReplacements(Color.ARENA + event.getRound().getArena().getName() + color).broadcast();
            TitleHelper.sendVictoryTitle(event.getRound(), tVic);
        }

        for (Entity ent : Bukkit.getWorld(event.getRound().getArena().getWorld()).getEntities()) {
            if (ent.getType() == EntityType.ARROW) {
                ent.remove();
            }
        }
        Optional<ScoreboardManager> sbMan = ScoreboardManager.get(event.getRound());
        if (sbMan.isPresent()) {
            sbMan.get().unregister();
        }
    }

    @Subscribe
    public void onStageChange(RoundChangeLifecycleStageEvent event) {
        if (event.getStageBefore() == Stage.PREPARING && event.getStageAfter() == Stage.WAITING) {
            ScoreboardManager.getOrCreate(event.getRound()).unregister();
            for (Challenger ch : event.getRound().getChallengers()) {
                Bukkit.getPlayer(ch.getUniqueId()).setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }

            for (Team team : event.getRound().getTeams()) {
                event.getRound().removeTeam(team);
            }
        }
    }

    @Subscribe
    public void onPlayerClickLobbySign(PlayerClickLobbySignEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayer());
        if (player.hasPermission("ttt.lobby.use")) {
            // lazy way of doing this, but it works
            new JoinCommand(player, new String[]{"join", event.getLobbySign().getArena().getId()}).handle();
        } else {
            TTTCore.locale.getLocalizable("error.perms.generic").withPrefix(Color.ERROR).sendTo(player);
        }
    }

}
