import com.emoji_er.*;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import java.sql.*;
import java.net.URI;

import net.dv8tion.jda.core.entities.Game;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class BOT
{
    public static void main(String[] arguments) throws Exception
    {
        Connection conn=null;

        Logger.tlogger.start();

        Logger.logger.logInit();
        Logger.logger.logGeneral("-----------SYSTEM STARTED------------");
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Missing posgresql JDBC Driver!");
            e.printStackTrace();
            return;
        }
        try {
            String url= System.getenv("DATABASE_URL");
            String username = System.getenv("DATABASE_USER");
            String password = System.getenv("DATABASE_PASSWORD");
            Logger.logger.logGeneral("Connecting to: "+ url);
            conn = DriverManager.getConnection("jdbc:"+url,username,password);
            Logger.logger.logGeneral("SQL INITIALIZZATED");
        } catch (SQLException ex) {
            Logger.logger.logGeneral("SQLException: " + ex.getMessage());
            Logger.logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logger.logGeneral("VendorError: " + ex.getErrorCode());
            System.exit(-1);
        }

        JDA api = new JDABuilder(AccountType.BOT).setToken(System.getenv("BOT_TOKEN")).buildAsync();

        MyListener listener = new MyListener(conn);

        Signal.handle(new Signal("INT"), sig -> {
            Logger.logger.closeFiles();
            System.err.println("Received SIGINT");
            listener.close();
            Logger.tlogger.interrupt();
            System.exit(sig.getNumber());
        });

        api.addEventListener(listener);
        api.getPresence().setGame(Game.playing("v1.7.5 - em prj"));

    }

}
