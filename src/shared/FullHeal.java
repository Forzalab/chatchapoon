package shared;
class FullHeal extends ItemEffect { 
    public FullHeal(int a) { super("FullHeal", a); }

    @Override   
    public boolean onHit(Player user) { return false; }   
}
