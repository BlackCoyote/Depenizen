package net.gnomeffinway.depenizen.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.aufdemrand.denizen.BukkitScriptEntryData;
import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.BracedCommand;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import net.gnomeffinway.depenizen.Depenizen;
import net.gnomeffinway.depenizen.objects.bungee.dServer;
import net.gnomeffinway.depenizen.support.bungee.packets.ClientPacketOutScript;

import java.util.List;

public class BungeeCommand extends BracedCommand {

    // <--[command]
    // @Name Bungee
    // @Syntax bungee [<server>/all] [<commands>]
    // @Group Depenizen
    // @Plugin Depenizen
    // @Required 2
    // @Stable untested
    // @Short Sends commands to other servers via BungeeCord.
    // @Author Morphan1

    // @Description
    // This command allows you to send multiple lines of script to your BungeeCord
    // server, which then relays it to the dServer you specified. Tags and definitions
    // will be parsed on the dServer, and the definitions in the current queue will
    // carry over.
    // Note that sending to ALL does not run the commands on the current server.

    // @Tags
    // None

    // @Usage
    // Use to send network-wide join messages.
    // - define name <player.name.display>
    // - bungee all {
    //   - announce "<yellow>%name% has joined the network."
    //   }

    // @Usage
    // Use to keep a player's inventory consistent across 2 creative servers.
    // - define player <player>
    // - define contents <player.inventory.list_contents>
    // - bungee creative2 {
    //   - if <def[player].has_played_before||false> {
    //     - inventory set d:<def[player].inventory> o:<def[contents]>
    //     }
    //   }

    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {

            if (!scriptEntry.hasObject("server")
                    && !scriptEntry.hasObject("all-servers")
                    && arg.matchesArgumentType(dServer.class)) {
                scriptEntry.addObject("server", arg.asType(dServer.class));
            }

            else if (!scriptEntry.hasObject("server")
                    && !scriptEntry.hasObject("all-servers")
                    && arg.matches("all")) {
                scriptEntry.addObject("all-servers", new Element(true));
            }

            else if (arg.matches("{")) {
                scriptEntry.addObject("braces", getBracedCommands(scriptEntry));
                break;
            }

        }

        if (!scriptEntry.hasObject("server") && !scriptEntry.hasObject("all-servers"))
            throw new InvalidArgumentsException("Must specify a valid server or 'ALL'.");

        if (!scriptEntry.hasObject("braces"))
            throw new InvalidArgumentsException("Must have braces.");

        scriptEntry.defaultObject("all-servers", new Element(false));
    }

    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        Element all = scriptEntry.getElement("all-servers");
        dServer server = scriptEntry.getdObject("server");
        List<ScriptEntry> bracedCommands = ((List<BracedData>) scriptEntry.getObject("braces")).get(0).value;

        dB.report(scriptEntry, getName(), (server != null ? server.debug() : "") + all.debug());

        if (Depenizen.getCurrentInstance().isSocketConnected()) {
            ClientPacketOutScript packet = new ClientPacketOutScript(all.asBoolean() ? "ALL" : server.getName(),
                    bracedCommands, scriptEntry.getResidingQueue().getAllDefinitions());
            Depenizen.getCurrentInstance().getSocketClient().send(packet);
        }
        else {
            dB.echoError("Server is not connected to a BungeeCord Socket.");
        }
    }
}