package shared;
class CoinMagnet extends ItemEffect { 
    public CoinMagnet(int a) { super("CoinMagnet", a); }

    @Override   
    public boolean onHit(Player user) { return false; }   
}
