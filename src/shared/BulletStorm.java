package shared;
class BulletStorm extends ItemEffect { 
    BulletStorm(int a) { super("BulletStorm", a); }

    @Override
    public void use(Player user) {}
    
    @Override
    public boolean onHit(Player user) { return false; }

    @Override
    public void tickDown(Player user) { }
}
