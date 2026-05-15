package shared;

class BankFlyer extends ItemEffect {
    static {
        IEProperty iep = new IEProperty("Bank Flyer", "-0.05% APY! You pay US to keep YOUR money!\nDoes absolutely nothing.", ItemEffect.IEProperty.Rarity.COMMON, Protocol.ONE_USE_ITEM_TIME);
        ItemEffect.register("BankFlyer", iep);
    }

    @Override
    public boolean onHit(Player user) { return false; }

    BankFlyer(int a) { super("BankFlyer", a); }

    @Override
    public void useSpecifics(Player user) { }
}
