package ru.palestra.wifichat.utils;

import android.os.Parcel;

import org.parceler.Parcels;
import org.parceler.converter.ArrayListParcelConverter;

import ru.palestra.wifichat.data.models.viewmodels.Client;

/**
 * Created by da.pavlov1 on 21.11.2017.
 */

public class ItemListParcelConverter extends ArrayListParcelConverter<Client> {
    private ItemListParcelConverter() {

    }

    @Override
    public void itemToParcel(Client item, Parcel parcel) {
        parcel.writeParcelable(Parcels.wrap(item), 0);
    }

    @Override
    public Client itemFromParcel(Parcel parcel) {
        return Parcels.unwrap(parcel.readParcelable(Client.class.getClassLoader()));
    }

    public static void itemToParcel(){

    }
}
