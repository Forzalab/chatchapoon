package client;

import java.util.*;
import java.util.HashMap;

import org.json.*;
import org.json.JSONArray;
import org.json.JSONException;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.*;

import shared.*;

public class GachaClient {
    private static final TextColor.RGB bkg = new TextColor.RGB(15, 23, 42);
    private static final TextColor.RGB panel = new TextColor.RGB(22, 28, 48);
    private static final TextColor.RGB whiteDefault = new TextColor.RGB(255, 255, 255);
    private static final TextColor.RGB white = new TextColor.RGB(215, 215, 215);
    
    private static final TextColor.RGB C_BKG = new TextColor.RGB(90, 10, 10);
    private static final TextColor.RGB C_FG = new TextColor.RGB(255, 110, 110);

    private static final TextColor.RGB S_BKG = new TextColor.RGB(35, 38, 48);
    private static final TextColor.RGB S_FG = new TextColor.RGB(210, 215, 225);

    private static final TextColor.RGB P_BKG = new TextColor.RGB(18, 18, 22);
    private static final TextColor.RGB P_FG = new TextColor.RGB(75, 75, 90);

    private static final TextColor.RGB REEL_FG_MID = new TextColor.RGB(110, 110, 120); 
    private static final TextColor.RGB REEL_FG_FAR = new TextColor.RGB(45, 45, 52);
    private static final TextColor.RGB REEL_HIGHLIGHT = new TextColor.RGB(160, 140, 50);

    private static final TextColor.RGB REVEAL_FLASH = new TextColor.RGB(255, 235, 90);

    private static final TextColor.RGB rDud = new TextColor.RGB(80, 80, 80);
    private static final TextColor.RGB rCommon = new TextColor.RGB(160, 160, 160);
    private static final TextColor.RGB rRare = new TextColor.RGB(90, 140, 255);
    private static final TextColor.RGB rLegendary = new TextColor.RGB(255, 195, 0);

    private static HashMap<String, ItemEffect.IEProperty> iepMap = new HashMap<>();

    private static final TerminalSize ts = new TerminalSize(Protocol.GACHA_WIDTH, Protocol.GACHA_HEIGHT);
    public static final HashMap<Integer, TextCharacter> symbolMap = new HashMap<>() {{
        put(1, new TextCharacter('C', C_FG, C_BKG));
        put(2, new TextCharacter('P', P_FG, P_BKG));        
        put(3, new TextCharacter('S', S_FG, S_BKG));        
    }};
    public static final HashMap<Character, Integer> symbolMapInverse = new HashMap<>() {{
        put('C', 1);
        put('P', 2);        
        put('S', 3);        
    }};
    
    private Random r = new Random();
    private final int reelsLength = 11;
    
    private static enum SlotState {
        SPIN(0), LOCK_1(1), LOCK_2(2), LOCK_3(3), REVEAL(4), STATIS(5);
        private int val = 0;
        private SlotState(int val) { this.val = val; }
        private static final SlotState[] vals = values();
        private SlotState next() {
             int index = Utility.mod(this.ordinal() + 1, vals.length);
             return vals[index];
        }
    }

    private SlotState ss = SlotState.STATIS;

    static {
        // recieves iepMap from server ONCE. its a lookup by name.
    }

    GachaClient() {}
    
    // p s c
    // c c c
    // p s s <
    // s s c
    // p p p
    // circular x y

    private static boolean RM4IsZero(int bits) {
        return (~(bits ^ (bits >> 2)) & 0b1111) == 0b1111;
    }

    private static int lerp(double start, double end, double t) {
        return (int)Math.round((1 - t) * start + t * end);
    }
    
    // Invariant: none = 00, c = 01, p = 10, s = 11
    static boolean satisfyState(int treel, int rarityIndex) {
        int reel = treel & 0b111111; // take only 6 bits
        
        // general: no empty slot
        if ((reel & 0b11) == 0 || (reel & 0b1100) == 0 || (reel & 0b110000) == 0)
            return false;

        if (rarityIndex == 0) return true;
        // case 1: at least one C or S
        else if ((reel & 0b010101) != 0 && rarityIndex == 1)
            return true;
        // case 2: 2 repeat pairs. 
        else if (rarityIndex == 2) {
            int il = reel & 0b11, is = (reel & 0b1100) >> 2, im = (reel & 0b110000) >> 4;
            return (il == is || is == im || im == il);
        }
        // case 3: 3 segment repeat
        else if (RM4IsZero(reel) && rarityIndex == 3)
            return true;
        else return false;
    }
    
    int[] buildStrips(int trueIdx, ItemEffect.IEProperty.Rarity rarity) {
        int[] reels = new int[reelsLength];
        for (int i = 0; i < reelsLength; i++) {
            do { reels[i] = r.nextInt(63); }
            while (!satisfyState(reels[i], rarity.getVal()) && (i == (int)Math.round(reelsLength/2.0f)));
        }
        return reels;
    }

    static ItemEffect.IEProperty.Rarity evalVisible(char c1, char c2, char c3) {
        int reel = 0;
        reel += symbolMapInverse.get(c1); reel <<= 2;
        reel += symbolMapInverse.get(c2); reel <<= 2;
        reel += symbolMapInverse.get(c3);        
        for (int i = 1; i <= 3; i++)
            if (satisfyState(reel, i)) return ItemEffect.IEProperty.Rarity.values()[i];
        return ItemEffect.IEProperty.Rarity.values()[0];
    }

    SlotState tickSlot(SlotState s) { return s.next(); }



    // state: LSD is author, 2nd LSD is flahsing
    void drawFrameBox(TextGraphics tg, TerminalPosition tp, SlotState s, int state) {
        tg.setBackgroundColor(panel);
        tg.setForegroundColor(white);

        final int StartX, StartY, EndX, EndY;

        if ((state & 0b1) == 1) {
            StartX = tp.getColumn(); EndX = StartX + Protocol.GACHA_WIDTH;
            EndX = tp.getRow(); EndY = StartY + Protocol.GACHA_HEIGHT;
        } else {
            StartX = tp.getColumn(); EndX = StartX + Protocol.GACHA_WIDTH_SMALL;
            EndX = tp.getRow(); EndY = StartY + Protocol.GACHA_HEIGHT_SMALL;
        }
        
        // frame big

        tg.setForegroundColor(panel);
        tg.fillRectangle(tp, ts, '.');
        tg.setForegroundColor(white);

        // flahsing frame
        if ((state & 0b10) == 0b10) {
            tg.setForegroundColor(white);
        } else {
            tg.setForegroundColor(REVEAL_FLASH);
        }

        tg.drawLine(StartX, EndX, StartY, StartY, '─');
        tg.drawLine(StartX, EndX, EndY, EndY, '─');
        tg.drawLine(EndX, EndX, StartY, EndY, '│');
        tg.drawLine(StartX, StartX, StartY, EndY, '│');                        
        tg.setCharacter(StartX, StartY, '╭');
        tg.setCharacter(StartX, EndY, '╮');
        tg.setCharacter(EndX, StartY, '╰');
        tg.setCharacter(EndX, EndY, '╯');                        
        
        // subframe
        final int boundTitleY = lerp(StartY, EndY, 0.33), boundReelY = lerp(StartY, EndY, 0.75);
        tg.drawLine(StartX, EndX, boundTitleY, boundTitleY, '━');
        tg.drawLine(StartX, EndX, boundReelY, boundReelY, '━');                                
        tg.setCharacter(StartX, boundTitleY, '┝');
        tg.setCharacter(StartX, boundReelY, '┝');
        tg.setCharacter(EndX, boundTitleY, '┥');
        tg.setCharacter(EndX, boundReelY, '┥');        
        
        // roll section, frame
        
        tg.setBackgroundColor(bkg);
        tg.setForegroundColor(whiteDefault);
    }

    void drawFoo(TextGraphics tg, TerminalPosition tp, SlotState s, boolean isAuthor) {
    
        final int StartX, StartY, EndX, EndY;

        if (isAuthor) {
            StartX = tp.getColumn(); EndX = StartX + Protocol.GACHA_WIDTH;
            EndX = tp.getRow(); EndY = StartY + Protocol.GACHA_HEIGHT;
        } else {
            StartX = tp.getColumn(); EndX = StartX + Protocol.GACHA_WIDTH_SMALL;
            EndX = tp.getRow(); EndY = StartY + Protocol.GACHA_HEIGHT_SMALL;
        }
    
        tg.setBackgroundColor(panel);
        tg.setForegroundColor(white);
        tg.setBackgroundColor(bkg);
        tg.setForegroundColor(whiteDefault);
    }
    
    void drawSlot(TextGraphics tg, TerminalPosition tp, SlotState s, int[] reels, boolean isAuthor) {
    
        final int StartX, StartY, EndX, EndY;

        if (isAuthor) {
            StartX = tp.getColumn(); EndX = StartX + Protocol.GACHA_WIDTH;
            EndX = tp.getRow(); EndY = StartY + Protocol.GACHA_HEIGHT;
        } else {
            StartX = tp.getColumn(); EndX = StartX + Protocol.GACHA_WIDTH_SMALL;
            EndX = tp.getRow(); EndY = StartY + Protocol.GACHA_HEIGHT_SMALL;
        }
    
        tg.setBackgroundColor(panel);
        tg.setForegroundColor(white);
        tg.setBackgroundColor(bkg);
        tg.setForegroundColor(whiteDefault);
    }
}
