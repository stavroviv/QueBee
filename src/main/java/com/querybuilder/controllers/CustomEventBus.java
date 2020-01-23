package com.querybuilder.controllers;

import net.engio.mbassy.bus.MBassador;

public class CustomEventBus {
    private static MBassador<CustomEvent> bus;

    static {
        bus = new MBassador<>();
    }

    public static void register(Argumentative subscriber) {
        bus.subscribe(subscriber);
    }

    public static void post(CustomEvent event) {
        bus.publish(event);
    }
}