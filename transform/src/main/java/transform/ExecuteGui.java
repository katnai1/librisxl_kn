package transform;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import whelk.Document;
import whelk.Whelk;
import whelk.util.TransformScript;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExecuteGui extends JFrame
{
    public JTextArea m_scriptTextArea;
    public JTextArea m_sqlTextArea;
    public JTextArea m_originalRecordArea;
    public JTextArea m_transformedRecordArea;

    public ExecuteGui()
    {
        ActionResponse actionResponse = new ActionResponse(this);

        this.setLocationRelativeTo(null);
        this.setTitle("Libris XL data transform");
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        BoxLayout boxLayout = new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS);
        this.getContentPane().setLayout(boxLayout);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        fileMenu.setMnemonic(KeyEvent.VK_F);
        this.setJMenuBar(menuBar);

        JMenuItem loadItem = new JMenuItem("Open");
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK));
        loadItem.addActionListener(actionResponse);
        fileMenu.add(loadItem);
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
        saveItem.addActionListener(actionResponse);
        fileMenu.add(saveItem);
        JMenuItem saveAsItem = new JMenuItem("SaveAs");
        saveAsItem.addActionListener(actionResponse);
        fileMenu.add(saveAsItem);
        JMenuItem loadEnvironment = new JMenuItem("LoadEnvironmentFile");
        loadEnvironment.addActionListener(actionResponse);
        fileMenu.add(loadEnvironment);

        m_sqlTextArea = getTextArea("SELECT id FROM lddb WHERE collection <> 'definitions'", 4, 40, true);
        JComponent jc = makeLeftAligned(m_sqlTextArea);
        this.getContentPane().add( makeLeftAligned(new JLabel("Select (short) IDs to operate on:")) );
        this.getContentPane().add( jc );

        m_scriptTextArea = getTextArea("# SCRIPT GOES HERE", 20, 40, true);
        JComponent scriptArea = makeLeftAligned(new JScrollPane(m_scriptTextArea));
        JComponent editPanel = makeLeftAligned(new JPanel());
        editPanel.setLayout(new BorderLayout());
        editPanel.add( makeLeftAligned(new JLabel("Transformation Script:")), BorderLayout.NORTH );
        editPanel.add( scriptArea, BorderLayout.CENTER );
        this.getContentPane().add( editPanel );

        JPanel jsonDisplay = new JPanel();
        jsonDisplay.setLayout(new GridLayout(1, 2));

        JPanel buttonPanel = new JPanel();
        this.getContentPane().add(makeLeftAligned(buttonPanel));
        JButton b1 = new JButton("Try next (without saving)");
        b1.setActionCommand("Try");
        b1.addActionListener(actionResponse);
        JButton b2 = new JButton("Execute and save (all records)");
        b2.setActionCommand("ExecuteAll");
        b2.addActionListener(actionResponse);
        JButton b3 = new JButton("Reset");
        b3.setActionCommand("Reset");
        b3.addActionListener(actionResponse);
        buttonPanel.add(b1);
        buttonPanel.add(b2);
        buttonPanel.add(b3);

        JPanel before = new JPanel();
        before.setLayout(new BorderLayout(10, 10));
        before.add( new JLabel("Before execution:"), BorderLayout.NORTH );
        m_originalRecordArea = getTextArea("", 20, 40, false);
        before.add( new JScrollPane(m_originalRecordArea), BorderLayout.CENTER );

        JPanel after = new JPanel();
        after.setLayout(new BorderLayout(10, 10));
        after.add( new JLabel("After execution:"), BorderLayout.NORTH );
        m_transformedRecordArea = getTextArea("", 20, 40, false);
        after.add( new JScrollPane(m_transformedRecordArea), BorderLayout.CENTER );

        jsonDisplay.add( makeLeftAligned(before), 0 );
        jsonDisplay.add( makeLeftAligned(after), 1 );

        this.getContentPane().add(makeLeftAligned(jsonDisplay));

        this.pack();
        this.setVisible(true);
    }

    private JTextArea getTextArea(String s, int lines, int columns, boolean editable)
    {
        JTextArea area = new JTextArea(s, lines, columns);
        area.setLineWrap(true);
        area.setEditable(editable);
        return area;
    }

    private JComponent makeLeftAligned(JComponent c)
    {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private class ActionResponse implements ActionListener
    {
        private Component m_parent;
        private JFileChooser m_fileChooser = new JFileChooser();
        private File m_currentFile = null;
        private Properties m_envProps = null;
        private JLabel m_progressLabel = null;
        private Whelk m_whelk = null;
        private ObjectMapper m_mapper = new ObjectMapper();

        private Connection m_connection;
        private PreparedStatement m_statement;
        private ResultSet m_resultSet;

        public ActionResponse(Component parent)
        {
            m_parent = parent;
        }

        @Override
        public void actionPerformed(ActionEvent ae)
        {
            int result;

            switch(ae.getActionCommand())
            {
                case "Open":
                    result = m_fileChooser.showOpenDialog(m_parent);
                    if (result == JFileChooser.APPROVE_OPTION)
                    {
                        m_currentFile = m_fileChooser.getSelectedFile();
                        load();
                    }
                    break;
                case "Save":
                    if (m_currentFile == null)
                    {
                        result = m_fileChooser.showOpenDialog(m_parent);
                        if (result == JFileChooser.APPROVE_OPTION)
                        {
                            m_currentFile = m_fileChooser.getSelectedFile();
                        }
                    }
                    if (m_currentFile != null)
                        save();
                    break;
                case "SaveAs":
                    result = m_fileChooser.showOpenDialog(m_parent);
                    if (result == JFileChooser.APPROVE_OPTION)
                    {
                        m_currentFile = m_fileChooser.getSelectedFile();
                        save();
                    }
                    break;
                case "LoadEnvironmentFile":
                    result = m_fileChooser.showOpenDialog(m_parent);
                    if (result == JFileChooser.APPROVE_OPTION)
                    {
                        executeUnderDialog("Connecting", "Connecting to XL environment, please wait..", () ->
                            {
                                try
                                {
                                    javax.swing.SwingUtilities.invokeLater( () -> m_progressLabel.setText("Loading config..") );
                                    File envFile = m_fileChooser.getSelectedFile();
                                    InputStream propStream = new FileInputStream(envFile);
                                    m_envProps = new Properties();
                                    m_envProps.load(propStream);
                                    javax.swing.SwingUtilities.invokeLater( () -> m_progressLabel.setText("Starting Whelk..") );
                                    m_whelk = new Whelk(m_envProps);
                                } catch (IOException ioe)
                                {
                                    JOptionPane.showMessageDialog(m_parent, ioe.toString());
                                }
                            });
                    }
                    break;
                case "Try":
                    if (m_whelk != null)
                    {
                        if (m_resultSet == null)
                        {
                            startNewTrySeries();
                        }
                        showNextInTrySeries();
                    }
                    break;
                case "Reset":
                    resetTrySeries();
                    break;
                case "ExecuteAll":
                    System.out.println("DO ALL");
                    break;
            }

        }

        private void startNewTrySeries()
        {
            ExecuteGui parent = (ExecuteGui) m_parent;
            String sqlString = parent.m_sqlTextArea.getText();
            if (isObviouslyBadSql(sqlString)) {
                JOptionPane.showMessageDialog(m_parent, "Denied: Suspicious SQL statement.");
                return;
            }

            try
            {
                m_connection = m_whelk.getStorage().getConnection();
                m_statement = m_connection.prepareStatement(sqlString);
                m_resultSet = m_statement.executeQuery();
            } catch (Exception e) {
                resetTrySeries();
                JOptionPane.showMessageDialog(m_parent, e.toString());
            }
        }

        private void showNextInTrySeries()
        {
            try
            {
                if (m_resultSet.next())
                {
                    String shortId = m_resultSet.getString(1);
                    Document document = m_whelk.getStorage().load(shortId);
                    String formattedOriginal = m_mapper.writerWithDefaultPrettyPrinter().writeValueAsString(document.data);

                    ExecuteGui parent = (ExecuteGui) m_parent;
                    parent.m_originalRecordArea.setText(formattedOriginal);
                    TransformScript script = new TransformScript(parent.m_scriptTextArea.getText());
                    Map transformedData = script.executeOn(document.data);
                    String formattedTransformed = m_mapper.writerWithDefaultPrettyPrinter().writeValueAsString(transformedData);
                    parent.m_transformedRecordArea.setText(formattedTransformed);
                }
                else
                {
                    JOptionPane.showMessageDialog(m_parent, "All changes reviewed.");
                    resetTrySeries();
                }
            } catch (IOException | SQLException | TransformScript.TransformSyntaxException e)
            {
                JOptionPane.showMessageDialog(m_parent, e.toString());
            }
        }

        private void resetTrySeries()
        {
            try { if (m_resultSet != null) m_resultSet.close(); } catch (SQLException e) { /* ignore */ }
            try { if (m_statement != null) m_statement.close(); } catch (SQLException e) { /* ignore */ }
            try { if (m_connection != null) m_connection.close(); } catch (SQLException e) { /* ignore */ }
            m_resultSet = null;
            m_statement = null;
            m_connection = null;
        }

        private boolean isObviouslyBadSql(String sql)
        {
            String[] badWords =
                    {
                            "DROP",
                            "TRUNCATE",
                            "MODIFY",
                            "ALTER",
                            "UPDATE",
                    };

            for (String word : badWords)
                if (StringUtils.containsIgnoreCase(sql, word))
                    return true;
            return false;
        }

        private void load()
        {
            try
            {
                BufferedReader reader = new BufferedReader(new FileReader(m_currentFile));
                StringBuilder sb = new StringBuilder();
                String line;
                while ( (line = reader.readLine()) != null)
                {
                    sb.append(line);
                    sb.append("\n");
                }
                ((ExecuteGui) m_parent).m_scriptTextArea.setText(sb.toString());
                reader.close();
            } catch (IOException ioe)
            {
                JOptionPane.showMessageDialog(m_parent, ioe.toString());
            }

        }

        private void save()
        {
            try
            {
                PrintWriter writer = new PrintWriter(m_currentFile);
                String text = ((ExecuteGui) m_parent).m_scriptTextArea.getText();
                writer.write(text);
                writer.close();
            } catch (IOException ioe)
            {
                JOptionPane.showMessageDialog(m_parent, ioe.toString());
            }
        }

        private void executeUnderDialog(String title, String description, Runnable runnable)
        {
            JDialog dialog = new JDialog( (JFrame) m_parent, title, true );
            dialog.setSize(280, 80);
            dialog.getContentPane().setLayout(new BoxLayout( dialog.getContentPane(), BoxLayout.Y_AXIS ));
            m_progressLabel = new JLabel(description);
            dialog.add(m_progressLabel);
            JProgressBar pb = new JProgressBar();
            pb.setIndeterminate(true);
            dialog.add(pb);
            dialog.setLocationRelativeTo(m_parent);
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            // The horror, FU swing.
            Thread worker = new Thread(runnable);
            worker.start();
            new Thread( () ->
            {
                while (true)
                {
                    if (worker.getState() == Thread.State.TERMINATED)
                    {
                        dialog.setVisible(false);
                        return;
                    }
                }
            }).start();
            new Thread( () -> dialog.setVisible(true) ).start();
        }
    }
}
