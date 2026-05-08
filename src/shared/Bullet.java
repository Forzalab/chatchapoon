package shared;

import java.io.*;

public class Bullet extends Entity {
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
    }
*/
    public int damage;
    public String ownerID;
    public int splitDepth;
    private int timeLeft;

    public void timeLeft(int tl) {
        if (tl <= 0) dead = true;
        timeLeft = tl;
    }

    public int timeLeft() { return timeLeft; }
    
    public Bullet(Position pos, float vx, float vy, String id, int damage, String ownerID, int splitDepth) {
        super(pos, vx, vy, "bullet", id);
        this.avatar = new Avatar('*', Avatar.Color.WHITE);
        this.ownerID = ownerID;
        this.damage = damage;
        this.splitDepth = splitDepth;
        this.timeLeft = Protocol.BULLET_LIFETIME;
    }
}
