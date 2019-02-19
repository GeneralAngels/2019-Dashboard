package gen.ang.dashboard;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final String DRIVER_STATION = "DriverStation.exe";
    private static final String DRIVER_STATION_FULL_PATH = "C:\\Program Files (x86)\\FRC Driver Station\\" + DRIVER_STATION;
    private static final File logFilesDir = new File("C:\\Users\\Public\\Documents\\FRC\\Log Files");
    private static JFrame frame;
    private static JPanel panel, right, left;
    private static JLabel barDesc, state, robotStatus, gear;
    private static JProgressBar bar;
    private static long laps = 0;
    private static File logFile;
    private static String splittingRegex = "(?<=\\<message\\> )(([\\u0020-\\u003B]|[=]|[\\u003F-\\u007D])+)";

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        if (!isRunning(DRIVER_STATION)) {
            startDS();
        }
        frame = new JFrame("2230 Dash");
        panel = new JPanel();
        panel.setLayout(new GridLayout(1, 2));
        right = new JPanel();
//        right.setPreferredSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width/2,300));
        left = new JPanel();
        panel.add(right);
        panel.add(left);
//        right.setLayout(new GridLayout(right, BoxLayout.Y_AXIS));
        bar = new JProgressBar();
//        bar.setPreferredSize(new Dimension(right.getPreferredSize().width, 80));
//        bar.setMinimumSize(bar.getPreferredSize());
        bar.setStringPainted(true);
        barDesc = new JLabel();
        state = new JLabel();
        robotStatus = new JLabel();
        gear = new JLabel();
        right.add(barDesc);
        right.add(bar);
        right.add(state);
        right.add(robotStatus);
        right.add(gear);
        Dimension size = new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width / 2, 300 / 6);
        barDesc.setMinimumSize(size);
        barDesc.setPreferredSize(size);
        bar.setMinimumSize(size);
        bar.setPreferredSize(size);
        state.setMinimumSize(size);
        state.setPreferredSize(size);
        robotStatus.setMinimumSize(size);
        robotStatus.setPreferredSize(size);
        gear.setMinimumSize(size);
        gear.setPreferredSize(size);
        frame.setUndecorated(true);
        frame.setContentPane(panel);
        frame.setVisible(true);
        frame.setSize(Toolkit.getDefaultToolkit().getScreenSize().width, 300);
        logFile = findLog();
        updateInfo(logFile);
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (laps % 10000 == 0) logFile = findLog();
//                updateInfo(logFile);
                laps++;
            }
        }, 1000, 50);
    }

    private static void updateInfo(File f) {
        ArrayList<String> info = parse(f);
        for (String m : info) {
            System.out.println(m);
        }
//        String message = info[info.length - 1];
//        if (message.contains("->") && message.contains("%")) {
//            String fullName = readUntil(message.substring(readUntil(message.substring(1), '>').length() + 1), '%');
//            String name = fullName.split(",")[0];
//            int percent = Integer.parseInt(fullName.split(",")[1].replace(" ", "").replace("%", ""));
////            int percent = Integer.parseInt(readUntil(message.substring(name.length()), '%').replace(" ", "").replace("%", ""));
//            bar.setMaximum(100);
//            barDesc.setText("<html><p style='text-align:center'>" + name + "</p></html>");
//            bar.setValue(percent);
//        } else if (message.contains("->") && message.contains("State")) {
//            String currentState = readUntil(message.substring(readUntil(message, '>').length() + 1), ';').replace(";", "");
//            state.setText(currentState);
//        }
    }

    private static String readUntil(String src, char until) {
        for (int i = 0; i < src.length(); i++) {
            if (src.charAt(i) == until) return src.substring(0, i + 1);
        }
        return src;
    }

    private static String clean(String thing) {
        return "";
    }

    private static ArrayList<String> parse(File f) {
        ArrayList<String> array = new ArrayList<>();
        Matcher m = Pattern.compile(splittingRegex).matcher(readFile(f));
        while (m.find()) {
            array.add(m.group());
        }
        return array;
//        return readFile(f).split("<message> ");
    }

    public static String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static File findLog() {
        File log = null;
        for (File file : Objects.requireNonNull(logFilesDir.listFiles(pathname -> pathname.getName().endsWith("dsevents")))) {
            if (file.isFile()) {
                if (log == null) {
                    log = file;
                } else {
                    if (log.lastModified() < file.lastModified()) {
                        log = file;
                    }
                }
            }
        }
        System.out.println(log.toString());
        return log;
    }

    private static void startDS() {
        try {
            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + DRIVER_STATION_FULL_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isRunning(String processName) {
        try {
            String line;
            StringBuilder pidInfo = new StringBuilder();
            Process p = Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\" + "tasklist.exe");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                pidInfo.append(line);
            }
            input.close();
            return pidInfo.toString().contains(processName);
        } catch (Exception e) {
            return false;
        }
    }
}
