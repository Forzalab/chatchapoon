package client;

import java.util.*;
import org.json.*;

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
}
