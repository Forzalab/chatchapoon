package shared;

class AmmoRefill extends ItemEffect { 
    static { 
        IEProperty iep = new IEProperty("Gun Magazine", "The newest magazine for your gun!\nAdds 85 bullets when used.", ItemEffect.IEProperty.Rarity.COMMON, Protocol.ONE_USE_ITEM_TIME);
        ItemEffect.register("AmmoRefill", iep);
    }

    @Override
    public boolean onHit(Player user) { return false; }
    
    AmmoRefill(int a) { 
        super("AmmoRefill", a);
    }

    @Override
    public void useSpecifics(Player user) { user.bullets += 85; }
}
