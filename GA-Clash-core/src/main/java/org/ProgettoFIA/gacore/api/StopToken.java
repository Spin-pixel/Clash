package org.ProgettoFIA.gacore.api;

import java.util.concurrent.atomic.AtomicBoolean;

public final class StopToken {
    private final AtomicBoolean stop = new AtomicBoolean(false);

    public void requestStop() { stop.set(true); }
    public boolean isStopRequested() { return stop.get(); }
}
