package shared;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.*;

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
        public volatile LinkedHashMap<String, ItemEffect> inventory = new LinkedHashMap<String, ItemEffect>();

        // overide amount
        private synchronized void changeAmount(String name, int amt) { try {
            ItemEffect item = get(name);

            // tombstone
            // collect dead keys first in a list, then remove after loop
            if (item.amount() <= 0 || item.countdown.getRemaining() <= 0)
                throw new Exception("No update a \"null\" item! item.amount = " + item.amount() +"; item.Countdown.remaining = " + item.countdown.getRemaining());

            item.mutateAmount(amt);
            this.inventory.put(name, item);
        } catch (Exception e) { System.out.println("Exp Player.Inventory.changeAmount(): item.mutateAmount(item.amount()+amt);"); } }
        

        // ONLY ADD ITEM FROM GACHA!!
        // onec added, use name to crawl
        public synchronized void add(ItemEffect item) {
            // have item? add the amt in!
            if (this.inventory.containsKey(item.name)) {
                ItemEffect stock = get(item.name);
                changeAmount(item.name, item.amount() + stock.amount());
            }
            // dont? add it in.
            else {
                this.inventory.put(item.name, item);
            }
        }

        public synchronized void useAll() {
            for (Map.Entry<String, ItemEffect> entry : inventory.entrySet())
                entry.getValue().use(Player.this); 
            inventory.entrySet().removeIf(
                entries->(entries.getValue().amount() == 0)
            );
        }
        
        public ItemEffect get(String name) {
            return this.inventory.get(name);
        }
        
        public synchronized void rmv(String name, int amt) {
            changeAmount(name, get(name).amount() - amt);
        }

        public synchronized void wipe() {
            inventory.clear();
        }
        
    }
    
    public volatile int score = 0;
    public volatile int currency = Protocol.NEW_MONEY;
    public final Position spawnPos;
    public final String name;
    public volatile int pityCounter = 0;
    public volatile int lives = 3;
    public volatile LinkedHashMap<String, String> milestone = new LinkedHashMap<String, String>();

    // id input only pls
    public String checkAddMilestone(String s) {
        String mst = milestone.getOrDefault(s, "");
        if (!mst.isEmpty()) return "HAD";

        // money
        if (s == "NEW_GACHA" && currency < Protocol.GACHA_COST && currency > (int)Math.round(Protocol.GACHA_COST * 0.5))  {
            int coinLeft = (int)Math.round(Protocol.GACHA_COST*0.5);
            String status = coinLeft + " more coin" + ((coinLeft > 1)?"s":"") + " to your first Gacha pull! :3";
            milestone.put(s, status);
            return status;
        }

        else if (s == "CAN_FIRST_GACHA" && currency >= Protocol.GACHA_COST) {
            String status = "Try your first GACHA! Pull one with [G].";
            milestone.put(s, status);
            return status;
        }

        // health
        else if (s == "NEAR_DEATH" && hp.justResus() && lives > 0 && lives < Math.min(4, Protocol.PLAYER_RESPAWN_ATTEMPT)) {
            String logo = lives < 2 ? "⼀  " : lives < 3 ? "⼆  " : "〣  ";
            String status = logo + String.valueOf(Protocol.PLAYER_RESPAWN_ATTEMPT - lives) + " li" + ((Protocol.PLAYER_RESPAWN_ATTEMPT - lives>1)?"ves":"fe") + " down, " + lives + " to go.";
            milestone.put(s, status);
            return status;
        }
        

        return "UKN";
    }

    // <item-name, Item> to which Item has amount
    public volatile Inventory inventory;
    public volatile int fireCooldown, bullets = 75;
   // no color (in Avatar)
   // no isDead, maxHP, deathTimer (in Actor.HisDead())

    public Player(Position pos, float vx, float vy, String id, int hp_max, String name) {
        super(pos, vx, vy, "player", id, hp_max);
        this.spawnPos = new Position(pos.getRenderY(), pos.getRenderX());
        this.name = name;
        this.inventory = new Inventory();
    }
}
