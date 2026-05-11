package shared;

import java.io.*;
import java.util.LinkedHashMap;

public class Player extends Actor {
/*    public volatile Position pos;
    public volatile float vx;
    public volatile float vy;
    public final String id;
    public final String type;

    Entity(Position pos, float vx, float vy, String type, String id) {
       this.pos = pos;
       this.vx = vx;
       this.vy = vy;
       this.type = type;
       this.id = id;
    */

    public class Inventory {
        // i trust myself to not tocuh this list raw
        // unless for iterating shit
        public volatile LinkedHashMap<String, ItemEffect> inventory;

        private synchronized void changeAmount(String name, int amt) {
            ItemEffect old = get(name);
            this.inventory.put(name, old.mutateAmount(old.amount+1));
        }
        
        public synchronized void add(String name, int amt) {
            // have item? add the amt in!
            if (this.inventory.containsKey(name)) {
                ItemEffect stock = get(name);
                changeAmount(name, amt + stock.amount);
            }
            // dont? add it in.
            else {
                ItemEffect item = new ItemEffect(name, amt);
                this.inventory.put(name, item);
            }
        }
        
        public ItemEffect get(String name) {
            return this.inventory.get(name);
        }
        
        public synchronized void rmv(String name, int amt) {
            changeAmount(name, get(name).amount - amt);
        }
    }
    
    public volatile int score = 0;
    public volatile int currency;
    public final Position spawnPos;
    public final String name;

    // <item-name, Item> to which Item has amount
    public volatile Inventory inventory;
    public volatile int fireCooldown, bullets = 100;
   // no color (in Avatar)
   // no isDead, maxHP, deathTimer (in Actor.HP.isDead())

    public Player(Position pos, float vx, float vy, String id, int hp_max, String name) {
        super(pos, vx, vy, "player", id, hp_max);
        this.spawnPos = new Position(pos.getRenderY(), pos.getRenderX());
        this.name = name;
    }
}
