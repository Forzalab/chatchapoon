package shared;
class Ghost extends ItemEffect { 
    @Override
    public boolean onHit(Player user) { return false; }
    
    public Ghost(int a) { super("Ghost", a); }

}
