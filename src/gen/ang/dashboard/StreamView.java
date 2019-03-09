package gen.ang.dashboard;

import gen.ang.dashboard.mjpeg.ImagePanel;
import gen.ang.dashboard.mjpeg.MjpegInputStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class StreamView extends JPanel {

    private static final int FONT_SIZE = 28;
    private static final String CONTENT_LENGTH = "Content-length: ";
    private static final String CONTENT_TYPE = "Content-type: image/jpeg";
    private ArrayList<String> streams;
    private ImagePanel imagePanel;
//    private JLabel imagePanel;
    private JPanel buttons;
    private JButton refresh, next, previous;
    private MjpegInputStream input;
    private URLConnection connection;
    private int streamIndex = 0;

    public StreamView() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        imagePanel = new ImagePanel(null);
//        imagePanel=new JLabel();
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
        add(imagePanel);
        add(buttons);
        updateStreams();
    }

    public void updateStreams() {
        streams = new ArrayList<>();
        updateStream();
        new Thread(() -> {
            try {
                Document document = Jsoup.connect("http://10.22.30.17:5800").timeout(3500).get();
                Elements links = document.select("a");
                for (Element e : links) {
                    if (!e.text().equals("Snapshot"))
                        streams.add(e.attr("href").replace("_viewer", ""));
                }
                updateStream();
            } catch (Exception ignored) {
            }
        });
    }

    public void updateStream() {
        try {
//            input = new URL(streams.get(streamIndex)).openConnection().getInputStream();
            connection= new URL("http://localhost:5800").openConnection();
            connection.setReadTimeout(5000);
            connection.connect();
            input=new MjpegInputStream(connection.getInputStream());
        } catch (Exception e) {
            System.out.println("Failed updating stream");
        }
    }

    public void update() throws Exception {
        imagePanel.setImage(input.readFrame());
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
        imagePanel.setMinimumSize(streamView);
        imagePanel.setMaximumSize(streamView);
        imagePanel.setPreferredSize(streamView);
        super.setPreferredSize(dimension);
        super.setMinimumSize(dimension);
        super.setMaximumSize(dimension);
    }

    @Override
    public void setSize(int width, int height) {
        setSize(new Dimension(width, height));
    }
}