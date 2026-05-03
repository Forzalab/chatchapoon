package server;

import java.net.*;
import java.io.*;
import shared.Protocol;

public class GameServer {
    public static void main(String[] args) throws Exception {
        // socket handshake
    	try {
    		ServerSocket ss = new ServerSocket(Protocol.PORT);
    	    System.out.println("Starting on port " + Protocol.PORT);
    		while (true) {
    			Socket s = ss.accept();
    			new Thread(new ClientHandler(s)).start();
    		    System.out.println("Client connected!");
    		}
        } catch (Exception e) {
        		System.out.println("Exception caught: " + e);
        }
    }
}
