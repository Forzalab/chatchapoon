package shared;

class AmmoRefill extends ItemEffect { 
    @Override
    public boolean onHit(Player user) { return false; }
    
    AmmoRefill(int a) { 
        super("AmmoRefill", a);
        IEProperty iep = new IEProperty("Gun Magazine", "The newest magazine for your gun!\nAdds 150 bullets when used.", IEProperty.Rarity.COMMON, Protocol.ONE_USE_ITEM_TIME);
        ItemEffect.register("AmmoRefill", iep);
    }
}
