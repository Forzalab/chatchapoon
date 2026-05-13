package shared;
class CoinMagnet extends ItemEffect { 
    CoinMagnet(int a) { super("CoinMagnet", a); }

    @Override
    public void useSpecifics(Player user) {}
    
    @Override
    public boolean onHit(Player user) { return false; }

    @Override
    public void tickDown(Player user) { }
}
