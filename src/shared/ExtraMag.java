package shared;
class ExtraMag extends ItemEffect { 
    public ExtraMag(int a) { super("ExtraMag", a); }

    @Override   
    public boolean onHit(Player user) { return false; }   
}
