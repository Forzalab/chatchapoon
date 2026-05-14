package client;

import java.util.*;
import java.util.concurrent.*;
import java.time.format.*;
import java.time.*;
import org.json.*;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.*;

import shared.*;

public class ChatClient {
    static int ARENA_WIDTH = Protocol.ARENA_WIDTH + 1;
    static int SIDEBAR_WIDTH = Protocol.SIDEBAR_WIDTH + 1;    
    static volatile String msgBuffer = "";
//    static volatile int scrollOffset = 0, renderOffset = 0; // from bottom up
    static CopyOnWriteArrayList<String> msgBlock = new CopyOnWriteArrayList<>();
    private static ConcurrentHashMap<String, String> msgBlockMapSender = new ConcurrentHashMap<String, String>();    
    static CopyOnWriteArrayList<JSONObject> msgQ = new CopyOnWriteArrayList<JSONObject>();    
    static TextGraphics tg = GameClient.screen.newTextGraphics();
    private static int cursor = 0;
    static int maxTxtWidth = ARENA_WIDTH+1 + SIDEBAR_WIDTH - 2 - (ARENA_WIDTH+5);
    static boolean blink = false; static int tick = 0;
    static synchronized void moveCursor(int dx) {
        int maxDisplayWidth = Math.min(maxTxtWidth, msgBuffer.length());
        if (cursor + dx < 0) cursor = 0;
        else if (cursor + dx > maxDisplayWidth) cursor = maxDisplayWidth;
        else cursor += dx;
    }
    static synchronized void resetCursor() {
        cursor = 0;
    }
    static synchronized void removeAt() {
        int txtLength = msgBuffer.length();
        int txtOffset = (txtLength > maxTxtWidth) ? (txtLength-maxTxtWidth) : 0;        
        int i = cursor - 1 + txtOffset; // rmeove BEFORE cursor
        if (i < 0 || cursor == 0) return;
        msgBuffer = msgBuffer.substring(0, i) + msgBuffer.substring(i+1);
        if (txtOffset == 0) moveCursor(-1);
    }
    static synchronized void insertChar(char c) {
        int txtLength = msgBuffer.length();
        int txtOffset = (txtLength > maxTxtWidth) ? (txtLength-maxTxtWidth) : 0;
        StringBuffer stringBuffer = new StringBuffer(msgBuffer);
        stringBuffer.insert(Math.max(0, cursor + txtOffset), c);
        msgBuffer = stringBuffer.toString();
        if (txtOffset == 0) moveCursor(1);
    }
    public static synchronized void send(String msg, String id, String name, String eventType) {
        JSONObject msgJSON = new JSONObject()
        .put("type", "CHAT")
        .put("msg", msg)
        .put("id", id)
        .put("name", name)
        .put("eventType", eventType);
        String msgJSONString = msgJSON.toString();
        GameClient.writer.println(msgJSONString);
        msgBuffer = "";
    }
    static synchronized String formatNormal(JSONObject obj) {
        // { "type": "CHAT", "id": "abc12345", "name": "alice", "msg": "yo" }
        // [{"content":"blah", "color":"red"}]
        if (obj == null) return "";
        String playerName = obj.optString("name"); playerName = playerName.substring(0, Math.min(playerName.length(), 10));
        String playerID = obj.optString("id");        
        String timestamp = getTimestamp();
        String msg = obj.optString("msg");
        String msgFormatted = "[" + timestamp + "] " + playerName + ": " + msg;           
        return msgFormatted;
    }
/*    static synchronized String formatPrize(JSONObject obj) {

    }*/
    static TextColor getColor(String playerID) {
        int r = 255, g = 255, b = 255;
        if (!GameClient.playerColor.containsKey(playerID)) {
            int hash = Math.abs(playerID.hashCode());
            
            int min = 120;
            int max = 235;
            int range = max - min + 1;

            r = (hash % range) + min;
            g = ((hash >> 8) % range) + min;
            b = ((hash >> 16) % range) + min;

            TextColor color = new TextColor.RGB(r,g,b);
            GameClient.playerColor.put(playerID, color);
        }
        return GameClient.playerColor.get(playerID);
    }
    static TextColor getLERP(TextColor tc0, TextColor tc1, float slider) {
        int r0 = tc0.getRed(), r1 = tc1.getRed(), r = (int)Math.round(r0 + slider * (r1 - r0));
        int g0 = tc0.getGreen(), g1 = tc1.getGreen(), g = (int)Math.round(g0 + slider * (g1 - g0));
        int b0 = tc0.getBlue(), b1 = tc1.getBlue(), b = (int)Math.round(b0 + slider * (b1 - b0));
        return new TextColor.RGB(r,g,b);
    }
    static TextColor getDimmed(TextColor tc, float dimFactor) {
        int r = tc.getRed(); r = (int)(r * dimFactor);
        int g = tc.getGreen(); g = (int)(g * dimFactor);
        int b = tc.getBlue(); b = (int)(b * dimFactor);
        return new TextColor.RGB(r,g,b);
    }
    static String getTimestamp() {
        LocalDateTime datetime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String text = datetime.format(formatter);
        return text;
    }
    static void tokenize(String String, String senderID) {
        String s = new String(String);
        while (s.length() > 0) {
            String sub = s.substring(0, Math.min(s.length(),Protocol.MAX_CHAR_PER_LINE));
            msgBlock.add(sub);
            msgBlockMapSender.put(sub, senderID);
            boolean stillHaveString = (s.length() > Protocol.MAX_CHAR_PER_LINE);
            s = stillHaveString ? s.substring(Protocol.MAX_CHAR_PER_LINE) : "";
        }
    }
    static int displayLine(String string, int offset, String senderID, boolean toEmphasize, String mType) {
        if (string.isEmpty()) return offset;
        tokenize(string, senderID);
        tg.setBackgroundColor(new TextColor.RGB(15,23,42));
        for (int i = msgBlock.size()-1; i >= 0 && (Protocol.ARENA_HEIGHT + 1-3-offset >= Protocol.BORDER + 1); i--, offset++) {    
            if (toEmphasize == true) tg.setForegroundColor(new TextColor.RGB(225,225,225)); 
            else tg.setForegroundColor(new TextColor.RGB(150,150,150));                    
            
            int textY = (Protocol.ARENA_HEIGHT + 1-3-offset-Protocol.BORDER-1);
            if (textY < 8) {
                float _dimFactor = Math.min(0.85f, (textY+1)/8.0f);
                TextColor _colorDimmed = getDimmed(new TextColor.RGB(225,225,225), _dimFactor);
                TextColor colorDimmedLoseFocus = getDimmed(_colorDimmed, _dimFactor * 0.65f);  
                if (toEmphasize == true) tg.setForegroundColor(_colorDimmed); 
                else tg.setForegroundColor(colorDimmedLoseFocus);             
            } 
    
            boolean isGacha = (mType.indexOf("G_") != -1); // add blink
            if (isGacha) tg.setBackgroundColor(new TextColor.RGB(40,20,60)); // purple for gacha
            else tg.setBackgroundColor(new TextColor.RGB(15, 23, 42)); // base

            // Main text
            
            tg.putString(ARENA_WIDTH+3, Protocol.ARENA_HEIGHT + 1-3-offset, msgBlock.get(i));
            int upToColon = msgBlock.get(i).indexOf(": ");
            if (upToColon == -1) continue;

            // Colored text
            
            TextColor color = getColor(senderID), bkgColor = getLERP(new TextColor.RGB(15,23,45), getDimmed(color, 0.2f), 0.5f);
                
            if (GameClient.playerID.equals(senderID)) {
                color = new TextColor.RGB(255,255,255);
                bkgColor = new TextColor.RGB(25,33,82);        
            }

            float dimFactor = 0.9f;            
            TextColor colorDimmed = getDimmed(color, dimFactor);
            
            if (textY < 8) {
                dimFactor = Math.min(0.85f, (textY+1)/8.0f);
                colorDimmed = getDimmed(color, dimFactor);
                TextColor colorDimmedLoseFocus = getDimmed(colorDimmed, dimFactor * 0.65f);  
            if (toEmphasize == true) tg.setForegroundColor(colorDimmed); 
                else tg.setForegroundColor(colorDimmedLoseFocus);             
            } else {
                if (toEmphasize == true) tg.setForegroundColor(color); 
                else tg.setForegroundColor(colorDimmed);             
            }

            tg.setBackgroundColor(getLERP(new TextColor.RGB(15,23,45), bkgColor, dimFactor));        
            
            String id = msgBlockMapSender.get(msgBlock.get(i));
            String colored = msgBlock.get(i).substring(0, upToColon + 1);

            tg.putString(ARENA_WIDTH+3, Protocol.ARENA_HEIGHT + 1-3-offset, colored, SGR.valueOf("BOLD"));
            tg.setBackgroundColor(new TextColor.RGB(15,23,42));
        }
        msgBlock.clear();
        return offset;
    }
    static synchronized void render(boolean toEmphasize) {
        // box
        tg.setBackgroundColor(new TextColor.RGB(15,23,42));
        tg.fillRectangle(new TerminalPosition(ARENA_WIDTH+1,0), new TerminalSize(SIDEBAR_WIDTH + 3 - 4, Protocol.ARENA_HEIGHT + 1),' ');

        // input box
        if (toEmphasize == true) tg.setForegroundColor(new TextColor.RGB(255,255,255));        
        else tg.setForegroundColor(new TextColor.RGB(45, 53, 72));              
        tg.putString(ARENA_WIDTH+3, Protocol.ARENA_HEIGHT, "> ");

        // msg here
        //tg.setForegroundColor(new TextColor.RGB(255,255,255));
        int txtWidth = msgBuffer.length();
        int txtOffset = (txtWidth > maxTxtWidth) ? (txtWidth-maxTxtWidth) : 0;
        
        if (toEmphasize == true) tg.setForegroundColor(new TextColor.RGB(255,255,255));        
        else tg.setForegroundColor(new TextColor.RGB(80,90,125));                
        String display = msgBuffer.substring(txtOffset, Math.min(maxTxtWidth + txtOffset, txtWidth));
        tg.putString(ARENA_WIDTH+5, Protocol.ARENA_HEIGHT, display);

        
        int freq = 15;
        tick = (++tick) % freq;
        boolean tick0 = (tick == 0);
        blink = blink ^ tick0;

        String cursorChar = ((cursor+txtOffset) < txtWidth) ? (msgBuffer.charAt(cursor+txtOffset) + "") : (" ");

        if (toEmphasize == true && blink && cursorChar == " ") { 
            tg.setForegroundColor(new TextColor.RGB(255,255,255));
            tg.setBackgroundColor(new TextColor.RGB(15,23,42));        
        }        
        else if (toEmphasize == true) { 
            tg.setBackgroundColor(new TextColor.RGB(255,255,255));
            tg.setForegroundColor(new TextColor.RGB(15,23,42));        
        }
        else {
            tg.setBackgroundColor(new TextColor.RGB(15,23,42));
            tg.setForegroundColor(new TextColor.RGB(45,53,72));              
        }
               
        tg.putString(ARENA_WIDTH+5+cursor, Protocol.ARENA_HEIGHT, cursorChar);

        tg.setBackgroundColor(new TextColor.RGB(15,23,42));
        if (toEmphasize == true) tg.setForegroundColor(new TextColor.RGB(225,225,225));        
        else tg.setForegroundColor(new TextColor.RGB(80,90,125));        
        
        // chats: prize and nonPrize
          tg.drawLine(
                new TerminalPosition(ARENA_WIDTH+1, Protocol.ARENA_HEIGHT + 1 + 0),
                new TerminalPosition(ARENA_WIDTH+1 + SIDEBAR_WIDTH - 1, Protocol.ARENA_HEIGHT + 1 + 0),
                '─'
            );
          tg.drawLine(
                new TerminalPosition(ARENA_WIDTH+1, 0),
                new TerminalPosition(ARENA_WIDTH+1 + SIDEBAR_WIDTH - 1, 0),
                '─'
            );            
            
        tg.drawLine(
            new TerminalPosition(ARENA_WIDTH+1 + SIDEBAR_WIDTH - 1, 0),
            new TerminalPosition(ARENA_WIDTH+1 + SIDEBAR_WIDTH - 1, Protocol.ARENA_HEIGHT + 1 + 0),
            '│'
        );
        tg.drawLine(
            new TerminalPosition(ARENA_WIDTH+1, 0),
            new TerminalPosition(ARENA_WIDTH+1, Protocol.ARENA_HEIGHT + 1 + 0),
            '│'
        );
          tg.putString(ARENA_WIDTH+1,0,"┌");
          tg.putString(ARENA_WIDTH+1 + SIDEBAR_WIDTH - 1,0,"┐");
          tg.putString(ARENA_WIDTH+1,Protocol.ARENA_HEIGHT + 1 + 0,"└");
          tg.putString(ARENA_WIDTH+1 + SIDEBAR_WIDTH - 1,Protocol.ARENA_HEIGHT + 1 + 0,"┘");

          
        if (toEmphasize == true) tg.setForegroundColor(new TextColor.RGB(255,255,255));        
        else tg.setForegroundColor(new TextColor.RGB(100,110,145));        
          int titleCenter = (ARENA_WIDTH+1) + (SIDEBAR_WIDTH - 1)/2 - 10/2;
          tg.putString(titleCenter,0,"{{ CHAT }}");          

          if (toEmphasize == true) tg.setForegroundColor(new TextColor.RGB(140, 150, 209));        
          else tg.setForegroundColor(new TextColor.RGB(45, 53, 72));        
        
          tg.drawLine(
                new TerminalPosition(ARENA_WIDTH+2, Protocol.ARENA_HEIGHT + 1 - 2),
                new TerminalPosition(ARENA_WIDTH+1 + SIDEBAR_WIDTH - 2, Protocol.ARENA_HEIGHT + 1 - 2),
                '─'
            );           

        if (toEmphasize == true) tg.setForegroundColor(new TextColor.RGB(255,255,255)); 
        else tg.setForegroundColor(new TextColor.RGB(150,150,150));             
        int offset = 0;
        for (int i = msgQ.size() - 1; i >= 0; i--) {
            String s = formatNormal(msgQ.get(i));
            String senderID = msgQ.get(i).optString("id");
            offset = displayLine(s, offset, senderID, toEmphasize, msgQ.get(i).optString("eventType"));
        }
    }
}
