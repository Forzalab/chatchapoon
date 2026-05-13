package shared;
class RapidFire extends ItemEffect { 
    RapidFire(int a) { super("RapidFire", a); }

    @Override
    public void useSpecifics(Player useSpecificsr) {}
    
    @Override
    public boolean onHit(Player useSpecificsr) { return false; }

    @Override
    public void tickDown(Player useSpecificsr) { }
}
