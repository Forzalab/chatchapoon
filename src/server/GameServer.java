package server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.json.*;
import org.json.JSONObject;
import org.json.JSONException;

import shared.Protocol;
import shared.GameState;

// called ONCE, no multiple isntances!!!!!
public class GameServer {
    static List < ClientHandler > clients = new CopyOnWriteArrayList < ClientHandler > ();
    static GameState gameState = new GameState();    
    static Random r = new Random();

    // https://stackoverflow.com/a/10174938
    public Boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                System.out.println("Exception caught GameServer parsing JSON: " + ex1);
            }
        }
        return true;
    }

    static synchronized void processIncomingQueue() {
        for (ClientHandler _ch: clients) {
            String msg;
            while ((msg = _ch.incoming.poll()) != null) {
                // process _ch msg here
            }
        }
    }

    // selective filter done at cli-level
    // must be JSON
    static synchronized void broadcastAll(String msg) {
        for (ClientHandler _ch: clients) {
            _ch.send(msg);
        }
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

                    // GameState placeholder
                    

                    // broadcast
                    long current = System.currentTimeMillis();
                    String fakeID = (current - start > 300000) ? "stale serv, pls restart" : "!!! L68 GameServer.java DUMBASS !!!";
                    String fakeState = new JSONObject().put("type", "STATE").put("playerId",fakeID).put("color", r.nextInt(100)).toString();
                    broadcastAll(fakeState);

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
