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
package net.caseif.ttt.command.handler.player;

import net.caseif.ttt.TTTCore;
import net.caseif.ttt.command.handler.CommandHandler;
import net.caseif.ttt.util.Constants;
import net.caseif.ttt.util.helper.event.DeathHelper;

import com.google.common.base.Optional;
import net.caseif.flint.challenger.Challenger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SlayCommand extends CommandHandler {

    public SlayCommand(CommandSender sender, String[] args) {
        super(sender, args);
    }

    @Override
    public void handle() {
        String name = args[1];

        @SuppressWarnings("deprecation")
        Player pl = Bukkit.getPlayer(name);
        if (pl == null) {
            TTTCore.locale.getLocalizable("error.round.player-offline").withPrefix(Constants.Color.ERROR)
                    .sendTo(sender);
            return;
        }

        Optional<Challenger> ch = TTTCore.mg.getChallenger(pl.getUniqueId());
        if (!ch.isPresent()) {
            TTTCore.locale.getLocalizable("error.round.no-such-player").withPrefix(Constants.Color.ERROR)
                    .sendTo(sender);
            return;
        }
        new DeathHelper(pl).handleEvent();
        TTTCore.locale.getLocalizable("info.personal.slay").withPrefix(Constants.Color.ERROR).sendTo(pl);
        TTTCore.locale.getLocalizable("info.personal.slay.other").withPrefix(Constants.Color.INFO)
                .withReplacements(ch.get().getName()).sendTo(sender);
    }

}
