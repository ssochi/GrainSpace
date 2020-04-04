package org.ssochi.grainspace;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BootFrame {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Config");
        frame.setSize(350, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        frame.add(panel);
        placeComponents(panel,frame);
        frame.setVisible(true);
    }

    private static void placeComponents(JPanel panel, JFrame frame) {
        panel.setLayout(null);

        JLabel lenLabel = new JLabel("World Length(recommend 1024):");
        lenLabel.setBounds(10,20,200,25);
        panel.add(lenLabel);

        JTextField lenText = new JTextField(20);
        lenText.setText("1024");
        lenText.setBounds(220,20,165,25);
        panel.add(lenText);

        JLabel lightLabel = new JLabel("Grain Light  (recommend 10000):");
        lightLabel.setBounds(10,50,200,25);
        panel.add(lightLabel);

        JTextField lightText = new JTextField(20);
        lightText.setBounds(220,50,165,25);
        lightText.setText("10000");
        panel.add(lightText);

        JButton btnBegin = new JButton("begin");
        btnBegin.setBounds(10, 80, 80, 25);
        panel.add(btnBegin);

        btnBegin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try{
                    int length = Integer.parseInt(lenText.getText());
                    int light = Integer.parseInt(lightText.getText());

                    frame.dispose();
                    GrainSpace grainSpace = new GrainSpace(length,light);
                    grainSpace.run();
                }catch (Exception ex){
                    ex.printStackTrace();
                    lenText.setText("");
                    lightText.setText("");
                }
            }
        });
    }

}