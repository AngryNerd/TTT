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
package net.caseif.ttt.managers.command.arena;

import static net.caseif.ttt.util.Constants.ERROR_COLOR;
import static net.caseif.ttt.util.Constants.INFO_COLOR;
import static net.caseif.ttt.util.MiscUtil.getMessage;

import net.caseif.ttt.Main;
import net.caseif.ttt.managers.command.SubcommandHandler;

import net.amigocraft.mglib.exception.NoSuchArenaException;
import org.bukkit.command.CommandSender;

public class RemoveArenaCommand extends SubcommandHandler {

	public RemoveArenaCommand(CommandSender sender, String[] args) {
		super(sender, args, "ttt.arena.remove");
	}

	@Override
	public void handle() {
		if (assertPermission()) {
			if (args.length > 1) {
				String name = args[1];
				try {
					Main.mg.deleteArena(name);
					sender.sendMessage(getMessage("info.personal.arena.remove.success", INFO_COLOR, name));
				}
				catch (NoSuchArenaException ex) {
					sender.sendMessage(getMessage("error.arena.dne", ERROR_COLOR));
				}
			}
		}
	}
}
