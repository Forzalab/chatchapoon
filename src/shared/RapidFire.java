package shared;
class RapidFire extends ItemEffect { 
    
    static {
        IEProperty iep = new IEProperty("AK-47", "Fires faster than your local school shooter.\nHalves shooting cooldown time when used.", ItemEffect.IEProperty.Rarity.RARE, 20);
        ItemEffect.register("RapidFire", iep);
    }

    public RapidFire(int a) { super("RapidFire", a); }

    @Override   
    public boolean onHit(Player user) { return false; }   
}
