package org.github.SilverNoir4434;

import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
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
        long channelID;
        switch(event.getName()) {
            case "addchannel":
                event.deferReply().queue();
                TextChannel watchedChannel = null;
                event.reply("Choose channel to add.")
                        .addActionRow(
                                EntitySelectMenu.create("choose-channel", EntitySelectMenu.SelectTarget.CHANNEL)
                                        .build())
                        .queue();
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
                try {
                    db.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "removechannel":
                event.deferReply().queue();
                channelID = 0;
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
                String remChan = "DELETE FROM CHANNEL WHERE" +
                        " CHANNEL_ID='"+channelID+"';";
                try {
                    Statement stmt = db.createStatement();
                    stmt.execute(remChan);
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
                break;
            case "checkformemory":
                break;
            case "getpins":
                try {
                    Main.getMemories(event.getJDA());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "help":
                break;
            case "setup":
                break;

            default:
                break;
        }
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        List<TextChannel> channels = event.getJDA().getTextChannels();
        if (event.getComponentId().equals("choose-channel-add")) {

        }
        else if (event.getComponentId().equals("choose-channel-remove")) {

        }
    }
}
