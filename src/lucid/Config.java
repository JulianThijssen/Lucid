package lucid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Config {
    private static final String path = "server.cfg";

    public static int GAME_PORT = 4444;
    public static int LOGIN_PORT = 4445;
    public static String DB_NAME = "lucid";
    public static String DB_USER = "root";
    public static String DB_PASS = "pass";

    public static int MAX_PLAYERS = 100;
    public static int MAX_PENDING_TCP_CONNECTIONS = 50;

    public static int READ_BUFFER = 1024;
    public static int WRITE_BUFFER = 1024;

    public static int MAX_WRITE_OVERFLOW = 2048;

    public static void loadConfig() {
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(new File(path)));
        } catch(FileNotFoundException e) {
            //Log.debug(String.format("The file %s could not be found", path));
        }

        try {
            String line = null;
            String key = null;
            String value = null;

            while((line = in.readLine()) != null) {
                String[] kv = line.split("=");
                if(kv.length == 2) {
                    key = kv[0];
                    value = kv[1];
                }

                if(key.equals("GAME_PORT")) {
                    GAME_PORT = Integer.parseInt(value);
                } else if (key.equals("LOGIN_PORT")) {
                    LOGIN_PORT = Integer.parseInt(value);
                } else if (key.equals("DB_NAME")) {
                    DB_NAME = value;
                } else if (key.equals("DB_USER")) {
                    DB_USER = value;
                } else if (key.equals("DB_PASS")) {
                    DB_PASS = value;
                } else if (key.equals("MAX_PLAYERS")) {
                    MAX_PLAYERS = Integer.parseInt(value);
                } else if (key.equals("MAX_PENDING")) {
                    MAX_PENDING_TCP_CONNECTIONS = Integer.parseInt(value);
                } else if (key.equals("READ_BUFFER")) {
                    READ_BUFFER = Integer.parseInt(value);
                } else if (key.equals("WRITE_BUFFER")) {
                    WRITE_BUFFER = Integer.parseInt(value);
                }
            }
        } catch(IOException ie) {
            //Log.debug("Error while reading " + path);
        } catch(NumberFormatException ne) {
            //Log.debug("Parameter error while reading " + path);
        }
    }
}
