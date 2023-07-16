import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class TFTPClientGUI extends JFrame {
    private TFTPClient tftpClient;
    private JTextField serverIPField;
    private JTextField remoteFilenameField;
    private JTextField localFilenameField;
    private JButton downloadButton;
    private JButton uploadButton;
    private JLabel errorLabel;

    public TFTPClientGUI() {
        // Set up GUI components
        serverIPField = new JTextField(15);
        remoteFilenameField = new JTextField(15);
        localFilenameField = new JTextField(15);
        downloadButton = new JButton("Download");
        uploadButton = new JButton("Upload");
        errorLabel = new JLabel();

        // Set up layout
        setLayout(new GridLayout(6, 2));
        add(new JLabel("Server IP:"));
        add(serverIPField);
        add(new JLabel("Remote Filename:"));
        add(remoteFilenameField);
        add(new JLabel("Local Filename:"));
        add(localFilenameField);
        add(downloadButton);
        add(uploadButton);
        add(new JLabel("Error:"));
        add(errorLabel);

        // Set up button actions
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    tftpClient = new TFTPClient(serverIPField.getText());
                    tftpClient.downloadFile(remoteFilenameField.getText(), localFilenameField.getText());
                    JOptionPane.showMessageDialog(TFTPClientGUI.this, "Download successful!");
                } catch (IOException ex) {
                    errorLabel.setText(ex.getMessage());
                }
            }
        });

        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    tftpClient = new TFTPClient(serverIPField.getText());
                    tftpClient.uploadFile(localFilenameField.getText(), remoteFilenameField.getText());
                    JOptionPane.showMessageDialog(TFTPClientGUI.this, "Upload successful!");
                } catch (IOException ex) {
                    errorLabel.setText(ex.getMessage());
                }
            }
        });

        // Set up frame
        setTitle("TFTP Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }

    public static void main(String[] args) {
        new TFTPClientGUI();
    }
}

