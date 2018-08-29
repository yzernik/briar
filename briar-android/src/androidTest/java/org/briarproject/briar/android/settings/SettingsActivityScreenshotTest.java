package org.briarproject.briar.android.settings;

import android.content.Intent;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;

import org.briarproject.briar.R;
import org.briarproject.briar.android.BriarUiTestComponent;
import org.briarproject.briar.android.navdrawer.NavDrawerActivity;
import org.briarproject.briar.android.test.ScreenshotTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerMatchers.isClosed;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.briarproject.briar.android.test.ViewActions.waitUntilMatches;
import static org.hamcrest.CoreMatchers.is;

@RunWith(AndroidJUnit4.class)
public class SettingsActivityScreenshotTest extends ScreenshotTest {

	@Rule
	public CleanAccountTestRule<SettingsActivity> testRule =
			new CleanAccountTestRule<>(SettingsActivity.class);

	@Override
	protected void inject(BriarUiTestComponent component) {
		component.inject(this);
	}

	@Test
	public void changeTheme() {
		onView(withText(R.string.settings_button))
				.check(matches(isDisplayed()));

		screenshot("manual_dark_theme_settings");

		// switch to dark theme
		onView(withText(R.string.pref_theme_title))
				.check(matches(isDisplayed()))
				.perform(click());
		onView(withText(R.string.pref_theme_dark))
				.check(matches(isDisplayed()))
				.perform(click());

		// open nav drawer and remove expiry warning
		openNavDrawer(true);

		screenshot("manual_dark_theme_nav_drawer");
	}

	@Test
	public void appLock() {
		// scroll down
		onView(withClassName(is(RecyclerView.class.getName())))
				.perform(scrollToPosition(13));

		// wait for settings to get loaded and enabled
		onView(withText(R.string.tor_mobile_data_title))
				.perform(waitUntilMatches(isEnabled()));

		// ensure app lock is displayed and enabled
		onView(withText(R.string.pref_lock_title))
				.check(matches(isDisplayed()))
				.check(matches(isEnabled()))
				.perform(click());
		onView(withChild(withText(R.string.pref_lock_timeout_title)))
				.check(matches(isDisplayed()))
				.check(matches(isEnabled()));

		screenshot("manual_app_lock");

		// no more expiry warning to remove, because sharedprefs cached?
		openNavDrawer(false);

		screenshot("manual_app_lock_nav_drawer");
	}

	private void openNavDrawer(boolean expiry) {
		// start main activity
		Intent i =
				new Intent(testRule.getActivity(), NavDrawerActivity.class);
		testRule.getActivity().startActivity(i);

		// close expiry warning
		if (expiry) {
			onView(withId(R.id.expiryWarningClose))
					.check(matches(isDisplayed()));
			onView(withId(R.id.expiryWarningClose))
					.perform(click());
		}

		// open navigation drawer
		onView(withId(R.id.drawer_layout))
				.check(matches(isClosed(Gravity.START)))
				.perform(DrawerActions.open());
	}

}
