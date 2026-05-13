package shared;
class SmallHeal extends ItemEffect { 
    SmallHeal(int a) { super("SmallHeal", a); }

    @Override
    public void useSpecifics(Player useSpecificsr) {}
    
    @Override
    public boolean onHit(Player useSpecificsr) { return false; }

    @Override
    public void tickDown(Player useSpecificsr) { }
}
