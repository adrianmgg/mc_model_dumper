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
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
//                        sender.sendMessage(new TextComponentString(new AMGGStringBuilder()
//                            .setIndent("  ")
//                            .addFormat("renderer for %s:", clzname)
//                            .pushIndent()
//                                .add("\nboxes:")
//                                .pushIndent()
//                                .forEach(rendererRLB.getMainModel().boxList,
//                                    (sb2, box) -> sb2
//                                        .add("\n")
//                                        .addFormatIf(box.boxName != null, "\"%s\"", box.boxName)
//                                        .bracketCurly()
//                                        .forEachDelimited(box.cubeList, " ",
//                                            (sb3, cube) -> sb3
//                                                .bracketSquare()
//                                                .addFormatIf(cube.boxName != null, "\"%s\" ", cube.boxName)
//                                                .addFormat("(%.2f,%.2f,%.2f)\u2192(%.2f,%.2f,%.2f)", cube.posX1, cube.posY1, cube.posZ1, cube.posX2, cube.posY2, cube.posZ2)
//                                                .closeBracket()
//                                        )
//                                        .closeBracket()
//                                )
//                                .popIndent()
//                            .popIndent()
//                            .finish()
//                        ));
                        String objText = new OBJBuilder()
                            .addFormat("# generated by %s version %s", Properties.NAME, Properties.VERSION)
                            .forEach(rendererRLB.getMainModel().boxList, (builder1, box) -> {
                                if(box.isHidden) { logger.info("skipping hidden cube"); return; }
                                builder1.forEach(box.cubeList, (builder2, cube) -> {
                                    builder2
                                        .pushMatrix(new Matrix4f()
                                            .translate(new Vector3f(box.rotationPointX, box.rotationPointY, box.rotationPointZ))
                                            .rotate(box.rotateAngleX, new Vector3f(1, 0, 0))
                                            .rotate(box.rotateAngleY, new Vector3f(0, 1, 0))
                                            .rotate(box.rotateAngleZ, new Vector3f(0, 0, 1))
//                                            .translate(new Vector3f(-box.rotationPointX, -box.rotationPointY, -box.rotationPointZ))
                                            .translate(new Vector3f(box.offsetX, box.offsetY, box.offsetZ))
                                        )
                                            .vert(cube.posX1, cube.posY1, cube.posZ1)
                                            .vert(cube.posX2, cube.posY1, cube.posZ1)
                                            .vert(cube.posX2, cube.posY2, cube.posZ1)
                                            .vert(cube.posX1, cube.posY2, cube.posZ1)
                                            .vert(cube.posX1, cube.posY1, cube.posZ2)
                                            .vert(cube.posX2, cube.posY1, cube.posZ2)
                                            .vert(cube.posX2, cube.posY2, cube.posZ2)
                                            .vert(cube.posX1, cube.posY2, cube.posZ2)
                                            .add("\nf -8 -7 -6 -5")
                                            .add("\nf -4 -3 -2 -1")
                                            .add("\nf -8 -5 -1 -4")
                                            .add("\nf -7 -6 -2 -3")
                                            .add("\nf -6 -5 -1 -2")
                                            .add("\nf -8 -7 -3 -4")
                                        .popMatrix()
                                        ;
                                });
                            })
                            .finish();
                        try {
                            Files.write(
                                Paths.get(String.format("./%s.%s.obj", clzname, (new SimpleDateFormat("yyyyMMddhhmmss").format(new Date())) )),
                                objText.getBytes(StandardCharsets.UTF_8)
                            );
                        } catch (IOException e) {
                            throw new CommandException("error saving obj", e);
                        }
                    },
                    (server, sender, args, targetPos, argsIdx) ->
                        Minecraft.getMinecraft().getRenderManager().entityRenderMap.keySet().stream()
                            .map(Class::getName)
                            .filter(s -> s.startsWith(args[argsIdx]))
                            .collect(Collectors.toList())
                )
            )
        ;

    public static abstract class AMGGStringBuilderBase<T extends AMGGStringBuilderBase<T>> {
        //        private static final Formatter formatter = new Formatter(Locale.ENGLISH);
//        private final List<String> chunks = new ArrayList<>();
        protected final Stack<String> indentStack = new Stack<>();
        protected @Nullable String currentIndent = null;
        protected String defaultIndent = "\t";
        protected final Stack<String> bracketStack = new Stack<>();
        protected final StringBuilder builder = new StringBuilder();

        protected abstract T self();

        public T add(@Nullable String s) { _add(String.valueOf(s)); return self(); }
        public T add(@Nullable Object o) { _add(String.valueOf(o)); return self(); }
        public T addIf(boolean condition, String s) { if(condition) { add(s); } return self(); }
        public T addIf(boolean condition, Object o) { if(condition) { add(o); } return self(); }
        public T addFormat(@PrintFormat String format, Object... args) {
            // TODO this will use default locale - do we actually want that?
            _add(String.format(format, args));
            return self();
        }
        public T addFormatIf(boolean condition, @PrintFormat String format, Object... args) {
            if(condition) return addFormat(format, args);
            return self();
        }

        public <T2> T forEach(Iterable<T2> it, BiConsumer<T, T2> func) {
            it.forEach(t -> func.accept(self(), t));
            return self();
        }
        public <T2> T forEachDelimited(Iterable<T2> it, String delimiter, BiConsumer<T, T2> func) {
            boolean first = true;
            for(T2 t : it) {
                if(first) first = false;
                else add(delimiter);
                func.accept(self(), t);
            }
            return self();
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
        public T setIndent(String indent) { defaultIndent = indent; return self(); }
        /**
         * push a new custom indent string to the indent stack.
         * @param indent indent string
         * @see #pushIndent()
         */
        public T pushIndent(String indent) { indentStack.push(indent); updateIndent(); return self(); }
        /**
         * push default indent (set by {@link #setIndent(String)} to indent stack
         * @see #setIndent
         * @see #pushIndent(String)
         */
        public T pushIndent() { indentStack.push(defaultIndent); updateIndent(); return self(); }
        /**
         * pop one level of indent from the indent stack
         * @see #clearIndent
         * @throws EmptyStackException if indent stack is already empty
         */
        public T popIndent() throws EmptyStackException { indentStack.pop(); updateIndent(); return self(); }
        /**
         * clear indent stack
         * @see #popIndent
         */
        public T clearIndent() { indentStack.clear(); updateIndent(); return self(); }

        // ==== bracketing stuff ====

        /**
         * immediately add left bracket, and push right bracket to bracket stack to be added later
         * @param left left bracket
         * @param right right bracket
         * @see #closeBracket
         */
        public T bracket(String left, String right) {
            _add(left);
            bracketStack.push(right);
            return self();
        }
        /** {@link #bracket} helper for parentheses/round brackets */
        public T bracketRound() { return bracket("(", ")"); }
        /** {@link #bracket} helper for chevrons/angle brackets */
        public T bracketSquare() { return bracket("[", "]"); }
        /** {@link #bracket} helper for square brackets */
        public T bracketCurly() { return bracket("{", "}"); }
        /** {@link #bracket} helper for braces/curly brackets */
        public T bracketAngle() { return bracket("<", ">"); }
        /**
         * pop a bracket from the bracket stack and add it
         * @throws EmptyStackException if bracket stack is already empty
         * @see #closeAllBrackets
         */
        public T closeBracket() throws EmptyStackException { _add(bracketStack.pop()); return self(); }
        /**
         * pop and add all remaining brackets from the bracket stack
         * @see #closeBracket
         */
        public T closeAllBrackets() {
            while(!bracketStack.empty()) _add(bracketStack.pop());
            return self();
        }

        @Override
        public String toString() {
            return builder.toString();
        }

        public String finish() {
            closeAllBrackets();
            clearIndent();
            return toString();
        }
    }
    public static class AMGGStringBuilder extends AMGGStringBuilderBase<AMGGStringBuilder> {
        @Override
        protected AMGGStringBuilder self() { return this; }
    }

    public static class OBJBuilder extends AMGGStringBuilderBase<OBJBuilder> {
        protected Stack<Matrix4f> matrixStack = new Stack<>();

        public OBJBuilder() {
            super();
            matrixStack.add(new Matrix4f());
        }

        @Override
        protected OBJBuilder self() { return this; }
        OBJBuilder vert(float x, float y, float z) {
            Vector4f v1 = new Vector4f(x, y, z, 1f);
            Vector4f v2 = new Vector4f();
            Matrix4f.transform(matrixStack.peek(), v1, v2);
            addFormat("\nv %f %f %f %f", v2.x, v2.y, v2.z, v2.w);
            return this;
        }

        OBJBuilder vert_uv(double u, double v) { addFormat("\nvt %f %f", u, v); return this; }

        // ==== matrix stack stuff ====
        public OBJBuilder pushMatrix(Matrix4f mat) {
            Matrix4f newmat = new Matrix4f();
            Matrix4f.mul(matrixStack.peek(), mat, newmat);
            matrixStack.push(newmat);
            return this;
        }
        public OBJBuilder pushRotate(float angle, float axisx, float axisy, float axisz) {
            return pushMatrix(new Matrix4f().rotate(angle, new Vector3f(axisx, axisy, axisz)));
        }
        public OBJBuilder pushTranslate(float x, float y, float z) {
            return pushMatrix(new Matrix4f().translate(new Vector3f(x, y, z)));
        }
        public OBJBuilder popMatrix() throws EmptyStackException {
            matrixStack.pop();
            return this;
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
