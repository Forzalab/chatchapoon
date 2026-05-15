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
    private static final TextColor.RGB bkg = new TextColor.RGB(15, 23, 45);
    private static final TextColor.RGB panel = new TextColor.RGB(5, 13, 15);
    private static final TextColor.RGB paleGold = new TextColor.RGB(255, 248, 200);
    private static final TextColor.RGB whiteDefault = new TextColor.RGB(255, 255, 255);
    private static final TextColor.RGB white = new TextColor.RGB(205, 205, 205);
    
    private static final TextColor.RGB C_BKG = new TextColor.RGB(90, 10, 10);
    private static final TextColor.RGB C_FG = new TextColor.RGB(255, 110, 110);

    private static final TextColor.RGB S_BKG = new TextColor.RGB(35, 38, 48);
    private static final TextColor.RGB S_FG = new TextColor.RGB(210, 215, 225);

    private static final TextColor.RGB P_BKG = new TextColor.RGB(18, 18, 22);
    private static final TextColor.RGB P_FG = new TextColor.RGB(75, 75, 90);

    private static final TextColor.RGB REVEAL_FLASH = new TextColor.RGB(255, 235, 90);

    private static final TextColor.RGB rDud = new TextColor.RGB(80, 80, 80);
    private static final TextColor.RGB rCommon = new TextColor.RGB(160, 160, 160);
    private static final TextColor.RGB rRare = new TextColor.RGB(90, 140, 255);
    private static final TextColor.RGB rLegendary = new TextColor.RGB(255, 195, 0);

    private static HashMap<String, ItemEffect.IEProperty> iepMap = new LinkedHashMap<>();

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

    // id - gacha data
    private static Map<String, JSONObject> data = new LinkedHashMap<>();
    private static final String authorID = GameClient.playerID;

    private static final boolean isAuthor(String id) { return authorID.equals(id); }
    private static final JSONObject getAuthorGacha() {
        return data.get(authorID);
    }

    private static boolean eligible = false;
    private static int stateTick = 0;
    private static int xs = 0, xe = 0, ys = 0, ye = 0;
    private static Random r = new Random();
    private static final int reelsLength = (int)Math.round(Protocol.GACHA_ROWS_HALF * 2 * 2);
    private static int[] reels = new int[reelsLength];
    
    static enum SlotState {
        SPIN(0), LOCK_1(1), LOCK_2(2), LOCK_3(3), REVEAL(4), STASIS(5), NOMONEY(6);
        private int val = 0;
        private SlotState(int val) { this.val = val; }
        private static final SlotState[] vals = values();
        private SlotState next() {
             if (val == 6) return vals[5];// NM -> STASIS
             int index = Utility.mod(this.ordinal() + 1, vals.length - 1);
             return vals[index];
        }
        // correspond to ordinals
        static final double[] duration = {0.34, 0.48, 0.61, 0.75, 0, 0, 0};
    }

    private static SlotState ss = SlotState.STASIS;

    private static final String dollarString = "$ ".repeat(100);
    private static final TextColor[] dollarColors = {REVEAL_FLASH, rLegendary, paleGold, rLegendary, REVEAL_FLASH, panel};
    private static final TextColor[] dollarDullColors = {rDud, rCommon, paleGold, rCommon, rDud, panel};    
    private static int aniTick = 1;

    static {
        // recieves iepMap from server ONCE. its a lookup by name.
        iepMap = ItemEffect.lookup;
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

//    static void triggerNoMoney() { ss = SlotState.NOMONEY; }    
    
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
    
    int[] buildStrips(ItemEffect.IEProperty.Rarity rarity) {
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

    SlotState tickNext(boolean forceNextState) { 
        boolean reachedMaxDuration = (stateTick >= SlotState.duration[ss.ordinal()] * Protocol.GACHA_REVEAL_IN);
        boolean canJumpstartFromStasis = (forceNextState && ss == SlotState.STASIS);
        boolean inStasis = ss == SlotState.STASIS;
        boolean inEndState = (ss == SlotState.NOMONEY) || (ss == SlotState.REVEAL);
        boolean userReadyExitGacha = inEndState && forceNextState;
        
        stateTick += (reachedMaxDuration || canJumpstartFromStasis || userReadyExitGacha) ? (-stateTick) : 1;
        aniTick++;
        if (canJumpstartFromStasis)
            // plauer eligible? go to Spin
            // else go to NM
            ss = (eligible) ? SlotState.SPIN : SlotState.NOMONEY;
        else if (userReadyExitGacha)
            ss = ss.next(); // terminates at R or NM as checkpoints
        else if (!inStasis && reachedMaxDuration && !inEndState)
            ss = ss.next(); // terminates at R or NM as checkpoints            
        return ss;
    }
    
    SlotState currentSlotState() { return ss; }    

    boolean active() { return ss != SlotState.STASIS; }

    synchronized void updateInternalDataWith(JSONObject jao, JSONObject author_jo) {
        JSONArray ja = new JSONArray(jao.getJSONArray("notifs"));
        if (ja == null) return;

        if (author_jo != null) {
            int gachaCost = Protocol.GACHA_COST;
            int coinsHave = author_jo.optInt("currency", -1);
            eligible = (coinsHave >= gachaCost); 
        }
       
        for (int i = 0; i < ja.length(); i++) {
            if (ja.getJSONObject(i) == null) continue;
            JSONObject j = ja.getJSONObject(i);
            if (j == null) continue;
            String pullerID = j.optString("pullerID");
            data.put(pullerID, j);

            // for author
            if (!authorID.equals(pullerID)) continue;            

            // start to slot up
            String itemName = j.optString("itemName");
            ItemEffect.IEProperty prop = ItemEffect.lookup.get(itemName);
            if (prop != null) reels = buildStrips(prop.rarity);
        }
    }

    // state: LSD is author, 2nd LSD is flahsing
    void drawFrameBox(TextGraphics tg, int fromX, int fromY, SlotState s, boolean fullSize) {
        tg.setBackgroundColor(panel);
        tg.setForegroundColor(white);

        int StartX = 0, StartY = 0, EndX = 0, EndY = 0;

        if (fullSize) {
            StartX = fromX; EndX = StartX + Protocol.GACHA_WIDTH;
            StartY = fromY; EndY = StartY + Protocol.GACHA_HEIGHT;
        } else {
            StartX = fromX; EndX = StartX + Protocol.GACHA_WIDTH_SMALL;
            StartY = fromY; EndY = StartY + Protocol.GACHA_HEIGHT_SMALL;
        }
        
        // frame big

        tg.setForegroundColor(panel);
        tg.fillRectangle(new TerminalPosition(fromX, fromY), ts, '.');
        tg.setForegroundColor(white);

        // flahsing frame
        if (ss != SlotState.REVEAL) {
            tg.setForegroundColor(white);
        } else {
            tg.setForegroundColor(REVEAL_FLASH);
        }

        tg.drawLine(StartX, StartY, EndX, StartY, '─');
        tg.drawLine(StartX, EndY, EndX, EndY, '─');
        tg.drawLine(EndX, StartY, EndX, EndY, '│');
        tg.drawLine(StartX, StartY, StartX, EndY, '│');
        tg.setCharacter(StartX, StartY, '╭');
        tg.setCharacter(EndX, StartY, '╮');
        tg.setCharacter(StartX, EndY, '╰');
        tg.setCharacter(EndX, EndY, '╯');
        // subframe
        final int boundTitleY = Utility.lerp(StartY, EndY, Protocol.GACHA_TITLE_RATIO), boundReelY = Utility.lerp(StartY, EndY, Protocol.GACHA_REELS_RATIO);
        tg.drawLine(StartX, boundTitleY, EndX, boundTitleY, '━');
        tg.drawLine(StartX, boundReelY, EndX, boundReelY, '━');
        tg.setCharacter(StartX, boundTitleY, '┝');
        tg.setCharacter(StartX, boundReelY, '┝');
        tg.setCharacter(EndX, boundTitleY, '┥');
        tg.setCharacter(EndX, boundReelY, '┥');
        
        // roll section, frame
        
        tg.setBackgroundColor(bkg);
        tg.setForegroundColor(whiteDefault);
//        try {} catch (Exception e) {}
    }

    void drawSideSlot(TextGraphics tg, int fromX, int fromY, SlotState s) {
    
        int StartX = 0, StartY = 0, EndX = 0, EndY = 0;

        // exclude author fron popup
        
        StartX = Protocol.ARENA_WIDTH/2 - Protocol.GACHA_WIDTH/2; EndX = StartX + Protocol.GACHA_WIDTH;
        StartY = Protocol.ARENA_HEIGHT/2 - Protocol.GACHA_HEIGHT/2; EndY = StartY + Protocol.GACHA_HEIGHT;
    
        tg.setBackgroundColor(panel);
        tg.setForegroundColor(white);
        tg.setBackgroundColor(bkg);
        tg.setForegroundColor(whiteDefault);
    }

    private static int mid(int start, int end) {
        return Utility.lerp(start, end, 0.5);
    }

    void drawDollarBkg(TextGraphics tg, SlotState s, int sX, int sY, int eX, int eY) {
        tg.setBackgroundColor(panel); 
        tg.setForegroundColor(white);
     
        int scale = (s == SlotState.REVEAL) ? 3 : 2;
        for (int y = sY; y <= eY; y++) { for (int x = sX; x <= eX; x++) {
            int charIndex = Utility.mod(x-sX + (aniTick/5), dollarString.length());
            int colorIndex = Utility.mod(x-sX + scale * (aniTick/5 * (y/7)%7) * ((y%2)*(-1) + (y+1)%2), dollarColors.length);
            TextColor dc = s == SlotState.NOMONEY ? dollarDullColors[colorIndex] : dollarColors[colorIndex];
            char c = dollarString.charAt(charIndex);
            tg.setForegroundColor(dc);
            tg.setCharacter(x, y, c);
        }}
       
        tg.setBackgroundColor(bkg); 
        tg.setForegroundColor(whiteDefault);
    }
    
    void drawSlotTitle(TextGraphics tg, SlotState s) {
        tg.setBackgroundColor(panel);
        tg.setForegroundColor(white);
       
        String pullerName = " da vault ";

        int StartX = 0, StartY = 0, EndX = 0, EndY = 0;
        StartX = xs + 1; EndX = xe - 1;
        StartY = ys + 1; EndY = Utility.lerp(ys, ye, Protocol.GACHA_TITLE_RATIO) - 1;

        int midX = mid(StartX, EndX), midY = mid(StartY, EndY);

        drawDollarBkg(tg, s, StartX, StartY, EndX, EndY);
        
//        String titleCenter = "   " + spinnerChar + " " + pullerName + " " + spinnerChar + "   ";
        char sp = "|/-\\".charAt((aniTick/4) % 4);
        TextColor spColor = (aniTick % 6 < 3) ? REVEAL_FLASH : rLegendary;

        String spinL = "< " + sp + " >";
        String spinR = "< " + sp + " >";
        String full  = spinL + " " + pullerName + " " + spinR;

        int drawX = mid(StartX, EndX) - full.length()/2;
        tg.setBackgroundColor(panel);
        tg.setForegroundColor(spColor);

        tg.fillRectangle(new TerminalPosition(midX - full.length()/2, midY - 1), new TerminalSize(full.length(), 3), ' ');
        tg.putString(drawX, midY, spinL);
        tg.setForegroundColor(s == SlotState.REVEAL ? REVEAL_FLASH : whiteDefault);
        tg.putString(drawX + spinL.length() + 1, midY, pullerName);
        tg.setForegroundColor(spColor);
        tg.putString(drawX + spinL.length() + 1 + pullerName.length() + 1, midY, spinR);
/*
        tg.setBackgroundColor(panel);
        tg.setForegroundColor(s == SlotState.REVEAL?REVEAL_FLASH:white);


        tg.putString(midX - titleCenter.length()/2, midY, titleCenter);
*/
        // draw dolar bkg first
        // then draw txt

        tg.setBackgroundColor(bkg); 
        tg.setForegroundColor(whiteDefault);
    }

    void drawSlotReels(TextGraphics tg, SlotState s) {
        tg.setBackgroundColor(panel);

        int StartX = 0, StartRY = 0, StartY = 0, EndX = 0, EndTY = 0, EndRY = 0;
        StartX = xs + 1; EndX = xe - 1;
        StartY = ys+1;
        EndTY = Utility.lerp(ys, ye, Protocol.GACHA_TITLE_RATIO) - 1; EndRY = Utility.lerp(ys, ye, Protocol.GACHA_REELS_RATIO) - 2;
        StartRY = EndTY + 3;
        
        int midRY = mid(StartRY, EndRY);
        int colW = (int)Math.round((EndX - StartX) / 3.0f);
        int[] rX = {StartX+colW/2, StartX+colW+colW/2, StartX+2*colW+colW/2};

        tg.fillRectangle(new TerminalPosition(StartX, StartRY), new TerminalSize(EndX-StartX-1, EndRY-EndTY-1), ' ');

        int[] syms = new int[3];
        
          // 5 rows 3 col
        for (int dy = -Protocol.GACHA_ROWS_HALF; dy <= Protocol.GACHA_ROWS_HALF; dy++) {
            int rY = midRY + dy;
            float dimFactor = (float)Math.pow(1.0/Math.abs(dy), Math.abs(dy));      

        for (int i = 0; i < 3; i++) {
        
            // index each RC, noting that a col can be locked or not
            int spinFactor = (s.ordinal() <= i) ? (aniTick + 17 * i) : 0;
            int rowIdx = Utility.mod(spinFactor + dy + reelsLength/2, reelsLength);
            int reelElem = reels[rowIdx]; 
            
            syms[i] = (reelElem >> (4 - (i << 1))) & 3;
            if (syms[i] == 0) syms[i] = 1;
            TextCharacter tc = symbolMap.get(syms[i]);
            if (tc == null) continue;

            // fadibg char and stuff
            TextColor bkg = tc.getBackgroundColor(), frg = tc.getForegroundColor();
            bkg = ChatClient.getLERP(bkg, panel, 1 - dimFactor);
            frg = ChatClient.getLERP(frg, panel, 1 - dimFactor);            
            if (dy == 0) {
//                bkg = ChatClient.getLERP(bkg, REVEAL_FLASH, 0.15f);
            }
            tg.setForegroundColor(frg).setBackgroundColor(bkg);            
//            tg.putString(rX[i] - 1, rY-1, " " + tc.getCharacter() + " ");            
            tg.putString(rX[i] - 1, rY, " " + tc.getCharacter() + " ");
//            tg.putString(rX[i] - 1, rY+1, " " + tc.getCharacter() + " ");            
        }}
        tg.setBackgroundColor(panel);
    }

    void drawSlotResult(TextGraphics tg, SlotState s) {
        int StartX = xs+1, EndX = xe-1;
        int startRZ = Utility.lerp(ys, ye, Protocol.GACHA_REELS_RATIO) + 1;
        int endRZ = ye - 1;
        int midRZY = mid(startRZ, endRZ);
        int midRZX = mid(StartX, EndX);

        tg.setBackgroundColor(panel);
        tg.fillRectangle(new TerminalPosition(StartX, startRZ), new TerminalSize(EndX-StartX, endRZ-startRZ+1), ' ');

        JSONObject author = getAuthorGacha();
        if (s == SlotState.REVEAL && author != null) {
            String displayName = author.optString("itemDisplayName", "???");
            String rarityStr = author.optString("itemRarity", "");
            String desc = author.optString("itemDesc", "");

            TextColor rarityColor
            = ("LEGENDARY".equals(rarityStr)) ? rLegendary
            : ("RARE".equals(rarityStr)) ? rRare
            : ("COMMON".equals(rarityStr)) ? rCommon
            : ("NA".equals(rarityStr)) ? rDud : white;

            tg.setForegroundColor(rarityColor);
            tg.putString(midRZX - rarityStr.length()/2, midRZY - 1, rarityStr);
            tg.setForegroundColor(whiteDefault);
            tg.putString(midRZX - displayName.length()/2, midRZY, displayName);
            int maxW = EndX - StartX - 2;
            String truncDesc = desc.length() > maxW ? desc.substring(0, maxW-3)+"..." : desc;
            tg.setForegroundColor(white);
            tg.putString(midRZX - truncDesc.length()/2, midRZY + 1, truncDesc);
        }
    }
    
    // bit 0 int state is redundant rn, but leave it for later usr    
    void drawSlot(TextGraphics tg, SlotState s) {
        xs = Protocol.ARENA_WIDTH/2 - Protocol.GACHA_WIDTH/2; xe = xs + Protocol.GACHA_WIDTH;
        ys = Protocol.ARENA_HEIGHT/2 - Protocol.GACHA_HEIGHT/2; ye = ys + Protocol.GACHA_HEIGHT;

        // DO NOT CHANGE XS YS FROM THIS STAGE
        if (s == SlotState.STASIS) return;

        drawFrameBox(tg, xs, ys, s, true);
        drawSlotTitle(tg, s);
        drawSlotReels(tg, s);
        drawSlotResult(tg, s);
        
        tg.setBackgroundColor(panel);
        tg.setForegroundColor(white);
        tg.setBackgroundColor(bkg);
        tg.setForegroundColor(whiteDefault);
    }
}
