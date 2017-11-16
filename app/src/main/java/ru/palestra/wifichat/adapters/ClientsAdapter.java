package ru.palestra.wifichat.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ru.palestra.wifichat.R;
import ru.palestra.wifichat.data.models.viewmodels.Client;


/**
 * Created by Dmitry on 01.11.2017.
 */

public class ClientsAdapter extends RecyclerView.Adapter<ClientsAdapter.ViewHolder> {
    public interface ItemClick {
        void onItemClick(Client client, boolean needRequestConnect);
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
    }

    public void setAllClients(Set<Client> clients) {
        this.clients.clear();
        this.clients.addAll(clients);
        notifyDataSetChanged();
    }

    public void removeClient(String idEndPoint) {
        clients.remove(
                searchDisconnectedClient(idEndPoint));
    }

    private Client searchDisconnectedClient(String idEndPoint) {
        for (Client client : clients) {
            if (client.getClientNearbyKey().equals(idEndPoint)) return client;
        }
        return Client.empty();
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
        holder.clientName.setText(clients.get(position).getClientName());
        holder.clientMac.setText(clients.get(position).getClientNearbyKey());

        holder.container.setOnClickListener(view -> listener.onItemClick(clients.get(position), true));
        holder.currentSend.setOnClickListener(view -> listener.onItemClick(clients.get(position), false));
    }

    @Override
    public int getItemCount() {
        return clients.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout container;
        private TextView clientName;
        private TextView clientMac;
        private Button currentSend;
        private ProgressBar waitConnected;


        public ViewHolder(View itemView) {
            super(itemView);

            container = itemView.findViewById(R.id.client_container);
            clientName = itemView.findViewById(R.id.txt_client_name);
            clientMac = itemView.findViewById(R.id.txt_client_mac);
            currentSend = itemView.findViewById(R.id.btn_send_current);
            waitConnected = itemView.findViewById(R.id.pb_wait_connect);
        }
    }
}
