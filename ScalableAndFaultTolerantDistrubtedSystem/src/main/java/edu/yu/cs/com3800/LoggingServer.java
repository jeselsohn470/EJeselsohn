package edu.yu.cs.com3800;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public interface LoggingServer {
    //Logger logger = null;
    default Logger initializeLogging(String s) throws IOException {
        LocalDateTime date = LocalDateTime.now();
        String time = DateTimeFormatter.ofPattern("yyyy-MM-dd-kk_mm").format(date);
        Logger logger = Logger.getLogger(s);

        int separator = s.indexOf("-");
        //creates the path to put the log files. it is under stage3/logs
        String path = System.getProperty("user.dir") + File.separator+ "logs" + File.separator + s.substring(0, separator) + time;

        boolean file = new File(path).mkdirs();

        FileHandler fileHandler = new FileHandler(path + File.separator +  s + ".log");
        fileHandler.setLevel(Level.ALL);
        logger.addHandler(fileHandler);
        logger.setLevel(Level.ALL);
//        logger.setUseParentHandlers(false);

        return logger;
    }
}
