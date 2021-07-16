package net.ccbluex.liquidbounce.features.command.commands

import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.ccbluex.liquidbounce.features.command.CommandManager;
import net.ccbluex.liquidbounce.features.command.commands;
import net.ccbluex.liquidbounce.utils.render.RenderUtils.java 

@Com(names = { "tp", "teleportplayer" })
public class TeleportPlayer extends Command
{
	
	Minecraft mc = Minecraft.getMinecraft();
	
    @Override
    public String getHelp() {
        return "teleportplayer";
    }
    
    @Override
    public void runCommand(final String[] args) {
        String player = "";
        if (args.length > 1) {
            player = args[1];
        }
        for (final Entity e : ClientUtils.loadedEntityList()) {
            if (e.getName().equalsIgnoreCase(player)) {
                mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(e.posX, e.posY, e.posZ, false));
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(e.posX, e.posY, e.posZ, false));
                mc.thePlayer.setPosition(e.posX, e.posY, e.posZ);
                mc.thePlayer.setPositionAndUpdate(e.posX, e.posY, e.posZ);
                ClientUtils.sendMessage("Teleported to " + e.getName() + " " + e.posX + " " + e.posY + " " + e.posZ);
            }
        }
    }
}
