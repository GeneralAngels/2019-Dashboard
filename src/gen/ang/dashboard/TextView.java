package gen.ang.dashboard;

import javax.swing.*;
import java.awt.*;

public class TextView extends JLabel {
    @Override
    public void setText(String text) {
        setHorizontalAlignment(JLabel.CENTER);
        setFont(new Font(Font.SANS_SERIF,Font.PLAIN,30));
        setOpaque(false);
        super.setText(text);
    }
}
