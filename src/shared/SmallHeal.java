package shared;
class SmallHeal extends ItemEffect { 
    SmallHeal(int a) { super("SmallHeal", a); }

    @Override
    public void useSpecifics(Player user) {}
    
    @Override
    public boolean onHit(Player user) { return false; }

    @Override
    public void tickDown(Player user) { }
}
