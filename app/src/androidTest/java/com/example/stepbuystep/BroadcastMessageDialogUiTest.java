package com.example.stepbuystep;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.stepbuystep.ActivityCoach.CoachHomeScreen.CoachHomeActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BroadcastMessageDialogUiTest {
    // Rule that launches CoachHomeActivity before each test
    @Rule
    public ActivityScenarioRule<CoachHomeActivity> activityRule = new ActivityScenarioRule<>(CoachHomeActivity.class);
    // Test that opening the broadcast dialog shows the message input field
    @Test
    public void testOpenBroadcastDialog_showsMessageInput() {
        // Click the broadcast message button on CoachHome screen
        onView(withId(R.id.btnBroadcastMessage)).perform(click());
        // Verify that the message input field is displayed
        onView(withId(R.id.etMessage)).check(matches(isDisplayed()));
    }
    // Test default state: "Send to all trainees" is checked and list is hidden
    @Test
    public void testDefaultState_sendToAllChecked_listHidden() {
        // Open the broadcast message dialog
        onView(withId(R.id.btnBroadcastMessage)).perform(click());
        // Verify that the checkbox is checked by default
        onView(withId(R.id.cbSendToAll)).check(matches(isChecked()));
        // Verify that the trainees list is hidden
        onView(withId(R.id.rvTraineesList)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        // Verify that the selection text is also hidden
        onView(withId(R.id.tvSelectTrainees)).check(matches(withEffectiveVisibility(Visibility.GONE)));
    }
    // Test that unchecking "Send to all" shows the trainees selection UI
    @Test
    public void testUncheckSendToAll_showsTraineesSelection() {
        // Open the broadcast message dialog
        onView(withId(R.id.btnBroadcastMessage)).perform(click());
        // Uncheck the "Send to all trainees" checkbox
        onView(withId(R.id.cbSendToAll)).perform(click());
        // Verify that the selection text becomes visible
        onView(withId(R.id.tvSelectTrainees)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        // Verify that the trainees RecyclerView becomes visible
        onView(withId(R.id.rvTraineesList)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }
    // Test validation: sending an empty message is not allowed
    @Test
    public void testSendEmptyMessage_keepsInputEmpty() {
        // Open the broadcast message dialog
        onView(withId(R.id.btnBroadcastMessage)).perform(click());
        // Ensure the message input is empty
        onView(withId(R.id.etMessage)).perform(replaceText(""), closeSoftKeyboard());
        // Click the send button
        onView(withId(R.id.btnSend)).perform(click());
        // Verify that the message input is still empty
        onView(withId(R.id.etMessage)).check(matches(withText("")));
    }
}
