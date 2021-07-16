package met.ccbluex.liquidbounce.utils.entity;

import net.ccbluex.liquidbounce.utils.entity;
import net.ccbluex.liquidbounce.utils.entity.EventTyped;

public abstract class EventTyped
implements Event,
Typed {
    private final byte type;

    protected EventTyped(byte eventType) {
        this.type = eventType;
    }

    @Override
    public byte getType() {
        return this.type;
    }
}
