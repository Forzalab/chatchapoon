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
            String line = reader.readLine();
            JSONObject j = new JSONObject(line);
            String _type = j.getString("type");
            String _playerId = j.getString("playerId");

            String ackState = new JSONObject().put("type", "JOIN_ACK").put("playerId", _playerId).put("color", 14).toString();
            send(ackState);

            while ((line = reader.readLine()) != null) {
                incoming.add(line);
            }
        } catch (Exception e) {
            System.out.println("Exception thrown ClientHandler main: " + e);
        }
    }
}
