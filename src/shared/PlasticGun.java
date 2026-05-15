package shared;

class PlasticGun extends ItemEffect {
    static {
        IEProperty iep = new IEProperty("Plas-thicc Gun", "Made in China. Battery not included.\nDoes absolutely nothing.", ItemEffect.IEProperty.Rarity.COMMON, Protocol.ONE_USE_ITEM_TIME);
        ItemEffect.register("PlasticGun", iep);
    }

    @Override
    public boolean onHit(Player user) { return false; }

    PlasticGun(int a) { super("PlasticGun", a); }

    @Override
    public void useSpecifics(Player user) { }
}
