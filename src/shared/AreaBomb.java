package shared;
class AreaBomb extends ItemEffect { 
    AreaBomb(int a) { super("AreaBomb", a); }

    @Override
    public void useSpecifics(Player useSpecificsr) {}
    
    @Override
    public boolean onHit(Player useSpecificsr) { return false; }

    @Override
    public void tickDown(Player useSpecificsr) { }
}
