package gen.ang.dashboard;

import com.teamdev.jxbrowser.chromium.*;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Timer;
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

    private static int width() {
        return Toolkit.getDefaultToolkit().getScreenSize().width;
    }

    public static void main(String[] args) {
        JButton saveCSV, addMarker, recordingHalt;
        StreamView main, leftCam, rightCam;
        JTextArea info;
        JScrollPane infoScroll;
        JPanel smallStreamHolder, csvShit, robotInfo;
        Dimension smallButtonDimensions = new Dimension((width() - 30) / 6, 30);
        Dimension infoSize = new Dimension(width() / 2 - 20, WINDOW_HEIGHT / 2 - 45);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        frame = new JFrame("2230 Dash");
        panel = new JPanel();
        right = new JPanel();
        robotInfo = new JPanel();
        csvShit = new JPanel();
        gearState = new TextView();
        stateState = new TextView();
        info = new JTextArea();
        recordingHalt = new JButton("Pause");
        addMarker = new JButton("Marker");
        saveCSV = new JButton("Save");
        infoScroll = new JScrollPane(info);
        main = new StreamView("main");
        leftCam = new StreamView("left");
        rightCam = new StreamView("right");
        smallStreamHolder = new JPanel();
        panel.setLayout(new GridLayout(1, 2));
        smallStreamHolder.setLayout(new GridLayout(1, 2));
        robotInfo.setLayout(new GridLayout(1, 2));
        info.setEditable(false);
        info.setBackground(Color.BLACK);
        info.setForeground(Color.GREEN);
        gearState.setText("⛭ Unknown");
        stateState.setText("➲ Unknown");
        leftCam.setSize((width() / 4) - 10, (int) (WINDOW_HEIGHT / 2.5));
        rightCam.setSize((width() / 4) - 10, (int) (WINDOW_HEIGHT / 2.5));
        main.setSize(width() / 2, WINDOW_HEIGHT);
        infoScroll.setPreferredSize(infoSize);
        infoScroll.setMinimumSize(infoSize);
        infoScroll.setMaximumSize(infoSize);
        stateState.setMinimumSize(smallButtonDimensions);
        stateState.setPreferredSize(smallButtonDimensions);
        gearState.setMinimumSize(smallButtonDimensions);
        gearState.setPreferredSize(smallButtonDimensions);
        saveCSV.setMinimumSize(smallButtonDimensions);
        saveCSV.setPreferredSize(smallButtonDimensions);
        addMarker.setMinimumSize(smallButtonDimensions);
        addMarker.setPreferredSize(smallButtonDimensions);
        recordingHalt.setMinimumSize(smallButtonDimensions);
        recordingHalt.setPreferredSize(smallButtonDimensions);
        robotInfo.add(gearState);
        robotInfo.add(stateState);
        smallStreamHolder.add(leftCam);
        smallStreamHolder.add(rightCam);
        csvShit.add(saveCSV);
        csvShit.add(addMarker);
        csvShit.add(recordingHalt);
        panel.add(main);
        panel.add(right);
        right.add(smallStreamHolder);
        right.add(csvShit);
        right.add(robotInfo);
        right.add(infoScroll);
        frame.setUndecorated(true);
        frame.setContentPane(panel);
        frame.setVisible(true);
        frame.setSize(width(), WINDOW_HEIGHT);
        saveCSV.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    csv.save();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Not Saved!");
                }
            }
        });
        recordingHalt.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                record = !record;
                if (record) {
                    recordingHalt.setText("Pause");
                } else {
                    recordingHalt.setText("Resume");
                }
            }
        });
        addMarker.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String line = JOptionPane.showInputDialog("Marker");
                csv.addLine(new ArrayList<>(Collections.singletonList(line)));
            }
        });
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                main.resize();
                leftCam.resize();
                rightCam.resize();
                if (laps % 1000 == 0) logFile = findLog();
                updateInfo(logFile, info);
                laps++;
            }
        }, 1000, 100);
    }

    private static void updateInfo(File f, JTextArea infoView) {
        ArrayList<String> info = parse(f);
        if (currentIndex > info.size()) currentIndex = 0;
        for (; currentIndex < info.size(); currentIndex++) {
            String m = info.get(currentIndex);
            if (m.startsWith("{")) {
                try {
                    currentObject = new JSONObject(m);
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
//        String message = info[info.length - 1];

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
        stateState.setText("➲ " + state);
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
        Matcher m = Pattern.compile(SPLITTING_REGEX).matcher(readFile(f));
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
        System.out.println(log.toString());
        return log;
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

    static class StreamView extends JPanel {

        private static final int FONT_SIZE = 28;

        private Browser browser;
        private BrowserView browserView;
        private ArrayList<String> streams;
        private JPanel buttons;
        private JButton refresh, next, previous;
        private int streamIndex = 0;

        public StreamView(String name) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            BrowserContextParams params = new BrowserContextParams("temp/browser/" + name);
            params.setStorageType(StorageType.DISK);
            BrowserContext context = new BrowserContext(params);
            browser = new Browser(BrowserType.LIGHTWEIGHT, context);
            browserView = new BrowserView(browser);
            buttons = new JPanel();
            refresh = new JButton("⟳");
            next = new JButton("▶");
            previous = new JButton("◀");
            refresh.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, FONT_SIZE));
            next.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, FONT_SIZE));
            previous.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, FONT_SIZE));
            refresh.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateStreams();
                }
            });
            next.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    next();
                }
            });
            previous.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    previous();
                }
            });
            buttons.add(previous);
            buttons.add(refresh);
            buttons.add(next);
            add(browserView);
            add(buttons);
            updateStreams();
            updateStream();
        }

        public void updateStreams() {
            streams = new ArrayList<>();
            try {
                Document document = Jsoup.connect("http://10.22.30.17:5800").timeout(3500).get();
                Elements links = document.select("a");
                for (Element e : links) {
                    if (!e.text().equals("Snapshot"))
                        streams.add(e.attr("href").replace("_viewer", ""));
                }
            } catch (Exception ignored) {
            }
        }

        public void resize() {
            browser.executeJavaScript("document.getElementsByTagName('img')[0].style.height=\"100%\";document.getElementsByTagName('img')[0].style.width=\"100%\";");
        }

        public void updateStream() {
            if (streamIndex < streams.size())
                browser.loadURL("http://10.22.30.17:5800" + streams.get(streamIndex));
        }

        public void next() {
            if (streams.size() > 0) {
                if (streamIndex < streams.size() - 1) {
                    streamIndex++;
                } else {
                    streamIndex = 0;
                }
                updateStream();
            }
        }

        public void previous() {
            if (streams.size() > 0) {
                if (streamIndex > 0) {
                    streamIndex--;
                } else {
                    streamIndex = streams.size() - 1;
                }
                updateStream();
            }
        }

        @Override
        public void setSize(Dimension dimension) {
            Dimension streamView = new Dimension(dimension.width, dimension.height - 55);
            browserView.setMinimumSize(streamView);
            browserView.setMaximumSize(streamView);
            browserView.setPreferredSize(streamView);
            super.setPreferredSize(dimension);
            super.setMinimumSize(dimension);
            super.setMaximumSize(dimension);
        }

        @Override
        public void setSize(int width, int height) {
            setSize(new Dimension(width, height));
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
