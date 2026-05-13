package shared;
class AreaBomb extends ItemEffect { 
    public AreaBomb(int a) { super("AreaBomb", a); }

    @Override   
    public boolean onHit(Player user) { return false; }   
}
