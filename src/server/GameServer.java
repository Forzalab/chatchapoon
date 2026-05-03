package server;

import java.net.*;
import java.io.*;
import java.util.*;
import org.json.JSONObject;
import shared.Protocol;

// called ONCE, no multiple isntances!!!!!
public class GameServer {
    static List<ClientHandler> clients = new Vector<ClientHandler>();

    // JSONObject serves as struct

    public static void main(String[] args) throws Exception {
        // socket handshake
    	try {
    		ServerSocket ss = new ServerSocket(Protocol.PORT);
    	    System.out.println("Starting on port " + Protocol.PORT);
    		while (true) {
    			Socket s = ss.accept();
    			ClientHandler ch = new ClientHandler(s);
    			clients.add(ch);
    			
                new Thread(() -> {
                    while (true) {
                        try {
                            Thread.sleep(Protocol.TICK_MS);
                            String fakeState = new JSONObject().put("type","STATE").put("wave",1).toString();
                            for (ClientHandler _ch : clients) _ch.send(fakeState);
                        } catch (Exception e) {
                            System.out.println("Exception caught: " + e);
                        }
                    }
                }).start();

    		    System.out.println("Client connected!");
    		}
        } catch (Exception e) {
        		System.out.println("Exception caught: " + e);
        }
    }
}
