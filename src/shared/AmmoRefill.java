package shared;
class AmmoRefill extends ItemEffect { 
    AmmoRefill(int a) { super("AmmoRefill", a); }

    @Override
    public void use(Player user) {}
    
    @Override
    public boolean onHit(Player user) { return false; }

    @Override
    public void tickDown(Player user) { }
}
