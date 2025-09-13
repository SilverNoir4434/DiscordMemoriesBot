package org.github.SilverNoir4434;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.*;

public class Main {
    static Connection memoryDB;
    static BufferedReader br;
    private static String BOT_TOKEN;
    public static void main(String[] args) throws Exception {

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
                    "MEMORY_AUTHOR TEXT NOT NULL," +
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

        api.updateCommands().addCommands(
                Commands.slash("add channel", "Adds channels for the bot to watch.")
                        .addOption(OptionType.NUMBER, "id","ChannelID of the channel to watch.")
        ).queue();
        memoryDB.close();
    }

    private static void getMemories() {
        /* Method checks all watched channels for memories and retrieves
           them, updating Memory table in database.
           TODO: Implement this method.
         */
    }

    private static void checkGuilds() {

    }

}

