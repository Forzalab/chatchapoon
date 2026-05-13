package shared;
class Shield extends ItemEffect { 
    public Shield(int a) { super("Shield", a); }

    @Override   
    public boolean onHit(Player user) { return false; }   
}
