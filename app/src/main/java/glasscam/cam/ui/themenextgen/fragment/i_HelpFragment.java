package glasscam.cam.ui.themenextgen.fragment;

import androidx.fragment.app.Fragment;

/**
 * Callback used by {@link HelpFragment} to tell its host to remove it once the
 * onboarding walkthrough has been dismissed.
 */
public interface i_HelpFragment
{
    void Close(Fragment fragment);
}
