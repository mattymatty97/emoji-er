import com.emoji_er.*;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import java.sql.*;

import net.dv8tion.jda.core.entities.Game;
import sun.misc.Signal;
import org.fusesource.jansi.AnsiConsole;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import static org.fusesource.jansi.Ansi.ansi;
public class BOT
{
    private static JDA api;
    private static MyListener listener;
    public static void main(String[] arguments) throws Exception
    {
        System.out.print((char)27+"[?25l");
        Connection conn=null;
        AnsiConsole.systemInstall();

        testEnv();

        Logger.tlogger.setPriority(Thread.NORM_PRIORITY - 1);
        Logger.tlogger.start();

        Logger.logger.logInit();
        Logger.logger.logGeneral("-----------STARTING SYSTEM------------");
        Logger.logger.logGeneral("SYSTEM VERSION: "+Global.version);
        Logger.logger.logGeneral("BUILD: "+Global.build+"\r\n");
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println(ansi().fgRed()+"Missing posgresql JDBC Driver!");
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
            Logger.logger.logGeneral(ansi().fgRed()+"SQLException: " + ex.getMessage());
            Logger.logger.logGeneral(ansi().fgRed()+"SQLState: " + ex.getSQLState());
            Logger.logger.logGeneral(ansi().fgRed()+"VendorError: " + ex.getErrorCode());
            System.exit(-1);
        }

        api = new JDABuilder(AccountType.BOT).setToken(System.getenv("BOT_TOKEN")).buildBlocking(JDA.Status.LOADING_SUBSYSTEMS);

        listener = new MyListener(conn);

        Thread pinger = new Thread(new Pinger(api.getSelfUser().getIdLong()),"Ping-er Thread");
        pinger.setPriority(Thread.MAX_PRIORITY);


        Thread main = Thread.currentThread();
        Signal.handle(new Signal("INT"), sig -> {
            main.interrupt();
            Logger.started = false;
            System.out.println((char)27+"[?25h");
            System.err.println(ansi().fgRed().a("Received SIGINT").reset());
            api.shutdown();
            pinger.interrupt();
            listener.close();
            Logger.tlogger.interrupt();
            try {
                Logger.tlogger.join();
            }catch (Exception ignore){}
            Logger.logger.closeFiles();
            System.exit(sig.getNumber());
        });

        api.addEventListener(listener);
        api.getPresence().setGame(Game.playing(Global.version));

        while (!Logger.started && !Thread.interrupted()) ;

        pinger.start();

        Output.run();
    }

    private static void testEnv() throws Exception {
        Map<String, String> env = System.getenv();

        String var = env.get("BOT_TOKEN");
        if (var == null || var.isEmpty())
            throw new Exception("Missing environement variable: BOT_TOKEN");

        var = env.get("DATABASE_URL");
        if (var == null || var.isEmpty())
            throw new Exception("Missing environement variable: DATABASE_URL");

        var = env.get("DATABASE_USER");
        if (var == null || var.isEmpty())
            throw new Exception("Missing environement variable: DATABASE_USER");

        var = env.get("DATABASE_PASSWORD");
        if (var == null || var.isEmpty())
            throw new Exception("Missing environement variable: DATABASE_PASSWORD");

        var = env.get("DEFAULT_EMOJI_PREFIX");
        if (var == null || var.isEmpty())
            throw new Exception("Missing environement variable: DEFAULT_EMOJI_PREFIX");

        var = env.get("DISCORDBOTS_KEY");
        if (var == null)
            throw new Exception("Missing environement variable: DISCORDBOTS_KEY (can be empty)");

        var = env.get("SUPPORT_GUILD_ID");
        if (var == null || var.isEmpty())
            throw new Exception("Missing environement variable: SUPPORT_GUILD_ID");
        else
            try {
                Long.parseLong(var);
            } catch (NumberFormatException ex) {
                throw new Exception("Environement variable ( SUPPORT_GUILD_ID ) is not valid");
            }

        var = env.get("OWNER_ID");
        if (var == null || var.isEmpty() || Long.parseLong(var) == 0)
            throw new Exception("Missing environement variable: OWNER_ID");
        else
            try {
                Long.parseLong(var);
            } catch (NumberFormatException ex) {
                throw new Exception("Environement variable ( OWNER_ID ) is not valid");
            }

        var = env.get("LISTENER_IP");
        if (var == null || var.isEmpty())
            throw new Exception("Missing environement variable: LISTENER_IP");

    }

    private static void setExHandler(){
        Thread main = Thread.currentThread();
        Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler(){

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                main.interrupt();
                System.err.println(e.getMessage());
                e.printStackTrace();
                Logger.started = false;
                System.out.println((char)27+"[?25h");
                System.err.println(ansi().fgRed().a("Aborting for unhandled Exception").reset());
                try {
                    api.shutdown();
                    listener.close();
                }catch (Exception ignore){}
                Logger.tlogger.interrupt();
                try {
                    Logger.tlogger.join();
                }catch (Exception ignore){}
                Logger.logger.closeFiles();
                System.exit(-1);
            }
        };

        Thread.setDefaultUncaughtExceptionHandler(handler);

    }

}
