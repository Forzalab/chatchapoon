package client;

import java.net.*;
import java.util.*;
import java.io.*;
import java.util.HashMap;

import org.json.JSONObject;
import org.json.JSONArray;
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
    static Screen screen;
    private static volatile int shift = 0; // volatile force update value for N threads potentially reading it
    private static volatile JSONObject to_render;
    private static TerminalPosition tp;
    private static String direction = "";
    private static int moneyTickCooldown = 0, scoreTickCooldown = 0, waveTickCooldown = 0, bulletTickCooldown;
    private static String moneyPrior = "", scorePrior = "", wavePrior = "", bulletsPrior = "";    

    // keyboard mode    
    private static State state = State.BLOCK;
    
    public static enum State {
        BLOCK(0), // alow join
        GAME(1), // bl6ocm all join
        CHAT(2); // block all join
        private final int val;
        private State(int v) { val = v; }
        private int getVal() { return val; }
        private static final State[] vals = values();
        private static State statePrev;
        private State mutate(State s) {
          statePrev = this;
          return s;
        }
    }
    private static synchronized State get() {
        return state;
    }
    private static synchronized State getPrev() {
        return State.statePrev;
    }
    private static synchronized void switchState(State s) {
        state = state.mutate(s);
    }
        
    // render mode is obtained thru server
    
    // player info, local copy
    public static final String playerID = UUID.randomUUID().toString().substring(0,8);
    static HashMap<String, TextColor> playerColor = new HashMap<String, TextColor>();    
    public static String playerName = "";
    
    // Sockets
    private static Socket socket;
    static PrintWriter writer;
    private static BufferedReader reader;

    // Magic lookup table
    // key mapping
    public static final HashMap<KeyStroke, String> KEYBIND_MAP = new HashMap<KeyStroke, String>() {{
        put(KeyStroke.fromString("w"), "UP");
        put(KeyStroke.fromString("a"), "LEFT");
        put(KeyStroke.fromString("s"), "DOWN");
        put(KeyStroke.fromString("d"), "RIGHT");
        put(KeyStroke.fromString("<Up>"), "UP");
        put(KeyStroke.fromString("<Left>"), "LEFT");
        put(KeyStroke.fromString("<Down>"), "DOWN");
        put(KeyStroke.fromString("<Right>"), "RIGHT");
        put(KeyStroke.fromString("q"), "ROTATE_CCW");
        put(KeyStroke.fromString("e"), "ROTATE_CW");
        put(KeyStroke.fromString("<Space>"), "SHOOT");
        put(KeyStroke.fromString("<Esc>"), "PING");
        put(KeyStroke.fromString("g"), "PULL");        
    }};

    private static void closeClient() {
        try {
            screen.setCursorPosition(tp);
            screen.stopScreen();
            try { writer.close(); } catch (Exception e) {}
            try { reader.close(); } catch (Exception e) {}
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
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
            closeClient();
            System.exit(0);
        } catch (Exception e) {
        
            String discnt = new JSONObject().put("type", "LEAVE").put("playerId", playerID).toString();
            writer.println(discnt);
            closeClient();
            System.out.println("Exception caught GameClient unfit_screen: " + e);          
            e.printStackTrace();
            System.exit(0);
        }
    }
    
    private static void draw_reject_screen() {
        try {
            TextGraphics tg = screen.newTextGraphics();
            screen.clear();
            while (true) {
                // render
                tg.setBackgroundColor(new TextColor.RGB(255, 255, 255));
                tg.setForegroundColor(new TextColor.RGB(0, 0, 0));
                tg.drawRectangle(new TerminalPosition(2, 2), new TerminalSize(cols - 2 - 2, rows - 2 - 2), '+');
                tg.setBackgroundColor(new TextColor.RGB(160, 0, 0));
                tg.setForegroundColor(new TextColor.RGB(255, 255, 255));
                tg.putString(cols / 3, rows / 2 - 1, "u came too late :(( game is ongoin'");
                tg.putString(cols / 3, rows / 2, "rejoin when lobby is opened");
                tg.putString(cols / 3, rows / 2 + 1, "[ PRESS ANY KEY TO EXIT ]");

                // keystroke
                screen.refresh();
                if (screen.readInput().getKeyType() != null) break;
                Thread.sleep(Protocol.TICK_MS);
            }

            // cease
            closeClient();
            System.exit(0);
        } catch (Exception e) {
        
            String discnt = new JSONObject().put("type", "LEAVE").put("playerId", playerID).toString();
            writer.println(discnt);
            closeClient();
            System.out.println("Exception caught GameClient reject_screen: " + e);          
            e.printStackTrace();
            System.exit(0);
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
            tp = screen.getCursorPosition();
            screen.clear();
            screen.setCursorPosition(null); // hides cursor

            if (cols < Protocol.MIN_COLS || rows < Protocol.MIN_ROWS) {
                draw_unfit_screen();
            }
        } catch (Exception e) {
        
            String discnt = new JSONObject().put("type", "LEAVE").put("playerId", playerID).toString();
            writer.println(discnt);
            closeClient();
            System.out.println("Exception caught GameClient lanterna_init(): " + e);
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void drawDirection(int rx, int ry, String d, TextGraphics tg) {
        TextColor tc = tg.getBackgroundColor();
        TextColor.RGB bkg = new TextColor.RGB(15,23,42);        
        TextColor.RGB grn = new TextColor.RGB(0,255,0);                
        tg.setBackgroundColor(bkg);        
        if (d == null) return;
        else if ("N".equals(d)) tg.putString(rx, ry-1, "|");
        else if ("S".equals(d)) tg.putString(rx, ry+1, "|");
        else if ("E".equals(d)) tg.putString(rx+1, ry, "—");
        else if ("W".equals(d)) tg.putString(rx-1, ry, "—");
        else if ("NE".equals(d)) tg.putString(rx+1, ry-1, "/");
        else if ("NW".equals(d)) tg.putString(rx-1, ry-1, "\\");
        else if ("SE".equals(d)) tg.putString(rx+1, ry+1, "\\");
        else if ("SW".equals(d)) tg.putString(rx-1, ry+1, "/");
       tg.setBackgroundColor(tc);
    }
    
    // JSONArray -> tg rendering
    private static void processPlayersArrayRender(JSONArray ja, TextGraphics tg, String ava, JSONObject jao) { try { 
    // hahsmap for color assignment id-color
//        HashMap<String, TextColor> playerColor = new HashMap<String, TextColor>();
    //            TextColor.RGB bkg_init = new TextColor.RGB(15,23,42);        
        TextColor.RGB bkg = new TextColor.RGB(15,23,42);
        TextColor.RGB wht = new TextColor.RGB(255,255,255);            
        TextColor.RGB red = new TextColor.RGB(255,0,0);              
        TextColor.RGB dim = new TextColor.RGB(64,64,64);                 
        TextColor.RGB grn = new TextColor.RGB(0,230,0);                  
        TextColor.RGB grn_dim = new TextColor.RGB(6,128,6);
        TextColor.RGB yel = new TextColor.RGB(255, 190, 0);
        TextColor.RGB cyn = new TextColor.RGB(0, 210, 210);


      // direction RENDER can be oevrriden if sth gets in its way (0,0)
        shift = 0;
        for (int i = 0; i < ja.length(); i++) {
            // early returns.
            // if player not found in one msg?
            // tough luck, ask the server what it sent lol
            // as THIS player must be present in the server's `clients` list! invariant.
            if (ja.getJSONObject(i) == null) continue;
            JSONObject j = ja.getJSONObject(i);
            if (j == null) continue;

            boolean hit = j.optBoolean("hit");

            // determine color for bullet B4 render
            String type = j.optString("type");
            String playerId = j.optString("id");
            int r = 255, g = 255, b = 255;
            if ("player".equals(type) && !playerColor.containsKey(playerId)) {
                if (playerID.equals(Utility.optString(j, "id"))) {
                    playerColor.put(playerId, new TextColor.RGB(255, 255, 255));
                } else {
                TextColor color = ChatClient.getColor(playerId);
                playerColor.put(playerId, color);
            }}

//            System.err.println(playerColor.keySet());

           // render coins
            if ("coins".equals(Utility.optString(j, "type"))) {
                
                int rx = j.optInt("x", -1);
                int ry = j.optInt("y", -1);
                String avatar = ava;
// ⛃⛂⭐ for coins, ❓ for gacha
                tg.setBackgroundColor(yel); tg.setForegroundColor(yel);
                if (rx != -1 && ry > 0)
                    tg.putString(rx, ry, avatar);
                tg.setBackgroundColor(bkg);

                tg.setForegroundColor(new TextColor.RGB(255, 255, 255));
                continue;
            }

                        
            // render non-players for now
            if (hit) tg.setBackgroundColor(red);
            if (!"player".equals(Utility.optString(j, "type"))) {
                if ("bullet".equals(j.optString("type"))) {            
                    String id = j.optString("ownerID");
                    TextColor color = playerColor.get(id);            
                    tg.setForegroundColor(color);
                } else tg.setForegroundColor(new TextColor.RGB(240,240,240));
                
                int rx = j.optInt("x", -1);
                int ry = j.optInt("y", -1);
                String direction = j.optString("direction");
                String avatar = ava; // for now, will customize later

                if ("enemy".equals(j.optString("type"))) {
                    if ("PATROL".equals(j.optString("subtype"))) { avatar = "p"; }
                    else if ("SNIPER".equals(j.optString("subtype"))) { avatar = "s"; }
                    else if ("COPS".equals(j.optString("subtype"))) { avatar = "c"; }     
                }

                tg.setForegroundColor(wht);
                if (rx != -1 && ry > 0) {
                      if (hit) tg.setBackgroundColor(red);
                    tg.putString(rx, ry, avatar);
                    if ("enemy".equals(j.optString("type")))
                        drawDirection(rx, ry, direction, tg);
                }
                tg.setForegroundColor(wht);
                tg.setBackgroundColor(bkg);
                continue;
            }
           
            if (!"player".equals(j.optString("type"))) continue;



            
            /// belownhere is players obly rendering logic
            playerId = Utility.optString(j,"id");
            if (playerId == null) continue;

            TextColor color = playerColor.get(playerId);
            
            int rx = j.optInt("x", -1);
            int ry = j.optInt("y", -1);
            String avatar = ava; // for now, will customize later

            // check id to parse direction
//            if (playerID.equals(Utility.optString(j, "id")))
                direction = Utility.optString(j, "direction");

            if (rx != -1 && ry > 0 && "player".equals(j.optString("type"))) {
                if (!playerID.equals(Utility.optString(j,"id"))) {
                    tg.setForegroundColor(color);
                    tg.setBackgroundColor(color);                        
                }
                else {
                    tg.setForegroundColor(color);
                    tg.setBackgroundColor(color);                    
                }
                if (hit) tg.setBackgroundColor(red);
                tg.putString(rx, ry, avatar);
                drawDirection(rx, ry, direction, tg);
//                tg.setForegroundColor(wht);                
            }            
            if (hit) tg.setBackgroundColor(bkg);
         
            // scoreboard
            String player = String.format("%-12s", Utility.optString(j, "name"));
            String score = String.format("%3d", j.optInt("score", -1));
            String display = player + score;
            if (!playerID.equals(Utility.optString(j,"id"))) {
                tg.setForegroundColor(color);
                tg.setBackgroundColor(ChatClient.getDimmed(color,0.1f));                
            } else {
                tg.setForegroundColor(wht);
                tg.setBackgroundColor(ChatClient.getDimmed(bkg, 1.3f));                
            }
            
            tg.putString(Protocol.ARENA_WIDTH-17, Protocol.ARENA_HEIGHT-1- shift, player);        
            int wRed = color.getRed() + (int)((255 - color.getRed()) * 0.3);
            int wGreen = color.getGreen() + (int)((255 - color.getGreen()) * 0.3);
            int wBlue = color.getBlue() + (int)((255 - color.getBlue()) * 0.3);                       
            TextColor whitened = new TextColor.RGB(wRed, wGreen, wBlue);
            tg.setForegroundColor(whitened);                            
            tg.putString(Protocol.ARENA_WIDTH-17 + player.length(), Protocol.ARENA_HEIGHT-1- shift++, score);                                tg.setForegroundColor(wht); tg.setBackgroundColor(bkg);

            // HUD bar
            if (!playerID.equals(Utility.optString(j, "id"))) continue;

            String tick = jao.optInt("tickCounter", 0) + "";
            String wave = jao.optInt("waveNumber", 1) + "";
            String lvlTicks = jao.optInt("levelTimer", Protocol.LEVEL_DURATION_TICKS) + "";

            String leftHUD = "HP [###]  ◆  SCORE: 420 ◆ 100 ⁍"; // 33
            String rightHUD = "$350  ◆  REINF #30  ◆  HEIST ENDS IN 2:45"; //40      
            String money = String.format("%3d", j.optInt("currency", -1));

            int scoreStrL = (j.optInt("score", -1)>0)?14:0;
            int amtBullets = j.optInt("bullets", -1);
            int bulletStrL = (amtBullets>0)?7:0;

            
            int dynSize = Protocol.ARENA_WIDTH - (43 + 40);
            int hp = j.optInt("hp", 3);
            int hp_max = j.optInt("hp_max", 3);            
            tg.putString(3, 0, "HP [");
            tg.setForegroundColor(red);
            for (int k = 0; k < hp; k++) tg.putString(4+k+3, 0, "♥");
            tg.setForegroundColor(red);
            for (int k = 0; k < hp_max - hp; k++) tg.putString(4+hp+k+3, 0, " ");       
            tg.setForegroundColor(wht);
            tg.putString(4+hp_max+3, 0, "] "); // 11
            tg.setForegroundColor(dim);
            
            
            if (j.optInt("score", -1) > 0) {
                tg.setForegroundColor(dim); tg.setBackgroundColor(bkg);
                tg.putString(4+hp_max+2+3, 0, "◆");
                tg.setForegroundColor(wht);
                tg.putString(4+hp_max+3+3, 0, " SCORE: " + score); // 11

                //score, 3
                if (!"".equals(scorePrior) && !score.equals(scorePrior))
                    scoreTickCooldown = 3;
                tg.setBackgroundColor((scoreTickCooldown-- > 0)?wht:bkg);
                tg.setForegroundColor((scoreTickCooldown > 0)?bkg:wht);
                tg.putString(4+hp_max+11+3, 0, score); // 11
            } else {
                tg.setForegroundColor(bkg);tg.setBackgroundColor(bkg);
                tg.drawRectangle(new TerminalPosition(4+hp_max+11, 0), new TerminalSize(14, 0), '.');
            }
            scorePrior = score;            
            tg.setForegroundColor(wht);tg.setBackgroundColor(bkg);
                

            amtBullets = (amtBullets > 999) ? 999 : amtBullets;
            String bullets = String.format("%d", amtBullets);
            if (amtBullets > 0) {
                //bullet, 5
                tg.setForegroundColor(dim); tg.setBackgroundColor(bkg);
                tg.putString(4+hp_max+3+2+scoreStrL, 0, "◆ ");
                tg.setForegroundColor(wht);            
                tg.putString(4+hp_max+3+2+scoreStrL+3, 0, "B# " + bullets + ((amtBullets==999)?"+":" "));
            } else {
                tg.setForegroundColor(bkg);tg.setBackgroundColor(bkg);
                tg.drawRectangle(new TerminalPosition(4+hp_max+3+2+scoreStrL, 0), new TerminalSize(7, 0), '.');
            }
              bulletsPrior = bullets;
              tg.setForegroundColor(wht);tg.setBackgroundColor(bkg);              
//            tg.setForegroundColor(wht);tg.setBackgroundColor(bkg);

            int rightHUDWidth = 4 + 5 + 7 + wave.length() + 5 + 18;
            // empty space for notif

            //money,4
            int moneyX = 4 + hp_max + 3 + 11 + dynSize + 4 + 3;            
//            int moneyX = Protocol.ARENA_WIDTH+3 - rightHUDWidth;
            if (j.optInt("currency", -1) > 0) {            
                if (!"".equals(moneyPrior) && !money.equals(moneyPrior))
                    moneyTickCooldown = 3;
                tg.setBackgroundColor((moneyTickCooldown-- > 0)?grn:grn_dim);
                tg.putString(4+hp_max+3+11+dynSize, 0, "＄"+money); // 11
            
                tg.setForegroundColor(dim); tg.setBackgroundColor(bkg);            
                tg.putString(moneyX, 0, "  ◆  ");                
            } else {
                tg.setForegroundColor(bkg);tg.setBackgroundColor(bkg);
                tg.drawRectangle(new TerminalPosition(4+hp_max+3+11+dynSize, 0), new TerminalSize(6, 0), '.');
                tg.putString(moneyX, 0, "     ");                                
            }
            moneyPrior = money;
            tg.setForegroundColor(wht); tg.setBackgroundColor(bkg);            
        
            //reinforcement
            if (Integer.parseInt(wave) > 0) {
                if (!wavePrior.equals(wave)) waveTickCooldown = 2;
            
                String reinfStr = "REINF #" + wave;
                tg.setForegroundColor(waveTickCooldown-- > 0 ? cyn : wht);
                tg.setBackgroundColor(bkg);
                tg.putString(moneyX + 4, 0, reinfStr);
            } else {
                tg.putString(moneyX + 5, 0, " ".repeat(7+wave.length()));
            }                
            wavePrior = wave;
        
            // timer
            int timerX = moneyX + 5 + 7 + wave.length();
            tg.setForegroundColor(dim); tg.setBackgroundColor(bkg);
            tg.putString(timerX, 0, (Integer.parseInt(wave) > 0)?"  ◆  ":"     ");
            timerX += 5;

            int secs = Integer.parseInt(lvlTicks) / 20;
            String timerStr = String.format("%d:%02d", secs / 60, secs % 60);
            boolean blink = secs < 10 && (Integer.parseInt(tick) % 2 == 1);
            TextColor.RGB timerClr = blink ? bkg : secs < 30 ? red : secs < 60 ? yel : wht;
            tg.setForegroundColor(timerClr);
            tg.setBackgroundColor(bkg);
            tg.putString(timerX, 0, "HEIST ENDS IN " + timerStr);
            tg.setForegroundColor(wht); tg.setBackgroundColor(bkg);
            //money flash
        }
            shift = 0;        
        }
        catch (Exception e) {
        
            String discnt = new JSONObject().put("type", "LEAVE").put("playerId", playerID).toString();
            writer.println(discnt);
            closeClient();
            System.out.println("Exception caught: processPlayersArrayRender " + e);
            e.printStackTrace();
            System.exit(0);
        }
    }
    
    private static void processServerBroadcast(BufferedReader reader, TextGraphics tg) {
        String line = "";
        try {
            while ((line = reader.readLine()) != null) {
                /// MUST CHANGE TO STH ELSE IG
                JSONObject j = new JSONObject(line);
                String _type = Utility.optString(j, "type");

                if (_type == null) continue;
                // JOIN_ACK test
                else if ("JOIN_REJECT".equals(_type)) {
                    draw_reject_screen();
                }
                else if ("JOIN_ACK".equals(_type)) {
                    // recieve
                    String key = KEYBIND_MAP.get(KeyStroke.fromString("<Esc>"));
                    String sendMsg = new JSONObject().put("type", "INPUT").put("playerId", playerID).put("key", key).toString();
                    writer.println(sendMsg);
                }
                else if ("ENTITY_STATE".equals(_type)) {
                    if ("LEADERBOARD".equals(Utility.optString(to_render, "type"))) continue;
                    if ("LOBBY".equals(Utility.optString(to_render, "type"))) { screen.clear(); switchState(State.GAME); }
                    to_render = new JSONObject(line); // shift handling onto render thread
                }
                else if ("LEADERBOARD".equals(_type)) {
//                    System.exit(0);
                    if ("LOBBY".equals(Utility.optString(to_render, "type"))) continue;
                    switchState(State.BLOCK);
                    to_render = new JSONObject(line);
                }
                else if ("LOBBY".equals(_type)) {
                    switchState(State.BLOCK);
                    to_render = new JSONObject(line);
                }                
                else if ("CHAT".equals(_type)) {
//                     String msg = j.optString("msg");
                    ChatClient.msgQ.add(j);
//                    System.err.println(msg);
                }
			}
        } catch (Exception e) {
        
            String discnt = new JSONObject().put("type", "LEAVE").put("playerId", playerID).toString();
            writer.println(discnt);
            closeClient();
            System.out.println("Exception caught GameClient listener thread: " + e);
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static void renderLeaderboard(TextGraphics tg) {
//        screen.clear();
        for (int i = 0; i < 15; i++) { try {
            TextColor.RGB red = new TextColor.RGB(255,0,0);
            TextColor.RGB white = new TextColor.RGB(255,255,255);          
            TextColor.RGB frg = (i%2==0)?red:white;
            TextColor.RGB bkg = (i%2!=0)?red:white;
                          
            TextCharacter space = new TextCharacter('.', bkg, bkg);
            TextCharacter frame = new TextCharacter('!', frg, frg);            
            
            tg.fillRectangle(new TerminalPosition(0,0), new TerminalSize(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH + 1, Protocol.ARENA_HEIGHT + Protocol.BORDER * 2), space);
            tg.drawRectangle(new TerminalPosition(0,0), new TerminalSize(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH + 1, Protocol.ARENA_HEIGHT + Protocol.BORDER * 2), frame);
            tg.setBackgroundColor(frg);
            tg.setForegroundColor(bkg);            
            tg.enableModifiers(SGR.BOLD);
            for (int j = -3; j <= 3; j++) {
                int length = "[[ GAME OVER ]]".length();
                tg.putString(Protocol.ARENA_WIDTH/2 + (int)(0.5+Protocol.SIDEBAR_WIDTH)/2 - length/2, Protocol.ARENA_HEIGHT/2 - Protocol.BORDER + j, "[[ GAME OVER ]]");
            }
            screen.refresh();
            Thread.sleep(Protocol.TICK_MS * 10);
        } catch (Exception e) {}}
        
        System.exit(0);
    }

    public static void splash() { try {
        TextColor.RGB vg = new TextColor.RGB(1,13,1);
        TextColor.RGB white = new TextColor.RGB(245,255,245);                      
        TextColor.RGB white_dim = new TextColor.RGB(80,110,80);                  
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();
        tg.setBackgroundColor(vg);
        TextCharacter space = new TextCharacter('.', vg, vg);
      tg.fillRectangle(new TerminalPosition(0, 0), new TerminalSize(Protocol.ARENA_WIDTH +  Protocol.SIDEBAR_WIDTH, Protocol.ARENA_HEIGHT + Protocol.BORDER), space);

      tg.setForegroundColor(white);
      tg.drawRectangle(new TerminalPosition(0, 0), new TerminalSize(Protocol.ARENA_WIDTH +  Protocol.SIDEBAR_WIDTH, 0), '─');
      tg.drawLine(
            new TerminalPosition(0, Protocol.ARENA_HEIGHT + Protocol.BORDER), 
            new TerminalPosition(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH - 1, Protocol.ARENA_HEIGHT + Protocol.BORDER),
            '─'
        );
      tg.drawRectangle(new TerminalPosition(0, 0), new TerminalSize(0, Protocol.ARENA_HEIGHT +  Protocol.BORDER), '│');
  //    tg.drawRectangle(new TerminalPosition(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH - 1, 0), new TerminalSize(0, Protocol.ARENA_HEIGHT +  Protocol.BORDER), '│'); 
    tg.drawLine(
        new TerminalPosition(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH - 1, 0),
        new TerminalPosition(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH - 1, Protocol.ARENA_HEIGHT + Protocol.BORDER),
        '│'
    );
      tg.putString(0,0,"┌");
      tg.putString(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH - 1,0,"┐");
      tg.putString(0,Protocol.ARENA_HEIGHT + Protocol.BORDER,"└");
      tg.putString(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH - 1,Protocol.ARENA_HEIGHT + Protocol.BORDER,"┘");
      
        String[] lines = {
            "                  ███████████|     ██|     ██|      █████████|",
            "                      ███|         ██|     ██|      ██|",
            "                      ███|         ██|     ██|      ██|",
            "                      ███|         ██████████|      █████████|",
            "                      ███|         ██|     ██|      ██|",
            "                      ███|         ██|     ██|      ██|",
            "                      ███|         ██|     ██|      ██|",
            "                      ███|         ██|     ██|      █████████|",
            "",
            "  ███|    ███|     ██████████|      ████████|       █████████|     ███████████|",
            "  ███|    ███|     ███|                ██|         ███|                ███|",
            "  ███|    ███|     ███|                ██|         ███|                ███|",
            "  ███████████|     █████████|          ██|          ██████|            ███|",
            "  ███|    ███|     ███|                ██|              █████|         ███|",
            "  ███|    ███|     ███|                ██|                 ██|         ███|",
            "  ███|    ███|     ███|                ██|                 ██|         ███|",
            "  ███|    ███|     ██████████|      ████████|      █████████|          ███|"
        };

        int length = lines[9].length();

        for (int tick = 0; tick <= 10; tick++) {
            if (tick%10==0) tg.setForegroundColor(white_dim);
            else if (tick%5==0) tg.setForegroundColor(white);        
            
            for (int i = 0; i < lines.length; i++) {
                tg.putString(Protocol.ARENA_WIDTH/2 + Protocol.SIDEBAR_WIDTH/2 - length/2, Protocol.ARENA_HEIGHT/2 - Protocol.BORDER + i - lines.length/2, lines[i]);
            }
            screen.refresh();
            Thread.sleep(Protocol.TICK_MS * 2);
        }
    } catch (Exception e) {} }


    public static void renderLobby(JSONObject to_render) { try {
        TextColor.RGB vg = new TextColor.RGB(1,13,1);
        TextColor.RGB white = new TextColor.RGB(225,245,225);                      
        TextColor.RGB white_dim = new TextColor.RGB(80,110,80);                  
        TextColor.RGB white_txtdim = new TextColor.RGB(145,155,145);                          
        
        TextGraphics tg = screen.newTextGraphics();
        tg.setBackgroundColor(vg);
        TextCharacter space = new TextCharacter('.', vg, vg);
      tg.fillRectangle(new TerminalPosition(0, 0), new TerminalSize(Protocol.ARENA_WIDTH +  Protocol.SIDEBAR_WIDTH, Protocol.ARENA_HEIGHT + Protocol.BORDER), space);
      tg.setForegroundColor(white);
      tg.drawRectangle(new TerminalPosition(0, 0), new TerminalSize(Protocol.ARENA_WIDTH +  Protocol.SIDEBAR_WIDTH, 0), '─');
      tg.drawLine(
            new TerminalPosition(0, Protocol.ARENA_HEIGHT + Protocol.BORDER),
            new TerminalPosition(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH - 1, Protocol.ARENA_HEIGHT + Protocol.BORDER),
            '─'
        );
      tg.drawRectangle(new TerminalPosition(0, 0), new TerminalSize(0, Protocol.ARENA_HEIGHT +  Protocol.BORDER), '│');
  //    tg.drawRectangle(new TerminalPosition(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH - 1, 0), new TerminalSize(0, Protocol.ARENA_HEIGHT +  Protocol.BORDER), '│'); 
    tg.drawLine(
        new TerminalPosition(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH - 1, 0),
        new TerminalPosition(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH - 1, Protocol.ARENA_HEIGHT + Protocol.BORDER),
        '│'
    );
      tg.putString(0,0,"┌");
      tg.putString(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH - 1,0,"┐");
      tg.putString(0,Protocol.ARENA_HEIGHT + Protocol.BORDER,"└");
      tg.putString(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH - 1,Protocol.ARENA_HEIGHT + Protocol.BORDER,"┘");
      
      String[] lines = {
            "                  ███████████|     ██|     ██|      █████████|",
            "                      ███|         ██|     ██|      ██|",
            "                      ███|         ██|     ██|      ██|",
            "                      ███|         ██████████|      █████████|",
            "                      ███|         ██|     ██|      ██|",
            "                      ███|         ██|     ██|      ██|",
            "                      ███|         ██|     ██|      ██|",
            "                      ███|         ██|     ██|      █████████|",
            "",
            "  ███|    ███|     ██████████|      ████████|       █████████|     ███████████|",
            "  ███|    ███|     ███|                ██|         ███|                ███|",
            "  ███|    ███|     ███|                ██|         ███|                ███|",
            "  ███████████|     █████████|          ██|          ██████|            ███|",
            "  ███|    ███|     ███|                ██|              █████|         ███|",
            "  ███|    ███|     ███|                ██|                 ██|         ███|",
            "  ███|    ███|     ███|                ██|                 ██|         ███|",
            "  ███|    ███|     ██████████|      ████████|      █████████|          ███|"
        };
        
        int length = lines[9].length();

        tg.setForegroundColor(white);        
        
        for (int i = 0; i < lines.length; i++) {
            tg.putString(Protocol.ARENA_WIDTH/2 + Protocol.SIDEBAR_WIDTH/2 - length/2, Protocol.ARENA_HEIGHT/2 - Protocol.BORDER + i - lines.length/2, lines[i]);
        }

        JSONArray players = to_render.getJSONArray("players");
        
        for (int i = 0; i < players.length(); i++) {
            if (playerID.equals(Utility.optString(players.getJSONObject(i), "id")))
                tg.setForegroundColor(white);
            else tg.setForegroundColor(white_txtdim); 
            String playerName = players.getJSONObject(i).optString("name");
            tg.putString(Protocol.ARENA_WIDTH/2 + Protocol.SIDEBAR_WIDTH/2 - playerName.length()/2, Protocol.ARENA_HEIGHT/2 - Protocol.BORDER + lines.length/2 + 4 + i, playerName);
            
        }
        
        screen.refresh();
        Thread.sleep(Protocol.TICK_MS * 2);

    } catch (Exception e) {} }
        
    // main({player_name, host})
    // player instance is "isolated" in each thread's main()
    public static void main(String[] args) {
        try {
            lanterna_init();

            splash();
            
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
            tg.setBackgroundColor(new TextColor.RGB(15,23,42));
            TextCharacter space = new TextCharacter('.', new TextColor.RGB(15,23,42), new TextColor.RGB(15,23,42));
//            tg.fillRectangle(new TerminalPosition(0, 0), new TerminalSize(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH, Protocol.ARENA_HEIGHT + Protocol.BORDER), space);

            // EVERYTHING ABOVE RUNS ONCE
            // EVERYTHING BELOW RUNS IN A LOOP

            // --- Thread 1: JSON bureaucracy loop ---
            Thread listener = new Thread(() -> {
                processServerBroadcast(reader, tg);
            }); listener.start();            

            // --- Thread 2: Render-keystroke loop ---
            while (!((!( "render".equals(to_render) && !"localhost".equals(host)) && !(!Utility.isJSONValid(join) && !"anon".equals(player_name))) && !(!("render".equals(to_render) || !Utility.isJSONValid(join)) || ("localhost".equals(host) || "anon".equals(player_name))))) {         

                // ==== RENDER ====
                // skip null
                // pls check null keystroke always
                if (to_render == null) {            
                    screen.refresh();
                    Thread.sleep(Protocol.TICK_MS);
                    continue;
                }
                else if (to_render.isEmpty()) {            
                    screen.refresh();
                    Thread.sleep(Protocol.TICK_MS);
                    continue;
                }
                
                if ("LOBBY".equals(Utility.optString(to_render, "type"))) {
                    tg.fillRectangle(new TerminalPosition(0, 0), new TerminalSize(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH, Protocol.ARENA_HEIGHT + Protocol.BORDER), space);
                    renderLobby(to_render);
//                    continue;
                }
                // Render sth first
                // all screen stuff, THEN indiv elem
                else if ("ENTITY_STATE".equals(Utility.optString(to_render, "type")) && to_render.optInt("tickCounter", -1) > 0) {
//                    switchState(State.GAME);
                    tg.fillRectangle(new TerminalPosition(0, 0), new TerminalSize(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH, Protocol.ARENA_HEIGHT + Protocol.BORDER + 1), space);
                    boolean toEmphasizeChat = (state == State.CHAT);
                    ChatClient.render(toEmphasizeChat);
                    if (!toEmphasizeChat) tg.setForegroundColor(new TextColor.RGB(255,255,255));
                    else tg.setForegroundColor(new
    TextColor.RGB(80,90,125));
                  //tg.drawRectangle(new TerminalPosition(0, 0), new TerminalSize(Protocol.ARENA_WIDTH, 0), '─');
                  tg.drawLine(
                        new TerminalPosition(0, Protocol.ARENA_HEIGHT + Protocol.BORDER),
                        new TerminalPosition(Protocol.ARENA_WIDTH, Protocol.ARENA_HEIGHT + Protocol.BORDER),
                        '─'
                    );
                  tg.drawRectangle(new TerminalPosition(0, 0), new TerminalSize(0, Protocol.ARENA_HEIGHT +  Protocol.BORDER), '│');
              //    tg.drawRectangle(new TerminalPosition(Protocol.ARENA_WIDTH + Protocol.SIDEBAR_WIDTH - 1, 0), new TerminalSize(0, Protocol.ARENA_HEIGHT +  Protocol.BORDER), '│'); 
                tg.drawLine(
                    new TerminalPosition(Protocol.ARENA_WIDTH, 0),
                    new TerminalPosition(Protocol.ARENA_WIDTH, Protocol.ARENA_HEIGHT + Protocol.BORDER),
                    '│'
                );
                  tg.putString(0,0,"┌");
                  tg.putString(Protocol.ARENA_WIDTH,0,"┐");
                  tg.putString(0,Protocol.ARENA_HEIGHT + Protocol.BORDER,"└");
                  tg.putString(Protocol.ARENA_WIDTH,Protocol.ARENA_HEIGHT + Protocol.BORDER,"┘");
                    JSONArray jap = new JSONArray(to_render.getJSONArray("players"));
                    JSONArray jab = new JSONArray(to_render.getJSONArray("bullets"));
                    JSONArray jae = new JSONArray(to_render.getJSONArray("enemies"));      
                    JSONArray jac = new JSONArray(to_render.getJSONArray("coins"));         
                    if (jac != null) processPlayersArrayRender(jac, tg, "*", to_render); 
                    if (jae != null) processPlayersArrayRender(jae, tg, "x", to_render);
                    if (jap != null) processPlayersArrayRender(jap, tg, "Ɵ", to_render);
                    if (jab != null) processPlayersArrayRender(jab, tg, "•", to_render);

                    screen.refresh();
                }
                else if ("LEADERBOARD".equals(Utility.optString(to_render, "type"))) {
                    renderLeaderboard(tg);
                }            

                // ==== keyboard ====                        
                KeyStroke keystroke;
                while ((keystroke = screen.pollInput()) != null) {
//                KeyStroke keystroke = screen.pollInput();

                // Handle disconnect + chat key
                if (keystroke.getKeyType() == KeyType.Escape) {
                    if (state == State.CHAT) {
                        state = state.mutate(State.GAME);
                        screen.refresh();
                        Thread.sleep(Protocol.TICK_MS);
                        continue;
                    }
                    String discnt = new JSONObject().put("type", "LEAVE").put("playerId", playerID).toString();
                    writer.println(discnt);
//                    listener.interrupt();
                    break;
                }

                if (state == State.CHAT) {
                    if (keystroke.getKeyType() == KeyType.Enter) {
                        ChatClient.msgBuffer = ChatClient.msgBuffer.replace("\n", "");
                        ChatClient.send(ChatClient.msgBuffer, playerID, player_name, "");
                        ChatClient.resetCursor();
                    }
                    else if (keystroke.getKeyType() == KeyType.Backspace) {
                        if (ChatClient.msgBuffer.isEmpty())
                            ChatClient.msgBuffer = "";
                        else {
                            ChatClient.removeAt();
                       }
                    }
                    else if (keystroke.getKeyType() == KeyType.ArrowLeft) {
                        ChatClient.moveCursor(-1);
                    }
                    else if (keystroke.getKeyType() == KeyType.ArrowRight) {
                        ChatClient.moveCursor(1);
                    }                    
                    else if (keystroke.getCharacter() == null) continue;                    
                    else {
                        ChatClient.insertChar(keystroke.getCharacter());
                    }
                    continue;
                }                
                
                // Handle chat switch
                if (state == State.GAME && keystroke.getKeyType() == KeyType.Character && 'c' == keystroke.getCharacter()) {
                    switchState(State.CHAT);
                    screen.refresh();
                    Thread.sleep(Protocol.TICK_MS);
                    continue;
                }

                // DO NOT REGISTER KEY IF NOT ON BATTLE MODE
                // only esc
                if (state == State.BLOCK) {
                    screen.refresh();
                    Thread.sleep(Protocol.TICK_MS);
                    continue;
                }
                
                // Register and send out key if not disconnect
                // wont send anything if it isnt a valid mapped key
                String key = KEYBIND_MAP.getOrDefault(keystroke, "");
                
                // Prevent accidental DDOS sending keys
                if (!key.isEmpty()) {
                    String sendMsg = new JSONObject().put("type", "INPUT").put("playerId", playerID).put("key", key).toString();
                    writer.println(sendMsg);
                }

                } // end of keystroke loop
                // ==== now render ====

                screen.refresh();
                Thread.sleep(Protocol.TICK_MS);
            }
            // Closing action
            closeClient();
            System.exit(0);

        } catch (Exception e) {
            String discnt = new JSONObject().put("type", "LEAVE").put("playerId", playerID).toString();
            writer.println(discnt);
            closeClient();
            System.out.println("Exception caught GameClient main(): " + e);
            e.printStackTrace();
            System.exit(0);
        }
    }
}
