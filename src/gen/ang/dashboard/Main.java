package gen.ang.dashboard;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;

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
    private static final int WINDOW_HEIGHT=528;
    private static final String DRIVER_STATION = "DriverStation.exe";
    private static final String DRIVER_STATION_FULL_PATH = "C:\\Program Files (x86)\\FRC Driver Station\\" + DRIVER_STATION;
    private static final File logFilesDir = new File("C:\\Users\\Public\\Documents\\FRC\\Log Files");
    private static JFrame frame;
    private static JPanel panel, right, left;
    private static TextView barDesc, state, robotStatus, gear;
    private static JLabel rviz,camera;
    private static JProgressBar bar;
    private static int currentIndex = 0;
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
        left = new JPanel();
        panel.add(right);
        panel.add(left);
        bar = new JProgressBar();
        bar.setStringPainted(true);
        barDesc = new TextView();
        state = new TextView();
        robotStatus = new TextView();
        gear = new TextView();
        rviz=new JLabel();
        camera=new JLabel();
        right.add(barDesc);
        right.add(bar);
        right.add(state);
        right.add(robotStatus);
        right.add(gear);
        Dimension views = new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width / 2, WINDOW_HEIGHT);
        Dimension texts = new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width / 2, WINDOW_HEIGHT / 6);
        openStream(views);
        barDesc.setMinimumSize(texts);
        barDesc.setPreferredSize(texts);
        bar.setMinimumSize(texts);
        bar.setPreferredSize(texts);
        state.setMinimumSize(texts);
        state.setPreferredSize(texts);
        robotStatus.setMinimumSize(texts);
        robotStatus.setPreferredSize(texts);
        gear.setMinimumSize(texts);
        gear.setPreferredSize(texts);
        rviz.setMinimumSize(views);
        rviz.setPreferredSize(views);
        camera.setMinimumSize(views);
        camera.setPreferredSize(views);
        frame.setUndecorated(true);
        frame.setContentPane(panel);
        frame.setVisible(true);
        frame.setSize(Toolkit.getDefaultToolkit().getScreenSize().width, WINDOW_HEIGHT);
        logFile = findLog();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (laps % 5000 == 0) logFile = findLog();
                updateInfo(logFile);
                laps++;
            }
        }, 1000, 50);
    }

    private static void openStream(Dimension d){
        Browser browser = new Browser();
        BrowserView browserView = new BrowserView(browser);
        browser.loadURL("http://bob-de-mini.local:5800/stream_viewer?topic=/camera/rgb");
        browserView.setMinimumSize(d);
        browserView.setPreferredSize(d);
        browser.setSize(d.width,d.height);
        left.add(browserView);
//        try {
//            Runtime.getRuntime().exec("\"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe\" --window-size="+x+","+y+" --window-position="+x+","+0+" --new-window http://bob-de-mini.local:5800/stream_viewer?topic=/camera/rgb");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private static void updateInfo(File f) {
        ArrayList<String> info = parse(f);
        if (currentIndex > info.size()) currentIndex = 0;
        for (; currentIndex < info.size(); currentIndex++) {
            String m = info.get(currentIndex);
            if (m.contains("->") && m.contains("%")) {
                String fullName = readUntil(m.substring(readUntil(m.substring(1), '>').length() + 1), '%');
                String name = fullName.split(",")[0];
                int percent = Integer.parseInt(fullName.split(",")[1].replace(" ", "").replace("%", ""));
                bar.setMaximum(100);
//                barDesc.setText("<html><p style='text-align:center'>" + name + "</p></html>");
                barDesc.setText(name);
                bar.setValue(percent);
            } else if (m.contains("->") && m.contains("State")) {
                String currentState = readUntil(m.substring(readUntil(m, '>').length() + 1), ';').replace(";", "");
                state.setText(currentState);
            } else if (m.contains(":") && m.contains("RobotStatus")) {
                String status = m.replace("RobotStatus: ","");
                robotStatus.setText(status);
            } else if (m.contains(":") && m.contains("Gear")) {
                String status = m.replace("Gear: ","");
                if(status.equals("UP"))robotStatus.setForeground(Color.GREEN);
                if(status.equals("DOWN"))robotStatus.setForeground(Color.RED);
                robotStatus.setText(status);
            }
        }
//        String message = info[info.length - 1];

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
