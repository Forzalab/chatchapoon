package server;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // c2s
            InputStream istream = socket.getInputStream();
            BufferedReader cli_reader = new BufferedReader(new InputStreamReader(istream));

            // s2c
            OutputStream ostream = socket.getOutputStream();
            PrintWriter svr_writer = new PrintWriter(ostream, true);
            
            while (true) {
                String cli_line = cli_reader.readLine();
                System.out.println("[CLIENT] " + cli_line);
                if (svr_writer != null) {
                    Scanner scanner = new Scanner(System.in);
                    String svr_line = scanner.nextLine();
                    svr_writer.println("[SERVER] " + svr_line);
                }
            }
        }
        catch (Exception e) {
            System.out.println("Exception thrown: " + e);
        }
    }
}

