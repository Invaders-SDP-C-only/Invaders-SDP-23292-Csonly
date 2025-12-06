package entity;

import engine.DrawManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FinalBossTest {

    @BeforeEach
    void resetItems() {
        ShopItem.resetAllItems();
    }

    @Test
    void updateTogglesSpriteFrame() {
        FinalBoss boss = new FinalBoss(0, 0, 200, 200);
        assertEquals(DrawManager.SpriteType.FinalBoss1, boss.getSpriteType());

        boss.update();

        assertEquals(DrawManager.SpriteType.FinalBoss2, boss.getSpriteType());
    }

    @Test
    void takeDamageDestroysWhenHpDepleted() {
        FinalBoss boss = new FinalBoss(0, 0, 200, 200);

        boss.takeDamage(1000);

        assertTrue(boss.isDestroyed());
        assertEquals(DrawManager.SpriteType.FinalBossDeath, boss.getSpriteType());
    }

    @Test
    void shootPatternsReturnBulletsOnFirstCall() {
        FinalBoss boss = new FinalBoss(0, 0, 200, 200);

        Set<BossBullet> pattern1 = boss.shoot1();
        Set<BossBullet> pattern2 = boss.shoot2();
        Set<BossBullet> pattern3 = boss.shoot3();

        assertEquals(5, pattern1.size());
        assertEquals(1, pattern2.size());
        assertEquals(2, pattern3.size());
    }
}
