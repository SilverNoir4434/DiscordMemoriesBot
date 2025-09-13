package org.github.SilverNoir4434;

import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

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
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch(event.getName()) {
            case "add channel":
                event.deferReply().queue();
                long channelID = 0;
                TextChannel watchedChannel = null;
                try {
                    channelID = Objects.requireNonNull(event.getOption("channelID")).getAsLong();
                } catch(Exception ex) {
                    event.reply("ChannelID not valid! Try again.").queue();
                }
                try {
                    watchedChannel = event.getGuild().getChannelById(TextChannel.class, channelID);
                } catch (NullPointerException ex) {
                    event.reply("Channel does not exist! Check the channelID.").queue();
                }
                String addChan = "INSERT INTO CHANNEL" +
                        "VALUES(" + watchedChannel.getIdLong() + ", '"
                        + watchedChannel.getName() + "', "
                        + watchedChannel.getGuild().getIdLong() +");";
                try {
                    Statement stmt = db.createStatement();
                    stmt.execute(addChan);
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
                break;
            default:
                break;
        }
    }
}
