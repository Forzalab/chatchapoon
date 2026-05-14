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
    private static HashMap<String, ItemEffect.IEProperty> iepMap = new HashMap<>();
    public static final HashMap<Integer, TextCharacter> symbolMap = new HashMap<>();
    
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
        return (bits & (bits >> 2)) == 0;
    }
    
    // Invariant: none = 00, c = 01, p = 10, s = 11
    static boolean satisfyState(int treel, ItemEffect.IEProperty.Rarity rarity) {
        int reel = treel & 0b111111; // take only 6 bits
        int rarityIndex = rarity.getVal(); // 0, 1, 2, 3
        
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
            while (satisfyState(reels[i], rarity) && (i == (int)Math.round(reelsLength/2.0f)));
        }
        return reels;
    }

    static ItemEffect.IEProperty.Rarity evalVisible(char c1, char c2, char c3) {
        return ItemEffect.IEProperty.Rarity.COMMON;
    }

    SlotState tickSlot(SlotState s) {}

    void drawSlot(TextGraphics tg, TerminalPosition tp, SlotState s, int[][] strips) {}
}
