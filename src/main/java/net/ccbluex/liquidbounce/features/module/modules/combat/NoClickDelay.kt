/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/Project-EZ4H/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory

public class NoClickDelay extends Module
{
    public NoClickDelay() {
        super("NoClickDelay", 0, Category.PLAYER);
    }
    
    @Override
    public void onUpdate() {
        if (this.getState()) {
            Wrapper.mc.rightClickDelayTimer = 0;
        }
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
    }
    
    @Override
    public void onDisable() {
        Wrapper.mc.rightClickDelayTimer = 6;
        super.onDisable();
    }
}
