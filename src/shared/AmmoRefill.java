package shared;

class AmmoRefill extends ItemEffect { 
    public static IEProperty property = new IEProperty("Gun Magazine", "The newest magazine for your gun!\nAdds 150 bullets when used.", ItemEffect.IEProperty.Rarity.COMMON, Protocol.ONE_USE_ITEM_TIME);

    static { ItemEffect.register("AmmoRefill", property); }

    @Override
    public boolean onHit(Player user) { return false; }
    
    AmmoRefill(int a) { 
        super("AmmoRefill", a);
    }

    @Override
    public void useSpecifics(Player user) { user.bullets += 150; }
}
