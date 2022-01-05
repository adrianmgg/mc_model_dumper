package amgg.mc_model_dumper;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.command.ICommand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import org.intellij.lang.annotations.PrintFormat;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
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
                        if(!RenderLivingBase.class.isAssignableFrom(renderer.getClass())) throw new CommandException("associated renderer doesn't derive from RenderLivingBase");
                        RenderLivingBase<?> rendererRLB = (RenderLivingBase<?>)renderer;
                        AMGGStringBuilder sb = new AMGGStringBuilder()
                            .setIndent("  ")
                            .addFormat("renderer for %s:", clzname)
                            .pushIndent()
                                .add("\nboxes:")
                                .pushIndent()
                                .forEach(rendererRLB.getMainModel().boxList,
                                    (sb2, box) -> sb2
                                        .add("\n")
                                        .addFormatIf(box.boxName != null, "\"%s\"", box.boxName)
                                        .bracketCurly()
                                        .forEachDelimited(box.cubeList, " ",
                                            (sb3, cube) -> sb3
                                                .bracketSquare()
                                                .addFormatIf(cube.boxName != null, "\"%s\" ", cube.boxName)
                                                .addFormat("(%.2f,%.2f,%.2f)\u2192(%.2f,%.2f,%.2f)", cube.posX1, cube.posY1, cube.posZ1, cube.posX2, cube.posY2, cube.posZ2)
                                                .closeBracket()
                                        )
                                        .closeBracket()
                                )
                                .popIndent()
                            .popIndent()
                            ;
                        sender.sendMessage(new TextComponentString(sb.toString()));
                    },
                    (server, sender, args, targetPos, argsIdx) ->
                        Minecraft.getMinecraft().getRenderManager().entityRenderMap.keySet().stream()
                            .map(Class::getName)
                            .filter(s -> s.startsWith(args[argsIdx]))
                            .collect(Collectors.toList())
                )
            )
        ;

    public static class AMGGStringBuilder {
        //        private static final Formatter formatter = new Formatter(Locale.ENGLISH);
//        private final List<String> chunks = new ArrayList<>();
        private final Stack<String> indentStack = new Stack<>();
        private @Nullable String currentIndent = null;
        private String defaultIndent = "\t";
        private final Stack<String> bracketStack = new Stack<>();
        private final StringBuilder builder = new StringBuilder();

        public AMGGStringBuilder() {}

        public AMGGStringBuilder add(@Nullable String s) { _add(String.valueOf(s)); return this; }
        public AMGGStringBuilder add(@Nullable Object o) { _add(String.valueOf(o)); return this; }
        public AMGGStringBuilder addIf(boolean condition, String s) { if(condition) { add(s); } return this; }
        public AMGGStringBuilder addIf(boolean condition, Object o) { if(condition) { add(o); } return this; }
        public AMGGStringBuilder addFormat(@PrintFormat String format, Object... args) {
            // TODO this will use default locale - do we actually want that?
            _add(String.format(format, args));
            return this;
        }
        public AMGGStringBuilder addFormatIf(boolean condition, @PrintFormat String format, Object... args) {
            if(condition) return addFormat(format, args);
            return this;
        }

        public <T> AMGGStringBuilder forEach(Iterable<T> it, BiConsumer<AMGGStringBuilder, T> func) {
            it.forEach(t -> func.accept(this, t));
            return this;
        }
        public <T> AMGGStringBuilder forEachDelimited(Iterable<T> it, String delimiter, BiConsumer<AMGGStringBuilder, T> func) {
            boolean first = true;
            for(T t : it) {
                if(first) first = false;
                else add(delimiter);
                func.accept(this, t);
            }
            return this;
        }

        // ==== indent handling stuff ====
        private void updateIndent() {
            if(indentStack.empty()) currentIndent = null;
            else currentIndent = String.join("", indentStack);
        }
        private void _add(String s) {
            if(!s.contains("\n")) builder.append(s);
            else { // "\n" in s
                int endidx = 0, startidx = 0;
                while((endidx = s.indexOf('\n', endidx)) >= 0) {
                    builder.append(s, startidx, endidx + 1);
                    if(currentIndent != null) builder.append(currentIndent);
                    startidx = endidx = endidx + 1;
                }
                builder.append(s, startidx, s.length());
            }
        }
        /**
         * set default indent for all later calls to {@link #pushIndent()}
         * @param indent default indent level
         * @see #pushIndent
         */
        public AMGGStringBuilder setIndent(String indent) { defaultIndent = indent; return this; }
        /**
         * push a new custom indent string to the indent stack.
         * @param indent indent string
         * @see AMGGStringBuilder#pushIndent()
         */
        public AMGGStringBuilder pushIndent(String indent) { indentStack.push(indent); updateIndent(); return this; }
        /**
         * push default indent (set by {@link #setIndent(String)} to indent stack
         * @see #setIndent
         * @see #pushIndent(String)
         */
        public AMGGStringBuilder pushIndent() { indentStack.push(defaultIndent); updateIndent(); return this; }
        /**
         * pop one level of indent from the indent stack
         * @see #clearIndent
         * @throws EmptyStackException if indent stack is already empty
         */
        public AMGGStringBuilder popIndent() throws EmptyStackException { indentStack.pop(); updateIndent(); return this; }
        /**
         * clear indent stack
         * @see #popIndent
         */
        public AMGGStringBuilder clearIndent() { indentStack.clear(); updateIndent(); return this; }

        // ==== bracketing stuff ====

        /**
         * immediately add left bracket, and push right bracket to bracket stack to be added later
         * @param left left bracket
         * @param right right bracket
         * @see #closeBracket
         */
        public AMGGStringBuilder bracket(String left, String right) {
            _add(left);
            bracketStack.push(right);
            return this;
        }
        /** {@link #bracket} helper for parentheses/round brackets */
        public AMGGStringBuilder bracketRound() { bracket("(", ")"); return this; }
        /** {@link #bracket} helper for chevrons/angle brackets */
        public AMGGStringBuilder bracketSquare() { bracket("[", "]"); return this; }
        /** {@link #bracket} helper for square brackets */
        public AMGGStringBuilder bracketCurly() { bracket("{", "}"); return this; }
        /** {@link #bracket} helper for braces/curly brackets */
        public AMGGStringBuilder bracketAngle() { bracket("<", ">"); return this; }
        /**
         * pop a bracket from the bracket stack and add it
         * @throws EmptyStackException if bracket stack is already empty
         * @see #closeAllBrackets
         */
        public AMGGStringBuilder closeBracket() throws EmptyStackException { _add(bracketStack.pop()); return this; }
        /**
         * pop and add all remaining brackets from the bracket stack
         * @see #closeBracket
         */
        public AMGGStringBuilder closeAllBrackets() {
            while(!bracketStack.empty()) _add(bracketStack.pop());
            return this;
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }

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

        public AMGGCommandParent(String name, @Nullable String[] aliases) {
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
        public AMGGCommandLeaf(String name, @Nullable String[] aliases, CommandExecute executeFunc, CommandTabComplete completeFunc) {
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
