package gen.ang.dashboard;

import edu.wpi.first.networktables.NetworkTableInstance;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final int WINDOW_HEIGHT = 528;
    private static final File LOG_FILES_DIRECTORY = new File("C:\\Users\\Public\\Documents\\FRC\\Log Files");
    private static final String SPLITTING_REGEX = "(?<=\\<message\\> )(([\\u0020-\\u003B]|[=]|[\\u003F-\\u007D])+)";
    private static JFrame frame;
    private static JPanel panel, right;
    private static TextView gearState, stateState;
    private static JSONObject currentObject;
    private static File logFile;
    private static CSV csv = new CSV();
    private static boolean record = true;
    private static int currentIndex = 0;
    private static long laps = 0;
    private static boolean dsState = false;
    private static StringBuilder fileReadBuilder;
    private static FileInputStream logFileInputStream;
    private static NetworkTableInstance nti;
    private static byte[] currentBuffer;

    private static int width() {
        return Toolkit.getDefaultToolkit().getScreenSize().width;
    }

    public static void main(String[] args) {
        JButton save, marker, playPause, clear, exit;
        StreamView main, leftCamera, rightCamera;
        JTextArea info;
        JScrollPane infoScroll;
        JPanel smallStreamHolder, csv, robotInfo;
        Dimension buttonDimensions = new Dimension((width() - 100) / 10, 30);
        Dimension statesDimensions = new Dimension((width() - 80) / 4, 30);
        Dimension infoSize = new Dimension(width() / 2 - 20, WINDOW_HEIGHT / 2 - 45);
        System.setProperty("sun.java2d.opengl", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        frame = new JFrame("2230 Dash");
        panel = new JPanel();
        right = new JPanel();
        robotInfo = new JPanel();
        csv = new JPanel();
        gearState = new TextView();
        stateState = new TextView();
        info = new JTextArea();
        playPause = new JButton("Pause");
        marker = new JButton("Marker");
        save = new JButton("Save");
        clear = new JButton("Clear");
        exit = new JButton("Exit");
        infoScroll = new JScrollPane(info);
        main = new StreamView("main");
        leftCamera = new StreamView("left");
        rightCamera = new StreamView("right");
        smallStreamHolder = new JPanel();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        panel.setLayout(new GridLayout(1, 2));
        smallStreamHolder.setLayout(new GridLayout(1, 2));
        robotInfo.setLayout(new GridLayout(1, 2));
        info.setEditable(false);
        info.setBackground(Color.BLACK);
        info.setForeground(Color.GREEN);
        gearState.setText("⛭ Unknown");
        stateState.setText("➤ Unknown");
        leftCamera.setSize((width() / 4) - 10, (int) (WINDOW_HEIGHT / 2.5));
        rightCamera.setSize((width() / 4) - 10, (int) (WINDOW_HEIGHT / 2.5));
        main.setSize(width() / 2, WINDOW_HEIGHT);
        infoScroll.setPreferredSize(infoSize);
        infoScroll.setMinimumSize(infoSize);
        infoScroll.setMaximumSize(infoSize);
        stateState.setMinimumSize(statesDimensions);
        stateState.setPreferredSize(statesDimensions);
        gearState.setMinimumSize(statesDimensions);
        gearState.setPreferredSize(statesDimensions);
        save.setMinimumSize(buttonDimensions);
        save.setPreferredSize(buttonDimensions);
        clear.setMinimumSize(buttonDimensions);
        clear.setPreferredSize(buttonDimensions);
        marker.setMinimumSize(buttonDimensions);
        marker.setPreferredSize(buttonDimensions);
        playPause.setMinimumSize(buttonDimensions);
        playPause.setPreferredSize(buttonDimensions);
        exit.setMinimumSize(buttonDimensions);
        exit.setPreferredSize(buttonDimensions);
        robotInfo.add(gearState);
        robotInfo.add(stateState);
        smallStreamHolder.add(leftCamera);
        smallStreamHolder.add(rightCamera);
        csv.add(save);
        csv.add(clear);
        csv.add(marker);
        csv.add(playPause);
        csv.add(exit);
        panel.add(main);
        panel.add(right);
        right.add(smallStreamHolder);
        right.add(csv);
        right.add(robotInfo);
        right.add(infoScroll);
        frame.setUndecorated(true);
        frame.setContentPane(panel);
        frame.setVisible(true);
        frame.setSize(width(), WINDOW_HEIGHT);
        nti = NetworkTableInstance.getDefault();

        save.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Main.csv.save();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Not Saved!");
                }
            }
        });
        clear.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Main.csv.clear();
            }
        });
        playPause.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                record = !record;
                if (record) {
                    playPause.setText("Pause");
                } else {
                    playPause.setText("Resume");
                }
            }
        });
        marker.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String line = JOptionPane.showInputDialog("Marker");
                Main.csv.addLine(new ArrayList<>(Collections.singletonList(line)));
            }
        });
        exit.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        nti.addEntryListener("json", entryNotification -> {
            updateInfo(entryNotification.value.getString(), info);
        }, 0);
//        new Timer().scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                boolean tempState = getDSState();
//                if (laps % 10 == 0 || dsState != tempState) logFile = findLog();
//                updateInfo(logFile, info);
//                dsState = tempState;
//                laps++;
//                nti.
//            }
//        }, 1000, 500);
    }

    private static void updateInfo(String s, JTextArea infoView) {
        if (s.startsWith("{")) {
            try {
                currentObject = new JSONObject(s);
                ArrayList<Value> inlinified = inlinify(currentObject, null);
                if (record) {
                    csv.addLine(lineify(inlinified));
                    csv.setTitles(titleify(inlinified));
                }
                StringBuilder infoBuilder = new StringBuilder();
                for (Value v : inlinified) {
                    infoBuilder.append(v.getKey()).append(": ").append(v.value).append("\n");
                }
                infoView.setText(infoBuilder.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static ArrayList<String> titleify(ArrayList<Value> values) {
        ArrayList<String> t = new ArrayList<>();
        for (Value v : values) {
            t.add(v.getKey());
        }
        return t;
    }

    private static ArrayList<String> lineify(ArrayList<Value> values) {
        ArrayList<String> t = new ArrayList<>();
        for (Value v : values) {
            t.add(v.getValue());
        }
        return t;
    }

    private static void updateGear(boolean state) {
        gearState.setText("⛭ " + (state ? "Power" : "Speed"));
    }

    private static void updateState(String state) {
        stateState.setText("➤ " + state);
    }

    private static ArrayList<Value> inlinify(JSONObject jsonObject, String parent) {
        ArrayList<Value> inlined = new ArrayList<>();
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object got = jsonObject.get(key);
            if (key.equals("gear")) {
                updateGear((boolean) got);
            }
            if (key.equals("current_state")) {
                updateState((String) got);
            }
            if (parent != null) key = parent + "->" + key;
            if (got instanceof JSONObject) {
                inlined.addAll(inlinify((JSONObject) got, key));
            } else if (got instanceof JSONArray) {
                JSONArray arr = (JSONArray) got;
                Value v = new Value();
                v.setKey(key);
                String val = "";
                for (int i = 0; i < arr.length(); i++) {
                    val += arr.get(i).toString();
                }
                v.setValue(val);
                inlined.add(v);
            } else {
                Value v = new Value();
                v.setKey(key);
                v.setValue(got.toString());
                inlined.add(v);
            }
        }
        return inlined;
    }

    private static ArrayList<String> parse(File f) {
        ArrayList<String> array = new ArrayList<>();
        Matcher m = Pattern.compile(SPLITTING_REGEX).matcher(readFile(f));
        while (m.find()) {
            array.add(m.group());
        }
        return array;
//        return readFile(f).split("<message> ");
    }

    public static String readFile(File file) {
        try {
            if (logFileInputStream == null) {
                logFileInputStream = new FileInputStream(file);
            }
            fileReadBuilder = new StringBuilder();
            currentBuffer = new byte[logFileInputStream.available() + 1];
            logFileInputStream.read(currentBuffer);
            for (byte b : currentBuffer) fileReadBuilder.append((char) b);
            return fileReadBuilder.toString();
        } catch (Exception e) {
            logFileInputStream = null;
            e.printStackTrace();
        }
        return "";
    }

    private static File findLog() {
        File log = null;
        for (File file : Objects.requireNonNull(LOG_FILES_DIRECTORY.listFiles(pathname -> pathname.getName().endsWith("dsevents")))) {
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
//        System.out.println("Update log");
        return log;
    }

    private static boolean getDSState() {
        String DRIVER_STATION = "DriverStation.exe";
        return isRunning(DRIVER_STATION);
    }

    private static void startDS() {
        String DRIVER_STATION = "DriverStation.exe";
        String DRIVER_STATION_FULL_PATH = "C:\\Program Files (x86)\\FRC Driver Station\\" + DRIVER_STATION;
        try {
            if (!isRunning(DRIVER_STATION))
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

    static class Value {
        String value;
        String key;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    static class CSV {
        ArrayList<String> titles = new ArrayList<>();
        ArrayList<ArrayList<String>> lines = new ArrayList<>();

        public void setTitles(ArrayList<String> titles) {
            this.titles = titles;
        }

        public void addLine(ArrayList<String> values) {
            lines.add(values);
        }

        public void clear() {
            lines.clear();
        }

        public void save() throws IOException {
            String date = new SimpleDateFormat("dd_MM-HH_mm_ss").format(new Date());
            File f = new File(System.getProperty("user.home") + "/DashCSVs/Data(" + date + ").csv");
            System.out.println(f.toString());
            f.createNewFile();
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(f));
            StringBuilder titleString = new StringBuilder();
            for (String title : titles) {
                if (titleString.length() > 0) titleString.append(",");
                titleString.append(title);
            }
            osw.append(titleString.toString()).append("\n");
            for (ArrayList<String> vals : lines) {
                StringBuilder valString = new StringBuilder();
                for (String val : vals) {
                    if (valString.length() > 0) valString.append(",");
                    valString.append(val);
                }
                osw.append(valString.toString()).append("\n");
            }
            osw.close();
        }
    }
}