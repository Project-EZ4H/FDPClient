package net.ccbluex.liquidbounce.features.command.commands

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.ccbluex.liquidbounce.features.command.CommandManager;
import net.ccbluex.liquidbounce.features.command.commands;
import net.ccbluex.liquidbounce.utils.ClientUtils.java;

@Com(names = { "tp", "teleport" })
public class Teleport extends Command
{
    @Override
    public void runCommand(final String[] args) {
        final double X = Double.parseDouble(args[1]);
        final double Y = Double.parseDouble(args[2]);
        final double Z = Double.parseDouble(args[3]);
        final Minecraft mc = Minecraft.getMinecraft();
        mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(X, Y, Z, false));
        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(X, Y, Z, false));
        mc.thePlayer.setPosition(X, Y, Z);
        mc.thePlayer.setPositionAndUpdate(X, Y, Z);
        ClientUtils.sendMessage("Teleported to " + X + " " + Y + " " + Z);
    }
    
    @Override
    public String getHelp() {
        return "teleport";
    }
}
