package org.github.SilverNoir4434;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.io.BufferedReader;
import java.io.Console;
import java.io.FileReader;
import java.sql.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Main {
    static Connection memoryDB;
    static BufferedReader br;
    private static String BOT_TOKEN;
    public static void main(String[] args) throws Exception {
        ScheduledExecutorService schedule = Executors.newScheduledThreadPool(1);

        try {
            memoryDB = DriverManager.getConnection("jdbc:sqlite:memories_database");
            br = new BufferedReader(new FileReader("token.txt"));
            BOT_TOKEN = br.readLine();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
            String createGuild = "CREATE TABLE IF NOT EXISTS GUILD" +
                    "(GUILD_ID INT PRIMARY KEY," +
                    "GUILD_NAME TEXT NOT NULL);";
            String createChannel = "CREATE TABLE IF NOT EXISTS CHANNEL" +
                    "(CHANNEL_ID INT PRIMARY KEY," +
                    "CHANNEL_NAME TEXT NOT NULL," +
                    "GUILD_ID INT NOT NULL," +
                    "FOREIGN KEY(GUILD_ID) REFERENCES GUILD(GUILD_ID));";
            String createMemories = "CREATE TABLE IF NOT EXISTS MEMORY" +
                    "(MEMORY_ID INT PRIMARY KEY," +
                    "MEMORY_LINK TEXT NOT NULL," +
                    "MEMORY_AUTHOR INT NOT NULL," +
                    "MEMORY_DATE DATE NOT NULL," +
                    "GUILD_ID INT NOT NULL," +
                    "CHANNEL_ID INT NOT NULL," +
                    "FOREIGN KEY(GUILD_ID) REFERENCES GUILD(GUILD_ID)," +
                    "FOREIGN KEY(CHANNEL_ID) REFERENCES CHANNEL(CHANNEL_ID));";
                Statement stmt = memoryDB.createStatement();
                Statement stmt2 = memoryDB.createStatement();
                Statement stmt3 = memoryDB.createStatement();
                stmt.execute(createGuild);
                stmt2.execute(createChannel);
                stmt3.execute(createMemories);
            // TODO: Add methods that both get watched channels and memories from server.
            // Maybe don't do it here, in case that hasn't been set up by user yet.

        JDA api = JDABuilder.createDefault(BOT_TOKEN).addEventListeners(new MyListener(memoryDB)).build();

        /*
        https://jda.wiki/using-jda/interactions/#entity-select-menus
        TODO: Implement this in add/remove channel methods, since it doesn't seem to like using IDs anyways.
         */
        api.updateCommands().addCommands(
                Commands.slash("addchannel", "Adds channels for the bot to watch.")
                        .addOption(OptionType., "id","ChannelID of the channel to watch."),
                Commands.slash("removechannel","Tells the bot to stop watching a channel.")
                        .addOption(OptionType.INTEGER, "id", "ChannelID of the channel to stop watching"),
                Commands.slash("getpins", "Checks watched channels for new pins.")
        ).queue();
        memoryDB.close();
    }

    public static void checkforMemory(JDA api) throws SQLException {
        /*
        TODO: Implement this method, make it check if there was a memory from a year ago today and post about it if there was.
         */
        memoryDB = DriverManager.getConnection("jdbc:sqlite:memories_database");

        String getMemories = "SELECT * FROM MEMORY" +
                "WHERE MEMORY_DATE=('now','-1 year');";

        PreparedStatement stmt = memoryDB.prepareStatement(getMemories);
        ArrayList<Long> memChannels = new ArrayList<>();
        ArrayList<Message> memories = new ArrayList<>();
        ResultSet results = stmt.executeQuery();

        System.out.println(results);

        memoryDB.close();
    }
    public static void getMemories(JDA api) throws SQLException {
        /*
        TODO: Test this.
         */
        memoryDB = DriverManager.getConnection("jdbc:sqlite:memories_database");
        String getChannels = "SELECT * FROM CHANNELS;";
        PreparedStatement stmt = memoryDB.prepareStatement(getChannels);
        ArrayList<Long> memChannels = new ArrayList<>();
        ArrayList<Message> memories = new ArrayList<>();
        ResultSet results = stmt.executeQuery();

        while (results.next()) {
            memChannels.add(results.getLong(1));
        }


        for (Guild g : api.getGuilds()) {
            List<TextChannel> channels = g.getTextChannels();
            for (TextChannel tc : channels) {
                for (Long l : memChannels) {
                    if (tc.getIdLong() == l) {
                        List<Message> interim = tc.retrievePinnedMessages().complete();
                        memories.addAll(interim);
                    }
                }
            }
        }

        for (Message m : memories) {
            String addMemory = "INSERT INTO MEMORY(MEMORY_ID, MEMORY_LINK, MEMORY_AUTHOR, MEMORY_DATE, GUILD_ID, CHANNEL_ID) VALUES" +
                    "(" + m.getIdLong() + ", '" + m.getJumpUrl() +"', '" + m.getAuthor().getId() +"', " + m.getTimeCreated() +
                    ", " + m.getGuildIdLong() + ", " + m.getChannelIdLong() + ");";
            Statement stmt2 = memoryDB.createStatement();
            stmt2.execute(addMemory);
        }
        memoryDB.close();
    }
}

