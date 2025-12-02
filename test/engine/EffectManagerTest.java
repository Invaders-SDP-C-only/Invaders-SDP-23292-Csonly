package engine;

import entity.ExplosionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EffectManagerTest {

    @Mock
    private ExplosionEntity explosionMock1;
    @Mock
    private ExplosionEntity explosionMock2;
    
    private EffectManager effectManager;

    @BeforeEach
    void setUp() {
        // Reset the singleton instance
        try {
            java.lang.reflect.Field instanceField = EffectManager.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        effectManager = EffectManager.getInstance();
    }

    @Test
    void createExplosion_addsEffect() {
        // This is not a true unit test because createExplosion news up a real ExplosionEntity.
        // Mockito (without PowerMock/JMockit) cannot mock constructor calls.
        // We test the side effect: the list size increases.
        int initialSize = effectManager.getEffects().size();
        effectManager.createExplosion(10, 20);
        assertEquals(initialSize + 1, effectManager.getEffects().size(), "An explosion should be added.");
    }
    
    @Test
    void update_removesFinishedEffects() {
        // Since createExplosion is hard to unit test, we'll manually add mocks to the list.
        effectManager.getEffects().add(explosionMock1);
        effectManager.getEffects().add(explosionMock2);

        // Mock behavior for the final ExplosionEntity class (requires mockito-inline)
        when(explosionMock1.isFinished()).thenReturn(false);
        when(explosionMock2.isFinished()).thenReturn(true);

        // Act
        effectManager.update();

        // Assert
        verify(explosionMock1, times(1)).update();
        verify(explosionMock2, times(1)).update();

        List<ExplosionEntity> remainingEffects = effectManager.getEffects();
        assertEquals(1, remainingEffects.size());
        assertSame(explosionMock1, remainingEffects.get(0), "The unfinished effect should remain.");
    }

    @Test
    void clearEffects() {
        effectManager.createExplosion(1,1);
        assertFalse(effectManager.getEffects().isEmpty());
        
        effectManager.clearEffects();
        
        assertTrue(effectManager.getEffects().isEmpty(), "Effects list should be empty after clearEffects.");
    }
}
