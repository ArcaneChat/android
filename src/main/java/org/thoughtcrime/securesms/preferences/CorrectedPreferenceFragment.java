package org.thoughtcrime.securesms.preferences;


import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.thoughtcrime.securesms.components.CustomDefaultPreference;
import org.thoughtcrime.securesms.util.ViewUtil;

public abstract class CorrectedPreferenceFragment extends PreferenceFragmentCompat {

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    View lv = getView().findViewById(android.R.id.list);
    if (lv != null) {
      lv.setPadding(0, 0, 0, 0);
      // For edge-to-edge: apply bottom padding for navigation bar and enable clipToPadding false
      // so the list can scroll behind the navigation bar
      if (lv instanceof RecyclerView) {
        ((RecyclerView) lv).setClipToPadding(false);
      }
      ViewUtil.applyWindowInsets(lv, true, false, true, true);
    }
  }

  @Override
  public void onDisplayPreferenceDialog(@NonNull Preference preference) {
    DialogFragment dialogFragment = null;

    if (preference instanceof CustomDefaultPreference) {
      dialogFragment = CustomDefaultPreference.CustomDefaultPreferenceDialogFragmentCompat.newInstance(preference.getKey());
    }

    if (dialogFragment != null) {
      dialogFragment.setTargetFragment(this, 0);
      dialogFragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
    } else {
      super.onDisplayPreferenceDialog(preference);
    }
  }


}
