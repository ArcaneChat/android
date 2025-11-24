package org.thoughtcrime.securesms;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.util.Util;

import java.util.List;

import chat.delta.rpc.Rpc;
import chat.delta.rpc.RpcException;
import chat.delta.rpc.types.EnteredLoginParam;

public class TransportListActivity extends BaseActionBarActivity
        implements TransportListAdapter.OnTransportClickListener {

    public static final String EXTRA_TRANSPORT_ADDR = "transport_addr";

    private RecyclerView recyclerView;
    private TransportListAdapter adapter;
    private FloatingActionButton fabAdd;
    private Rpc rpc;
    private int accId;
    private String mainTransportAddr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transport_list);

        rpc = DcHelper.getRpc(this);
        accId = DcHelper.getContext(this).getAccountId();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.transports);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setElevation(0);
        }

        CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinator_layout);
        recyclerView = findViewById(R.id.transport_list);
        fabAdd = findViewById(R.id.fab_add_transport);

        adapter = new TransportListAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> openAddTransport());

        // Handle window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(coordinatorLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // Apply top inset to RecyclerView as padding
            recyclerView.setPadding(
                recyclerView.getPaddingLeft(),
                insets.top,
                recyclerView.getPaddingRight(),
                insets.bottom
            );
            recyclerView.setClipToPadding(false);

            // Apply bottom inset to FAB as margin
            CoordinatorLayout.LayoutParams fabParams = (CoordinatorLayout.LayoutParams) fabAdd.getLayoutParams();
            int fabMargin = (int) (16 * getResources().getDisplayMetrics().density);
            fabParams.bottomMargin = insets.bottom + fabMargin;
            fabParams.rightMargin = fabMargin;
            fabAdd.setLayoutParams(fabParams);

            return WindowInsetsCompat.CONSUMED;
        });

        loadTransports();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTransports();
    }

    private void loadTransports() {
        Util.runOnBackground(() -> {
            try {
                List<EnteredLoginParam> transports = rpc.listTransports(accId);
                mainTransportAddr = DcHelper.get(this, DcHelper.CONFIG_CONFIGURED_ADDRESS);
                Util.runOnMain(() -> adapter.setTransports(transports, mainTransportAddr));
            } catch (RpcException e) {
                mainTransportAddr = DcHelper.get(this, DcHelper.CONFIG_CONFIGURED_ADDRESS);
                Util.runOnMain(() -> adapter.setTransports(null, mainTransportAddr));
            }
        });
    }

    private void openAddTransport() {
        Intent intent = new Intent(this, EditTransportActivity.class);
        startActivity(intent);
    }

    @Override
    public void onTransportClick(EnteredLoginParam transport) {
        // Clicking on a transport makes it the main transport
        if (transport.addr != null && !transport.addr.equals(mainTransportAddr)) {
            Util.runOnBackground(() -> {
                DcHelper.getContext(this).setConfig(DcHelper.CONFIG_CONFIGURED_ADDRESS, transport.addr);
                Util.runOnMain(this::loadTransports);
            });
        }
    }

    @Override
    public void onTransportEdit(EnteredLoginParam transport) {
        Intent intent = new Intent(this, EditTransportActivity.class);
        intent.putExtra(EXTRA_TRANSPORT_ADDR, transport.addr);
        startActivity(intent);
    }

    @Override
    public void onTransportDelete(EnteredLoginParam transport) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_transport)
                .setMessage(getString(R.string.confirm_remove_transport, transport.addr))
                .setPositiveButton(R.string.ok, (dialog, which) -> deleteTransport(transport))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteTransport(EnteredLoginParam transport) {
        Util.runOnBackground(() -> {
            try {
                rpc.deleteTransport(accId, transport.addr);
                Util.runOnMain(this::loadTransports);
            } catch (RpcException e) {
                Util.runOnMain(() -> {
                    new AlertDialog.Builder(this)
                            .setMessage(getString(R.string.error) + ": " + e.getMessage())
                            .setPositiveButton(R.string.ok, null)
                            .show();
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
