package shared;
class Shield extends ItemEffect { 
    Shield(int a) { super("Shield", a); }

    @Override
    public void use(Player user) {}
    
    @Override
    public boolean onHit(Player user) { return false; }

    @Override
    public void tickDown(Player user) { }
}
