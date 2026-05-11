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
    static volatile String msgBuffer = "";
    static volatile int scrollOffset = 0, renderOffset = 0; // from bottom up
    static CopyOnWriteArrayList<String> msgBlock = new CopyOnWriteArrayList<String>();
    static CopyOnWriteArrayList<JSONObject> msgQ = new CopyOnWriteArrayList<JSONObject>();    
    static TextGraphics tg = GameClient.screen.newTextGraphics();
    static synchronized void send(String msg, String id, String name) {
        JSONObject msgJSON = new JSONObject()
        .put("type", "CHAT")
        .put("msg", msg)
        .put("id", id)
        .put("name", name);
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
        String msgFormatted = "[" + timestamp + "] " + playerName + ((playerName.length()>=10)?"...":"") + ": " + msg;           
        return msgFormatted;
    }
/*    static synchronized String formatPrize(JSONObject obj) {

    }*/
    static TextColor getColor(String playerID) {
        int r = 255, g = 255, b = 255;
        if (!GameClient.playerColor.containsKey(playerID)) { do {
            int hashColor = playerID.hashCode();
            r = ((((hashColor & 0xFF0000) >> 16) << 16) | 0x40) % 255;
            g = ((((hashColor & 0x00FF00) >> 8) << 8) | 0x40) % 255;
            b = (((hashColor & 0x0000FF)) | 0x40) % 255;
            TextColor color = new TextColor.RGB(r, g, b);
            GameClient.playerColor.put(playerID, color);
        } while (r < 240 && r > 190 && g < 240 && g > 190 && b < 240 && b > 190); }
        return GameClient.playerColor.get(playerID);
    }
    static String getTimestamp() {
        LocalDateTime datetime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String text = datetime.format(formatter);
        return text;
    }
    static void tokenize(String string) {
        String s = new String(string);
        while (s.length() > 0) {
            String sub = s.substring(0, Math.min(s.length(),Protocol.MAX_CHAR_PER_LINE));
            msgBlock.add(sub);
            boolean stillHaveString = (s.length() > Protocol.MAX_CHAR_PER_LINE);
            s = stillHaveString ? s.substring(Protocol.MAX_CHAR_PER_LINE) : "";
        }
    }
    static int displayLine(String string, int offset) {
        if (string.isEmpty()) return offset;
        tokenize(string);
        tg.setBackgroundColor(new TextColor.RGB(15,23,42));
        for (int i = msgBlock.size()-1; i >= 0 && (Protocol.ARENA_HEIGHT + 1-3-offset >= Protocol.BORDER + 1); i--, offset++) {
            int textY = (Protocol.ARENA_HEIGHT + 1-3-offset-Protocol.BORDER-1);
            if (textY == 0) tg.setForegroundColor(new TextColor.RGB(55, 62, 80));
            else if (textY == 1) tg.setForegroundColor(new TextColor.RGB(95, 100, 115));
            else if (textY == 2) tg.setForegroundColor(new TextColor.RGB(135, 139, 150));
            else if (textY == 3) tg.setForegroundColor(new TextColor.RGB(175, 178, 185));
            else if (textY == 4) tg.setForegroundColor(new TextColor.RGB(215, 216, 220));
            else if (textY == 5) tg.setForegroundColor(new TextColor.RGB(255, 255, 255));
            tg.putString(Protocol.ARENA_WIDTH+3, Protocol.ARENA_HEIGHT + 1-4-offset, msgBlock.get(i));
        }
        msgBlock.clear();
        return offset;
    }
    static synchronized void render(boolean toEmphasize) {
        // box
        tg.setBackgroundColor(new TextColor.RGB(15,23,42));
        if (toEmphasize == true) tg.setForegroundColor(new TextColor.RGB(255,255,255));        
        else tg.setForegroundColor(new TextColor.RGB(120,130,165));        
        tg.fillRectangle(new TerminalPosition(Protocol.ARENA_WIDTH+1,0), new TerminalSize(Protocol.SIDEBAR_WIDTH - 4, Protocol.ARENA_HEIGHT + 1),' ');
        // input box
        // chats: prize and nonPrize
          tg.drawLine(
                new TerminalPosition(Protocol.ARENA_WIDTH+1, Protocol.ARENA_HEIGHT + 1 + 0),
                new TerminalPosition(Protocol.ARENA_WIDTH+1 + Protocol.SIDEBAR_WIDTH - 1, Protocol.ARENA_HEIGHT + 1 + 0),
                '─'
            );
          tg.drawLine(
                new TerminalPosition(Protocol.ARENA_WIDTH+1, 0),
                new TerminalPosition(Protocol.ARENA_WIDTH+1 + Protocol.SIDEBAR_WIDTH - 1, 0),
                '─'
            );            
            
        tg.drawLine(
            new TerminalPosition(Protocol.ARENA_WIDTH+1 + Protocol.SIDEBAR_WIDTH - 1, 0),
            new TerminalPosition(Protocol.ARENA_WIDTH+1 + Protocol.SIDEBAR_WIDTH - 1, Protocol.ARENA_HEIGHT + 1 + 0),
            '│'
        );
        tg.drawLine(
            new TerminalPosition(Protocol.ARENA_WIDTH+1, 0),
            new TerminalPosition(Protocol.ARENA_WIDTH+1, Protocol.ARENA_HEIGHT + 1 + 0),
            '│'
        );
          tg.putString(Protocol.ARENA_WIDTH+1,0,"┌");
          tg.putString(Protocol.ARENA_WIDTH+1 + Protocol.SIDEBAR_WIDTH - 1,0,"┐");
          tg.putString(Protocol.ARENA_WIDTH+1,Protocol.ARENA_HEIGHT + 1 + 0,"└");
          tg.putString(Protocol.ARENA_WIDTH+1 + Protocol.SIDEBAR_WIDTH - 1,Protocol.ARENA_HEIGHT + 1 + 0,"┘");

          if (toEmphasize == true) tg.setForegroundColor(new TextColor.RGB(136, 152, 189));        
          else tg.setForegroundColor(new TextColor.RGB(70,95,110));        
        
          tg.drawLine(
                new TerminalPosition(Protocol.ARENA_WIDTH+2, Protocol.ARENA_HEIGHT + 1 - 3),
                new TerminalPosition(Protocol.ARENA_WIDTH+1 + Protocol.SIDEBAR_WIDTH - 2, Protocol.ARENA_HEIGHT + 1 - 3),
                '─'
            );           

        if (toEmphasize == true) tg.setForegroundColor(new TextColor.RGB(255,255,255)); 
        else tg.setForegroundColor(new TextColor.RGB(170,170,170));             
        int offset = 0;
        for (int i = msgQ.size() - 1; i >= 0; i--) {
            String s = formatNormal(msgQ.get(i));
            offset = displayLine(s, offset);
        }
    }
}
