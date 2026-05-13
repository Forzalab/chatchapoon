package shared;

import java.io.*;
import java.util.HashMap;

interface Effect {
    void use(Player user);
    default void tickDown(Player user) {}
    default boolean onHit(Player user) { return false; }
}

// every item IS an effect
public class ItemEffect {
    public class IEProperty {
        // Rarity and Countdown type
        public static enum Rarity {
            COMMON,
            RARE,
            LEGENDARY
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
                    System.out.println("Exception caught in ItemEffect.IEProperty.Countdown.Countdown(): " + e);
                }
            }
        }

        // now with stuff IN IEProperty
        public final String desc;
        public final Rarity rarity;
        public volatile Countdown countdown;

        public IEProperty(String desc, Rarity rarity, int max) {
            this.desc = desc;
            this.rarity = rarity;
            this.countdown = new Countdown(max);
        }
    }

    public final String name;
    public IEProperty property;
    public final int amount;

    // <effect-name, property> PUT IN PROTOCOL FILE PLS
//    public static final HashMap<String, IEProperty> lookup = new HashMap<String, IEProperty>();
    
    public ItemEffect(String name, int amount) {
  //      IEProperty iep = lookup.get(name);
        this.name = name;
        this.property = new IEProperty("blah", IEProperty.Rarity.COMMON, 67);  /*iep MUST IMPLEMENT*/
        this.amount = amount;
    }

    public ItemEffect(String name) {
        this(name, 1);
    }
    
    public Boolean isActive() {
        return (property.countdown.getRemaining() > 0);
    }

    public synchronized void use() {
        property.countdown.setRemaining(property.countdown.max);
    }

    public ItemEffect mutateAmount(int new_amt) {
        return new ItemEffect(name, new_amt);
    }
}

