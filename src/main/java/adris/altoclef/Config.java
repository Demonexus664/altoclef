package adris.altoclef;

import adris.altoclef.movement.ParkourMode;

public class Config {
    public ParkourMode parkourMode = ParkourMode.OFF;
    public boolean smoothingEnabled = false;
    public int smoothingMinJitterMs = 30;
    public int smoothingMaxJitterMs = 80;
}
