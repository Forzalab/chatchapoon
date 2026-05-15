package shared;

import com.googlecode.lanterna.TerminalSize;

public class Protocol {
    public static final int PORT = 4267;
    public static final int TICK_MS = 45;
    public static int ARENA_WIDTH = 225; // can chsnge later if too small
    public static int ARENA_HEIGHT = 87;
    public static final int MIN_COLS = 80;
    public static final int MIN_ROWS = 40;
    public static final int MAX_PLAYERS = 1;

    // for GameState
    public static final int LEVEL_DURATION_TICKS = 4800;
    public static final int SIDEBAR_WIDTH = (int)Math.round(ARENA_WIDTH/3.0f);
    public static final int HUD_HEIGHT = 1;
    public static final int BORDER = 1;
    public static final int FIRE_COOLDOWN_TICKS = 5;
    public static final int HIT_COOLDOWN_TICKS = 60;
    public static final int LOBBY_CLOSE_IN = 60000 * TICK_MS;
    public static final int ONE_USE_ITEM_TIME = -76;
    public static final int ONE_USE_ITEM_TIME_ACTIVE = -84; // can be enum but idc
    public static final int NEW_MONEY = 30;    
    
//    public static final int WAVE_INTERVAL_TICKS = 600;
    public static final int MAX_BULLETS = 1000;
    public static final float PLAYER_SPEED = 0.5f;
    public static final float PLAYER_SPEED_FAST = 1.0f;
    public static final float BULLET_CARDINAL = 1.0f;
    public static final float BULLET_DIAGONAL = 0.7071f;
    public static final float INV_SQRT2 = 0.7071f;
    public static final float SWAMP_SLOW_FACTOR = 0.5f;
    public static final float CHASER_SPEED = 0.25f;
    public static final float SHOOTER_SPEED = 0.1667f;
    public static final float DRIFTER_SPEED = 0.3333f;
    public static final float DRIFTER_SINE_AMP = (float)(ARENA_HEIGHT*2);
    public static final float DRIFTER_SINE_FREQ = 0.15f;
    public static final int DEATH_COOLDOWN = 60; // instant gratification 3s
    public static final int DEATH_COOLDOWN_PERM = -4968; // aptx idk
    public static final int PLAYER_MAX_HP = 20;
    public static final int PLAYER_HP_MAX = 20;

    public static final int BULLET_LIFETIME = 200;
    public static final int WAVE_INTERVAL = 180;
    public static final int DESPAWN_TIMER = 2400;

    public static final int GACHA_COST = 10;
    public static final int GACHA_REVEAL_IN = 70;
    public static final int PLAYER_RESPAWN_ATTEMPT = 3;

    // chat
    public static final int MAX_CHAR_PER_LINE = SIDEBAR_WIDTH - 5;
    public static final int HIT_FLASH_TICK = 2;

    // gacha
    public static final int GACHA_WIDTH = (int)Math.round(ARENA_WIDTH/5.0f);
    public static final int GACHA_HEIGHT = (int)Math.round(ARENA_HEIGHT/3.0f);

    public static final int GACHA_WIDTH_SMALL = (int)Math.round(ARENA_WIDTH/8.0f);
    public static final int GACHA_HEIGHT_SMALL = (int)Math.round(ARENA_HEIGHT/8.0f);    

    public static final int GACHA_ROWS_HALF = 5;

    public static final double GACHA_TITLE_RATIO = 0.2;
    public static final double GACHA_REELS_RATIO = 0.8;    
}
