package amgg.mc_model_dumper;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.List;

public class AMGGCommandLeaf extends AMGGCommandBase {
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

    @FunctionalInterface
    public static interface CommandExecute {
        void execute(MinecraftServer server, ICommandSender sender, List<String> args) throws CommandException;
    }

    @FunctionalInterface
    public static interface CommandTabComplete {
        List<String> getTabCompletions2(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, int argsIdx);
    }
}
