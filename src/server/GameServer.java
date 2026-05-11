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
    public static long startTimeLobby = System.currentTimeMillis();    
    static volatile GameState currentGameState = new GameState(startTimeLobby);    
    static Random r = new Random();
    static boolean changed = false;
//    static volatile long currentTime = System.currentTimeMillis();    
    private static JSONObject getTopFive(List<Player> players) {
        PriorityQueue<Player> prq = new PriorityQueue<>((a, b) -> a.score - b.score);
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
    
    public static synchronized void send(String msg, String id, String name, String eventType) {
        JSONObject msgJSON = new JSONObject()
        .put("type", "CHAT")
        .put("msg", msg)
        .put("id", id)
        .put("name", name);
        String msgJSONString = msgJSON.toString();
        broadcastAll(msgJSONString);
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
                     
                if (cmd.equals("PULL")) {
                    Player p = currentGameState.playerIdMap.get(authorID);
                    if (p == null) continue;
                    String rarity = currentGameState.pull(p);
                    if (rarity.isEmpty()) continue;
                    GameServer.send("❗❗ I pulled a " + rarity +  " loot ❗❗", p.id, p.name, rarity);
                }
            }
            else if (type.equals("CHAT")) {
                broadcastAll(msg); // relay old msg
            } 
            else if (type.equals("LEAVE")) {
                System.out.println("Client disconnected!");
                broadcastAll(msg); // relay old msg
            }
        }
     }}

    static synchronized void alterState(String cmd, String authorID) {
           // game loop here ig
           // indirection to GameState
           System.out.println("[" + authorID + "] sent key: " + cmd);
           currentGameState.alterState(cmd, authorID);
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

    static synchronized void checkBroadcastMilestone(String mtype, Player p) { 
        String milestone = p.checkAddMilestone(mtype);
        if (!"HAD".equals(milestone) && !"UKN".equals(milestone)) send(milestone, p.id, p.name, mtype);
//        if ("UKN".equals(milestone)) send(milestone, p.id, p.name, milestone);        
    }

    public static synchronized void lobby() {
            // state swotch
            long currentTime = System.currentTimeMillis();
            if (currentGameState.players.size() == Protocol.MAX_PLAYERS && !changed) {
                startTimeLobby = currentTime - Protocol.LOBBY_CLOSE_IN;
                changed = true;
            }

            // 40 > 20 + 30 false 40 = 20 + 20
            // 65 > 60 + 25 false 65 = 60 + 5 
            boolean lobbyWaitEnds = (currentTime > Protocol.LOBBY_CLOSE_IN + startTimeLobby);
            boolean enoughPlayersJoined = currentGameState.players.size() > Protocol.MAX_PLAYERS;

            if (lobbyWaitEnds || enoughPlayersJoined) {
                currentGameState.switchNextState();
            }

            if (currentGameState.getPrev() == GameState.State.LOBBY
            && currentGameState.get() == GameState.State.BATTLE)
                return;
            
            // broadcast HOW MANY PLAYERS WAITING
            JSONObject jao = new JSONObject();
            jao.put("type", "LOBBY");

            JSONArray ja = new JSONArray();
            for (Player player : currentGameState.players) {
                JSONObject j = new JSONObject()
                .put("id", player.id)
                .put("name", player.name);
                ja.put(j);
            }

            jao.put("players", ja);
            broadcastAll(jao);


            // sleep, or not if thread time exceeds 50ms
    }
    
    public static void main(String[] args) throws Exception {
        // socket handshake
        try {
            ServerSocket ss = new ServerSocket(Protocol.PORT);
            System.out.println("Starting on port " + Protocol.PORT);

            // thread processQ THEN broadcast - SEPERATE from socket handlin'
            // wont be slow
            new Thread(() -> { 
            try { Thread.sleep(Protocol.TICK_MS * 100); } catch (Exception e) {}
            while (true) { try {
                long start = System.currentTimeMillis();
                if (currentGameState.get() == GameState.State.POST_BATTLE) {
                    broadcastAll(getTopFive(currentGameState.players));
                    Thread.sleep(Protocol.TICK_MS);
                    continue;
                }
                // == Mutate GameState ==
                // do sth pls *poke stick* :[

                // keystroke and shit is in here
                // see what player needs b4 incr tick?
                processIncomingPlayerRequests();

                // Tick increase
                currentGameState.updateTick();

                        
                if (currentGameState.get() == GameState.State.LOBBY) {
                    lobby();
                    long end = System.currentTimeMillis();
                    Thread.sleep(Math.max(0, Protocol.TICK_MS - (end - start)));
                    continue;
                }
                
                // bullet movement and death
                for (Bullet bullet : currentGameState.bullets) {
                    bullet.pos.iHaveValidatedB4Setting();
                    if (!bullet.dead()) {
                        bullet.pos.accum(bullet.vy, bullet.vx);
                        bullet.timeLeft(bullet.timeLeft() - 1);
                    }
                    else
                        // hide corpses for now
                        bullet.pos.set(-69, -420);
                }

//                currentGameState.bullets.removeIf(b -> b.timeLeft <= 0);
                
                // enemy stuff
                if (currentGameState.getCurrentTick() % Protocol.WAVE_INTERVAL == 0)
                    currentGameState.spawnWave(6);
                currentGameState.updateEnemies();

                // now, pos uodated, we do collision check and porcess
                currentGameState.processAllCollisions();

                // revive
                // anything players really
                for (Player p : currentGameState.players) {
                    p.uptickHitCooldown();
                    if (p.fireCooldown > 0) p.fireCooldown--;
                    currentGameState.processCollectableCoin(p);
                    if (!p.dead()) continue;
                    p.hp.resuscitate().deathTickUp();
                    p.pos.set(p.spawnPos.getRenderY(), p.spawnPos.getRenderX());
                    p.bullets = 100;
                    checkBroadcastMilestone("NEW_GACHA", p);
                    checkBroadcastMilestone("NEAR_DEATH", p);                                        
                }
                
                // == Encode result ==
                // -- NO MORE CHANGING GameState AFTER THIS --
                
                // after state mutate, send it back to the
                // country where it came from
                // broadcast PLAYER STATUS
                JSONObject stateArrayJSON = new JSONObject()
                .put("type", "ENTITY_STATE")
                .put("tickCounter", currentGameState.getCurrentTick())
                .put("waveNumber", currentGameState.getWaveLevel())
                .put("levelTimer", currentGameState.getLevelTimeLeft());

                JSONArray bulletArray = new JSONArray();
                JSONArray playerArray = new JSONArray();
                JSONArray enemyArray = new JSONArray(); 
                JSONArray coinsArray = new JSONArray();
                            
                // TEMPORSRY!!!!!!!
                // Bullet
                for (Bullet bullet : currentGameState.bullets) {
                    if (bullet.dead()) continue;                
                    bulletArray.put(new JSONObject()
                    .put("direction", bullet.direction)
                    .put("ownerID", bullet.ownerID)
                    .put("type", bullet.type)        
                    .put("x", bullet.pos.getRenderX())
                    .put("y", bullet.pos.getRenderY()));
                }
                
                // player info
                for (Player player : currentGameState.players) {
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
                    .put("bullets", player.bullets)                    
                    .put("currency", player.currency)                    
                    .put("direction", player.direction));
                }
                                    
                // enemy info
                for (Enemy enemy : currentGameState.enemies) {
                    if (enemy.dead()) continue;                
                    enemyArray.put(new JSONObject()
                    .put("id", enemy.id)
                    .put("type", enemy.type)             
                    .put("x", enemy.pos.getRenderX())
                    .put("y", enemy.pos.getRenderY()));
                }

                for (Map.Entry<Position, Integer> entry : currentGameState.coinsLoc.entrySet()) {
                    Position key = entry.getKey();
                    int x = key.getRenderX();
                    int y = key.getRenderY();
                    coinsArray.put(new JSONObject()
                    .put("x", x)
                    .put("y", y));                    
//                    int amt = entry.getValue();
                    // now work with key and value...
                }              
                
                stateArrayJSON.put("bullets", bulletArray);
                stateArrayJSON.put("players", playerArray);              
                stateArrayJSON.put("enemies", enemyArray);
                stateArrayJSON.put("coins", coinsArray);                

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

                // if (currentGameState.getLevelTimeLeft() == 0) → broadcast GAME_OVER with winner (highest score) → write leaderboard.txt → (eventually) close sockets
                // switch OVER state
                
                if (currentGameState.getLevelTimeLeft() != 0) continue;
                currentGameState.switchNextState();
                broadcastAll(getTopFive(currentGameState.players));
                Thread.sleep(Protocol.TICK_MS);
//                System.exit(0); // placeholder
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
