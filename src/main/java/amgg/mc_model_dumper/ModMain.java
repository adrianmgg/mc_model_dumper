package amgg.mc_model_dumper;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
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
        event.registerServerCommand(new AMGGCommandParent("dumpmodel")
            .addSubcommand(
                new AMGGCommandLeaf("entity_all",
                    (server, sender, args) -> {
                        Minecraft.getMinecraft().getRenderManager().entityRenderMap.forEach((entityClass, renderer) -> {
                            try {
                                entityRendererToObj(sender, renderer)
                                    .finishIntoFile(standardExportedFilePath(entityClass.getName(), ".obj"), StandardCharsets.UTF_8);
                                // TODO chat status message
                            } catch (IOException | CommandException e) {
                                logger.warn(e);
                                // TODO! chat status message
                            }
                        });
                    },
                    (server, sender, args, targetPos, argsIdx) -> null
                )
            )
            .addSubcommand(
                new AMGGCommandLeaf("entity",
                    (server, sender, args) -> {
                        if(args.size() < 1) throw new CommandException("argument [class] not specified");
                        String clzname = args.get(0);
                        Class<?> clz;
                        try {
                            clz = Class.forName(clzname);
                        } catch (ClassNotFoundException e) {
                            throw new CommandException("can't find class for name " + clzname);
                        }
                        if(!Entity.class.isAssignableFrom(clz)) {
                            throw new CommandException("class " + clzname + " not assignable to net.minecraft.entity.Entity");
                        }
                        net.minecraft.client.renderer.entity.Render<? extends Entity> renderer = Minecraft.getMinecraft().getRenderManager().entityRenderMap.get(clz);
                        if(renderer == null) throw new CommandException("no renderer found for class " + clzname);
                        try {
                            entityRendererToObj(sender, renderer)
                                .finishIntoFile(standardExportedFilePath(clzname, ".obj"), StandardCharsets.UTF_8);
                            sender.sendMessage(new TextComponentString(new AMGGStringBuilder()
                                .addFormat("saved model for %s", clzname)
                                .finish()
                            ));
                        } catch (IOException e) {
                            throw new CommandException("error saving obj", e);
                        }
                    },
                    // TODO tab completion should return grouped up to first package level where names differ,
                    //      rather than just doing all matching prefix
                    //      for example, with the classes [foo.bar.A, foo.bar.B, foo.baz.C, foo.baz.D, foo.qux.E],
                    //      tab completion should give the following responses
                    //          ""          "foo.bar" "foo.baz" "foo.qux"
                    //          "foo."      "foo.bar" "foo.baz" "foo.qux"
                    //          "foo.b"     "foo.bar" "foo.baz"
                    //          "foo.bar"   "foo.bar.A" "foo.bar.B"
                    //          "foo.q"     "foo.qux.E"
                    (server, sender, args, targetPos, argsIdx) -> Minecraft.getMinecraft().getRenderManager().entityRenderMap.keySet().stream()
                        .map(Class::getName)
                        .filter(s -> s.startsWith(args[argsIdx]))
                        .collect(Collectors.toList())
                )
            )
        );
    }

    // TODO probably doesnt need to be its own function? idk
    private static OBJBuilder entityRendererToObj(ICommandSender sender, net.minecraft.client.renderer.entity.Render<? extends Entity> renderer) throws CommandException {
        if(!RenderLivingBase.class.isAssignableFrom(renderer.getClass())) throw new CommandException("associated renderer doesn't derive from RenderLivingBase");
        return new OBJBuilder().addModel((RenderLivingBase<?>) renderer);
    }

    /** helper to standardize naming convention & location of exported files */
    private static Path standardExportedFilePath(String name, String suffix) {
        return Paths.get(String.format(
            "./%s.%s%s",
            name,
            new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()),
            suffix
        ));
    }

}
