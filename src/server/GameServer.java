package server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.PriorityQueue;

import org.json.*;
import org.json.JSONObject;
import org.json.JSONException;

import shared.*;

// called ONCE, no multiple isntances!!!!!
public class GameServer {
    static List < ClientHandler > clients = new CopyOnWriteArrayList < ClientHandler > ();
    static GameState gameState = new GameState();    
    static Random r = new Random();

    private static JSONObject getTopFive(List<Player> players) {
        PriorityQueue<Player> prq = new PriorityQueue<>(Collections.reverseOrder());
        // dead player also gets their trophy
        for (Player p : players) prq.add(p);

        JSONObject jao = new JSONObject()
        .put("type", "LEADERBOARD");
        JSONArray ja = new JSONArray();

        for (int i = 0; (i < 5 && (prq.peek() != null)); i++) {
            Player player = prq.poll();
            JSONObject j = new JSONObject()
            .put("id", player.id)
            .put("name", player.name)
            .put("type", player.type)
            .put("score", player.score);
            ja.put(j);
        }
        
        jao.put("players", ja);
        return jao;
    }

    static synchronized void processIncomingPlayerRequests() {
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
            new Thread(() -> { while (true) { try {
                long start = System.currentTimeMillis();

                // == Mutate GameState ==
                // do sth pls *poke stick* :[

                // keystroke and shit is in here
                // see what player needs b4 incr tick?
                processIncomingPlayerRequests();

                // Tick increase
                gameState.updateTick();
                
                // bullet movement and death
                for (Bullet bullet : gameState.bullets) {
                    bullet.pos.iHaveValidatedB4Setting();
                    if (!bullet.dead()) {
                        bullet.pos.accum(bullet.vy, bullet.vx);
                        bullet.timeLeft(bullet.timeLeft() - 1);
                    }
                    else
                        // hide corpses for now
                        bullet.pos.set(-69, -420);
                }

//                gameState.bullets.removeIf(b -> b.timeLeft <= 0);
                
                // enemy stuff
                if (gameState.getCurrentTick() % Protocol.WAVE_INTERVAL == 0)
                    gameState.spawnWave(2);
                gameState.updateEnemies();

                // now, pos uodated, we do collision check and porcess
                gameState.processAllCollisions();

                // revive
                for (Player p : gameState.players) {
                    p.uptickHitCooldown();
                    if (!p.dead()) continue;
                    p.hp.resuscitate().deathTickUp();
                    p.pos.set(p.spawnPos.getRenderY(), p.spawnPos.getRenderX());
                }
                
                // == Encode result ==
                // -- NO MORE CHANGING GameState AFTER THIS --
                
                // after state mutate, send it back to the
                // country where it came from
                // broadcast PLAYER STATUS
                JSONObject stateArrayJSON = new JSONObject()
                .put("type", "ENTITY_STATE")
                .put("tickCounter", gameState.getCurrentTick())
                .put("waveNumber", gameState.getWaveLevel())
                .put("levelTimer", gameState.getLevelTimeLeft());

                JSONArray bulletArray = new JSONArray();
                JSONArray playerArray = new JSONArray();
                JSONArray enemyArray = new JSONArray();                           
                // TEMPORSRY!!!!!!!
                // Bullet
                for (Bullet bullet : gameState.bullets) {
                    if (bullet.dead()) continue;                
                    bulletArray.put(new JSONObject()
                    .put("direction", bullet.direction)
                    .put("type", bullet.type)        
                    .put("x", bullet.pos.getRenderX())
                    .put("y", bullet.pos.getRenderY()));
                }
                
                // player info
                for (Player player : gameState.players) {
                    if (player.dead()) continue;
                    playerArray.put(new JSONObject()
                    .put("id", player.id)
                    .put("name", player.name)
                    .put("type", player.type)
                    .put("x", player.pos.getRenderX())
                    .put("y", player.pos.getRenderY())
                    .put("hp", player.hp.getHP())
                    .put("hp_max", player.hp._hpMax)
                    .put("score", player.score)
                    .put("direction", player.direction));
                }
                                    
                // enemy info
                for (Enemy enemy : gameState.enemies) {
                    if (enemy.dead()) continue;                
                    enemyArray.put(new JSONObject()
                    .put("id", enemy.id)
                    .put("type", enemy.type)             
                    .put("x", enemy.pos.getRenderX())
                    .put("y", enemy.pos.getRenderY()));
                }

                stateArrayJSON.put("bullets", bulletArray);
                stateArrayJSON.put("players", playerArray);              
                stateArrayJSON.put("enemies", enemyArray);

                // broadcast entities
                broadcastAll(stateArrayJSON);

                // broadcast GENERAL STATUS
                long current = System.currentTimeMillis();
                String staleStatus = (current - start > 300000) ? "stale serv, pls restart" : "!!! L68 GameServer.java DUMBASS !!!";
                JSONObject staleStatusJSON = new JSONObject().put("type", "STATE").put("message", staleStatus);
                broadcastAll(staleStatusJSON);

                // sleep, or not if thread time exceeds 50ms
                long end = System.currentTimeMillis();
                Thread.sleep(Math.max(0, Protocol.TICK_MS - (end - start)));

                // if (gameState.getLevelTimeLeft() == 0) → broadcast GAME_OVER with winner (highest score) → write leaderboard.txt → (eventually) close sockets
                // switch OVER state
                if (gameState.getLevelTimeLeft() != 0) continue;
                broadcastAll(getTopFive(gameState.players));
                Thread.sleep(Protocol.TICK_MS * 10);
                System.exit(0); // placeholder
            } catch (Exception e) {
                System.out.println("Exception caught GameServer broadcast thread: " + e);
                e.printStackTrace();
            }}}).start();

            while (true) {
                Socket s = ss.accept();
                ClientHandler ch = new ClientHandler(s);
                new Thread(ch).start();
                System.out.println("Client connected!");
            }
        } catch (Exception e) {
            System.out.println("Exception caught GameServer socket main thread: " + e);
            e.printStackTrace();
        }
    }
}
