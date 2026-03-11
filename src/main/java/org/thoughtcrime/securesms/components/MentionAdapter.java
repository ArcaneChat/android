package org.thoughtcrime.securesms.components;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.b44t.messenger.DcContact;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.mms.GlideRequests;
import org.thoughtcrime.securesms.recipients.Recipient;

import java.util.ArrayList;
import java.util.List;

public class MentionAdapter extends RecyclerView.Adapter<MentionAdapter.ViewHolder> {

    public interface OnMentionClickListener {
        void onMentionClicked(DcContact contact);
    }

    private final List<DcContact> contacts = new ArrayList<>();
    private final GlideRequests glideRequests;
    private OnMentionClickListener listener;

    public MentionAdapter(@NonNull GlideRequests glideRequests) {
        this.glideRequests = glideRequests;
    }

    public void setContacts(List<DcContact> newContacts) {
        contacts.clear();
        contacts.addAll(newContacts);
        notifyDataSetChanged();
    }

    public void setOnMentionClickListener(OnMentionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.mention_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DcContact contact = contacts.get(position);
        Context context = holder.itemView.getContext();

        holder.displayName.setText(contact.getDisplayName());
        String addr = contact.getAddr();
        String name = contact.getName();
        if (!name.isEmpty() && !name.equals(addr)) {
            holder.address.setText(addr);
            holder.address.setVisibility(View.VISIBLE);
        } else {
            holder.address.setVisibility(View.GONE);
        }

        holder.avatar.setAvatar(glideRequests, new Recipient(context, contact), false);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMentionClicked(contact);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final AvatarImageView avatar;
        final TextView displayName;
        final TextView address;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.mention_avatar);
            displayName = itemView.findViewById(R.id.mention_display_name);
            address = itemView.findViewById(R.id.mention_address);
        }
    }
}
