package org.thoughtcrime.securesms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import chat.delta.rpc.types.EnteredLoginParam;

public class TransportListAdapter extends RecyclerView.Adapter<TransportListAdapter.TransportViewHolder> {

    private List<EnteredLoginParam> transports = new ArrayList<>();
    private final OnTransportClickListener listener;
    private String mainTransportAddr;

    public interface OnTransportClickListener {
        void onTransportClick(EnteredLoginParam transport);
        void onTransportEdit(EnteredLoginParam transport);
        void onTransportDelete(EnteredLoginParam transport);
    }

    public TransportListAdapter(OnTransportClickListener listener) {
        this.listener = listener;
    }

    public void setTransports(List<EnteredLoginParam> transports, String mainTransportAddr) {
        this.transports = transports != null ? transports : new ArrayList<>();
        this.mainTransportAddr = mainTransportAddr;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.transport_list_item, parent, false);
        return new TransportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransportViewHolder holder, int position) {
        EnteredLoginParam transport = transports.get(position);
        boolean isMain = transport.addr != null && transport.addr.equals(mainTransportAddr);
        holder.bind(transport, isMain, listener);
    }

    @Override
    public int getItemCount() {
        return transports.size();
    }

    static class TransportViewHolder extends RecyclerView.ViewHolder {
        private final TextView emailText;
        private final TextView serverText;
        private final ImageView mainIndicator;
        private final ImageView editButton;
        private final ImageView deleteButton;

        public TransportViewHolder(@NonNull View itemView) {
            super(itemView);
            emailText = itemView.findViewById(R.id.transport_email);
            serverText = itemView.findViewById(R.id.transport_server);
            mainIndicator = itemView.findViewById(R.id.main_transport_indicator);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }

        public void bind(EnteredLoginParam transport, boolean isMain, OnTransportClickListener listener) {
            emailText.setText(transport.addr);
            
            String serverInfo = "";
            if (transport.imapServer != null && !transport.imapServer.isEmpty()) {
                serverInfo = transport.imapServer;
            }
            serverText.setText(serverInfo);
            serverText.setVisibility(serverInfo.isEmpty() ? View.GONE : View.VISIBLE);

            mainIndicator.setVisibility(isMain ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransportClick(transport);
                }
            });

            editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransportEdit(transport);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransportDelete(transport);
                }
            });
        }
    }
}
