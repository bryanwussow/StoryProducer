package org.sil.storyproducer.androidtest.happypath

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.containsString
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.sil.storyproducer.androidtest.happypath.base.ImageBase
import org.sil.storyproducer.androidtest.happypath.base.SharedBase
import org.sil.storyproducer.androidtest.happypath.base.annotation.ImageTest
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator

@ImageTest
@RunWith(AndroidJUnit4::class)
class RegistrationActivityTest : PhaseTestBase() {
    override fun navigateToPhase() {
        // This function is intentionally empty, since
        // this test verifies the registration screen, which
        // appears prior to any phase.
    }

    val base : SharedBase = ImageBase()

    @Before
    fun setup() {
        revertWorkspaceToCleanState(base)

        // launch the registration dialog so it can be tested to be dismissed
        mActivityRegistrationRule.launchActivity(null)
        Thread.sleep(1000)
    }

    @Test
    fun should_beAbleToSkipRegistration() {
        PhaseNavigator.skipRegistration()
        Thread.sleep(1000)
        onView(withText(containsString(base.getStoryName()))).check(matches(isDisplayed()))
    }

}
