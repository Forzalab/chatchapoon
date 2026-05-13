package shared;
class AreaBomb extends ItemEffect { 
    AreaBomb(int a) { super("AreaBomb", a); }

    @Override
    public void use(Player user) {}
    
    @Override
    public boolean onHit(Player user) { return false; }

    @Override
    public void tickDown(Player user) { }
}
