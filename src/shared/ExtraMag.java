package shared;
class ExtraMag extends ItemEffect { 
    ExtraMag(int a) { super("ExtraMag", a); }

    @Override
    public void useSpecifics(Player useSpecificsr) {}
    
    @Override
    public boolean onHit(Player useSpecificsr) { return false; }

    @Override
    public void tickDown(Player useSpecificsr) { }
}
