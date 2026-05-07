package server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.json.*;
import org.json.JSONObject;
import org.json.JSONException;

import shared.*;

// called ONCE, no multiple isntances!!!!!
public class GameServer {
    static List < ClientHandler > clients = new CopyOnWriteArrayList < ClientHandler > ();
    static GameState gameState = new GameState();    
    static Random r = new Random();

    static synchronized void processIncomingQueue() {
        for (ClientHandler _ch: clients) { String msg; while ((msg = _ch.incoming.poll()) != null) {
            // process _ch msg here
            if (!Utility.isJSONValid(msg)) continue;

            JSONObject json = new JSONObject(msg);
            String type = json.getString("type");

            if (type.equals("INPUT")) {
                String cmd = json.getString("key");
                String authorID = json.getString("playerId");
                alterState(cmd, authorID);
            }
            else if (type.equals("LEAVE")) {
                broadcastAll(msg); // relay old msg
                System.out.println("Client disconnected!");
            }
        }
     }}

    static synchronized void alterState(String cmd, String authorID) {
           // game loop here ig
           // indirection to GameState
           System.out.println("[" + authorID + "] sent key: " + cmd);
           gameState.alterState(cmd, authorID);
    }
    
    // selective filter done at cli-level
    // must be JSON
    static synchronized void broadcastAll(String msg) {
        if (!Utility.isJSONValid(msg)) return; // crash at bad JSON
        for (ClientHandler _ch: clients) {
            _ch.send(msg);
        }
    }

    static synchronized void broadcastAll(JSONObject j) {
        broadcastAll(j.toString());
    }
    
    public static void main(String[] args) throws Exception {
        // socket handshake
        try {
            ServerSocket ss = new ServerSocket(Protocol.PORT);
            System.out.println("Starting on port " + Protocol.PORT);

            // thread processQ THEN broadcast - SEPERATE from socket handlin'
            // wont be slow
            new Thread(() -> {
            while (true) { try {
                    long start = System.currentTimeMillis();

                    // process Q
                    processIncomingQueue();

                    // GameState placeholder, do sth pls *poke stick* :[

                    // after state mutate, send it back to the
                    // country where it came from
                    // broadcast PLAYER STATUS
                    JSONObject playerStateArrayJSON = new JSONObject();
                    JSONArray playerStateArray = new JSONArray();
                    for (Player player : gameState.players) {
                        JSONObject playerState = new JSONObject();
                        playerState.put("id", player.id);
                        playerState.put("x", player.pos.getRenderX());
                        playerState.put("y", player.pos.getRenderY());
                        playerState.put("hp", player.hp.getHP());
                        playerState.put("hp_max", player.hp._hpMax);
                        playerState.put("direction", player.direction.toString());
                        playerStateArray.put(playerState);
                    }
                    playerStateArrayJSON.put("type", "PLAYER_INFO");
                    playerStateArrayJSON.put("players", playerStateArray);
                    broadcastAll(playerStateArrayJSON);

                    // broadcast GENERAL STATUS
                    long current = System.currentTimeMillis();
                    String staleStatus = (current - start > 300000) ? "stale serv, pls restart" : "!!! L68 GameServer.java DUMBASS !!!";
                    JSONObject staleStatusJSON = new JSONObject().put("type", "STATE").put("message", staleStatus);
                    broadcastAll(staleStatusJSON);

                    // sleep, or not if thread time exceeds 50ms
                    long end = System.currentTimeMillis();
                    Thread.sleep(Math.max(0, Protocol.TICK_MS - (end - start)));
            } catch (Exception e) {
                System.out.println("Exception caught GameServer broadcast thread: " + e);
            }}}).start();

            while (true) {
                Socket s = ss.accept();
                ClientHandler ch = new ClientHandler(s);
                new Thread(ch).start();
                System.out.println("Client connected!");
            }
        } catch (Exception e) {
            System.out.println("Exception caught GameServer socket main thread: " + e);
        }
    }
}
