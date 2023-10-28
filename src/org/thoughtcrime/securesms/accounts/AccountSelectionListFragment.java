package org.thoughtcrime.securesms.accounts;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;

import org.thoughtcrime.securesms.ConnectivityActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.ViewUtil;

import static com.b44t.messenger.DcContact.DC_CONTACT_ID_ADD_ACCOUNT;

public class AccountSelectionListFragment extends DialogFragment implements DcEventCenter.DcEventDelegate
{
  private RecyclerView recyclerView;
  private AccountSelectionListAdapter adapter;

  @Override
  public void onActivityCreated(Bundle icicle) {
    super.onActivityCreated(icicle);
    adapter = new AccountSelectionListAdapter(getActivity(), GlideApp.with(getActivity()), new ListClickListener());
    recyclerView.setAdapter(adapter);
    refreshData();
    DcEventCenter eventCenter = DcHelper.getEventCenter(requireActivity());
    eventCenter.addObserver(DcContext.DC_EVENT_CONNECTIVITY_CHANGED, this);
    eventCenter.addObserver(DcContext.DC_EVENT_INCOMING_MSG, this);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.switch_account)
            .setNeutralButton(R.string.connectivity, ((dialog, which) -> {
              startActivity(new Intent(getActivity(), ConnectivityActivity.class));
            }))
            .setNegativeButton(R.string.cancel, null);

    LayoutInflater inflater = getActivity().getLayoutInflater();
    View view = inflater.inflate(R.layout.account_selection_list_fragment, null);
    recyclerView = ViewUtil.findById(view, R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    return builder.setView(view).create();
  }

  @Override
  public void handleEvent(@NonNull DcEvent event) {
    refreshData();
  }

  private void refreshData() {
    if (adapter == null) return;

    DcAccounts accounts = DcHelper.getAccounts(requireActivity());
    int[] accountIds = accounts.getAll();

    int[] ids = new int[accountIds.length + 1];
    ids[0] = DC_CONTACT_ID_ADD_ACCOUNT;
    int j = 0;
    for (int accountId : accountIds) {
      ids[++j] = accountId;
    }
    adapter.changeData(ids, accounts.getSelectedAccount().getAccountId());
  }


  private class ListClickListener implements AccountSelectionListAdapter.ItemClickListener {

    @Override
    public void onItemClick(AccountSelectionListItem contact) {
      Activity activity = getActivity();
      AccountSelectionListFragment.this.dismiss();
      int accountId = contact.getAccountId();
      if (accountId == DC_CONTACT_ID_ADD_ACCOUNT) {
        AccountManager.getInstance().switchAccountAndStartActivity(activity, 0, null);
      } else if (accountId != DcHelper.getAccounts(activity).getSelectedAccount().getAccountId()) {
        AccountManager.getInstance().switchAccountAndStartActivity(activity, accountId, null);
      }
    }

    @Override
    public void onDeleteButtonClick(int accountId) {
      Activity activity = getActivity();
      AccountSelectionListFragment.this.dismiss();
      if (activity == null) return;
      DcAccounts accounts = DcHelper.getAccounts(activity);
      new AlertDialog.Builder(activity)
              .setTitle(accounts.getAccount(accountId).getNameNAddr())
              .setMessage(R.string.forget_login_confirmation_desktop)
              .setNegativeButton(R.string.cancel, (dialog, which) -> AccountManager.getInstance().showSwitchAccountMenu(activity))
              .setPositiveButton(R.string.ok, (dialog2, which2) -> {
                DcHelper.getNotificationCenter(activity).removeAllNotifiations(accountId);
                accounts.removeAccount(accountId);
                AccountManager.getInstance().showSwitchAccountMenu(activity);
              })
              .show();
    }
  }

}
