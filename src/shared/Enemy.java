package shared;

import java.io.*;

public class Enemy extends Actor {
/* 
    ref:
    public volatile Position pos;
    public volatile float vx;
    public volatile float vy;
    public final String id;
    public final String type;
*/
/*    public enum Direction {
        N, NE, E, SE, S, SW, W, NW
    };

    public class HP {
        private int _hp;
        public final int _hpMax; // 'int' will bite me in the ass after
        public int deathTimer = 0; // some class need to handle subtraction lol
        public Boolean willRespawn = false;
        
        public HP(int hpMax) { 
            this._hpMax = hpMax; 
            try {
                if (hpMax <= 0)
                    throw new Exception("hpMax shouldnt be <= 0!");
                this.setHP(_hpMax);
            } catch (Exception e) {
                System.out.println("Exception caught in Actor.HP cstor: " + e);
            }
        }
        
        public synchronized void setHP(int hp) {
            try {
                if (_hp <= 0) throw new Exception("Actor already ded, double stabbing!");
                this._hp = (hp >= 0) ? hp : 0;
                this._hp = (hp <= _hpMax) ? _hp : _hpMax;
             } catch (Exception e) {
                System.out.println("Exception caught in Actor.HP.setHP(): " + e);
             }
        }

        public int getHP() {
            return _hp;
        }

        // VERY BAD. have to track 2 fucntion together. 
        // mut add soem form of takeDamge() somehow.
        private synchronized void triggerRespawn(Boolean will_respawn) {
            deathTimer = (!will_respawn) ? Protocol.DEATH_COOLDOWN_PERM : Protocol.DEATH_COOLDOWN;
        }

        public synchronized void resuscitate() {
            if (deathTimer > 0) return;
            _hp = _hpMax;
        }
        
        public Boolean isDead() {
            return (_hp <= 0);
        }

        public Boolean isDeadPerm() {
            return (_hp <= 0 && deathTimer == Protocol.DEATH_COOLDOWN_PERM);
        }
    }

    public Direction direction = Direction.N;
    public volatile HP hp;
*/
    public String behaviourType;
    public int moveCooldownTimer;
        
    Enemy(Position pos, float vx, float vy, String type, String id, int hpMax, String behaviourType, int moveCooldownTimer) {
        super(pos, vx, vy, type, id, hpMax);
        this.behaviourType = behaviourType;
        this.moveCooldownTimer = moveCooldownTimer;
    }
}
