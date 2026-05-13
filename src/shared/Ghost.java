package shared;
class Ghost extends ItemEffect { 
    Ghost(int a) { super("Ghost", a); }

    @Override
    public void useSpecifics(Player user) {}
    
    @Override
    public boolean onHit(Player user) { return false; }

    @Override
    public void tickDown(Player user) { }
}
