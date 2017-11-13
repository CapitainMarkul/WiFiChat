package ru.palestra.wifichat.model;

import com.google.auto.value.AutoValue;

/**
 * Created by Dmitry on 13.11.2017.
 */

@AutoValue
public abstract class EndPoint {
    public enum State {
        NEW_FOUND, LOST
    }
    public abstract String getIdEndPoint();
    public abstract String getNameEndPoint();
    public abstract boolean isLost();

    public static EndPoint newFound(String endPoint, String nameEndPoint) {
        return new AutoValue_EndPoint(endPoint, nameEndPoint, false);
    }

    public static EndPoint lost(String endPoint) {
        return new AutoValue_EndPoint(endPoint, null, true);
    }

    public State getState(){
        return isLost() ? State.LOST : State.NEW_FOUND;
    }
}
