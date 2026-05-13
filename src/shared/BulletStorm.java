package shared;
class BulletStorm extends ItemEffect { 
    public BulletStorm(int a) { super("BulletStorm", a); }

    @Override   
    public boolean onHit(Player user) { return false; }   
}
