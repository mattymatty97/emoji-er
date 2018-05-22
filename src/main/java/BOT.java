import com.emoji_er.*;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import java.sql.*;

import net.dv8tion.jda.core.entities.Game;
import sun.misc.Signal;
import org.fusesource.jansi.AnsiConsole;

import static org.fusesource.jansi.Ansi.ansi;
public class BOT
{
    public static void main(String[] arguments) throws Exception
    {

        Connection conn=null;
        AnsiConsole.systemInstall();

        Logger.tlogger.start();

        Logger.logger.logInit();
        Logger.logger.logGeneral("-----------SYSTEM STARTED------------");
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("\r"+"Missing posgresql JDBC Driver!");
            e.printStackTrace();
            return;
        }
        try {
            String url= System.getenv("DATABASE_URL");
            String username = System.getenv("DATABASE_USER");
            String password = System.getenv("DATABASE_PASSWORD");
            Logger.logger.logGeneral("Connecting to: "+ url);
            conn = DriverManager.getConnection("jdbc:"+url,username,password);
            conn.setAutoCommit(false);
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
            System.err.println("\r"+ansi().fgRed().a("Received SIGINT").reset());
            listener.close();
            api.shutdown();
            Logger.tlogger.interrupt();
            try {
                Logger.tlogger.join();
            }catch (Exception ignore){}
            Logger.logger.closeFiles();
            System.exit(sig.getNumber());
        });

        api.addEventListener(listener);
        api.getPresence().setGame(Game.playing("v1.7.9 - em prj"));

        while (!Thread.interrupted()){
            Thread.sleep(300);
            System.out.print("\r                     ");
            System.out.print(ansi().fgCyan().a("\rActive Threads: ").fgBrightGreen().a(Thread.activeCount()).fgBlack());
        }
    }

}
