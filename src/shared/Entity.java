package shared;

import java.io.*;
import java.util.Random;

public class Entity {
    public static class Avatar {
        public enum Color {
            CYAN,
            YELLOW,
            MAGENTA,
            RED,
            GREEN,
            WHITE,
            BLACK,
            TRANSPARENT
        }

        public char avatar; // can change espc in gane
        public Color avatarColor; // can change

        public Avatar(char avatar, Color color) {
            this.avatar = avatar;
            this.avatarColor = color;
        }
    }
    
    public enum Direction {
        N(0), NE(1), E(2), SE(3), S(4), SW(5), W(6), NW(7), NONE(-1);
        private final int val;
        private Direction(int v) { val = v; }
        public int getVal() { return val; }
        private static final Direction[] vals = values();
        public Direction next() {
            int index = Utility.mod(this.ordinal() + 1, vals.length - 1);
            return vals[index];
        }
        public Direction prev() {
            int index = Utility.mod(this.ordinal() - 1, vals.length - 1);
            return vals[index];
        }
        public static Direction rand() {
            int index = new Random().nextInt(vals.length-2);
            return vals[index];
        }
    };
        
    public volatile Position pos;
    public volatile float vx;
    public volatile float vy;
    public final String id;
    public final String type;
    public Direction direction;
    public volatile Avatar avatar;
    protected volatile boolean dead = false;

    public final boolean dead() { return dead; }
    
    Entity(Position pos, float vx, float vy, String type, String id) {
       this.pos = pos;
       this.vx = vx;
       this.vy = vy;
       this.type = type;
       this.id = id;
       this.direction = Direction.N; // set direction for urself
    }
    
    public static final Entity nullEntity = new Entity(new Position(-420, -69), 0.0f, 0.0f, "", "");

}
