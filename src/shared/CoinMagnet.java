package shared;
class CoinMagnet extends ItemEffect { 
    CoinMagnet(int a) { super("CoinMagnet", a); }

    @Override
    public void useSpecifics(Player useSpecificsr) {}
    
    @Override
    public boolean onHit(Player useSpecificsr) { return false; }

    @Override
    public void tickDown(Player useSpecificsr) { }
}
