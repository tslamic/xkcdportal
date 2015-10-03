package io.github.tslamic.xkcdportal;

import com.squareup.otto.Bus;

public enum BusProvider {

    INSTANCE;

    private final Bus mBus;

    BusProvider() {
        mBus = new Bus();
    }

    public void post(Object object) {
        mBus.post(object);
    }

    public void register(Object object) {
        mBus.register(object);
    }

    public void unregister(Object object) {
        mBus.unregister(object);
    }

}
