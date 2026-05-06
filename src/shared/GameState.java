package shared;

import java.util.concurrent.*;
import java.util.HashMap;
import java.util.*;

public class GameState {
/*
List<Player/Enemy/Bullet> + playerById + nextId() + colorTaken[] + tickCounter, levelTimer, waveNumber + terrain[][]
*/
    private int[] idCounter = {0, 0, 0}; // hardcoded P E B
    public List<Player> players = new CopyOnWriteArrayList<Player>();
    public List<Enemy> enemies = new CopyOnWriteArrayList<Enemy>();
    public List<Bullet> bullets = new CopyOnWriteArrayList<Bullet>();
    private HashMap<String, Player> playerIdMap = new HashMap<String, Player>();
    public HashSet<Entity.RenderProperty.Color> colorTaken = new HashSet<Entity.RenderProperty.Color>();
    public Player playerById(String id) {
        return playerIdMap.get(id);
    }
    public String genNextId(String type) {
        String newId = type;
        if (type.equals("player")) newId += idCounter[0]++;
        else if (type.equals("enemy")) newId += idCounter[1]++;
        else if (type.equals("bullet")) newId += idCounter[2]++;
        return newId;
    }
    private int tickCounter = 0, levelTimer = Protocol.LEVEL_DURATION_TICKS, waveNumber = 0;
    public int getCurrentTick() {
        return tickCounter;
    }
    public int getLevelTimeLeft() {
        return levelTimer;
    }
    public int getWaveLevel() {
        return waveNumber;
    }
    public void updateTick() {
        tickCounter++;
        if (levelTimer > 0) levelTimer--;
        // blah tick incr stuff go here
    }
    private int terrain[][];
}
