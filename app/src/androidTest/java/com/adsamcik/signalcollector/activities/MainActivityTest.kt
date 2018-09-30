package com.adsamcik.signalcollector.activities


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.AndroidJUnit4
import com.adsamcik.signalcollector.R
import com.adsamcik.signalcollector.exists
import com.adsamcik.signalcollector.services.TrackerService
import com.adsamcik.signalcollector.test.isTestMode
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val mPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)!!

    @get:Rule
    val mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun newUIActivityTest() {
        if (isTestMode)
            return

        val spotlightView = onView(
                allOf(withClassName(`is`("com.takusemba.spotlight.SpotlightView")), isDisplayed()))

        if (spotlightView.exists()) {
            spotlightView.perform(click())

            val spotlightView2 = onView(
                    allOf(withClassName(`is`("com.takusemba.spotlight.SpotlightView")), isDisplayed()))
            spotlightView2.perform(click())

            val spotlightView3 = onView(
                    allOf(withClassName(`is`("com.takusemba.spotlight.SpotlightView")), isDisplayed()))
            spotlightView3.perform(click())

            val spotlightView4 = onView(
                    allOf(withClassName(`is`("com.takusemba.spotlight.SpotlightView")), isDisplayed()))
            spotlightView4.perform(click())

            val spotlightView5 = onView(
                    allOf(withClassName(`is`("com.takusemba.spotlight.SpotlightView")), isDisplayed()))
            spotlightView5.perform(click())

            sleep()
        }
        sleep(5000)

        val statsButton = onView(withId(R.id.button_stats)).check(matches(isDisplayed()))
        val mapDraggable = onView(withId(R.id.button_map)).check(matches(isDisplayed()))
        val activityButton = onView(withId(R.id.button_activity)).check(matches(isDisplayed()))
        val topPanel = onView(withId(R.id.top_panel)).check(matches(isDisplayed()))

        mapDraggable.perform(click())

        sleep()

        mapDraggable.perform(click())

        sleep()

        val imageButton4 = onView(withId(R.id.button_settings)).check(matches(isDisplayed()))
        imageButton4.perform(click())

        sleep()

        val recyclerView = onView(
                allOf(withId(R.id.list),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)))
        recyclerView.perform(actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, click()))

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        val appCompatImageButton = onView(
                allOf(withId(R.id.back_button), withContentDescription("back"), isDisplayed()))
        appCompatImageButton.perform(click()).perform(click())

        sleep()

        val imageButton5 = onView(
                allOf(withId(R.id.button_tracking),
                        childAtPosition(
                                allOf(withId(R.id.top_panel),
                                        childAtPosition(
                                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                                0)),
                                2),
                        isDisplayed()))
        imageButton5.perform(click())

        sleep()

        assert(TrackerService.isServiceRunning.value)
        assert(!TrackerService.isBackgroundActivated)

        val imageButton6 = onView(
                allOf(withId(R.id.button_tracking),
                        childAtPosition(
                                allOf(withId(R.id.top_panel),
                                        childAtPosition(
                                                withClassName(`is`("android.support.constraint.ConstraintLayout")),
                                                0)),
                                2),
                        isDisplayed()))
        imageButton6.perform(click())

    }

    private fun sleep(millis: Long = 200) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return (parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position))
            }
        }
    }
}
