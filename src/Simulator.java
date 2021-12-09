import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

public class Simulator {
    private final CPU cpu;
    private final Memory memory;

    private JTable registerTable;

    private JTable memoryTable;
    private Label memoryLabel;
    private JButton expandButton;
    private DefaultTableModel memoryTableModel;

    private JTextPane logTextPane;

    private JTextPane printerTextPane;

    private JTextArea cardReaderTextArea;
    private JButton cardReaderButton;

    private JButton IPL;
    private JButton runButton;
    private JButton stepButton;
    private JButton loadButton1;
    private JButton loadButton2;

    private JTextField inputTextField;

    private JTextField keyboardTextField;

    // main entrance
    public static void main(String[] args) {
        new Simulator();
    }

    // constructor
    Simulator() {
        memory = new Memory(); // create memory
        cpu = new CPU(memory); // create CPU
        initComponents(); // initiate all components on console
        initListener(); // initiate all listeners for components
        memory.setTextPane(logTextPane); // link memory and log console
        cpu.setTextPane(logTextPane); // link CPU and log console
        cpu.setPrinterTextPane(printerTextPane); // link CPU and printer console
    }

    // initiate components
    public void initComponents() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");  // Windows Theme
        } catch (Exception e) {
            e.printStackTrace();
        }
        // main frame
        JFrame window = new JFrame("Simulator");
        window.setLayout(null);
        window.setSize(810, 730);
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // registers
        String[] registerColumnName = {"Reg", "BiValue"};
        String[][] registerData = {{"R0"}, {"R1"}, {"R2"}, {"R3"}, {"XR1"}, {"XR2"}, {"XR3"}, {"PC"}, {"IR"}, {"CC"}, {"MAR"}, {"MBR"}, {"MFR"}};
        registerTable = new JTable(new DefaultTableModel(registerData, registerColumnName) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex != 0;
            }
        });
        registerTable.setGridColor(Color.BLACK);
        registerTable.setRowHeight(30);
        registerTable.getColumnModel().getColumn(0).setMaxWidth(50);

        JScrollPane registerScrollPane = new JScrollPane(registerTable);
        registerScrollPane.setBounds(2, 30, 220, 410);

        Label registerLabel = new Label("Register");
        registerLabel.setBounds(3, 0, 100, 30);

        // memory
        String[] memoryColumnName = {"Idx", "BiValue"};
        String[][] memoryData = new String[2048][2];
        for (int i = 0; i < 2048; i++) {
            memoryData[i][0] = Integer.toString(i);
        }
        memoryTableModel = new DefaultTableModel(memoryData, memoryColumnName) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex != 0;
            }
        };
        memoryTable = new JTable(memoryTableModel);
        memoryTable.setGridColor(Color.BLACK);
        memoryTable.setRowHeight(20);
        memoryTable.getColumnModel().getColumn(0).setMaxWidth(40);

        JScrollPane memoryScrollPane = new JScrollPane(memoryTable);
        memoryScrollPane.setBounds(235, 30, 220, 610);
        // memory expand button
        expandButton = new JButton("Expand");
        expandButton.setBounds(380, 0, 80, 30);

        memoryLabel = new Label("Memory");
        memoryLabel.setBounds(236, 0, 140, 30);

        // log console
        logTextPane = new JTextPane();
        logTextPane.setEditable(false);

        JScrollPane logScrollPane = new JScrollPane(logTextPane);
        logScrollPane.setBounds(465, 30, 300, 300);

        Label logLabel = new Label("Log");
        logLabel.setBounds(466, 0, 100, 30);

        // printer
        printerTextPane = new JTextPane();
        printerTextPane.setEditable(false);

        JScrollPane printerScrollPane = new JScrollPane(printerTextPane);
        printerScrollPane.setBounds(465, 390, 300, 170);

        Label printerLabel = new Label("Printer");
        printerLabel.setBounds(465, 350, 300, 30);

        // card reader
        cardReaderTextArea = new JTextArea();
        cardReaderTextArea.setDocument(new PlainDocument() {
            public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
                if (str != null) {
                    StringBuilder s = new StringBuilder();
                    for (int i = 0; i < str.length(); i++) {
                        char ch = str.charAt(i);
                        if (ch == '0' || ch == '1' || ch == '\n')
                            s.append(ch);
                    }
                    super.insertString(offset, s.toString(), attr);
                }
            }
        });

        JScrollPane cardReaderScrollPane = new JScrollPane(cardReaderTextArea);
        cardReaderScrollPane.setBounds(2, 470, 220, 170);
        // card reader input button
        cardReaderButton = new JButton("Read");
        cardReaderButton.setBounds(137, 440, 80, 30);

        Label cardReaderLabel = new Label("Card Reader");
        cardReaderLabel.setBounds(2, 440, 110, 30);

        // buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 5));
        buttonPanel.setBounds(2, 650, 400, 30);

        IPL = new JButton("IPL");
        runButton = new JButton("Run");
        stepButton = new JButton("Step");
        loadButton1 = new JButton("Load1");
        loadButton2 = new JButton("Load2");

        buttonPanel.add(IPL);
        buttonPanel.add(runButton);
        buttonPanel.add(stepButton);
        buttonPanel.add(loadButton1);
        buttonPanel.add(loadButton2);

        // execute instruction label
        Label executeLabel = new Label("Execute");
        executeLabel.setBounds(465, 580, 55, 30);
        // input field for single instruction
        inputTextField = new JTextField();
        inputTextField.setBounds(530, 580, 150, 30);
        inputTextField.setDocument(new PlainDocument() {
            public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
                if (str != null && getLength() < 16) {
                    StringBuilder s = new StringBuilder();
                    for (int i = 0; i < str.length(); i++) {
                        char ch = str.charAt(i);
                        if (ch == '0' || ch == '1')
                            s.append(ch);
                    }
                    if ((getLength() + s.length()) > 16)
                        s = new StringBuilder(s.substring(0, 16 - getLength()));
                    super.insertString(offset, s.toString(), attr);
                }
            }
        });

        // keyboard label
        Label keyboardLabel = new Label("Keyboard");
        keyboardLabel.setBounds(465, 620, 65, 30);
        // keyboard input field
        keyboardTextField = new JTextField();
        keyboardTextField.setBounds(530, 620, 90, 30);

        // add all components into main frame
        window.add(registerLabel);
        window.add(registerScrollPane);

        window.add(memoryLabel);
        window.add(memoryScrollPane);
        window.add(expandButton);

        window.add(logLabel);
        window.add(logScrollPane);

        window.add(printerLabel);
        window.add(printerScrollPane);

        window.add(cardReaderLabel);
        window.add(cardReaderButton);
        window.add(cardReaderScrollPane);

        window.add(buttonPanel);

        window.add(executeLabel);
        window.add(inputTextField);

        window.add(keyboardLabel);
        window.add(keyboardTextField);

        window.setVisible(true);
    }

    // initiate listeners
    public void initListener() {
        // register table input listener
        registerTable.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                // use key Enter to input value
                if (e.getKeyChar() == '\n') {
                    int row = registerTable.getSelectedRow();
                    int column = registerTable.getSelectedColumn();
                    String s = (String) registerTable.getValueAt(row, column);
                    StringBuilder ss = new StringBuilder();
                    int count = 0;
                    for (int i = 0; i < s.length(); i++) {
                        char ch = s.charAt(i);
                        if (ch != '0' && ch != '1' && ch != ',') {
                            printLog("Set Register Value Failed! Value Has Invalid Character");
                            refresh();
                            return;
                        } else if (ch != ',') {
                            ss.append(ch);
                            count++;
                            if (count > 16) {
                                printLog("Set Register Value Failed! Value Is Over Range");
                                refresh();
                                return;
                            }
                        }
                    }
                    cpu.setRegister(row, (char) Integer.parseInt(ss.toString(), 2));
                }
                refresh();
            }
        });
        registerTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (memoryTable.isEditing())
                    memoryTable.getCellEditor().stopCellEditing();
                memoryTable.clearSelection();
                refresh();
            }
        });

        // memory table listener
        memoryTable.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                // use key Enter to input value
                if (e.getKeyChar() == '\n') {
                    int row = memoryTable.getSelectedRow();
                    int column = memoryTable.getSelectedColumn();
                    String s = (String) memoryTable.getValueAt(row, column);
                    StringBuilder ss = new StringBuilder();
                    int count = 0;
                    for (int i = 0; i < s.length(); i++) {
                        char ch = s.charAt(i);
                        if (ch != '0' && ch != '1' && ch != ',') {
                            printLog("Set Memory Value Failed! Value Has Invalid Character");
                            refresh();
                            return;
                        } else if (ch != ',') {
                            ss.append(ch);
                            count++;
                            if (count > 16) {
                                printLog("Set Memory Value Failed! Value Is Over Range");
                                refresh();
                                return;
                            }
                        }
                    }
                    memory.store(row, (char) Integer.parseInt(ss.toString(), 2));
                    printLog("Set Memory[" + row + "] = " + ss);
                }
                refresh();
            }
        });
        memoryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (registerTable.isEditing())
                    registerTable.getCellEditor().stopCellEditing();
                registerTable.clearSelection();
                refresh();
            }
        });

        //input text field listener
        inputTextField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                // use key Enter to input single instruction
                if (e.getKeyChar() == '\n') {
                    String s = inputTextField.getText();
                    if (s != null && s.length() > 0) {
                        if (s.length() < 16) {
                            printLog("Execute Failed! Please input 16 bits instruction");
                        } else {
                            int tmp = Integer.parseInt(s, 2);
                            cpu.setIR((char) tmp);
                            printLog("Execute Single Instruction:");
                            cpu.runInstruction();
                        }
                    }
                    inputTextField.setText("");
                }
            }
        });

        // keyboard text field listener
        keyboardTextField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                // use key Enter to input value
                if (e.getKeyChar() == '\n') {
                    String s = keyboardTextField.getText();
                    if (s.length() > 0) {
                        Vector<Character> in = new Vector<>();
                        boolean flag = true;
                        for (int i = 0; i < s.length(); i++) {
                            if (s.charAt(0) < '0' || s.charAt(0) > '9') {
                                flag = false;
                                break;
                            }
                        }
                        if (flag) {
                            int tmp = Integer.parseInt(keyboardTextField.getText());
                            in.add((char) tmp);
                        } else {
                            for (int i = 0; i < s.length(); i++) {
                                in.add(s.charAt(i));
                            }
                            in.add((char) 4);
                        }
                        cpu.setKeyboardInput(in);
                    }
                    keyboardTextField.setText("");
                }
            }
        });

        // button action listener
        ActionListener buttonListener = e -> {
            switch (e.getActionCommand()) {
                case "IPL": {
                    logTextPane.setText("-------Start-------");
                    memoryTableModel.setRowCount(2048);
                    expandButton.setVisible(true);
                    memoryLabel.setText("Memory (2048 Words)");
                    memory.loadROM();
                    cpu.clear();
                    cpu.setRegister(7, (char) 6);
                    break;
                }
                case "Run":
                    cpu.run();
                    break;
                case "Step":
                    cpu.stepRun();
                    break;
                case "Load1": {
                    memory.load1();
                    cpu.clear();
                    cpu.setRegister(7, (char) 61);
                    break;
                }
                case "Load2": {
                    memory.load2();
                    cpu.clear();
                    cpu.setRegister(7, (char) 1000);
                    break;
                }
                case "Read": {
                    String s = cardReaderTextArea.getText();
                    String[] ss = s.split("\n");
                    int tmpFlag = 0;
                    for (String value : ss) {
                        if (value.length() != 16 && value.length() != 0) {
                            printLog("Read Failed! Every line must be 16 bits");
                            tmpFlag = 1;
                            cardReaderTextArea.setText("");
                            break;
                        }
                    }
                    if (tmpFlag == 0) {
                        cpu.setCardReaderInput(ss);
                        cardReaderTextArea.setText("");
                    }
                    break;
                }
                case "Expand": {
                    memory.expand();
                    for (int i = 2048; i < 4096; i++) {
                        Vector<String> v = new Vector<>();
                        v.add(Integer.toString(i));
                        memoryTableModel.addRow(v);
                    }
                    expandButton.setVisible(false);
                    memoryLabel.setText("Memory (4096)");
                    break;
                }
            }
            refresh();
        };
        IPL.addActionListener(buttonListener);
        runButton.addActionListener(buttonListener);
        stepButton.addActionListener(buttonListener);
        loadButton1.addActionListener(buttonListener);
        loadButton2.addActionListener(buttonListener);
        cardReaderButton.addActionListener(buttonListener);
        expandButton.addActionListener(buttonListener);
    }

    // refresh the display value
    public void refresh() {
        // registers
        for (int i = 0; i < registerTable.getRowCount(); i++) {
            int value = cpu.getRegister(i);
            toBi(i, value, registerTable);
        }

        // memory
        for (int i = 0; i < memoryTable.getRowCount(); i++) {
            int value = memory.load(i);
            toBi(i, value, memoryTable);
        }
    }

    private void toBi(int i, int value, JTable memoryTable) {
        String s = Integer.toBinaryString(value);
        s = "0000000000000000" + s;
        s = s.substring(s.length() - 16);
        StringBuilder ss = new StringBuilder();
        for (int j = 0; j < s.length(); j++) {
            ss.append(s.charAt(j));
            if (j % 4 == 3 && j < 15)
                ss.append(",");
        }
        memoryTable.setValueAt(ss.toString(), i, 1);
    }

    // print log
    public void printLog(String s) {
        Document doc = logTextPane.getDocument();
        s = "\n" + s;
        SimpleAttributeSet attrSet = null;
        if (s.contains("Failed")) {
            attrSet = new SimpleAttributeSet();
            StyleConstants.setForeground(attrSet, Color.RED);
        }
        try {
            doc.insertString(doc.getLength(), s, attrSet);
        } catch (BadLocationException e) {
            System.out.println("BadLocationException: " + e);
        }
        logTextPane.setCaretPosition(doc.getLength());
    }
}