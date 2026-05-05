package shared;

import java.io.*;

public class Entity {
    public class RenderProperty {
        public enum Color {
            CYAN,
            YELLOW,
            MAGENTA,
            RED,
            GREEN,
            WHITE
        }

        public char avatar; // can change espc in gane
        public Color avatarColor; // can change

        public RenderProperty(char avatar, Color color) {
            this.avatar = avatar;
            this.avatarColor = color;
        }
    }
    
    public volatile Position pos;
    public volatile float vx;
    public volatile float vy;
    public final String id;
    public final String type;
    public volatile RenderProperty renderProperty;
    
    Entity(Position pos, float vx, float vy, String type, String id) {
       this.pos = pos;
       this.vx = vx;
       this.vy = vy;
       this.type = type;
       this.id = id;
    }
}
