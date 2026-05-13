package shared;
class Ghost extends ItemEffect { 
    @Override
    public boolean onHit(Player user) { return true; }
    static {
        IEProperty iep = new IEProperty("Matrix Bullet Dodger", "Be Keanu Reaves in 67 ticks.\nNothing can hurt you in 67 ticks.", ItemEffect.IEProperty.Rarity.RARE,
67);
        ItemEffect.register("AmmoRefill", iep);
    }    
    public Ghost(int a) { super("Ghost", a); }

}
