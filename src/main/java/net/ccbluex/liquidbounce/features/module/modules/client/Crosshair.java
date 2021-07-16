/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/Project-EZ4H/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import java.awt.Color;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovementInput;

public class Crosshair
extends Module {
    private final Setting dynamic;
    private final Setting colorRed = new Setting("Red", this, 249.0, 0.0, 255.0, true);
    private final Setting colorGreen;
    private final Setting colorBlue;
    private final Setting width;
    private final Setting gap;
    private final Setting length;
    private final Setting dynamicGap;

    public Crosshair() {
        super("Crosshair", Category.Visuals);
        Main.instance.setmgr.rSetting(this.colorRed);
        this.colorGreen = new Setting("Green", this, 255.0, 0.0, 255.0, true);
        Main.instance.setmgr.rSetting(this.colorGreen);
        this.colorBlue = new Setting("Blue", this, 0.0, 0.0, 255.0, true);
        Main.instance.setmgr.rSetting(this.colorBlue);
        this.width = new Setting("Width", this, 1.0, 0.5, 8.0, true);
        Main.instance.setmgr.rSetting(this.width);
        this.gap = new Setting("Gap", this, 2.0, 0.5, 10.0, true);
        Main.instance.setmgr.rSetting(this.gap);
        this.length = new Setting("Length", this, 3.0, 0.5, 30.0, true);
        Main.instance.setmgr.rSetting(this.length);
        this.dynamic = new Setting("Dynamic", this, false);
        Main.instance.setmgr.rSetting(this.dynamic);
        this.dynamicGap = new Setting("Dynamic Gap", this, 3.0, 1.0, 20.0, true);
        Main.instance.setmgr.rSetting(this.dynamicGap);
    }

    @EventTarget
    public void on2D(Event2D event) {
        int color = new Color(this.colorRed.getValFloat() / 255.0f, this.colorGreen.getValFloat() / 255.0f, this.colorBlue.getValFloat() / 255.0f).getRGB();
        int screenWidth = (int)event.getWidth();
        int screenHeight = (int)event.getHeight();
        int wMiddle = screenWidth / 2;
        int hMiddle = screenHeight / 2;
        boolean dyn = this.dynamic.getValue();
        double dyngap = this.dynamicGap.getValDouble();
        double wid = this.width.getValDouble();
        double len = this.length.getValDouble();
        boolean wider = dyn && this.isMoving();
        double gaps = wider ? dyngap : this.gap.getValDouble();
        RenderUtil.drawBorderedRect((double)wMiddle - gaps - len, (double)hMiddle - wid / 2.0, (double)wMiddle - gaps, (double)hMiddle + wid / 2.0, 0.5, Color.black.getRGB(), color, false);
        RenderUtil.drawBorderedRect((double)wMiddle + gaps, (double)hMiddle - wid / 2.0, (double)wMiddle + gaps + len, (double)hMiddle + wid / 2.0, 0.5, Color.black.getRGB(), color, false);
        RenderUtil.drawBorderedRect((double)wMiddle - wid / 2.0, (double)hMiddle - gaps - len, (double)wMiddle + wid / 2.0, (double)hMiddle - gaps, 0.5, Color.black.getRGB(), color, false);
        RenderUtil.drawBorderedRect((double)wMiddle - wid / 2.0, (double)hMiddle + gaps, (double)wMiddle + wid / 2.0, (double)hMiddle + gaps + len, 0.5, Color.black.getRGB(), color, false);
    }
  
    public boolean isMoving() {
        if (MovementInput.moveForward != 0.0f) return true;
        return Minecraft.player.moveStrafing != 0.0f;
    }
}
