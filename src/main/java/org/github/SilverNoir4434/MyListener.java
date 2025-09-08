package org.github.SilverNoir4434;

import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;

public class MyListener extends ListenerAdapter {

    Connection db;

    MyListener(Connection db) {
        super();
        this.db = db;
    }
    @Override
    public void onGuildAuditLogEntryCreate(GuildAuditLogEntryCreateEvent event) {
        if (event.getEntry().getType() == ActionType.MESSAGE_PIN) {
            System.out.println("Got pin!");
            // STORE PIN IN DATABASE
        }
    }
}
