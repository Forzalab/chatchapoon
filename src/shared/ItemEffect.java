package shared;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

interface Effect {
    void useSpecifics(Player user);
    void tickDown(Player user);
    boolean onHit(Player user);
}

// every item IS an effect
public abstract class ItemEffect implements Effect {
    public static class IEProperty {
        // Rarity and Countdown type
        public static enum Rarity {
            COMMON,
            RARE,
            LEGENDARY,
            NA
        }

        // now with stuff IN IEProperty
        public final String displayName, desc;
        public String name;
        public final Rarity rarity;
        public final int time;

        public IEProperty(String displayName, String desc, Rarity rarity, int max) {
            this.displayName = displayName;
            this.desc = desc;
            this.rarity = rarity;
            this.time = max;
        }
    }

    // Effect time left
    public class Countdown {
        private int remaining;
        public final int max;
        private void setRemaining(int time) {
            if (remaining == Protocol.ONE_USE_ITEM_TIME_ACTIVE && time == 0) {
                remaining = Protocol.ONE_USE_ITEM_TIME;
                return;
            }
            else if (remaining == Protocol.ONE_USE_ITEM_TIME && time == max) {
                remaining = Protocol.ONE_USE_ITEM_TIME_ACTIVE;
                mutateAmount(amount - 1);
                return;
            }
            remaining = (time < 0) ? 0 : time;
            remaining = (time > max) ? max : remaining;
            if (remaining <= 0) mutateAmount(amount - 1);
        }
        public int getRemaining() {
            return remaining;
        }
        public Countdown(int max) {
            this.max = max;
            try {
                if (max < 0 && !(max == Protocol.ONE_USE_ITEM_TIME) && !(max == Protocol.ONE_USE_ITEM_TIME_ACTIVE) )
                    throw new Exception("dumbass set max time to less than zero");
            }
            catch (Exception e) {
                System.out.println("Exception caught in ItemEffect.countdown.Countdown(): " + e);
            }
        }
    }
    
    public final String name;
    public IEProperty property;
    private int amount;
    public volatile Countdown countdown;
    
    // An item can only be applied ONE at a time, no double-stacking. Each use deduct once, only whem eff drained can next item of same type be used.

    // <effect-name, property> PUT IN PROTOCOL FILE PLS
    public static LinkedHashMap<String, IEProperty> lookup = new LinkedHashMap<>();

    protected static boolean register(String subItemClassName, IEProperty iep) {
        iep.name = subItemClassName;
        boolean result = (null != lookup.putIfAbsent(subItemClassName, iep));
        return result;
        // subitem must pack their own property.
        // must be static final
    }
    
    protected ItemEffect(String name, int amount) {
  //      IEProperty iep = lookup.get(name);
        this.name = name;
        this.property = lookup.get(name);  /*iep MUST IMPLEMENT*/
        this.amount = amount;
        this.countdown = new Countdown(this.property.time);        
    }

    public Boolean isActive() {
        if (amount <= 0) return false;
        return (countdown.getRemaining() > 0 || countdown.getRemaining() == Protocol.ONE_USE_ITEM_TIME_ACTIVE);
    }

    @Override
    public final void tickDown(Player p) {
        if (countdown.max == Protocol.ONE_USE_ITEM_TIME) return;
        countdown.setRemaining(countdown.getRemaining()-1);
    }

    @Override
    public void useSpecifics(Player p) {}
    
    public synchronized final void use(Player user) {
        if (amount <= 0) return;
        else if (isActive()) return; 
        countdown.setRemaining(countdown.max);
        this.useSpecifics(user);
    }

    // Item is endUse when manually triggered
    // tick down enough, it will also endUse, but this is handled in setCountdown directly
    public synchronized final void forceEndUse(Player user) {
        countdown.setRemaining(0);
    }
    
    public int amount() { return (amount > 0)?amount:0; }
    
    public void mutateAmount(int new_amt) {
        if (new_amt < 0) return;
        else if (amount <= 0) return;
        amount = new_amt;
    }

    // name MUST MATCH ITEM CLASS NAME!!!
    public static ItemEffect create(String name, int amt) { try {
        // reflection to avoid if-else
        ItemEffect item = (ItemEffect)Class.forName("shared." + name)
                               .getDeclaredConstructors()[0] // blinddart
                               .newInstance(amt);
        return item;
    } catch (Exception e) { System.out.println(e); return null; }}
}

