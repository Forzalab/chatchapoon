package server;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.json.JSONObject;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;


    // player data for CH "branch"
    private String playerId = "";
    private String playerName = "";
    
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // server msg mailbox queue for each socket
    ConcurrentLinkedQueue < String > incoming = new ConcurrentLinkedQueue < > ();

    // "push" a msg line into writer
    public synchronized void send(String msg) {
        if (writer != null) writer.println(msg);
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

            String ackState = new JSONObject().put("type", "JOIN_ACK").put("playerId", playerId).put("color", 0).toString();
            send(ackState);

            GameServer.clients.add(this); // GameServer::clients lol

            // now await cli forevarrr
            while ((line = reader.readLine()) != null) {
                incoming.add(line);
            }

        } catch (Exception e) {
            System.out.println("Exception thrown ClientHandler main: " + e);
        } finally {
            GameServer.clients.remove(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
