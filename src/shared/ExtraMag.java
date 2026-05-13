package shared;
class ExtraMag extends ItemEffect { 
    ExtraMag(int a) { super("ExtraMag", a); }

    @Override
    public void useSpecifics(Player user) {}
    
    @Override
    public boolean onHit(Player user) { return false; }

    @Override
    public void tickDown(Player user) { }
}
