package com.querybuilder.home.lab.controllers;

import com.google.common.eventbus.EventBus;

public class CustomEventBus {
    private static CustomEventBus instance;
    private static EventBus bus;

    static {
        instance = new CustomEventBus();
        bus = new EventBus();
    }

    public static void register(Argumentative subscriber) {
        bus.register(subscriber);
    }

    public static void post(CustomEvent event) {
        bus.post(event);
    }
}