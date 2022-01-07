package amgg.mc_model_dumper;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// TODO hide leaf/parent distinction from end user (use 2 different add commands instead?)
//      if do then also need to have a way to make commands consisting only of a single leaf
//          (maybe a static class for the initial entry with 2 functions similar to above idea

public abstract class AMGGCommandBase implements ICommand {
    public final List<String> aliases;
    public final String name;

    AMGGCommandBase(String name, @Nullable String[] aliases) {
        this.name = name;
        // create List containing [name, ...aliases] and freeze it
        List<String> aliases_ = new ArrayList<>();
        aliases_.add(name);
        if (aliases != null) Collections.addAll(aliases_, aliases);
        this.aliases = Collections.unmodifiableList(aliases_);
    }

    AMGGCommandBase(String name) {
        this(name, null);
    }

    public String getName() {
        return name;
    }

    public String getUsage(ICommandSender sender) {
        return "usage for " + name + " (TODO)";
    }

    public List<String> getAliases() {
        return aliases;
    }

    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    public int compareTo(ICommand other) {
        return name.compareTo(other.getName());
    }

    abstract @Nullable
    List<String> getTabCompletions2(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos, int argsIdx);

    public final List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        List<String> ret = getTabCompletions2(server, sender, args, targetPos, 0);
        return (ret == null) ? Collections.emptyList() : ret;
    }

    abstract void execute2(MinecraftServer server, ICommandSender sender, List<String> args) throws CommandException;

    public final void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        execute2(server, sender, Arrays.asList(args));
    }
}
