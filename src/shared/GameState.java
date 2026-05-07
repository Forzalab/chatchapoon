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
    private Random r = new Random();
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
    public String registerNewId(String type) {
        String newId = UUID.randomUUID().toString().substring(0,8);
        
        if ("player".equals(type)) 
            idCounter[0]++;
        else if ("enemy".equals(type)) 
            idCounter[1]++;
        else if ("bullet".equals(type)) 
            idCounter[2]++;

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
        entity.pos.iHaveValidatedB4Setting();
        if (cmd.isEmpty()) return;
        else if ("UP".equals(cmd))
            entity.pos.accum(-1, 0);
        else if ("DOWN".equals(cmd))
            entity.pos.accum(1, 0);
        else if ("LEFT".equals(cmd))
            entity.pos.accum(0, -1);
        else if ("RIGHT".equals(cmd))
            entity.pos.accum(0, 1);
   }

    public synchronized void shootFrom(Entity e) {
        float[] VX = { 0, Protocol.INV_SQRT2,  1,  Protocol.INV_SQRT2, 0, -Protocol.INV_SQRT2, -1, -Protocol.INV_SQRT2 };
        float[] VY = {-1,-Protocol.INV_SQRT2,  0,  Protocol.INV_SQRT2, 1,  Protocol.INV_SQRT2,  0, -Protocol.INV_SQRT2 };

        Entity.Direction d = e.direction;  // dont change direction ordering pls
        if (d == Entity.Direction.NONE) return;
        
        float speed = (d.getVal() % 2 == 0) ? Protocol.BULLET_CARDINAL : Protocol.BULLET_DIAGONAL;
        float vx = VX[d.getVal()] * speed;
        float vy = VY[d.getVal()] * speed;
        
        Bullet b = new Bullet(new Position(e.pos.getRenderY(), e.pos.getRenderX()), vx, vy, registerNewId("bullet"), 1, e.id, 0);
        b.direction = d; // sus
        bullets.add(b);
    }

    public synchronized void rotate(Entity e, String cmd) {
        for (Entity.Direction dir : Entity.Direction.values()) { if (dir.equals(e.direction)) {
            if ("ROTATE_CW".equals(cmd)) e.direction = dir.next();
            else if ("ROTATE_CCW".equals(cmd)) e.direction = dir.prev();
            break;
        }}
    }
    
    public synchronized void alterState(String cmd, String authorID) {
//        if (!moveCmds.contains(cmd)) return;
        Entity entity = Entity.nullEntity;

        // assume ID wont collide, or else there will be
        // dupl assignment
        for (Entity e : players)
            if (authorID.equals(e.id)) entity = e;
        for (Entity e : enemies)
            if (authorID.equals(e.id)) entity = e;
        for (Entity e : bullets)
            if (authorID.equals(e.id)) entity = e;

        if (entity == Entity.nullEntity) return;

        if (moveCmds.contains(cmd))
            shiftPos(cmd, entity);
        else if ("SHOOT".equals(cmd))
            shootFrom(entity);
        else if ("ROTATE_CW".equals(cmd) || "ROTATE_CCW".equals(cmd))
            rotate(entity, cmd);
   }

        
    public GameState() {
        avatarMatrix = new Entity.Avatar[Protocol.ARENA_WIDTH][Protocol.ARENA_HEIGHT];
        for (int i = 0; i < Protocol.ARENA_WIDTH; i++)
            for (int j = 0; j < Protocol.ARENA_HEIGHT; j++)
                avatarMatrix[i][j] = new Entity.Avatar(' ', Entity.Avatar.Color.TRANSPARENT);
    }

}
