package shared;

class ExtraMag extends ItemEffect { 
    @Override
    public boolean onHit(Player user) { return false; }

    static {
        IEProperty iep = new IEProperty("Gun Magazine Fanily-Sized", "Pet-friendly and safe for children! \nAdds 150 bullets when used.", IEProperty.Rarity.RARE, Protocol.ONE_USE_ITEM_TIME);
        ItemEffect.register("ExtraMag", iep);
    }
    
    ExtraMag(int a) { 
        super("ExtraMag", a);
    }

    @Override
    public void useSpecifics(Player user) { user.bullets += 150; }
}
