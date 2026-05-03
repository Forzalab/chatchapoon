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
		     	System.out.println("Client connected!");
//			ss.close();
		}
	}
	catch (Exception e) {
		System.out.println("Exception caught: " + e);
	}
    }
}
