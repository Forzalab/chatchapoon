package shared;
class FullHeal extends ItemEffect { 
    FullHeal(int a) { super("FullHeal", a); }

    @Override
    public void useSpecifics(Player user) {}
    
    @Override
    public boolean onHit(Player user) { return false; }

    @Override
    public void tickDown(Player user) { }
}
