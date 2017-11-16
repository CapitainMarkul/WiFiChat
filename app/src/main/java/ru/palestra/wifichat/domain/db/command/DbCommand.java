package ru.palestra.wifichat.domain.db.command;

import ru.palestra.wifichat.data.models.daomodels.DaoSession;

/**
 * Created by da.pavlov1 on 16.11.2017.
 */

public interface DbCommand<T> {
    T execute(final DaoSession daoSession);
}
