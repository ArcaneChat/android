package org.thoughtcrime.securesms.accounts;

import static com.b44t.messenger.DcContact.DC_CONTACT_ID_ADD_ACCOUNT;
import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_PRIVATE_TAG;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import java.util.Arrays;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

public class AccountSelectionListFragment extends DialogFragment
    implements DcEventCenter.DcEventDelegate {
  private static final String TAG = "AcctSelectionListFrag";
  private static final String ARG_SELECT_ONLY = "select_only";
  private RecyclerView recyclerView;
  private AccountSelectionListAdapter adapter;
  private boolean selectOnly;

  public static AccountSelectionListFragment newInstance(boolean selectOnly) {
    AccountSelectionListFragment fragment = new AccountSelectionListFragment();
    Bundle args = new Bundle();
    args.putBoolean(ARG_SELECT_ONLY, selectOnly);
    fragment.setArguments(args);
    return fragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    selectOnly = getArguments() != null && getArguments().getBoolean(ARG_SELECT_ONLY, false);
    AlertDialog.Builder builder =
        new AlertDialog.Builder(requireActivity())
            .setTitle(R.string.switch_account)
            .setNegativeButton(R.string.cancel, null);

    LayoutInflater inflater = getActivity().getLayoutInflater();
    View view = inflater.inflate(R.layout.account_selection_list_fragment, null);
    recyclerView = ViewUtil.findById(view, R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    adapter =
        new AccountSelectionListAdapter(
            this, GlideApp.with(getActivity()), new ListClickListener());
    recyclerView.setAdapter(adapter);
    refreshData();
    DcEventCenter eventCenter = DcHelper.getEventCenter(requireActivity());
    eventCenter.addMultiAccountObserver(DcContext.DC_EVENT_CONNECTIVITY_CHANGED, this);
    eventCenter.addMultiAccountObserver(DcContext.DC_EVENT_INCOMING_MSG, this);
    eventCenter.addMultiAccountObserver(DcContext.DC_EVENT_MSGS_NOTICED, this);

    return builder.setView(view).create();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    DcHelper.getEventCenter(requireActivity()).removeObservers(this);
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    refreshData();
  }

  private void refreshData() {
    if (adapter == null) return;

    DcAccounts accounts = DcHelper.getAccounts(getActivity());
    int[] accountIds = accounts.getAll();

    int[] ids = new int[(selectOnly ? 0 : 1) + accountIds.length];
    int j = 0;
    for (int accountId : accountIds) {
      ids[j++] = accountId;
    }
    if (!selectOnly) ids[j] = DC_CONTACT_ID_ADD_ACCOUNT;
    adapter.changeData(ids, accounts.getSelectedAccount().getAccountId());
  }

  @Override
  public void onCreateContextMenu(
      @NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    if (selectOnly) return;

    requireActivity().getMenuInflater().inflate(R.menu.account_item_context, menu);

    AccountSelectionListItem listItem = (AccountSelectionListItem) v;
    int accountId = listItem.getAccountId();
    DcAccounts dcAccounts = DcHelper.getAccounts(requireActivity());

    Util.redMenuItem(menu, R.id.delete);

    if (dcAccounts.getAccount(accountId).isMuted()) {
      menu.findItem(R.id.menu_mute_notifications).setTitle(R.string.menu_unmute);
    }

    // hack to make onContextItemSelected() work with DialogFragment,
    // see
    // https://stackoverflow.com/questions/15929026/oncontextitemselected-does-not-get-called-in-a-dialogfragment
    MenuItem.OnMenuItemClickListener listener =
        item -> {
          onContextItemSelected(item, accountId);
          return true;
        };
    for (int i = 0, n = menu.size(); i < n; i++) {
      menu.getItem(i).setOnMenuItemClickListener(listener);
    }
    // /hack
  }

  private void onContextItemSelected(MenuItem item, int accountId) {
    int itemId = item.getItemId();
    if (itemId == R.id.delete) {
      onDeleteProfile(accountId);
    } else if (itemId == R.id.menu_mute_notifications) {
      onToggleMute(accountId);
    } else if (itemId == R.id.menu_set_tag) {
      onSetTag(accountId);
    } else if (itemId == R.id.menu_move_to_top) {
      onMoveToTop(accountId);
    } else if (itemId == R.id.menu_mark_all_as_read) {
      try {
        DcHelper.getRpc(requireActivity()).marknoticedAllChats(accountId);
      } catch (RpcException e) {
        Log.e(TAG, "Error calling rpc.marknoticedAllChats()", e);
      }
    }
  }

  private void onMoveToTop(int accountId) {
    Activity activity = getActivity();
    if (activity == null) return;

    int[] accountIds = DcHelper.getAccounts(activity).getAll();
    Integer[] ids = new Integer[accountIds.length];
    ids[0] = accountId;
    int j = 1;
    for (int accId : accountIds) {
      if (accId != accountId) {
        ids[j++] = accId;
      }
    }

    Rpc rpc = DcHelper.getRpc(activity);
    try {
      rpc.setAccountsOrder(Arrays.asList(ids));
    } catch (RpcException e) {
      Log.e(TAG, "Error calling rpc.setAccountsOrder()", e);
    }

    refreshData();
  }

  private void onSetTag(int accountId) {
    ConversationListActivity activity = (ConversationListActivity) requireActivity();
    AccountSelectionListFragment.this.dismiss();

    DcContext dcContext = DcHelper.getAccounts(activity).getAccount(accountId);
    View view = View.inflate(activity, R.layout.single_line_input, null);
    EditText inputField = view.findViewById(R.id.input_field);
    inputField.setHint(R.string.profile_tag_hint);
    inputField.setText(dcContext.getConfig(CONFIG_PRIVATE_TAG));

    new AlertDialog.Builder(activity)
        .setTitle(R.string.profile_tag)
        .setMessage(R.string.profile_tag_explain)
        .setView(view)
        .setPositiveButton(
            android.R.string.ok,
            (d, b) -> {
              String newTag = inputField.getText().toString().trim();
              dcContext.setConfig(CONFIG_PRIVATE_TAG, newTag);
              AccountManager.getInstance().showSwitchAccountMenu(activity, selectOnly);
            })
        .setNegativeButton(
            R.string.cancel,
            (d, b) -> AccountManager.getInstance().showSwitchAccountMenu(activity, selectOnly))
        .show();
  }

  private void onDeleteProfile(int accountId) {
    AccountSelectionListFragment.this.dismiss();
    ConversationListActivity activity = (ConversationListActivity) requireActivity();
    DeleteProfileDialog.show(
        activity,
        accountId,
        () -> activity.onDeleteProfile(accountId),
        () -> AccountManager.getInstance().showSwitchAccountMenu(activity, selectOnly));
  }

  private void onToggleMute(int accountId) {
    DcAccounts dcAccounts = DcHelper.getAccounts(requireActivity());
    DcContext dcContext = dcAccounts.getAccount(accountId);
    dcContext.setMuted(!dcContext.isMuted());
    recyclerView.getAdapter().notifyDataSetChanged();
  }

  private class ListClickListener implements AccountSelectionListAdapter.ItemClickListener {

    @Override
    public void onItemClick(AccountSelectionListItem contact) {
      AccountSelectionListFragment.this.dismiss();
      ConversationListActivity activity = (ConversationListActivity) requireActivity();
      int accountId = contact.getAccountId();
      if (accountId == DC_CONTACT_ID_ADD_ACCOUNT) {
        AccountManager.getInstance().switchAccountAndStartActivity(activity, 0);
      } else if (accountId != DcHelper.getAccounts(activity).getSelectedAccount().getAccountId()) {
        AccountManager.getInstance().switchAccount(activity, accountId);
        activity.onProfileSwitched(accountId);
      }
    }
  }
}
