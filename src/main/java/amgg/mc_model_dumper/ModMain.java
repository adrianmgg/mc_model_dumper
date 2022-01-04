package amgg.mc_model_dumper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.command.ICommand;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
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
import java.util.stream.Collectors;

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
                new AMGGCommandLeaf("entity",
                    (server, sender, args) -> {
                        logger.info("{}", args);
                        if(args.size() < 1) throw new CommandException("argument [class] not specified");
                        String clzname = args.get(0);
                        Class<?> clz;
                        try {
                            clz = Class.forName(clzname);
                        } catch (ClassNotFoundException e) {
                            throw new CommandException("can't find class for name " + clzname);
                        }
                        if(!net.minecraft.entity.Entity.class.isAssignableFrom(clz)) {
                            throw new CommandException("class " + clzname + " not assignable to net.minecraft.entity.Entity");
                        }
                        net.minecraft.client.renderer.entity.Render<? extends net.minecraft.entity.Entity> renderer = Minecraft.getMinecraft().getRenderManager().entityRenderMap.get(clz);
                        if(renderer == null) throw new CommandException("no renderer found for class " + clzname);
                    },
                    (server, sender, args, targetPos, argsIdx) ->
                        Minecraft.getMinecraft().getRenderManager().entityRenderMap.keySet().stream()
                            .map(clz->clz.getName())
                            .filter(s -> s.startsWith(args[argsIdx]))
                            .collect(Collectors.toList())
                )
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

        abstract void execute2(MinecraftServer server, ICommandSender sender, List<String> args) throws CommandException;
        public final void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            execute2(server, sender, Arrays.asList(args));
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

        public void execute2(MinecraftServer server, ICommandSender sender, List<String> args) throws CommandException {
            if(args.size() == 0) throw new CommandException("no subcommand specified for command " + name);
            String cmdName = args.get(0);
            AMGGCommandBase cmd = subcommands.get(cmdName);
            if(cmd == null) throw new CommandException("unknown subcommand " + cmdName + " for command " + name);
            cmd.execute2(server, sender, args.subList(1, args.size()));
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

    @FunctionalInterface
    public static interface CommandExecute {
        void execute(MinecraftServer server, ICommandSender sender, List<String> args) throws CommandException;
    }
    @FunctionalInterface
    public static interface CommandTabComplete {
        List<String> getTabCompletions2(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, int argsIdx);
    }

    public static class AMGGCommandLeaf extends AMGGCommandBase {
        private final CommandExecute executeFunc;
        private final CommandTabComplete completeFunc;
        public AMGGCommandLeaf(String name, String[] aliases, CommandExecute executeFunc, CommandTabComplete completeFunc) {
            super(name, aliases);
            this.executeFunc = executeFunc;
            this.completeFunc = completeFunc;
        }
        AMGGCommandLeaf(String name, CommandExecute executeFunc, CommandTabComplete completeFunc) {
            this(name, null, executeFunc, completeFunc);
        }

        public void execute2(MinecraftServer server, ICommandSender sender, List<String> args) throws CommandException {
            executeFunc.execute(server, sender, args);
        }
        public List<String> getTabCompletions2(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, int argsIdx) {
            return completeFunc.getTabCompletions2(server, sender, args, targetPos, argsIdx);
        }
    }
}
