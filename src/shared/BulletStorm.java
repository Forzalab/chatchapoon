package shared;

class BulletStorm extends ItemEffect { 
    @Override
    public boolean onHit(Player user) { return false; }

    static {
        IEProperty iep = new IEProperty("Prefilled AK-67 Costco Edition", "For when spamming the keyboard is not enough.\nAdds 267 bullets and no cooldown when used.", IEProperty.Rarity.LEGENDARY, 20);
        ItemEffect.register("BulletStorm", iep);
    }
    
    BulletStorm(int a) { 
        super("BulletStorm", a);
    }

    @Override
    public void useSpecifics(Player user) { user.bullets += 267; }
}
