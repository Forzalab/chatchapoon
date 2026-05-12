package shared;

import java.util.concurrent.*;
import java.util.HashMap;
import java.util.Arrays;
import java.util.*;

public class GameState {
/*
List<Player/Enemy/  Bullet> + playerById + nextId() + colorTaken[] + tickCounter, levelTimer, waveNumber + terrain[][]
*/
    private int[] idCounter = {0, 0, 0}; // hardcoded P E B
    private Random r = new Random();
    private State state = State.LOBBY;
    private static long inception;
    public List<Player> players = new CopyOnWriteArrayList<Player>();
    public List<Enemy> enemies = new CopyOnWriteArrayList<Enemy>();
    public List<Bullet> bullets = new CopyOnWriteArrayList<Bullet>();
    public ConcurrentHashMap<String, Player> playerIdMap = new ConcurrentHashMap<String, Player>();
    public HashSet<Entity.Avatar.Color> colorTaken = new HashSet<Entity.Avatar.Color>();
    public HashSet<String> idTaken = new HashSet<String>();
    public LinkedHashMap<Position, Integer> coinsLoc = new LinkedHashMap<Position, Integer>();
//    public 
    
    // game state 3 ones
    public static enum State {
        LOBBY(0), // alow join    
        BATTLE(1), // bl6ocm all join
        POST_BATTLE(2); // block all join        
        private final int val;
        private State(int v) { val = v; }
        private int getVal() { return val; }
        private static final State[] vals = values();
        private static State statePrev;
        private State next() {
          int index = Utility.mod(this.ordinal() + 1, vals.length);
          statePrev = this;
          return vals[index];
        }
    }
//    private State state = State.LOBBY;
    public synchronized State get() {
        return state;
    }
    public synchronized State getPrev() {
        return State.statePrev;
    }
    public synchronized void switchNextState() {
        state = state.next();
    }
    
//    public ServerState 
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
        if (entity instanceof Player p) processCollectableCoin(p);
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
        for (Entity e : players)
            if (authorID.equals(e.id)) entity = e;
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
        waveNumber++;

        // pick side -> rnd coords
        int side = r.nextInt(11);
        int wx = -156, wy = -751;

        // 0 1 2 3 clockwise, 0 north.
        // always subtract 1 for bounding shit

        for (int i = 0; i < amt; i++) {
            int type = r.nextInt(11);

            float rSpeed = (r.nextInt(7 - 3) + 3) * 0.01f;
            
            String etype = "";
            if (type == 0) { etype = "COPS"; }
            else if (type == 1) { etype = "SNIPER"; rSpeed = 0; }        
            else if (type == 2) { etype = "PATROL"; }        
//            else if (type == 3) { etype = "CAPTAIN"; }        

            int tempSide = ("SNIPER".equals(type)) ? (side % 4) : side;
            if (tempSide == 0) {
                wx = r.nextInt(Protocol.ARENA_WIDTH - 1);
                wy = 1;
            } else if (tempSide == 1) {
                wy = r.nextInt(Protocol.ARENA_HEIGHT - 1);
                wx = Protocol.ARENA_WIDTH - 1;
            } else if (tempSide == 2) {
                wx = r.nextInt(Protocol.ARENA_WIDTH - 1);
                wy = Protocol.ARENA_HEIGHT - 1;
            } else if (tempSide == 3) {
                wy = r.nextInt(Protocol.ARENA_HEIGHT - 1);
                wx = 1;
            } else if (tempSide >= 4 && tempSide <= 7) { 
                int cornerOffset = 10;
                wx = (tempSide == 4) ? r.nextInt(cornerOffset) : Protocol.ARENA_WIDTH - 1 - r.nextInt(cornerOffset);
                wy = (tempSide == 5) ? r.nextInt(cornerOffset) : Protocol.ARENA_HEIGHT - 1 - r.nextInt(cornerOffset);
            } else if (tempSide >= 8 && tempSide <= 11) {
                int angle = r.nextInt(360) * 6;
                wx = (int) (Protocol.ARENA_WIDTH/2 + Math.cos(angle) * 20);
                wy = (int) (Protocol.ARENA_HEIGHT/2 + Math.sin(angle) * 20);
            }

            Enemy e = new Enemy(new Position(wy, wx), 0, 0, "enemy", registerNewId("enemy"), Protocol.PLAYER_HP_MAX, etype, 0, rSpeed);
            e.direction = ("SNIPER".equals(etype)) ? Entity.Direction.rand() : Entity.Direction.NONE;
            enemies.add(e);            
        }
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
            String type = e.behaviourType;
            if ("PATROL".equals(type)) {
                int cycle = (int)(tickCounter % 360) / 18 * 3;
                float sinShift = (float)Math.sin(cycle);
                e.pos.accum((int)(sinShift * e.speed * Protocol.DRIFTER_SINE_AMP * Protocol.DRIFTER_SINE_FREQ), e.speed);
            }
            else if ("SNIPER".equals(type)) {
                e.pos.accum(0, 0);
                if (e.moveCooldownTimer%Protocol.FIRE_COOLDOWN_TICKS==0 && e.moveCooldownTimer != 0) e.direction = Entity.Direction.rand();
                /*if (tickCounter%4 == 0)*/ shootFrom(e);
            }
            else if ("COPS".equals(type))  e.pos.accum(stepY * e.speed, stepX * e.speed);
        }
    }
    private void processBulletHit(Bullet bullet, Actor victim, int dmg) {
        if (!bullet.pos.equals(victim.pos)) return; // same pos?
//        else if (bullet.ownerID.equals(victim.id)) return; // suicide?
        else if (victim.dead()) return; // ded? dont hit a zombie
        else if (bullet.dead()) return; // dont accidentally call a bullet that had hit

        int damage = bullet.damage;
        if (victim instanceof Enemy e && bullet.ownerID.equals(victim.id)) damage = 0;
        else if (bullet.ownerID.equals(victim.id)) damage = 1;
        
        victim.hp.setHP(victim.hp.getHP() - damage); // e.hp -= bullet.damage;
        bullet.timeLeft(0);
        
        // killer find and set pt
        if (!victim.hp.isDead()) return; // onyl cred pt when ded

        if ("player".equals(victim.type))
            victim.hp.triggerRespawn(true);
        
        Player bOwner = playerIdMap.get(bullet.ownerID);
        if (bOwner == null) return;
        bOwner.score += dmg;

        if (victim instanceof Enemy e) {
            int[] weights = {40,25,20,10,5};
            int roll = r.nextInt(100);
            int cum = 0, coins = 1;
            for (int i = 0; i < weights.length; i++) {
                cum += weights[i];
                if (roll < cum) { coins = i+1; break; }
            }

            // map coin loc
            Position pos = new Position(e.pos.getRenderY(), e.pos.getRenderX());
            Integer coinAtAmt = coinsLoc.get(pos);
            if (coinAtAmt != null) coins += coinAtAmt;
            if (coins > 0) coinsLoc.put(pos, coins);

//            bOwner.currency += coins;
            bOwner.bullets += 1;
        }
        else if (victim instanceof Player p) {
            int coins = p.currency;
            Position pos = new Position(p.pos.getRenderY(), p.pos.getRenderX());
            Integer coinAtAmt = coinsLoc.get(pos);
            if (coinAtAmt != null) coins += coinAtAmt;
            coinsLoc.put(pos, coins); p.currency = 0;
            bOwner.bullets += p.bullets; p.bullets = 0;
        }
    }
    
    public synchronized void processCollectableCoin(Player p) {
        Position pos = new Position(p.pos.getRenderY(), p.pos.getRenderX());
        Integer coin = coinsLoc.remove(pos);
        if (coin == null) return;
        p.currency += coin;
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

    public synchronized String pull(Player p) {
        if (p.currency < Protocol.GACHA_COST) return "";
        p.currency -= Protocol.GACHA_COST;
        p.pityCounter++;
        float roll = (float)Math.random();
        String rarity;
        if (p.pityCounter >= 30) rarity = "LEGENDARY";
        else if (p.pityCounter >= 10) rarity = "RARE";
        else rarity = roll < 0.85 ? "COMMON" : roll < 0.99 ? "RARE" : "LEGENDARY";
        if (!rarity.equals("COMMON")) p.pityCounter = 0;
        return rarity;
    }
    
    public GameState(long inception) {
            this.inception = inception;
        avatarMatrix = new Entity.Avatar[Protocol.ARENA_WIDTH][Protocol.ARENA_HEIGHT];
        for (int i = 0; i < Protocol.ARENA_WIDTH; i++)
            for (int j = 0; j < Protocol.ARENA_HEIGHT; j++)
                avatarMatrix[i][j] = new Entity.Avatar(' ', Entity.Avatar.Color.TRANSPARENT);
    }
}
