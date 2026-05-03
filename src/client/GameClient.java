package client;

import java.net.*;
import java.util.*;
import org.json.JSONObject;
import com.googlecode.lanterna.*;

public class GameClient {
    // Render
    private int cols = 0, rows = 0;
    private Screen screen;

    // Sockets
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private void draw_unfit_screen() {
        // draw error to screen
        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);

        MessageDialog window = new MessageDialogBuilder()
              .setTitle("smol screen bad")
              .setText("screen too small! maximize screen, then restart game pls :3")
              .build();
        gui.addWindow(window);

        // wait key q for quit
        while (true) {
            gui.updateScreen();
            KeyStroke keystroke = screen.readInput();
            if (keystroke.getKeyType() == KeyType.Escape) break;
        }    

        gui.clearScreen();
    }
    
    private void lanterna_init() {
        // bureaucracy
        DefaultTerminalFactory factory = new DefaultTerminalFactory();
        screen = factory.createScreen();
        screen.startScreen();

        // size
        sz = screen.getTerminalSize();
        cols = sz.getColumns();
        rows = sz.getRows();

        // cleanup screen features
        screen.enterPrivateMode(); //save screen state
        screen.clear();
        screen.setCursorPosition(null); // hides cursor

        // plan: level 2 rendering, see lanterna github fmi.
        
        if (cols < Protocol.MIN_COLS || rows < Protocol.MIN_ROWS) {
            draw_unfit_screen();
        }
    }
    
    public static void main(String[] args) {
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

        // text i/o
        new Thread(() -> {
            string line;
            while ((line = reader.readLine()) != null /* replace with better condition, this is demo*/) {
                try {
                    Thread.sleep(Protocol.TICK_MS);
                } catch (Exception e) {
                    System.out.println("Exception caught: " + e);
                }
            }
        }).start();
        
        // --- keystroke loop ---
        while (true) {
            KeyStroke keystroke = screen.readInput();
            if (keystroke.getKeyType() == KeyType.Escape) {
                break;
            }
        }
        
        screen.exitPrivateMode();
    }    
}
