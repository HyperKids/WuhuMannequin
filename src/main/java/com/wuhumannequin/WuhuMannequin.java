package com.wuhumannequin;

import com.wuhumannequin.command.MannequinCommand;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public class WuhuMannequin extends JavaPlugin {

    private static WuhuMannequin instance;
    private MannequinCommand mannequinCommand;

    @Override
    public void onEnable() {
        instance = this;
        mannequinCommand = new MannequinCommand(this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register("mannequin", "WuhuMannequin debug commands", mannequinCommand);
        });

        getLogger().info("WuhuMannequin enabled.");
    }

    @Override
    public void onDisable() {
        if (mannequinCommand != null) mannequinCommand.cleanupAll();
        getLogger().info("WuhuMannequin disabled.");
    }

    public static WuhuMannequin getInstance() {
        return instance;
    }
}
