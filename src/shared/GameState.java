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
    public ConcurrentHashMap<String, Player> playerIdMap = new ConcurrentHashMap<String, Player>();
    public HashSet<Entity.Avatar.Color> colorTaken = new HashSet<Entity.Avatar.Color>();
    public HashSet<String> idTaken = new HashSet<String>();

    public Player playerById(String id) {
        return playerIdMap.get(id);
    }
    public String registerNewId(String type) {
        String newId;

        // prevent collison, like lottery level rare
        do {
            newId = UUID.randomUUID().toString().substring(0,8);
        }
        while (idTaken.contains(newId));
        idTaken.add(newId);
        
        if ("player".equals(type)) 
            idCounter[0]++;
        else if ("enemy".equals(type)) 
            idCounter[1]++;
        else if ("bullet".equals(type)) 
            idCounter[2]++;

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
    private int terrain[][] = new int[Protocol.ARENA_HEIGHT][Protocol.ARENA_WIDTH];
    private Entity.Avatar[][] avatarMatrix;
    
    // ---state mutate zone---
    private final HashSet<String> moveCmds = new HashSet<String>(Arrays.asList(
        "UP", "DOWN", "LEFT", "RIGHT"
    ));
    
    // Assuming enemies vx vy is fixed
    public synchronized void shiftPos(String cmd, Entity entity) {
        if (entity.dead()) return;
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
        if (e.dead()) return;
        else if (e instanceof Player p && (p.fireCooldown > 0 || p.bullets <= 0)) return;
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

        if (e instanceof Player p) {
            p.fireCooldown = Protocol.FIRE_COOLDOWN_TICKS;
            p.bullets--;
        }
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
        for (Entity e : players) {
            if (authorID.equals(e.id)) entity = e;
        }
        for (Entity e : enemies)
            if (authorID.equals(e.id)) entity = e;
        for (Entity e : bullets)
            if (authorID.equals(e.id)) entity = e;

        if (entity == Entity.nullEntity) return;
        else if (entity.dead()) return;

        if (moveCmds.contains(cmd))
            shiftPos(cmd, entity);
        else if ("SHOOT".equals(cmd))
            shootFrom(entity);
        else if ("ROTATE_CW".equals(cmd) || "ROTATE_CCW".equals(cmd))
            rotate(entity, cmd);
   }

    public synchronized void spawnWave(int amt) {
        // pick side -> rnd coords
        int side = r.nextInt(4);
        int wx = -156, wy = -751;
        
        // 0 1 2 3 clockwise, 0 north.
        // always subtract 1 for bounding shit
        for (int i = 0; i < amt; i++) {
            if (side == 0) {
                wx = r.nextInt(Protocol.ARENA_WIDTH - 1);
                wy = 0;
            } else if (side == 1) {
                wy = r.nextInt(Protocol.ARENA_HEIGHT - 1);
                wx = Protocol.ARENA_WIDTH - 1;
            } else if (side == 2) {
                wx = r.nextInt(Protocol.ARENA_WIDTH - 1);
                wy = Protocol.ARENA_HEIGHT - 1;
            } else if (side == 3) {
                wy = r.nextInt(Protocol.ARENA_HEIGHT - 1);
                wx = 0;
            } 

            float rSpeed = (r.nextInt(7 - 3) + 3) * 0.01f;
            Enemy e = new Enemy(new Position(wy, wx), 0, 0, "enemy", registerNewId("enemy"), Protocol.PLAYER_HP_MAX, "COPS", 0, rSpeed);
            enemies.add(e);            
        }
        waveNumber++;
    }        
    public synchronized void updateEnemies() {
        // O(enemies * players) not gud as Quadtree
        // but fuck Quadtree
        for (Enemy e : enemies) {
            e.uptickHitCooldown();
            if (e.despawnTimer-- <= 0)
                e.hp.setHP(0).triggerRespawn(false);
                
            int minDist = Protocol.ARENA_HEIGHT * Protocol.ARENA_WIDTH;
            int stepX = 0, stepY = 0;
            Player pWithMinDist;
            // nearest player to follow
            for (Player p : players) {
                if (p.hp.isDead()) continue;
                Position pe = e.pos, pp = p.pos;
                int distX = pp.getRenderX() - pe.getRenderX(), distY = pp.getRenderY() - pe.getRenderY();
                if (distX >  Protocol.ARENA_WIDTH/2) distX -= Protocol.ARENA_WIDTH;
                if (distX < -Protocol.ARENA_WIDTH/2) distX += Protocol.ARENA_WIDTH;
                if (distY >  Protocol.ARENA_HEIGHT/2) distY -= Protocol.ARENA_HEIGHT;
                if (distY < -Protocol.ARENA_HEIGHT/2) distY += Protocol.ARENA_HEIGHT;
                int dist = (int)Math.round(Math.sqrt(Math.pow(distX, 2.0) + Math.pow(distY, 2)));
                if (minDist > dist) {
                    minDist = dist;
                    stepX = (int)Math.signum(distX);
                    stepY = (int)Math.signum(distY);
                    pWithMinDist = p;
                }
            }
            // do follow the player
            e.pos.iHaveValidatedB4Setting();
            e.pos.accum(stepY * e.speed, stepX * e.speed);
        }
    }
    private void processBulletHit(Bullet bullet, Actor victim, int dmg) {
        if (!bullet.pos.equals(victim.pos)) return; // same pos?
        else if (bullet.ownerID.equals(victim.id)) return; // suicide?
        else if (victim.dead()) return; // ded? dont hit a zombie
        else if (bullet.dead()) return; // dont accidentally call a bullet that had hit
        
        victim.hp.setHP(victim.hp.getHP() - bullet.damage); // e.hp -= bullet.damage;
        bullet.timeLeft(0);
        
        // killer find and set pt
        if (!victim.hp.isDead()) return; // onyl cred pt when ded

        if ("player".equals(victim.type))
            victim.hp.triggerRespawn(true);
        
        Player bOwner = playerIdMap.get(bullet.ownerID);
        if (bOwner == null) return;
        bOwner.score += dmg;
        bOwner.bullets += 10;
    }
    
    // lol wont do dispatch
    private void processActorHit(Actor hitter, Actor victim) {
        if (!hitter.pos.equals(victim.pos)) return; // same pos?
        else if (hitter.id.equals(victim.id)) return; // suicide?
        else if (victim.hp.isDead()) return; // ded? dont hit a zombie
        else if (hitter.hitCooldown() > 0) return; // cooldown

        hitter.startHitCooldown();
        victim.hp.setHP(victim.hp.getHP() - 1); // e.hp -= hitter.damage;
        
        if (victim.hp.isDead() && "player".equals(victim.type))
            victim.hp.triggerRespawn(true);        
    }
    
    public synchronized void processAllCollisions() {
        // -- 1. bullet --
        for (Bullet b : bullets) {
            // bullet hit enemy
            for (Enemy e : enemies) processBulletHit(b, e, 1);
            // bullet hit player
            for (Player p : players) processBulletHit(b, p, 10);
        }

        // corpse clean-up
        enemies.removeIf(e -> e.dead());
        bullets.removeIf(b -> b.dead());        
//        players.removeIf(p -> p.hp.isDead());

        // -- 2. enemy hit player --
        for (Enemy e : enemies)
            for (Player p : players)
                processActorHit(e, p);
    }
    public GameState() {
        avatarMatrix = new Entity.Avatar[Protocol.ARENA_WIDTH][Protocol.ARENA_HEIGHT];
        for (int i = 0; i < Protocol.ARENA_WIDTH; i++)
            for (int j = 0; j < Protocol.ARENA_HEIGHT; j++)
                avatarMatrix[i][j] = new Entity.Avatar(' ', Entity.Avatar.Color.TRANSPARENT);
    }
}
