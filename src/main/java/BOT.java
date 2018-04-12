import com.emoji_er.*;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import java.sql.*;
import java.net.URI;

import net.dv8tion.jda.core.entities.Game;
public class BOT
{
    public static void main(String[] arguments) throws Exception
    {
        Connection conn=null;
        Logger.logInit();
        Logger.logGeneral("-----------SYSTEM STARTED------------");
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            Logger.logGeneral("Missing posgresql JDBC Driver!");
            e.printStackTrace();
            return;
        }
        try {
            String url= System.getenv("DATABASE_URL");
            String username = System.getenv("DATABASE_USER");
            String password = System.getenv("DATABASE_PASSWORD");
            Logger.logGeneral("Connecting to: "+ url);
            conn = DriverManager.getConnection("jdbc:"+url,username,password);
            Logger.logGeneral("SQL INITIALIZZATED");
        } catch (SQLException ex) {
            Logger.logGeneral("SQLException: " + ex.getMessage());
            Logger.logGeneral("SQLState: " + ex.getSQLState());
            Logger.logGeneral("VendorError: " + ex.getErrorCode());
            System.exit(-1);
        }

        JDA api = new JDABuilder(AccountType.BOT).setToken(System.getenv("BOT_TOKEN")).buildAsync();

        api.addEventListener(new MyListener(conn));
        api.getPresence().setGame(Game.playing("v1.5.1 - em prj"));

    }

}
