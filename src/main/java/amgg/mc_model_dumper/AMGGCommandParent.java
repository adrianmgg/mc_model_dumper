package amgg.mc_model_dumper;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AMGGCommandParent extends AMGGCommandBase {
    protected Map<String, AMGGCommandBase> subcommands = new HashMap<>();

    public AMGGCommandParent(String name, @Nullable String[] aliases) {
        super(name, aliases);
    }

    AMGGCommandParent(String name) {
        this(name, null);
    }

    public AMGGCommandParent addSubcommand(AMGGCommandBase command) {
        for (String alias : command.getAliases()) subcommands.put(alias, command);
        return this;
    }

    public void execute2(MinecraftServer server, ICommandSender sender, List<String> args) throws CommandException {
        if (args.size() == 0) throw new CommandException("no subcommand specified for command " + name);
        String cmdName = args.get(0);
        AMGGCommandBase cmd = subcommands.get(cmdName);
        if (cmd == null) throw new CommandException("unknown subcommand " + cmdName + " for command " + name);
        cmd.execute2(server, sender, args.subList(1, args.size()));
    }

    public List<String> getTabCompletions2(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, int argsIdx) {

        if (argsIdx >= args.length) { // TODO better check here
//            ModMain.logger.error("args.length >= argsIdx");
            return null;
        }
        if (argsIdx + 1 < args.length) {
            AMGGCommandBase cmd = subcommands.get(args[argsIdx]);
            if (cmd == null) return null;
            else return cmd.getTabCompletions2(server, sender, args, targetPos, argsIdx + 1);
        } else {
            List<String> completions = new ArrayList<>();
            String arg = args[argsIdx];
            for (Map.Entry<String, AMGGCommandBase> entry : subcommands.entrySet()) {
                if (entry.getKey().startsWith(arg)) completions.add(entry.getKey());
            }
            return completions;
        }
    }
}
