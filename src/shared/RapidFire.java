package shared;
class RapidFire extends ItemEffect { 
    public RapidFire(int a) { super("RapidFire", a); }

    @Override   
    public boolean onHit(Player user) { return false; }   
}
