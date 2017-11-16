package ru.palestra.wifichat.data.models.daomodels;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;

import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

@Entity(nameInDb = "Message")
public class MessageSql {
    @Id
    private Long id;

    @NotNull
    private String fromName;

    @NotNull
    private String fromUUID;    //messageUUID Sender

    @NotNull
    private String messageUUID;    //messageUUID Current Message

    @NotNull
    private String text;

    @NotNull
    private boolean statusDelivered;

    @NotNull
    private long timeSend;

    @Generated(hash = 371483593)
    public MessageSql(Long id, @NotNull String fromName, @NotNull String fromUUID,
                      @NotNull String messageUUID, @NotNull String text,
                      boolean statusDelivered, long timeSend) {
        this.id = id;
        this.fromName = fromName;
        this.fromUUID = fromUUID;
        this.messageUUID = messageUUID;
        this.text = text;
        this.statusDelivered = statusDelivered;
        this.timeSend = timeSend;
    }

    @Generated(hash = 1759594602)
    public MessageSql() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFromName() {
        return this.fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getFromUUID() {
        return this.fromUUID;
    }

    public void setFromUUID(String fromUUID) {
        this.fromUUID = fromUUID;
    }

    public String getMessageUUID() {
        return this.messageUUID;
    }

    public void setMessageUUID(String messageUUID) {
        this.messageUUID = messageUUID;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean getStatusDelivered() {
        return this.statusDelivered;
    }

    public void setStatusDelivered(boolean statusDelivered) {
        this.statusDelivered = statusDelivered;
    }

    public long getTimeSend() {
        return this.timeSend;
    }

    public void setTimeSend(long timeSend) {
        this.timeSend = timeSend;
    }
}
