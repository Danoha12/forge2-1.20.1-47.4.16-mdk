package com.trolmastercard.sexmod;

import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Swing JFrame age-gate shown the first time the mod is loaded.
 * Players must confirm they are 18+ to continue; declining deletes the mod.
 *
 * Note: in 1.20.1 Forge the actual display should be triggered from
 * {@link net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent} via
 * {@link EventQueue#invokeLater}.
 *
 * Obfuscated name: g2
 */
@OnlyIn(Dist.CLIENT)
public class AgeWarningScreen extends JFrame {

    private final JPanel contentPane;

    static AgeWarningScreen instance;

    /** Set to {@code false} once the user has interacted with the dialog. */
    public static boolean showing = true;

    // -- Static factory --------------------------------------------------------

    public static void show() {
        EventQueue.invokeLater(() -> {
            try {
                instance = new AgeWarningScreen();
                instance.setVisible(true);
                instance.requestFocus();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // -- Constructor -----------------------------------------------------------

    public AgeWarningScreen() {
        setResizable(false);
        setBounds(100, 100, 600, 260);

        this.contentPane = new JPanel();
        this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(this.contentPane);

        // Title
        JPanel north = new JPanel();
        this.contentPane.add(north, BorderLayout.NORTH);
        JTextPane title = new JTextPane();
        title.setFont(new Font("Tahoma", Font.PLAIN, 16));
        title.setBackground(SystemColor.control);
        title.setText(I18n.get("window.pornwarning.title"));
        north.add(title);

        // Buttons
        JPanel south = new JPanel();
        this.contentPane.add(south, BorderLayout.SOUTH);
        JCheckBox dontAsk = new JCheckBox(I18n.get("window.pornwarning.dontaskagain"));
        south.add(dontAsk);

        JButton am18 = new JButton(I18n.get("window.pornwarning.am18"));
        am18.addActionListener(e -> {
            showing = false;
            if (dontAsk.isSelected()) {
                File dir = new File("sexmod");
                dir.mkdir();
                try {
                    new File("sexmod/dontAskAgain").createNewFile();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            instance.dispose();
        });
        south.add(am18);

        JButton notAm18 = new JButton(I18n.get("window.pornwarning.not18"));
        notAm18.addActionListener(e -> {
            showing = false;
            System.out.println("MINOR!!! WHEOO WOOO WHEEE WHOOO WHEEE WHOO");
            try {
                FileUtils.deleteDirectory(new File("sexmod"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // Write a batch script to delete the mod jar, then exit
            File bat = new File("mods/youCanJustDeleteMe.bat");
            try (FileWriter fw = new FileWriter(bat)) {
                fw.write("@echo off\n");
                fw.write("TIMEOUT /T 5\n");
                fw.write("DEL \"mods\\*sexmod*.jar\"\n");
                fw.write("exit 0");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                Runtime.getRuntime().exec("cmd /c start " + bat.getPath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });
        south.add(notAm18);

        // Body text
        JPanel center = new JPanel();
        this.contentPane.add(center, BorderLayout.CENTER);
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        JTextPane body = new JTextPane();
        body.setContentType("text/html");
        body.setBackground(SystemColor.control);
        body.setEditable(false);
        body.setText("<html><center><p style='font-family: Tahoma'>"
                + I18n.get("window.pornwarning.text")
                + "</p></center></html> ");
        center.add(body);
    }
}
