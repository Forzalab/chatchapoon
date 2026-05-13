package shared;
class SmallHeal extends ItemEffect { 
    public SmallHeal(int a) { super("SmallHeal", a); }

    @Override   
    public boolean onHit(Player user) { return false; }   
}
