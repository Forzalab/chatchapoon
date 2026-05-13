package shared;
class FullHeal extends ItemEffect { 
    FullHeal(int a) { super("FullHeal", a); }

    @Override
    public void useSpecifics(Player useSpecificsr) {}
    
    @Override
    public boolean onHit(Player useSpecificsr) { return false; }

    @Override
    public void tickDown(Player useSpecificsr) { }
}
