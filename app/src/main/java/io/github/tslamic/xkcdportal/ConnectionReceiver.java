package io.github.tslamic.xkcdportal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.github.tslamic.xkcdportal.event.ConnectionEvent;

/**
 * Receives network changes and propagates them through the event bus.
 */
public class ConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        BusProvider.INSTANCE.post(ConnectionEvent.getCurrent());
    }

}
