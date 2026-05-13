package shared;
class Shield extends ItemEffect { 

    static {
        IEProperty iep = new IEProperty("Plas-thicc Shield", "Doesn't protect against Canvas hackers tho. Trust me, I tested it, it doesn't.\nAbsorbs 1 hit from any oppoent.", ItemEffect.IEProperty.Rarity.RARE,
Protocol.ONE_USE_ITEM_TIME);
        ItemEffect.register("Shield", iep);
    }

    public Shield(int a) { super("Shield", a); }

    @Override   
    public boolean onHit(Player user) { 
        this.forceEndUse(user);
        return true;
    }   
}
