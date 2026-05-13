package shared;
class AreaBomb extends ItemEffect { 
    
    static {
        IEProperty iep = new IEProperty("Pomb", "Doubles as a horse tranquilizer. Permanently.\nKill all enemies in the vicinity when used.", ItemEffect.IEProperty.Rarity.LEGENDARY, Protocol.ONE_USE_ITEM_TIME);
        ItemEffect.register("AreaBomb", iep);
    }

    AreaBomb(int a) { super("AreaBomb", a); }

    @Override   
    public boolean onHit(Player user) { return false; }   
}
