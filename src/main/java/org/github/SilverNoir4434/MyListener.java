package org.github.SilverNoir4434;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogOption;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.github.SilverNoir4434.Main.log;
import static org.github.SilverNoir4434.Main.postEmbed;

public class MyListener extends ListenerAdapter {

    static Connection db;

    MyListener(Connection db) {
        super();
        MyListener.db = db;
    }
    @Override
    public void onGuildAuditLogEntryCreate(GuildAuditLogEntryCreateEvent event) {
        // Watches for message to get pinned, adds to database is pin is from watched channel
        if (event.getEntry().getType() == ActionType.MESSAGE_PIN) {
            MessageChannel pinChan = (MessageChannel) event.getGuild().getGuildChannelById(event.getEntry().getOption(AuditLogOption.CHANNEL));
            Message pin = pinChan.retrieveMessageById(event.getEntry().getOption(AuditLogOption.MESSAGE)).complete();

            if (pin == null) {
                return;
            }

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
        if (!event.isFromGuild()) return;
        switch(event.getName()) {
            case "addchannel":
                EntitySelectMenu menu = EntitySelectMenu.create("choose-channel-add", EntitySelectMenu.SelectTarget.CHANNEL)
                        .setChannelTypes(ChannelType.TEXT)
                        .build();
                event.reply("Choose channel to add.").setComponents(ActionRow.of(menu)).setEphemeral(true).queue(hook -> {
                    hook.deleteOriginal().queueAfter(30, TimeUnit.SECONDS);
                });
                break;
            case "removechannel":
                EntitySelectMenu menu2 = EntitySelectMenu.create("choose-channel-remove", EntitySelectMenu.SelectTarget.CHANNEL)
                        .setChannelTypes(ChannelType.TEXT)
                        .build();
                event.reply("Choose channel to remove.").setComponents(ActionRow.of(menu2)).setEphemeral(true).queue(hook -> {
                    hook.deleteOriginal().queueAfter(30, TimeUnit.SECONDS);
                });
                break;
            case "checkformemory":
                try {
                    event.reply("Checking...").queue();
                    Main.checkForMemory(event.getJDA());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "getmemories":
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
            case "randommemory":
                try {
                    db = DriverManager.getConnection("jdbc:sqlite:memories_database");
                    String randMem = "SELECT * FROM memory WHERE GUILD_ID = " + event.getGuild().getId() + " ORDER BY RANDOM() LIMIT 1;";

                    ResultSet res = db.prepareStatement(randMem).executeQuery();

                    String year = res.getString("MEMORY_DATE").substring(0,4);
                    event.reply("Posting memory from " + year + ":").queue();

                    postEmbed(event.getJDA(), res, event.getChannel());
                    db.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "checkday":
                try {
                    db = DriverManager.getConnection("jdbc:sqlite:memories_database");
                    LocalDate date;

                    String input = event.getOption("day").getAsString();
                    try {
                        DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                        date = LocalDate.parse(input, format);
                        date.format(DateTimeFormatter.ISO_DATE);
                    } catch (DateTimeParseException ex) {
                        event.reply("Incorrect format! Date must be given in MM/DD/YYYY format.").queue();
                        db.close();
                        return;
                    }

                    String getMemories = "SELECT * FROM MEMORY " +
                            "WHERE DATE(MEMORY_DATE)=DATE('" + date + "') AND GUILD_ID = " + event.getGuild().getId() + ";";

                    PreparedStatement stmt = db.prepareStatement(getMemories);
                    ResultSet results = stmt.executeQuery();

                    if (!results.next()) {
                        event.reply("No memory found for that day.").queue();
                        db.close();
                    }
                    else {
                        event.reply("Memory/memories found, posting now...").queue();
                        while (results.next()) {
                            postEmbed(event.getJDA(), results, event.getChannel());
                            db.close();
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            case "listchannels":
                try {
                    event.deferReply().queue();
                    db = DriverManager.getConnection("jdbc:sqlite:memories_database");

                    String getChan = "SELECT CHANNEL_ID FROM CHANNEL WHERE GUILD_ID = " + event.getGuild().getId() + ";";
                    ResultSet res = db.prepareStatement(getChan).executeQuery();

                    List<TextChannel> channels = new ArrayList<>();

                    while (res.next()) {
                        channels.add(event.getGuild().getTextChannelById(res.getLong("CHANNEL_ID")));
                    }
                    EmbedBuilder embed = new EmbedBuilder();

                    embed.setTitle("Watched Channels:");

                    for (TextChannel chan : channels) {
                        embed.addField(chan.getName(), chan.getJumpUrl(), false);
                    }

                    event.getHook().sendMessageEmbeds(embed.build()).queue();


                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
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
                Main.getMemories(event.getJDA());
            } catch (SQLException ex) {
                log.error(ex.getMessage());
                event.reply("An error occurred! Are you sure that the channel you " +
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
