package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.redesky

import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer

class RedeSkyGround : SpeedMode("RedeSkyGround") {
    private val timer=MSTimer()
    private var stage=false

    override fun onMotion() {
        if(MovementUtils.isMoving()){
            if(stage){
                mc.timer.timerSpeed=1.81F
                if(timer.hasTimePassed(250)){
                    timer.reset()
                    stage=!stage
                }
            }else{
                mc.timer.timerSpeed=0.92F
                if(timer.hasTimePassed(350)){
                    timer.reset()
                    stage=!stage
                }
            }
        }
    }
}
