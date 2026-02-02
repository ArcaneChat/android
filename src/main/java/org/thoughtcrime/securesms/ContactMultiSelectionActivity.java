/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Activity container for selecting a list of contacts.
 *
 * @author Moxie Marlinspike
 *
 */
public class ContactMultiSelectionActivity extends ContactSelectionActivity {

  public static final String CONTACTS_EXTRA = "contacts_extra";
  public static final String REMOVED_CONTACTS_EXTRA = "removed_contacts_extra";
  
  private ArrayList<Integer> preselectedContacts;

  @Override
  protected void onCreate(Bundle icicle, boolean ready) {
    getIntent().putExtra(ContactSelectionListFragment.MULTI_SELECT, true);
    super.onCreate(icicle, ready);
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

    // it's a bit confusing having one "X" button on the left and one on the right -
    // and the "clear search" button is not that important.
    getToolbar().setUseClearButton(false);
    
    // Store preselected contacts to track which ones were removed
    preselectedContacts = getIntent().getIntegerArrayListExtra(ContactSelectionListFragment.PRESELECTED_CONTACTS);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getMenuInflater();
    menu.clear();

    inflater.inflate(R.menu.add_members, menu);
    super.onPrepareOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    if (item.getItemId() == R.id.menu_add_members) {
      saveSelection();
      finish();
      return true;
    }

    return false;
  }

  private void saveSelection() {
    Intent resultIntent = getIntent();
    List<Integer> selectedContacts = contactsFragment.getSelectedContacts();
    resultIntent.putIntegerArrayListExtra(CONTACTS_EXTRA, new ArrayList<>(selectedContacts));
    
    // Calculate which contacts were removed (preselected but not in final selection)
    if (preselectedContacts != null) {
      Set<Integer> selectedSet = new HashSet<>(selectedContacts);
      ArrayList<Integer> removedContacts = new ArrayList<>();
      for (Integer preselectedId : preselectedContacts) {
        if (!selectedSet.contains(preselectedId)) {
          removedContacts.add(preselectedId);
        }
      }
      resultIntent.putIntegerArrayListExtra(REMOVED_CONTACTS_EXTRA, removedContacts);
    }
    
    setResult(RESULT_OK, resultIntent);
  }
}
