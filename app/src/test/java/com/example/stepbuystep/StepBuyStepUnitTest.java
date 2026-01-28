package com.example.stepbuystep;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import com.example.stepbuystep.ActivityTrainee.TraineeStoreScreen.ShoeProgressionManager;
import com.example.stepbuystep.model.Equipment;
import com.example.stepbuystep.model.Message;
import org.junit.Test;

public class StepBuyStepUnitTest {
    @Test
    public void testPurchaseWithInsufficientFunds() {
        // Current shoe level of the trainee
        int currentLevel = 1;
        // Trainee's coin balance (not enough to buy next level)
        long lowBalance = 500;
        // Call the method under test
        boolean canBuy = ShoeProgressionManager.canPurchaseNextLevel(currentLevel, lowBalance);
        // Assert that the purchase is NOT allowed
        assertFalse(
                "Trainee should not be able to buy shoes with insufficient coins",
                canBuy
        );
    }
    @Test
    public void testEquipmentModelConsistency() {
        // Expected values for the Equipment object
        String expectedId = "eq_1";
        String expectedName = "Speed Shoes";
        String expectedType = "running_shoes";
        int expectedTier = 2;
        int expectedPrice = 3000;
        double expectedMultiplier = 1.25;
        // Create a new Equipment object
        Equipment equipment = new Equipment(
                expectedId,
                expectedName,
                expectedType,
                expectedTier,
                expectedPrice,
                expectedMultiplier
        );
        // Verify that each getter returns the correct value
        assertEquals(expectedId, equipment.getId());
        assertEquals(expectedName, equipment.getName());
        assertEquals(expectedType, equipment.getType());
        assertEquals(expectedTier, equipment.getTier());
        assertEquals(expectedPrice, equipment.getPrice());
        // For double comparison, include a delta value
        assertEquals(expectedMultiplier, equipment.getMultiplier(), 0.0001);
    }
    @Test
    public void testMessageWithMock() {
        // Create a mock instance of the Message class
        Message mockedMessage = mock(Message.class);
        when(mockedMessage.getCoachId()).thenReturn("Coach_123");
        when(mockedMessage.getMessageText()).thenReturn("Keep Running!");
        // Call the mocked methods
        String coachId = mockedMessage.getCoachId();
        String text = mockedMessage.getMessageText();
        // Assert that the returned values match the mocked behavior
        assertEquals("Coach_123", coachId);
        assertEquals("Keep Running!", text);
        // Verify that the getters were called exactly once
        verify(mockedMessage, times(1)).getCoachId();
        verify(mockedMessage, times(1)).getMessageText();
        // Verify that no other interactions happened with the mock
        verifyNoMoreInteractions(mockedMessage);
    }
}
