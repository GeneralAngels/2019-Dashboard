package gen.ang.dashboard;

import com.teamdev.jxbrowser.chromium.*;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

public class StreamView extends JPanel {

    private static final int FONT_SIZE = 28;
    private ArrayList<String> streams;
    private Browser browser;
    private BrowserView image;
    private JPanel buttons;
    private JButton refresh, next, previous;
    private int streamIndex = 0;

    public StreamView(String name) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//        BrowserPreferences.setChromiumSwitches("--disable-gpu","--proxy-server='direct://'","--proxy-bypass-list=*");
        BrowserPreferences.setChromiumSwitches("--proxy-server='direct://'","--proxy-bypass-list=*");
        BrowserContextParams params = new BrowserContextParams("browser/" + name);
        params.setStorageType(StorageType.DISK);
        BrowserContext context = new BrowserContext(params);
        browser = new Browser(BrowserType.LIGHTWEIGHT, context);
        image = new BrowserView(browser);
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
        add(image);
        add(buttons);
        updateStreams();
        updateStream();
    }

    public void updateStreams() {
        streams = new ArrayList<>();
        streams.add("");
        new Thread(() -> {
            try {
                Document document = Jsoup.connect("http://10.22.30.17:5800").get();
                Elements links = document.select("a");
                for (Element e : links) {
                    if (!e.text().equals("Snapshot")) {
                        String url = e.attr("href").replace("_viewer", "");
                        System.out.println("Found " + url);
                        streams.add(url);
                    }
                }
                if(streams.size()==1)System.out.println("Found None!");
                updateStream();
            } catch (Exception ignored) {
            }
        }).start();
    }

    public void updateStream() {
        try {
            String url = null;
            if (streamIndex < streams.size())
                url = streams.get(streamIndex);
            else {
                streamIndex = 0;
                url = streams.get(streamIndex);
            }
            String html = "<html><head></head><body style=\"margin:0;background:#808080;\">" + ((url != null) ? (url.length() > 0 ? "<img height=\"100%\" width=\"100%\" src=\"http://10.22.30.17:5800" + url + "\"></img>" : "<p style=\"text-align:center;padding-top: 46vh;\" height=\"100%\" width=\"100%\">Disabled</p>") : "<p style=\"text-align:center;padding-top: 46vh;\" height=\"100%\" width=\"100%\">Unable to load stream</p>") + "</body></html>";
            browser.loadHTML(html);
        } catch (Exception e) {
            System.out.print("Failed updating stream: ");
            e.printStackTrace();
        }
    }

    public void next() {
        if (streams.size() > 0) {
            if (streamIndex < streams.size() - 1) {
                streamIndex++;
            } else {
                streamIndex = 0;
            }
        }
        updateStream();
    }

    public void previous() {
        if (streams.size() > 0) {
            if (streamIndex > 0) {
                streamIndex--;
            } else {
                streamIndex = streams.size() - 1;
            }
        }
        updateStream();
    }

    @Override
    public void setSize(Dimension dimension) {
        Dimension streamView = new Dimension(dimension.width, dimension.height - 55);
        browser.setSize(streamView.width, streamView.height);
        image.setMinimumSize(streamView);
        image.setMaximumSize(streamView);
        image.setPreferredSize(streamView);
        super.setPreferredSize(dimension);
        super.setMinimumSize(dimension);
        super.setMaximumSize(dimension);
    }

    @Override
    public void setSize(int width, int height) {
        setSize(new Dimension(width, height));
    }
}