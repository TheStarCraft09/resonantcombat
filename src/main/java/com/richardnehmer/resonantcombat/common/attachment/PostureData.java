package com.richardnehmer.resonantcombat.common.attachment;

/** Transient posture state attached to eligible target entities. */
public class PostureData {
    public float posture;
    public long lastDamageTick;
    public long staggerEndTick;
    public boolean wasStaggered;
    public float lastSentPosture = -1F;
}
