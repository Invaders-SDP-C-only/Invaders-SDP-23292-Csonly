package engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import entity.ExplosionEntity;

public class EffectManager {

    private static EffectManager instance;
    
    private List<ExplosionEntity> explosions;

    private EffectManager() {
        explosions = new ArrayList<>();
    }

    public static EffectManager getInstance() {
        if (instance == null) {
            instance = new EffectManager();
        }
        return instance;
    }

    public void createExplosion(final int x, final int y) {
        ExplosionEntity effect = new ExplosionEntity(x, y);
        this.explosions.add(effect);
    }

    public void update() {
        Iterator<ExplosionEntity> iter = this.explosions.iterator();
        while (iter.hasNext()) {
            ExplosionEntity effect = iter.next();
            effect.update();
            
            if (effect.isFinished()) {
                iter.remove();
            }
        }
    }

    public List<ExplosionEntity> getEffects() {
        return this.explosions;
    }

    public void clearEffects() {
        this.explosions.clear();
    }
}
