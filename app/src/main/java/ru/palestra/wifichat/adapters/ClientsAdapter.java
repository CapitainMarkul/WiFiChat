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

import ru.palestra.wifichat.MainActivity;
import ru.palestra.wifichat.R;
import ru.palestra.wifichat.model.DeviceInfo;


/**
 * Created by Dmitry on 01.11.2017.
 */

public class ClientsAdapter extends RecyclerView.Adapter<ClientsAdapter.ViewHolder> {

    private List<DeviceInfo> clients = new ArrayList<>();
    private MainActivity.ItemClick listener;

    public void setClient(DeviceInfo client) {
        DeviceInfo[] devicesInfo = new DeviceInfo[clients.size()];
        devicesInfo = clients.toArray(devicesInfo);

        //Удаляем дубликаты клиентов с неверными точками доступа
        for (int i = 0; i < devicesInfo.length; i++) {
            if (devicesInfo[i].getClientName().equals(client.getClientName())
                    || devicesInfo[i].getClientNearbyKey().equals(client.getClientNearbyKey())) {
                clients.remove(devicesInfo[i]);
                notifyItemRemoved(i);
            }
        }

        clients.add(client);
        notifyDataSetChanged();
    }

    public void setAllClients(Set<DeviceInfo> clients) {
        this.clients.clear();
        this.clients.addAll(clients);
        notifyDataSetChanged();
    }

    public void removeClient(DeviceInfo device) {
        if (device.getState() != DeviceInfo.State.EMPTY) {
            clients.remove(device);
        }
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
