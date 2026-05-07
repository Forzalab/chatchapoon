package shared;

import java.util.concurrent.*;
import java.util.HashMap;
import java.util.Arrays;
import java.util.*;

public class GameState {
/*
List<Player/Enemy/Bullet> + playerById + nextId() + colorTaken[] + tickCounter, levelTimer, waveNumber + terrain[][]
*/
    private int[] idCounter = {0, 0, 0}; // hardcoded P E B
    public static enum State {
        BATTLE,
        LOBBY
    }
    public State state = State.LOBBY;
    public List<Player> players = new CopyOnWriteArrayList<Player>();
    public List<Enemy> enemies = new CopyOnWriteArrayList<Enemy>();
    public List<Bullet> bullets = new CopyOnWriteArrayList<Bullet>();
    public HashMap<String, Player> playerIdMap = new HashMap<String, Player>();
    public HashSet<Entity.Avatar.Color> colorTaken = new HashSet<Entity.Avatar.Color>();
    public Player playerById(String id) {
        return playerIdMap.get(id);
    }
    public String attendance(String type) {
        String newId = type;
        if (type.equals("player")) newId += idCounter[0]++;
        else if (type.equals("enemy")) newId += idCounter[1]++;
        else if (type.equals("bullet")) newId += idCounter[2]++;
        return newId;
    }
    private int tickCounter = 0, levelTimer = Protocol.LEVEL_DURATION_TICKS, waveNumber = 0;
    int getCurrentTick() {
        return tickCounter;
    }
    int getLevelTimeLeft() {
        return levelTimer;
    }
    int getWaveLevel() {
        return waveNumber;
    }
    void updateTick() {
        tickCounter++;
        if (levelTimer > 0) levelTimer--;
        // blah tick incr stuff go here
    }
    private int terrain[][] = new int[Protocol.ARENA_HEIGHT][Protocol.ARENA_WIDTH];
    private Entity.Avatar[][] avatarMatrix;
    
    // ---state mutate zone---
    private final HashSet<String> moveCmds = new HashSet<String>(Arrays.asList(
        "UP", "DOWN", "LEFT", "RIGHT"
    ));
    
    // Assuming enemies vx vy is fixed
    public synchronized void shiftPos(String cmd, Entity entity) {
        if (cmd.isEmpty()) return;
        else if (cmd == "UP")
            entity.pos.accum(-1, 0);
        else if (cmd == "DOWN")
            entity.pos.accum(1, 0);        
        else if (cmd == "LEFT")
            entity.pos.accum(0, -1);        
        else if (cmd == "RIGHT")
            entity.pos.accum(0, 1);        
    }

    public synchronized void alterState(String cmd, String authorID) {
        Entity entity = Entity.nullEntity;

        // assume ID wont collide, or else there will be
        // dupl assignment
        for (int i = 0; i < players.size(); i++)
            if (players.get(i).id == authorID) entity = players.get(i);
        for (int i = 0; i < enemies.size(); i++)
            if (enemies.get(i).id == authorID) entity = enemies.get(i);
        for (int i = 0; i < bullets.size(); i++)
            if (bullets.get(i).id == authorID) entity = bullets.get(i);       
            
        if (moveCmds.contains(cmd) && entity != Entity.nullEntity) shiftPos(cmd, entity);
    }

        
    public GameState() {
        avatarMatrix = new Entity.Avatar[Protocol.ARENA_WIDTH][Protocol.ARENA_HEIGHT];
        for (int i = 0; i < Protocol.ARENA_WIDTH; i++)
            for (int j = 0; j < Protocol.ARENA_HEIGHT; j++)
                avatarMatrix[i][j] = new Entity.Avatar(' ', Entity.Avatar.Color.TRANSPARENT);
    }

}
