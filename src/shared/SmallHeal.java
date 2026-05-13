package shared;
class SmallHeal extends ItemEffect { 
    
    static {
        IEProperty iep = new IEProperty("Band-Eid®", "...for all your problems.\nAdds 2 HP.", ItemEffect.IEProperty.Rarity.COMMON,
Protocol.ONE_USE_ITEM_TIME);
        ItemEffect.register("SmallHeal", iep);
    }

    public SmallHeal(int a) { super("SmallHeal", a); }

    @Override   
    public boolean onHit(Player user) { return false; }   
}
