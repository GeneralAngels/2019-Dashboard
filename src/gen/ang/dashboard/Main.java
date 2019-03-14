package gen.ang.dashboard;

import edu.wpi.first.networktables.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.function.Consumer;

public class Main {
    private static final int WINDOW_HEIGHT = 528;
    private static final String SPLITTING_REGEX = "(?<=\\<message\\> )(([\\u0020-\\u003B]|[=]|[\\u003F-\\u007D])+)";
    private static JFrame frame;
    private static JPanel panel, right;
    private static TextView gearState, stateState, batteryState;
    private static JSONObject currentObject;
    private static CSV csv = new CSV();
    private static boolean record = true;
    private static NetworkTableInstance nti;
    private static NetworkTable database;

    private static int width() {
        return Toolkit.getDefaultToolkit().getScreenSize().width;
    }

    public static void main(String[] args) {
        JButton save, marker, playPause, clear, exit;
        StreamView mainCamera, secondaryCamera;
        JTextArea info;
        JScrollPane infoScroll;
        JPanel smallStreamHolder, csv, robotInfo;
        Dimension buttonDimensions = new Dimension((width() - 100) / 10, 30);
        Dimension statesDimensions = new Dimension((width() - 80) / 6, 30);
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
        batteryState = new TextView();
        info = new JTextArea();
        playPause = new JButton("Pause");
        marker = new JButton("Marker");
        save = new JButton("Save");
        clear = new JButton("Clear");
        exit = new JButton("Exit");
        infoScroll = new JScrollPane(info);
        mainCamera = new StreamView("main");
        secondaryCamera = new StreamView("left");
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
        batteryState.setText("\uD83D\uDDF2 Unknown");
        secondaryCamera.setSize((width() / 2) - 10, (int) (WINDOW_HEIGHT / 2.5));
        mainCamera.setSize(width() / 2, WINDOW_HEIGHT);
        infoScroll.setPreferredSize(infoSize);
        infoScroll.setMinimumSize(infoSize);
        infoScroll.setMaximumSize(infoSize);
        stateState.setMinimumSize(statesDimensions);
        stateState.setPreferredSize(statesDimensions);
        gearState.setMinimumSize(statesDimensions);
        gearState.setPreferredSize(statesDimensions);
        batteryState.setMinimumSize(statesDimensions);
        batteryState.setPreferredSize(statesDimensions);
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
        robotInfo.add(batteryState);
        smallStreamHolder.add(secondaryCamera);
        csv.add(save);
        csv.add(clear);
        csv.add(marker);
        csv.add(playPause);
        csv.add(exit);
        panel.add(mainCamera);
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
        database = nti.getTable("database");
        NetworkTableEntry json = database.getEntry("json");
        NetworkTableEntry battery = database.getEntry("battery");
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
        nti.startClientTeam(2230);
        nti.startDSClient();
        json.addListener(entryNotification -> updateInfo(entryNotification.value.getString(), info), EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);
        battery.addListener(entryNotification -> updateBattery(entryNotification.value.getString()),EntryListenerFlags.kNew | EntryListenerFlags.kUpdate);
    }

    private static void updateBattery(String value){
        batteryState.setText("\uD83D\uDDF2 "+value+"%");
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

    private static void updateRobotState(boolean state) {
        stateState.setText("➤ " + (state ? "Auto" : "Manual"));
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
            if (key.equals("autonomous")) {
                updateRobotState((boolean) got);
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