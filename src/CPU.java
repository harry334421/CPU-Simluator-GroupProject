import java.awt.Color;
import java.util.Vector;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class CPU extends Thread {
    private final Memory memory;    // reference of memory
    private JTextPane printerTextPane; //reference of printer console in UI
    private JTextPane logTextPane;    // reference of log console in UI

    private char[] Reg;    // General Purpose Register (GPR) 16 bits
    private char[] XReg;    // Index Register (XR) 16 bits
    private char PC;        // Program Counter 12 bits
    private char IR;        // Instruction Register 16 bits
    private char CC;        // Condition Code 4 bits
    private char MAR;    // Memory Address Register 16 bits
    private char MBR;    // Memory Buffer Register 16 bits
    private char MFR;    // Machine Fault Register 4 bits

    private Vector<Character> keyboardInput;    // number from the UI input console
    private int inputFlag;    // mark if the CPU is waiting for user to input a number

    // constructor
    CPU(Memory mem) {
        memory = mem;
        // initiate registers with 0
        Reg = new char[]{0, 0, 0, 0};
        XReg = new char[]{0, 0, 0};
        PC = IR = CC = 0;
        MAR = MBR = MFR = 0;
        // initiate input flag, 0 means not waiting, 1 means has an input, -1 means waiting
        keyboardInput = new Vector<>();
        inputFlag = 0;
    }

    // run the CPU until PC go to the HLT address
    public void run() {
        do {
            stepRun();
            // if need a input from user, stop and wait
            if (inputFlag == -1)
                break;
        } while (IR != 0);
    }

    // run step by step
    public void stepRun() {
        IR = (char) load(PC);
        runInstruction();
    }

    // run one instruction
    public void runInstruction() {
        //decode the instruction
        int opcode, reg, xreg, I, Addr, EA, L_R, Immed, code, devID;
        opcode = IR >> 10;
        reg = (IR & 0x0300) >> 8;
        xreg = (IR & 0x00C0) >> 6;
        L_R = xreg & 0b01;
        I = (IR & 0x0020) >> 5;
        Addr = IR & 0x001F;
        Immed = Addr;
        code = Addr;
        devID = Addr;

        // calculate the EA (effective address)
        if (opcode != 041 && opcode != 042 && xreg != 0)
            EA = XReg[xreg - 1];
        else
            EA = 0;
        EA += Addr;
        if (I == 1) {
            int tmp = load(EA);
            if (tmp == Integer.MIN_VALUE)
                return;
            else
                EA = MBR;
        }
        // switch to each instruction
        // memory address bounds of the reserved part
        int reservedMemoryBounds = 27;
        switch (opcode) {
            case 0: // HLT
            {
                printLog("HLT: PC = " + (int) PC);
                break;
            }
            case 01: // LDR
            {
                int tmp = load(EA);
                if (tmp == Integer.MIN_VALUE)
                    return;
                else {
                    Reg[reg] = MBR;
                    PC++;
                    printLog("LDR: Reg[" + reg + "] = " + (int) Reg[reg]);
                }
                break;
            }
            case 02: // STR
            {
                MAR = (char) EA;
                if (MAR <= reservedMemoryBounds)
                    handleMachineFault(0);
                else {
                    if (store(EA, Reg[reg]) == Integer.MIN_VALUE)
                        return;
                    else {
                        PC++;
                        printLog("STR: Memory[" + EA + "] = " + (int) Reg[reg]);
                    }
                    break;
                }
            }
            case 03: // LDA
            {
                Reg[reg] = (char) EA;
                PC++;
                printLog("LDA: Reg[" + reg + "] = " + EA);
                break;
            }
            case 04: // AMR
            {
                int tmp = load(EA);
                if (tmp == Integer.MIN_VALUE)
                    return;
                else {
                    short r = (short) Reg[reg];
                    short m = (short) MBR;
                    tmp = r + m;
                    String s = "";
                    if (tmp > Short.MAX_VALUE) {
                        CC = 0b1000;
                        s += " Result OVERFLOW";
                    } else if (tmp < Short.MIN_VALUE) {
                        CC = 0b0100;
                        s += " Result UNDERFLOW";
                    } else
                        CC = 0b0000;
                    Reg[reg] += MBR;
                    PC++;
                    printLog("AMR: Reg[" + reg + "] = " + (int) Reg[reg] + s);
                    break;
                }
            }
            case 05: // SMR
            {
                int tmp = load(EA);
                if (tmp == Integer.MIN_VALUE)
                    return;
                else {
                    short r = (short) Reg[reg];
                    short m = (short) MBR;
                    tmp = r - m;
                    String s = "";
                    if (tmp > Short.MAX_VALUE) {
                        CC = 0b1000;
                        s += " Result OVERFLOW";
                    } else if (tmp < Short.MIN_VALUE) {
                        CC = 0b0100;
                        s += " Result UNDERFLOW";
                    } else
                        CC = 0b0000;
                    Reg[reg] -= MBR;
                    PC++;
                    printLog("SMR: Reg[" + reg + "] = " + (int) Reg[reg] + s);
                    break;
                }
            }
            case 06: // AIR
            {
                short r = (short) Reg[reg];
                int tmp = r + Immed;
                String s = "";
                if (tmp > Short.MAX_VALUE) {
                    CC = 0b1000;
                    s += " Result OVERFLOW";
                } else CC = 0b0000;
                Reg[reg] += Immed;
                PC++;
                printLog("AIR: Reg[" + reg + "] = " + (int) Reg[reg] + s);
                break;
            }
            case 07: // SIR
            {
                short r = (short) Reg[reg];
                int tmp = r - Immed;
                String s = "";
                if (tmp < Short.MIN_VALUE) {
                    CC = 0b0100;
                    s += " Result UNDERFLOW";
                } else
                    CC = 0b0000;
                Reg[reg] = (char) tmp;
                PC++;
                printLog("SIR: Reg[" + reg + "] = " + (int) Reg[reg] + s);
                break;
            }
            case 010: // JZ
            {
                if (Reg[reg] == 0) {
                    PC = (char) EA;
                    printLog("JZ: Jump To " + (int) PC);
                } else {
                    PC++;
                    printLog("JZ: Not Jump");
                }
                break;
            }
            case 011: // JNE
            {
                if (Reg[reg] != 0) {
                    PC = (char) EA;
                    printLog("JNE: Jump To " + (int) PC);
                } else {
                    PC++;
                    printLog("JNE: Not Jump");
                }
                break;
            }
            case 012: // JCC
            {
                int tmp = 1 << (3 - reg);
                if ((int) CC == tmp) {
                    PC = (char) EA;
                    printLog("JCC: Jump To " + (int) PC);
                } else {
                    PC++;
                    printLog("JCC: Not Jump");
                }
                break;
            }
            case 013: // JMA
            {
                PC = (char) EA;
                printLog("JMA: Jump To " + (int) PC);
                break;
            }
            case 014: // JSR
            {
                Reg[3] = (char) (PC + 1);
                PC = (char) EA;
                printLog("JSR: Reg[3] = " + (int) Reg[3] + " Jump To " + (int) PC);
                break;
            }
            case 015: // RFS
            {
                Reg[0] = (char) Immed;
                PC = Reg[3];
                printLog("RFS: Reg[0] = " + (int) Reg[0] + " Return To " + (int) PC);
                break;
            }
            case 016: // SOB
            {
                short r = (short) Reg[reg];
                r--;
                Reg[reg] = (char) r;
                if (r > 0) {
                    PC = (char) EA;
                    printLog("SOB: Reg[" + reg + "] = " + (int) Reg[reg] + " Branch To " + (int) PC);
                } else {
                    PC++;
                    printLog("SOB: Reg[" + reg + "] = " + (int) Reg[reg] + " Not Branch");
                }
                break;
            }
            case 017: // JGE
            {
                short r = (short) Reg[reg];
                if (r >= 0) {
                    PC = (char) EA;
                    printLog("JGE: Jump To " + (int) PC);
                } else {
                    PC++;
                    printLog("JGE: Not Jump");
                }
                break;
            }
            case 020: // MLT
            {
                short r1 = (short) Reg[reg];
                short r2 = (short) Reg[xreg];
                long result = r1 * r2;
                String s = "";
                CC = 0b0000;
                int re = (int) result;
                Reg[reg] = (char) (re >>> 16);
                Reg[reg + 1] = (char) (re & 0x0000FFFF);
                PC++;
                printLog("MLT: Reg[" + reg + "] = " + (int) Reg[reg]);
                printLog("     Reg[" + (reg + 1) + "] = " + (int) Reg[reg + 1] + s);
                break;
            }
            case 021: // DVD
            {
                if (Reg[xreg] == 0) {
                    CC = 0b0010;
                    printLog("DVD: DIVZERO");
                } else {
                    CC = 0b0000;
                    short r1 = (short) Reg[reg];
                    short r2 = (short) Reg[xreg];
                    int quotient = r1 / r2;
                    int remainder = r1 % r2;
                    Reg[reg] = (char) quotient;
                    Reg[reg + 1] = (char) remainder;
                    printLog("DVD: Reg[" + reg + "] = " + (int) Reg[reg]);
                    printLog("     Reg[" + (reg + 1) + "] = " + (int) Reg[reg + 1]);
                }
                PC++;
                break;
            }
            case 022: // TRR
            {
                String s = "";
                if (Reg[reg] == Reg[xreg]) {
                    CC = 0b0001;
                    s += " Equal";
                } else {
                    CC = 0b0000;
                    s += " Not Equal";
                }
                PC++;
                printLog("TRR: CC = " + (int) CC + s);
                break;
            }
            case 023: // AND
            {
                Reg[reg] &= Reg[xreg];
                PC++;
                printLog("AND: Reg[" + reg + "] = " + (int) Reg[reg]);
                break;
            }
            case 024: // ORR
            {
                Reg[reg] |= Reg[xreg];
                PC++;
                printLog("ORR: Reg[" + reg + "] = " + (int) Reg[reg]);
                break;
            }
            case 025: // NOT
            {
                Reg[reg] = (char) ~Reg[reg];
                PC++;
                printLog("NOT: Reg[" + reg + "] = " + (int) Reg[reg]);
                break;
            }
            case 031: // SRC
            {
                short r = (short) Reg[reg];
                if (L_R == 1)
                    r = (short) (r << Addr);
                else
                    r = (short) (r >> Addr);
                Reg[reg] = (char) r;
                PC++;
                printLog("SRC: Reg[" + reg + "] = " + (int) Reg[reg]);
                break;
            }
            case 032: // RRC
            {
                if (L_R == 1) {
                    int flag;
                    for (int i = 0; i < Addr; i++) {
                        flag = ((Reg[reg] & 0x8000) == 0) ? 0 : 1;
                        Reg[reg] = (char) (Reg[reg] << 1);
                        Reg[reg] = (char) (Reg[reg] | flag);
                    }
                } else {
                    int flag;
                    for (int i = 0; i < Addr; i++) {
                        flag = ((Reg[reg] & 1) == 0) ? 0x0000 : 0x8000;
                        Reg[reg] = (char) (Reg[reg] >> 1);
                        Reg[reg] = (char) (Reg[reg] | flag);
                    }
                }
                PC++;
                printLog("RRC: Reg[" + reg + "] = " + (int) Reg[reg]);
                break;
            }
            case 036: // TRAP
            {
                store(2, (char) (PC + 1));
                PC = 0;
                printLog("TRAP: code = " + code);
                // the size of trap entries table
                int trapCodeRange = 8;
                if (code > trapCodeRange)
                    handleMachineFault(1);
                else
                    PC = (char) (load(PC) + code);
                break;
            }
            case 041: // LDX
            {
                int tmp = load(EA);
                if (tmp == Integer.MIN_VALUE)
                    return;
                else {
                    XReg[xreg - 1] = MBR;
                    PC++;
                    printLog("LDX: XReg[" + xreg + "] = " + (int) XReg[xreg - 1]);
                    break;
                }
            }
            case 042: // STX
            {
                MAR = (char) EA;
                if (MAR <= reservedMemoryBounds)
                    handleMachineFault(0);
                else {
                    if (store(EA, XReg[xreg - 1]) == Integer.MIN_VALUE)
                        return;
                    else {
                        PC++;
                        printLog("STX: Memory[" + EA + "] = " + (int) XReg[xreg - 1]);
                    }
                }
                break;
            }
            case 061: // IN
            {
                if (!keyboardInput.isEmpty()) {
                    if (devID == 0)
                        Reg[reg] = keyboardInput.get(0);
                    keyboardInput.remove(0);
                    PC++;
                    printLog("IN: Reg[" + reg + "] = " + (int) Reg[reg]);
                    inputFlag = 0;
                } else {
                    inputFlag = -1;
                    printLog("Waiting for input for keyboard");
                }
                break;
            }
            case 062: // OUT
            {
                printLog("OUT");
                if (devID == 1)
                    print("" + Reg[reg]);
                else if (devID == 2)
                    print("" + (int) Reg[reg]);
                PC++;
                break;
            }
            default: {
                handleMachineFault(2);
                break;
            }
        }
    }

    // load value from memory
    public int load(int address) {
        MAR = (char) address;
        int tmp = memory.loadCache(address);
        if (tmp == Integer.MIN_VALUE)
            handleMachineFault(3);
        else
            MBR = (char) tmp;
        return tmp;
    }

    // store value into memory
    public int store(int address, char value) {
        // check if address and value are valid
        MAR = (char) address;
        MBR = value;
        int tmp = memory.storeCache(address, value);
        if (tmp == Integer.MIN_VALUE)
            handleMachineFault(3);
        return tmp;
    }

    // handle machine fault
    public void handleMachineFault(int id) {
        store(4, (char) (PC + 1));
        PC = 1;
        MFR = (char) (1 << id);
        printLog("Machine Fault: MFR = " + (int) MFR);
    }

    // for the outside to set the IR value
    public void setIR(char ir) {
        IR = ir;
    }

    // for outside to set register value
    public void setRegister(int index, char value) {
        String s = "";
        switch (index) {
            case 0:
                Reg[0] = value;
                s += "Reg[0]";
                break;
            case 1:
                Reg[1] = value;
                s += "Reg[1]";
                break;
            case 2:
                Reg[2] = value;
                s += "Reg[2]";
                break;
            case 3:
                Reg[3] = value;
                s += "Reg[3]";
                break;
            case 4:
                XReg[0] = value;
                s += "XReg[1]";
                break;
            case 5:
                XReg[1] = value;
                s += "XReg[2]";
                break;
            case 6:
                XReg[2] = value;
                s += "XReg[3]";
                break;
            case 7:
                PC = value;
                s += "PC";
                break;
            case 8:
                IR = value;
                s += "IR";
                break;
            case 9:
                CC = value;
                s += "CC";
                break;
            case 10:
                MAR = value;
                s += "MAR";
                break;
            case 11:
                MBR = value;
                s += "MBR";
                break;
            case 12:
                MFR = value;
                s += "MFR";
                break;
        }
        printLog("Set " + s + " = " + (int) value);
    }

    // for out side to get registers value
    public int getRegister(int index) {
        switch (index) {
            case 0:
                return Reg[0];
            case 1:
                return Reg[1];
            case 2:
                return Reg[2];
            case 3:
                return Reg[3];
            case 4:
                return XReg[0];
            case 5:
                return XReg[1];
            case 6:
                return XReg[2];
            case 7:
                return PC;
            case 8:
                return IR;
            case 9:
                return CC;
            case 10:
                return MAR;
            case 11:
                return MBR;
            case 12:
                return MFR;
            default: // if index is invalid, return an invalid value
                return Integer.MIN_VALUE;
        }
        // set when get a input from keyboard panel
    }

    // set the input from keyboard
    public void setKeyboardInput(Vector<Character> key) {
        if (inputFlag == -1) {
            keyboardInput = key;
            inputFlag = 1;
            run();
        }
    }

    // set the input from card reader
    public void setCardReaderInput(String[] ss) {
        boolean flag = true;
        for (String s : ss) {
            if (s.length() > 0) {
                if (flag) {
                    printLog("Execute from Card Reader:");
                    flag = false;
                }
                int tmp = Integer.valueOf(s, 2);
                IR = (char) tmp;
                runInstruction();
            }
        }
    }

    // clear the CPU, reset all values to initial state
    public void clear() {
        Reg = new char[]{0, 0, 0, 0};
        XReg = new char[]{0, 0, 0};
        PC = IR = CC = 0;
        MAR = MBR = MFR = 0;
        keyboardInput = new Vector<>();
        inputFlag = 0;
    }

    //set printer console reference
    public void setPrinterTextPane(JTextPane printer) {
        printerTextPane = printer;
    }

    // print to printer
    public void print(String s) {
        Document doc = printerTextPane.getDocument();
        SimpleAttributeSet attrSet = new SimpleAttributeSet();
        StyleConstants.setForeground(attrSet, Color.BLUE);
        try {
            doc.insertString(doc.getLength(), s, attrSet);
        } catch (BadLocationException e) {
            System.out.println("BadLocationException: " + e);
        }
        logTextPane.setCaretPosition(doc.getLength());
    }

    // set the log console reference
    public void setTextPane(JTextPane log) {
        logTextPane = log;
    }

    // print log of CPU
    public void printLog(String s) {
        Document doc = logTextPane.getDocument();
        s = "\n" + s;
        SimpleAttributeSet attrSet = null;
        if (s.contains("Error") || s.contains("Fault")) {
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