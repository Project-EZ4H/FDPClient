package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent;
import net.ccbluex.liquidbounce.file.configs.FriendsConfig.java 
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.input.Mouse;

public class MCF
extends Module {
    public boolean onFriend = true;

    public MCF() {
        super("MCF", Category.Player);
    }

    @Override
    public void onDisable() {
        this.onFriend = true;
        super.onDisable();
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @EventTarget
    public void onUpdate(EventPreMotionUpdate event) {
        if (Mouse.isButtonDown(2)) {
            if (MCF.mc.pointedEntity != null) {
                if (MCF.mc.pointedEntity instanceof EntityLivingBase && this.onFriend) {
                    this.onFriend = false;
                    if (Main.instance.friendManager.getFriends().stream().anyMatch(paramFriend -> paramFriend.getName().equals(MCF.mc.pointedEntity.getName()))) {
                        Main.instance.friendManager.getFriends().remove(Main.instance.friendManager.getFriend(MCF.mc.pointedEntity.getName()));
                        ChatUtils.addChatMessage("Removed '" + MCF.mc.pointedEntity.getName() + "' as Friend!");
                    } else {
                        Main.instance.friendManager.addFriend(new Friend("", MCF.mc.pointedEntity.getName(), false));
                        ChatUtils.addChatMessage("Added " + MCF.mc.pointedEntity.getName() + " as Friend!");
                    }
                }
            }
        }
        if (!Mouse.isButtonDown(2)) {
            this.onFriend = true;
        }
    }
}
