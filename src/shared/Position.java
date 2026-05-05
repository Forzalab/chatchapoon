package shared;

import java.io.*;

class Position {
    private volatile int x, y;
    private volatile float accumx, accumy;
    private boolean validated = false;

    // note to future self: VALIDATE B4 SETTING COORDS!!!
    public synchronized void accum(float dx, float dy) {
        checker();

        // processs x-axis
        // assume x = 1, accumx + dx = -2.5, border = 6
        accumx += dx; // b4, is always < 1.0f
        int accumxInt = (int)accumx; // -2
        x += accumxInt; // plus naively
        x = (((x % Protocol.ARENA_WIDTH) + Protocol.ARENA_WIDTH) % Protocol.ARENA_WIDTH); // now, wtf stackoverflow
        accumx -= (float)accumxInt; // -2.5 - (-2.0) = -0.5

        // process y-axis
        accumy += dy; // b4, is always < 1.0f
        int accumyInt = (int)accumy; // -2
        y += accumyInt; // plus naively
        y = (((y % Protocol.ARENA_HEIGHT) + Protocol.ARENA_HEIGHT) %   Protocol.ARENA_HEIGHT); // now, wtf stackoverflow
        accumy -= (float)accumyInt; // -2.5 - (-2.0) = -0.5

        validated = false;
    }

    // override xy
    // 0ls only call at respawn
    public synchronized void set(float x, float y) {
        this.x = (int)x;
        this.y = (int)y;
        accumx = (float)this.x - accumx;
        accumy = (float)this.y - accumy;

        validated = false;
    }

    private void checker() {
        try {
            if (validated == false)
                throw new Exception("");
            else validated = true;
        }
        catch (Exception e) {
            System.out.println("Exception caught at... u'know what? dumbass, u didnt validate sh*t b4 setting value, didnt u? run iHaveValidatedB4Setting() as a signature.");
        }
    }
    
    public void iHaveValidatedB4Setting() { // ADHD-Proof Technology™
        validated = true;
    }

    // no wall collsion. ALL COLLISIKN R BASED ON WHATS ON SCREEN
    public int getRenderX() {
        return x;
    }

    public int getRendery() {
        return y;
    }
}
