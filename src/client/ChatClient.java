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
    static volatile String msgBuffer;
    static volatile int scrollOffset = 0, renderOffset = 0; // from bottom up
    static ConcurrentLinkedQueue<String> msgBlock = new ConcurrentLinkedQueue<String>();
    static ConcurrentLinkedQueue<JSONObject> msgQ = new ConcurrentLinkedQueue<JSONObject>();    
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
    static void displayLine(String string, int offset) {
        if (string.isEmpty()) return offset;
        tokenize(string);
        tg.setBackgroundColor(new TextColor.RGB(15,23,42));
        while (msgBlock.peek() != null) {
            tg.putString(Protocol.ARENA_WIDTH+3, Protocol.ARENA_HEIGHT-3-offset++, msgBlock.poll());
        }
        return offset;
    }
    static synchronized void render() {
        // box
        // input box
        // chats: prize and nonPrize
        String s = formatNormal(msgQ.peek());
        displayLine(s, renderOffset);
    }
}
