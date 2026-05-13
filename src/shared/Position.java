package shared;

import java.io.*;
import java.util.Objects;

public class Position {
    private volatile int x, y;
    private volatile float accumx, accumy;
    private volatile boolean validated = false;

    public Position(int y, int x) {
        this.x = x;
        this.y = y;
        accumx = 0;
        accumy = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Position pos)) return false;
        else return (this.x == pos.x && this.y == pos.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.y, this.x);
    }

    // note to future self: VALIDATE B4 SETTING COORDS!!!
    public synchronized void accum(float dy, float dx) {
        checker();

        // processs x-axis
        // assume x = 1, accumx + dx = -2.5, border = 6
        accumx += dx; // b4, is always < 1.0f
        int accumxInt = (int)Math.round(accumx); // -2
        x += accumxInt; // plus naively
        x = Utility.mod(x, Protocol.ARENA_WIDTH);
        accumx -= (float)accumxInt; // -2.5 - (-2.0) = -0.5

        // process y-axis
        accumy += dy; // b4, is always < 1.0f
        int accumyInt = (int)Math.round(accumy); // -2
        y += accumyInt; // plus naively
        y = Utility.mod(y, Protocol.ARENA_HEIGHT);
        accumy -= (float)accumyInt; // -2.5 - (-2.0) = -0.5

        validated = false;
    }

    // override xy
    // 0ls only call at respawn
    public synchronized void set(float y, float x) {
        this.x = (int)x;
        this.y = (int)y;
        accumx = 0;
        accumy = 0;

        validated = false;
    }

    public boolean equals(Position other) {
        return getRenderX() == other.getRenderX() && getRenderY() ==
other.getRenderY();
    }
    
    private void checker() {
        try {
            if (validated == false) throw new Exception("");
        }
        catch (Exception e) {
            System.out.println("Exception caught at... u'know what? dumbass, u didnt validate sh*t b4 setting value, didnt u? run iHaveValidatedB4Setting() as a signature.");
            System.exit(0);
        }
    }
    
    public synchronized void iHaveValidatedB4Setting() { // ADHD-Proof Technology™
        validated = true;
    }

    // no wall collsion. ALL COLLISIKN R BASED ON WHATS ON SCREEN
    public synchronized int getRenderX() {
        return x;
    }

    public synchronized int getRenderY() {
        return y;
    }
}
