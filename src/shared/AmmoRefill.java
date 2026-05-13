package shared;
class AmmoRefill extends ItemEffect { 
    AmmoRefill(int a) { super("AmmoRefill", a); }

    @Override
    public void useSpecifics(Player useSpecificsr) {}
    
    @Override
    public boolean onHit(Player useSpecificsr) { return false; }

    @Override
    public void tickDown(Player useSpecificsr) { }
}
