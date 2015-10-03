package io.github.tslamic.xkcdportal.event;

import io.github.tslamic.xkcdportal.Util;

public enum ConnectionEvent {

    ONLINE, OFFLINE;

    public static ConnectionEvent getCurrent() {
        return Util.isNetworkAvailable() ? ONLINE : OFFLINE;
    }

}
