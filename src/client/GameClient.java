package client;

import java.net.*;
import java.util.*;
import java.io.*;
import java.util.HashMap;

import org.json.JSONObject;
import org.json.JSONException;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.*;

import shared.*;

// each client screen = ONE file run
// so everything is static
// EVERYTHING IS STATIC!!!!!! NO INSTANCE VAR HERE!!!!!
public class GameClient {
    // Render
    private static int cols = 0, rows = 0;
    private static Screen screen;
    private static volatile int shift = 0; // volatile force update value for N threads potentially reading it
    private static volatile JSONObject to_render;

    // player info, local copy
    public static final String playerID = UUID.randomUUID().toString().substring(0,8);
    
    // Sockets
    private static Socket socket;
    private static PrintWriter writer;
    private static BufferedReader reader;

    // Magic lookup table
    // key mapping
    public static final HashMap<KeyStroke, String> KEYBIND_MAP = new HashMap<KeyStroke, String>() {{
        put(KeyStroke.fromString("w"), "UP");
        put(KeyStroke.fromString("a"), "LEFT");
        put(KeyStroke.fromString("s"), "DOWN");
        put(KeyStroke.fromString("d"), "RIGHT");
        put(KeyStroke.fromString("q"), "ROTATE_CCW");
        put(KeyStroke.fromString("e"), "ROTATE_CW");
        put(KeyStroke.fromString("<Space>"), "SHOOT");
    }};

    private static void draw_unfit_screen() {
        try {
            TextGraphics tg = screen.newTextGraphics();
            while (true) {
                // render
                tg.setBackgroundColor(new TextColor.RGB(255, 255, 255));
                tg.setForegroundColor(new TextColor.RGB(0, 0, 0));
                tg.drawRectangle(new TerminalPosition(2, 2), new TerminalSize(cols - 2 - 2, rows - 2 - 2), '+');
                tg.setBackgroundColor(new TextColor.RGB(160, 0, 0));
                tg.setForegroundColor(new TextColor.RGB(255, 255, 255));
                tg.putString(cols / 3, rows / 2 - 1, "uwu putty scween too smol :3");
                tg.putString(cols / 3, rows / 2, "need at least " + Protocol.MIN_COLS + "x" + Protocol.MIN_ROWS + ", rn is " + cols + "x" + rows);
                tg.putString(cols / 3, rows / 2 + 1, "[ PRESS ANY KEY TO EXIT ]");
                // keystroke
                screen.refresh();
                if (screen.readInput().getKeyType() != null) break;
                Thread.sleep(Protocol.TICK_MS);
            }

            // cease
            try { screen.stopScreen(); } catch (IOException ioe) {}
            System.exit(0);
        } catch (Exception e) {
            try { screen.stopScreen(); } catch (IOException ioe) {}
            System.out.println("Exception caught GameClient unfit_screen: " + e);
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
            try { screen.stopScreen(); } catch (IOException ioe) {}
            System.out.println("Exception caught GameClient lanterna_init(): " + e);
        }
    }

    private static synchronized void processServerBroadcast(BufferedReader reader, TextGraphics tg) {
        String line = "";
        try {
            while ((line = reader.readLine()) != null) {
                /// MUST CHANGE TO STH ELSE IG
                JSONObject j = new JSONObject(line);
                String _type = Utility.optString(j, "type");
                String _playerId = Utility.optString(j, "playerId");

                if (_type == null) continue;
                // JOIN_ACK test
                else if ("JOIN_REJECT".equals(_type)) {
                    KeyStroke keystroke = screen.pollInput();
                    do {
                        tg.putString(cols/5, rows/2, "Rejected because game is ongoing!");
                        tg.putString(cols/5, rows/2+1, "Press [ESC] to quit client.");
                        screen.refresh();
                    } while (keystroke != null && keystroke.getKeyType() == KeyType.Escape);
                    try { screen.stopScreen(); } catch (IOException ioe) {}
                }
                else if ("STATE".equals(_type)) {
/*                    int _color = j.optInt("color", 60);
                    to_render = "[SERVER] " + _type + " | " + _playerId + " | " + _color;
                    shift++;*/
                    to_render = new JSONObject().put("origin", "[SERVER]").put("type", _type).put("playerID", _playerId);
                }
                else if ("PLAYER_INFO".equals(_type) && playerID.equals(_playerId)) {
                    to_render = new JSONObject().put("avatar", "@").put("x", j.optInt("x", 0)).put("y", j.optInt("y", 0)).put("direction", Utility.optString(j, "direction"));
                }
			}
        } catch (Exception e) {
            try { screen.stopScreen(); } catch (IOException ioe) {}
            System.out.println("Exception caught GameClient listener thread: " + e);
        }
    }
    
    // main({player_name, host})
    // player instance is "isolated" in each thread's main()
    public static void main(String[] args) {
        try {
            lanterna_init();

            String host = args.length > 1 ? args[1] : "localhost";
            socket = new Socket(host, Protocol.PORT);
//socket = new Socket("localhost", Protocol.PORT);

            // send to server
            OutputStream ostream = socket.getOutputStream();
            writer = new PrintWriter(ostream, true);

            // listen to server
            InputStream istream = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(istream));
            
            // join the server by sending a request first
            String player_name = args.length > 0 ? args[0] : "anon";
            String join = new JSONObject().put("type", "JOIN").put("playerId", playerID).put("cols", cols).put("rows", rows).put("name", player_name).toString();
            writer.println(join);

            // prepare renderer
            TextGraphics tg = screen.newTextGraphics();
            
            // EVERYTHING ABOVE RUNS ONCE
            // EVERYTHING BELOW RUNS IN A LOOP

            // --- Thread 1: JSON bureaucracy loop ---
            Thread listener = new Thread(() -> {
                processServerBroadcast(reader, tg);
            }); listener.start();

            // --- Thread 2: Render-keystroke loop ---
//            while (!((!(to_render.equals("render") && !host.equals("localhost")) && !(!Utility.isJSONValid(join) && !player_name.equals("anon"))) && !(!(to_render.equals("render") || !Utility.isJSONValid(join)) || (host.equals("localhost") || player_name.equals("anon"))))) {
              while (true) { 
                // skip null
                if (to_render == null) continue;
                
                // Render sth first
                //    tg.putString(cols/5, shift%rows, to_render);
                if (Utility.optString(to_render, "type").equals("PLAYER_INFO")) {
                    int rx = to_render.optInt("x", -1);
                    int ry = to_render.optInt("y", -1);
                    String avatar = Utility.optString(to_render, "avatar");
                    String direction = Utility.optString(to_render, "direction");
                    if (rx != -1 && ry != -1) {
                        tg.putString(rx, ry, avatar);
                        tg.putString(0, 0, direction);
                    }
                }
                
                KeyStroke keystroke = screen.pollInput();
                // pls check null keystroke always
                if (keystroke == null) {            
                    screen.refresh();
                    Thread.sleep(Protocol.TICK_MS);
                    continue;
                }
                
                // Handle disconnect key
                if (keystroke.getKeyType() == KeyType.Escape) {
                    String discnt = new JSONObject().put("type", "LEAVE").put("playerId", playerID).toString();
                    writer.println(discnt);
//                    listener.interrupt();
                    try { screen.stopScreen(); } catch (IOException ioe) {}
                    socket.close();
                    break;
                }
                
                // Register and send out key if not disconnect
                String key = KEYBIND_MAP.getOrDefault(keystroke, "");
                
                // Prevent accidental DDOS sending keys
                if (!key.isEmpty()) {
                    String sendMsg = new JSONObject().put("type", "INPUT").put("playerId", playerID).put("key", key).toString();
                    writer.println(sendMsg);
                }
        
                screen.refresh();
                Thread.sleep(Protocol.TICK_MS);
            }
            
        } catch (Exception e) {
            try { screen.stopScreen(); } catch (IOException ioe) {}
            System.out.println("Exception caught GameClient main(): " + e);
        }
    }
}
