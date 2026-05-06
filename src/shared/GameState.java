package shared;

import java.util.concurrent.*;
import java.util.HashMap;
import java.util.*;

public class GameState {
/*
List<Player/Enemy/Bullet> + playerById + nextId() + colorTaken[] + tickCounter, levelTimer, waveNumber + terrain[][]
*/
    private int[] idCounter = {0, 0, 0}; // hardcoded P E B
    List<Player> players = new CopyOnWriteArrayList<Player>();
    List<Enemy> enemies = new CopyOnWriteArrayList<Enemy>();
    List<Bullet> bullets = new CopyOnWriteArrayList<Bullet>();
    private HashMap<String, Player> playerIdMap = new HashMap<String, Player>();
    public Player playerById(String id) {
        return playerIdMap.get(id);
    }
    public String genNextId(String type) {
        String newId = type;
        if (type == "player") newId += idCounter[0]++;
        else if (type == "enemy") newId += idCounter[1]++;
        else if (type == "bullet") newId += idCounter[2]++;
        return newId;
    }
}
