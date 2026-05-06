package shared;

public class Protocol {
    public static final int PORT = 4267;
    public static final int TICK_MS = 50;
    public static int ARENA_WIDTH = 60; // can chsnge later if too small
    public static int ARENA_HEIGHT = 21;
    public static final int MIN_COLS = 80;
    public static final int MIN_ROWS = 40;
    public static final int MAX_PLAYERS = 20;

    // for GameState
    public static final int LEVEL_DURATION_TICKS = 3600;
    public static final int SIDEBAR_WIDTH = 18;
    public static final int HUD_HEIGHT = 1;
    public static final int BORDER = 1;
    public static final int FIRE_COOLDOWN_TICKS = 4;
    public static final int WAVE_INTERVAL_TICKS = 600;
    public static final int MAX_BULLETS = 200;
    public static final float PLAYER_SPEED = 0.5f;
    public static final float PLAYER_SPEED_FAST = 1.0f;
    public static final float BULLET_CARDINAL = 1.0f;
    public static final float BULLET_DIAGONAL = 0.7071f;
    public static final float INV_SQRT2 = 0.7071f;
    public static final float SWAMP_SLOW_FACTOR = 0.5f;
    public static final float CHASER_SPEED = 0.25f;
    public static final float SHOOTER_SPEED = 0.1667f;
    public static final float DRIFTER_SPEED = 0.3333f;
    public static final float DRIFTER_SINE_AMP = 3.0f;
    public static final float DRIFTER_SINE_FREQ = 0.15f;
    public static final int DEATH_COOLDOWN = 60; // instant gratification 3s
    public static final int DEATH_COOLDOWN_PERM = -4968; // aptx idk
    public static final int PLAYER_MAX_HP = 300;
}
