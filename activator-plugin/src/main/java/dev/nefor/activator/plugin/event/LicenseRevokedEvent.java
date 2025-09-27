package dev.nefor.activator.plugin.event;

import dev.nefor.activator.api.LicenseStatus;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LicenseRevokedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final LicenseStatus reason;

    public LicenseRevokedEvent(LicenseStatus reason) {
        super(true);
        this.reason = reason;
    }

    public LicenseStatus getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
