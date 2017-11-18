package ru.palestra.wifichat.adapters;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import ru.palestra.wifichat.R;
import ru.palestra.wifichat.adapters.diffutil.ClientsDiffUtil;
import ru.palestra.wifichat.data.models.viewmodels.Client;
import ru.palestra.wifichat.databinding.ItemClientBinding;


/**
 * Created by Dmitry on 01.11.2017.
 */

public class ClientsAdapter extends RecyclerView.Adapter<ClientsAdapter.ViewHolder> {
    public interface ItemClick {
        void onItemClick(Client client);
    }

    private List<Client> clients = new ArrayList<>();

    private ItemClick listener;

    public void setClient(Client client) {
        if (!isNewClient(client)) return;

        clients.add(client);
        notifyDataSetChanged();
    }

    private boolean isNewClient(Client client) {
        return !clients.contains(client);
//        for (ClientMessageWrap client : clients) {
//            if (client.getClient().equals(clientMessageWrap.getClient())) return false;
//        }
//// TODO: 17.11.2017 Хз
//        return true;
    }

    public void updateClients(List<Client> newClients) {
        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new ClientsDiffUtil(this.clients, newClients));

        this.clients.clear();
        this.clients.addAll(newClients);
        diffResult.dispatchUpdatesTo(this);
        notifyDataSetChanged(); // TODO: 18.11.2017 Исправить diffUtil
    }

    public List<Client> getAllClients() {
        return clients;
    }

    public void clearAll() {
        clients.clear();
        notifyDataSetChanged();
    }

    public void setListener(ItemClick listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Client client = clients.get(position);
//        final Message lastMessage = clients.get(position);
//        final LocalDateTime timeSend = TimeUtils.longToLocalDateTime(lastMessage.getTimeSend());

        holder.binding.txtClientName.setText(client.getName());

        if (client.isOnline()) {
            holder.itemView.setBackgroundColor(Color.GREEN);
        } else {
            holder.itemView.setBackgroundColor(Color.GRAY);
        }
//        holder.binding.itemMessage.txtTimeSend
//                .setText(String.format("%s: %s", timeSend.getHour(), timeSend.getMinute()));
//        holder.binding.itemMessage.txtItemMessage.setText(lastMessage.getText());
//
//        if (!lastMessage.isDelivered())
//            holder.binding.itemMessage.btnStatusMessage.setVisibility(View.VISIBLE);
//        else
//            holder.binding.itemMessage.btnStatusMessage.setVisibility(View.INVISIBLE);

        holder.itemView.setOnClickListener(view -> listener.onItemClick(clients.get(position)));
//        holder.binding.itemMessage.btnStatusMessage.setOnClickListener(view -> {  todo CreateListener, for resending massge
//
//        });
    }

    @Override
    public int getItemCount() {
        return clients.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemClientBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
        }
    }
}
