package net.ccbluex.liquidbounce.utils.entity;

import net.ccbluex.liquidbounce.utils.entity;
import net.ccbluex.liquidbounce.utils;

public abstract class EventCancellable
implements Event,
Cancellable {
    private boolean cancelled;

    protected EventCancellable() {
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean state) {
        this.cancelled = state;
    }
}
