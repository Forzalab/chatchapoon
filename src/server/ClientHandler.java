package server;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.json.JSONObject;

import shared.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    
    // player data for CH "branch"
    private String playerId = "";
    private String playerName = "";
    private int playerIndex = -1;

        
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // server msg mailbox queue for each socket
    ConcurrentLinkedQueue < String > incoming = new ConcurrentLinkedQueue < > ();

    // "push" a msg line into writer
    public synchronized void send(String msg) {
        if (writer == null) return;
        if (writer != null) writer.println(msg);
        if (writer.checkError()) writer.flush();
    }

    private synchronized void addPlayer() {
        // name is managed by CH, player is id-ed by id.
        Player player = new Player(new Position(GameServer.r.nextInt(Protocol.ARENA_WIDTH), GameServer.r.nextInt( Protocol.ARENA_HEIGHT)), 0, 0, playerId, Protocol.PLAYER_MAX_HP);
        GameServer.gameState.players.add(player);
        playerIndex = GameServer.gameState.players.indexOf(player);
        GameServer.gameState.playerIdMap.put(playerId, player);
        // colorTaken handling
    }

    private synchronized void removePlayer() {
        GameServer.gameState.players.removeIf(p -> p.id.equals(playerId));
    }
    
    public void run() {
        try {
            // write to client
            OutputStream ostream = socket.getOutputStream();
            writer = new PrintWriter(ostream, true);

            // listen to client
            InputStream istream = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(istream));

            // enter the queueing ( msg is feed thru send() )
            // pack msg onto the cli
            String line = reader.readLine();
            JSONObject j = new JSONObject(line);
            String _type = j.getString("type");

            this.playerId = j.getString("playerId");
            this.playerName = j.optString("name", "anon");

            // force disconnect late player
/*            if (GameServer.gameState.state == GameState.State.BATTLE) {
                String rjtState = new JSONObject().put("type", "JOIN_REJECT").put("playerId", playerId).toString();
                send(rjtState);
                try { socket.close(); } catch (IOException ignored) {}
                return;
            }
*/            
            String ackState = new JSONObject().put("type", "JOIN_ACK").put("playerId", playerId).put("color", 0).toString();
            send(ackState);

            GameServer.clients.add(this); // GameServer::clients lol, CH
            addPlayer(); // add into GameState list
            
            // now await cli forevarrr
            while ((line = reader.readLine()) != null) {
                incoming.add(line);
            }

        } catch (Exception e) {
            System.out.println("Exception thrown ClientHandler main: " + e);
        } finally {
            GameServer.clients.remove(this);
            removePlayer();
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
