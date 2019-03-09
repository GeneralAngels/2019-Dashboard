package gen.ang.dashboard.mjpeg;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImagePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private BufferedImage image;

    public ImagePanel(BufferedImage image) {
        this.image = image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (image != null) {

            g.drawImage(image,0,0, getWidth(), getHeight(), this);
//            Graphics2D g2d = (Graphics2D) g.create();
//            int width = getWidth();
//            int height = getHeight();
//            int x = ((width - image.getWidth()) / 2);
//            int y = ((height - image.getHeight()) / 2);
//            g2d.drawImage(image, x, y, this);
//            g2d.dispose();
        }
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }
}
