package dev.nefor.activator.plugin.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LicenseGrantedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    public LicenseGrantedEvent() {
        super(true);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
