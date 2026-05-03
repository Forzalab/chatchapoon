package client;

import java.net.*;
import java.util.*;
import java.io.*;
import org.json.JSONObject;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.*;

import shared.Protocol;

// each client screen = ONE file run
// so everything is static
public class GameClient {
    // Render
    private static int cols = 0, rows = 0;
    private static Screen screen;
    private static int shift = 0;
    private static String to_render = "";

    // Sockets
    private static Socket socket;
    private static PrintWriter writer;
    private static BufferedReader reader;

    private static void draw_unfit_screen() {
        try {
            TextGraphics tg = screen.newTextGraphics();
            while (true) {
                // render
                tg.setBackgroundColor(new TextColor.RGB(255,255,255));
                tg.setForegroundColor(new TextColor.RGB(0,0,0));
                tg.drawRectangle(new TerminalPosition (2,2), new TerminalSize(cols - 2 - 2, rows - 2 - 2), '+');
                tg.setBackgroundColor(new TextColor.RGB(160,0,0));
                tg.setForegroundColor(new TextColor.RGB(255,255,255));
                tg.putString(cols/3, rows/2 - 1, "uwu putty scween too smol :3");
                tg.putString(cols/3, rows/2, "need at least "+Protocol.MIN_COLS+"x"+Protocol.MIN_ROWS+", rn is " + cols + "x" + rows);
                tg.putString(cols/3, rows/2 + 1, "[ PRESS ANY KEY TO EXIT ]");              

                // keystroke
                screen.refresh();
                if (screen.readInput().getKeyType() != null) break;
                Thread.sleep(Protocol.TICK_MS);
            }
            
            // cease
            screen.stopScreen();
            System.exit(0);
        }
        catch (Exception e) {
            System.out.println("Exception caught: " + e);
        }
    }
    
    private static void lanterna_init() {
        try {
            // bureaucracy
            DefaultTerminalFactory factory = new DefaultTerminalFactory();
            screen = factory.createScreen();
            screen.startScreen();

            // size
            TerminalSize sz = screen.getTerminalSize();
            cols = sz.getColumns();
            rows = sz.getRows();

            // cleanup screen features
            screen.clear();
            screen.setCursorPosition(null); // hides cursor
            
            if (cols < Protocol.MIN_COLS || rows < Protocol.MIN_ROWS) {
                draw_unfit_screen();
            }
        } catch (Exception e) {
              System.out.println("Exception caught: " + e);
        }
    }
    
    public static void main(String[] args) {
        try {
            lanterna_init();
            
            socket = new Socket("localhost", Protocol.PORT);

            // send to server
            OutputStream ostream = socket.getOutputStream();
            writer = new PrintWriter(ostream, true);

            // listen to server
            InputStream istream = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(istream));

            // join the server by sending a request first
            String join = new JSONObject().put("type","JOIN").put("playerId","test").put("cols",cols).put("rows",rows).toString();
            writer.println(join);

//            String to_render = "";
            TextGraphics tg = screen.newTextGraphics();
//            int shift = 0;
            
            // text i/o
            Thread listener = new Thread(() -> {
                try {
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                    /// MUST CHANGE TO STH ELSE IG
                        JSONObject j = new JSONObject(line);
                        String _type = j.getString("type");
                        int _cols = j.optInt("cols", 60);
                        to_render = _type + cols;
                        shift++;
                    } 
                } catch (Exception e) {
                    System.out.println("Exception caught: " + e);
                }
            });

            listener.start();
            
            // --- keystroke-render loop ---
            while (true) {
                tg.putString(cols/3, rows/2 - 1 + shift, to_render);
                KeyStroke keystroke = screen.pollInput();
                if (keystroke != null && keystroke.getKeyType() == KeyType.Escape) {
                    screen.stopScreen();
                    listener.interrupt();
                    break;
                }
                screen.refresh();
                Thread.sleep(Protocol.TICK_MS);
            }
         } catch (Exception e) {
               System.out.println("Exception caught: " + e);
         }
    }    
}
