package client;

import java.util.*;
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
        String playerName = obj.optString("name");
        String msg = obj.optString("msg");
        return "";
    }
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
}
