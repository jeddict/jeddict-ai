import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.FlowLayout;

public class Main {
    public static void main(String[] args) {
        // Create the main window
        JFrame frame = new JFrame("Hello App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 150);

        // Add a label with "Hello" message
        JLabel label = new JLabel("Hello to you!");
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(label);

        // Display the window
        frame.setVisible(true);
    }
}