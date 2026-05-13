package shared;
class FullHeal extends ItemEffect { 

    static {
        IEProperty iep = new IEProperty("100g of Fentanyl", "FDA-approved.\nRefill HP to maximum.", ItemEffect.IEProperty.Rarity.LEGENDARY, Protocol.ONE_USE_ITEM_TIME);
        ItemEffect.register("FullHeal", iep);
    }

    public FullHeal(int a) { super("FullHeal", a); }

    @Override   
    public boolean onHit(Player user) { return false; }   
}
