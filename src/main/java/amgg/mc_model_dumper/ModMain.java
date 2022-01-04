package amgg.mc_model_dumper;

import net.minecraft.command.ICommand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

// command stuff
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.*;

@Mod(
    modid = Properties.MODID,
    name = Properties.NAME,
    version = Properties.VERSION,
    acceptableRemoteVersions = "*"
)
public class ModMain {
    private static Logger logger;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(dumpModelCommand);
    }

    public static final AMGGCommandBase dumpModelCommand =
        new AMGGCommandParent("dumpmodel")
            .addSubcommand(
                new AMGGCommandParent("aaa")
                    .addSubcommand(new AMGGCommandLeaf("a01"))
                    .addSubcommand(new AMGGCommandLeaf("a02"))
                    .addSubcommand(new AMGGCommandLeaf("a03"))
            )
            .addSubcommand(
                new AMGGCommandParent("bbb")
                    .addSubcommand(new AMGGCommandLeaf("b01"))
                    .addSubcommand(new AMGGCommandLeaf("b02"))
                    .addSubcommand(new AMGGCommandLeaf("b03"))
            )
            .addSubcommand(
                new AMGGCommandParent("ccc")
                    .addSubcommand(new AMGGCommandLeaf("c01"))
                    .addSubcommand(new AMGGCommandLeaf("c02"))
                    .addSubcommand(new AMGGCommandLeaf("c03"))
            )
        ;
 
    public static abstract class AMGGCommandBase implements ICommand {
        public final List<String> aliases;
        public final String name;
        AMGGCommandBase(String name, @Nullable String[] aliases) {
            this.name = name;
            // create List containing [name, ...aliases] and freeze it
            List<String> aliases_ = new ArrayList<>();
            aliases_.add(name);
            if(aliases != null) Collections.addAll(aliases_, aliases);
            this.aliases = Collections.unmodifiableList(aliases_);
        }
        AMGGCommandBase(String name) { this(name, null); }
        public String getName() { return name; }
        public String getUsage(ICommandSender sender) { return "usage for " + name + " (TODO)"; }
        public List<String> getAliases() { return aliases; }
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) { return true; }
        public boolean isUsernameIndex(String[] args, int index) { return false; }
        public int compareTo(ICommand other) {
            return name.compareTo(other.getName());
        }

        abstract @Nullable List<String> getTabCompletions2(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, int argsIdx);
        public final List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
            List<String> ret = getTabCompletions2(server, sender, args, targetPos, 0);
            if(ret == null) return new ArrayList<>();  // TODO keep one frozen empty list to return every time instead?
            else return ret;
        }
    }

    public static class AMGGCommandParent extends AMGGCommandBase {
        protected Map<String, AMGGCommandBase> subcommands = new HashMap<>();

        public AMGGCommandParent(String name, String[] aliases) {
            super(name, aliases);
        }
        AMGGCommandParent(String name) { this(name, null); }

        public AMGGCommandParent addSubcommand(AMGGCommandBase command) {
            for(String alias : command.getAliases()) subcommands.put(alias, command);
            return this;
        }

        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        }
        public List<String> getTabCompletions2(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, int argsIdx) {

            if(argsIdx >= args.length) { // TODO better check here
                logger.error("args.length >= argsIdx");
                return null;
            }
            if(argsIdx + 1 < args.length) {
                AMGGCommandBase cmd = subcommands.get(args[argsIdx]);
                if(cmd == null) return null;
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

    public static class AMGGCommandLeaf extends AMGGCommandBase {
        public AMGGCommandLeaf(String name, String[] aliases) {
            super(name, aliases);
        }
        AMGGCommandLeaf(String name) { this(name, null); }

        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        }
        public List<String> getTabCompletions2(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, int argsIdx) {
            return null;
        }
    }
}
