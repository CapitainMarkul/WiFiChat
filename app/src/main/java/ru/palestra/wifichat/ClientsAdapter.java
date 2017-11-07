package ru.palestra.wifichat;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ru.palestra.wifichat.model.DeviceInfo;


/**
 * Created by Dmitry on 01.11.2017.
 */

public class ClientsAdapter extends RecyclerView.Adapter<ClientsAdapter.ViewHolder> {

    private List<DeviceInfo> clients = new ArrayList<>();
    private MainActivity.ItemClick listener;

    public void setClient(DeviceInfo client) {
        clients.add(client);
        notifyDataSetChanged();
    }

    public void removeClient(DeviceInfo client) {
        clients.remove(client);
        notifyDataSetChanged();
    }

    public void clearAll() {
        clients.clear();
        notifyDataSetChanged();
    }

    public void setListener(MainActivity.ItemClick listener) {
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

        holder.container.setOnClickListener(view -> listener.onItemClick(clients.get(position)));
    }

    @Override
    public int getItemCount() {
        return clients.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout container;
        private TextView clientName;
        private TextView clientMac;


        public ViewHolder(View itemView) {
            super(itemView);

            container = itemView.findViewById(R.id.client_container);
            clientName = itemView.findViewById(R.id.txt_client_name);
            clientMac = itemView.findViewById(R.id.txt_client_mac);
        }
    }
}
