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

    static {}

    GachaClient() {}
    
    // p s c
    // c c c
    // p s s <
    // s s c
    // p p p
    // circular x y
    int[][] buildStrips(int trueIdx, int[] nearMissIdxs, ItemEffect.IEProperty.Rarity rarity) {
        int[][] reels = new int[3][reelsLength];
        int showIndex = (int)Math.round(reelsLength / 2.0f);

        // to keep track w/o tracin g2nd time
        int p = 0, s = 0, c = 0;
        for (int genIndex = 0; genIndex < reelsLength; ) {
            for (int i = 0; i < 3; i++) {
                reels[genIndex][i] = r.nextInt(2); // P S C
                if (reels[genIndex][i] == 0) p++;
                else if (reels[genIndex][i] == 1) s++;
                else if (reels[genIndex][i] == 2) c++;
            }
            // the ordering only affect show rows
            if (genIndex != showIndex) genIndex++;
            else if (rarity == ItemEffect.IEProperty.Rarity.NA) {
                // do nothing, skip next
                genIndex++;
            }
            else if (rarity == ItemEffect.IEProperty.Rarity.COMMON) {
                if (s == 1 || c == 1) genIndex++; // C x S or so
            }
            else if (rarity == ItemEffect.IEProperty.Rarity.RARE) {
                if (p == 2 || s == 2 || c == 2) genIndex++; // P P S
            }
            else if (rarity == ItemEffect.IEProperty.Rarity.LEGENDARY) { 
                if (p == 3 || s == 3 || c == 3) genIndex++; // S S S
            } 
        }
        return reels;
    }

    static ItemEffect.IEProperty.Rarity evalVisible(char c1, char c2, char c3) {
        return ItemEffect.IEProperty.Rarity.COMMON;
    }

    void tickSlot(SlotState s) {}

    void drawSlot(TextGraphics tg, TerminalPosition tp, SlotState s, int[][] strips) {}
}
