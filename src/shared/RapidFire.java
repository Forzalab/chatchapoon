package shared;
class RapidFire extends ItemEffect { 
    RapidFire(int a) { super("RapidFire", a); }

    @Override
    public void useSpecifics(Player user) {}
    
    @Override
    public boolean onHit(Player user) { return false; }

    @Override
    public void tickDown(Player user) { }
}
