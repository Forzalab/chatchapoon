package shared;

import java.io.*;
import java.util.HashMap;

interface Effect {
    void use(Player user);
    void tickDown(Player user);
    boolean onHit(Player user);
}

// every item IS an effect
public abstract class ItemEffect implements Effect {
    public class IEProperty {
        // Rarity and Countdown type
        public static enum Rarity {
            COMMON,
            RARE,
            LEGENDARY
        }

        // now with stuff IN IEProperty
        public final String displayName, desc;
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
            remaining = (time < 0) ? 0 : time;
            remaining = (time > max) ? max : remaining;
        }
        public int getRemaining() {
            return remaining;
        }
        public Countdown(int max) {
            this.max = max;
            try {
                if (max < 0) 
                    throw new Exception("dumbass set max time to less than zero");
                setRemaining(max);
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
    private static HashMap<String, IEProperty> lookup = new HashMap<String, IEProperty>();

    protected static boolean register(String subItemClassName, IEProperty iep) {
        boolean result = (null != lookup.putIfAbsent(subItemClassName, iep));
        return result;
        // subitem must pack their own property.
        // must be static final
    }
    
    private ItemEffect(String name, int amount) {
  //      IEProperty iep = lookup.get(name);
        this.name = name;
        this.property = lookup.get(name);  /*iep MUST IMPLEMENT*/
        this.amount = amount;
        this.countdown = new Countdown(this.property.time);        
    }

    public Boolean isActive() {
        if (amount <= 0) return false;
        return (countdown.getRemaining() > 0);
    }

    public synchronized final void use() {
        if (amount <= 0) return;
        else if (isActive()) return;
        countdown.setRemaining(countdown.max);
    }

    public int amount() { return (amount > 0)?amount:0; }
    
    public void mutateAmount(int new_amt) {
        if (new_amt < 0) return;
        else if (amount <= 0) return;
        amount = new_amt;
    }
}

