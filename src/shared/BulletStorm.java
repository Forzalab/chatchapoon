package shared;
class BulletStorm extends ItemEffect { 
    BulletStorm(int a) { super("BulletStorm", a); }

    @Override
    public void useSpecifics(Player useSpecificsr) {}
    
    @Override
    public boolean onHit(Player useSpecificsr) { return false; }

    @Override
    public void tickDown(Player useSpecificsr) { }
}
