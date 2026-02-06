package org.github.SilverNoir4434;

import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogOption;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import org.w3c.dom.Text;

import java.sql.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MyListener extends ListenerAdapter {

    static Connection db;

    MyListener(Connection db) {
        super();
        MyListener.db = db;
    }
    @Override
    public void onGuildAuditLogEntryCreate(GuildAuditLogEntryCreateEvent event) {
        if (event.getEntry().getType() == ActionType.MESSAGE_PIN) {
            MessageChannel pinChan = (MessageChannel) event.getGuild().getGuildChannelById(event.getEntry().getOption(AuditLogOption.CHANNEL));
            Message pin = pinChan.retrieveMessageById(event.getEntry().getOption(AuditLogOption.MESSAGE)).complete();

            String checkIfWatchedChannel = "SELECT * FROM CHANNEL WHERE CHANNEL_ID = " + pinChan.getIdLong();
            try {
                db = DriverManager.getConnection("jdbc:sqlite:memories_database");
                Statement stmt = db.createStatement();
                ResultSet results = stmt.executeQuery(checkIfWatchedChannel);
                if(!results.next()) {
                    String insert = "INSERT INTO MEMORY(MEMORY_ID, MEMORY_LINK, MEMORY_AUTHOR, MEMORY_DATE, GUILD_ID, CHANNEL_ID) VALUES" +
                            "(" + pin.getIdLong() + ", '" + pin.getJumpUrl() +"', '" + pin.getAuthor().getId() +"', '" + pin.getTimeCreated().toInstant().getEpochSecond() +
                            "', " + pin.getGuildIdLong() + ", " + pin.getChannelIdLong() + ");";
                    stmt = db.createStatement();
                    stmt.execute(insert);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch(event.getName()) {
            case "addchannel":
                event.reply("Choose channel to add.")
                        .addActionRow(
                                EntitySelectMenu.create("choose-channel-add", EntitySelectMenu.SelectTarget.CHANNEL)
                                        .setChannelTypes(ChannelType.TEXT)
                                        .build())
                        .queue();
                break;
            case "removechannel":
                event.reply("Choose channel to remove.")
                        .addActionRow(
                                EntitySelectMenu.create("choose-channel-remove", EntitySelectMenu.SelectTarget.CHANNEL)
                                        .build())
                        .queue();
            case "checkformemory":
                try {
                    event.reply("Checking...").queue();
                    Main.checkforMemory(event.getJDA());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "getpins":
                try {
                    int res = Main.getMemories(event.getJDA());
                    if (res == 0) {
                        event.reply("Didn't find a new memory!").queue();
                    } else {
                        event.reply("Found a memory and archived it!").queue();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "help":
                break;
            default:
                break;
        }
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        List<GuildChannel> chan = event.getMentions().getChannels();
        TextChannel watchedChannel = (TextChannel) chan.get(0);
        try {
            db = DriverManager.getConnection("jdbc:sqlite:memories_database");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (event.getComponentId().equals("choose-channel-add")) {
            String addChan = "INSERT INTO CHANNEL " +
                    "VALUES(" + watchedChannel.getIdLong() + ", '"
                    + watchedChannel.getName() + "', "
                    + watchedChannel.getGuild().getIdLong() +");";
            try {
                Statement stmt = db.createStatement();
                stmt.execute(addChan);
                event.reply("Added channel to list!").queue();
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                event.reply("An error occurred! Are you sure that the channel you" +
                        "targeted isn't already in the list?").queue();
            }
            try {
                db.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        else if (event.getComponentId().equals("choose-channel-remove")) {
            String rmChan = "DELETE FROM CHANNEL " +
                    "WHERE CHANNEL_ID=" + watchedChannel.getIdLong() + ";";
            try {
                Statement stmt = db.createStatement();
                stmt.execute(rmChan);
                event.reply("Removed channel from list!").queue();
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                event.reply("An error occurred! Are you sure that the channel you" +
                        "targeted is in the list?").queue();
            }
            try {
                db.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
