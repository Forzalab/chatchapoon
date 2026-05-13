package shared;

class BulletStorm extends ItemEffect { 
    @Override
    public boolean onHit(Player user) { return false; }

    static {
        IEProperty iep = new IEProperty("Gun Magazine Costco-Edition", "Only $67.99, but it's yours for free (not really)! \nAdds 670 bullets when used.", IEProperty.Rarity.LEGENDARY, Protocol.ONE_USE_ITEM_TIME);
        ItemEffect.register("BulletStorm", iep);
    }
    
    BulletStorm(int a) { 
        super("BulletStorm", a);
    }

    @Override
    public void useSpecifics(Player user) { user.bullets += 670; }
}
