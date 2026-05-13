package shared;
class CoinMagnet extends ItemEffect { 
    static {
        IEProperty iep = new IEProperty("Coin Magnet", "Magnets, how do they work?\nWill collect nearby coins on your behalf.", ItemEffect.IEProperty.Rarity.RARE, 5);
        ItemEffect.register("CoinMagnet", iep);
    }

    CoinMagnet(int a) { super("CoinMagnet", a); }

    @Override   
    public boolean onHit(Player user) { return false; }   
}
