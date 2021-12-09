import java.awt.Color;
import java.util.LinkedList;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class Memory {
    private JTextPane logTextPane;    // reference of log console on UI

    private char[] memory;        // 2048 words each is 16 bits
    private boolean expandFlag;    // flag mark if the memory has been expanded
    private LinkedList<CacheLine> cache;    // cache list

    // constructor
    Memory() {
        // use short value to simulate the memory to store word
        memory = new char[2048];
        expandFlag = false;
        cache = new LinkedList<>();
    }

    // load data from memory
    public char load(int address) {
        if (address >= 4096 || (!expandFlag && address >= 2048)) {
            printLog("Error: Load Memory Address Out of Range: " + address);
            return 0;
        } else
            return memory[address];
    }

    // store data into memory
    public void store(int address, char value) {
        if (address >= 4096 || (!expandFlag && address >= 2048))
            printLog("Error: Store Memory Address Out of Range: " + address);
        else
            memory[address] = value;
    }

    // load data from cache
    public int loadCache(int address) {
        // check if the address is valid
        if (address >= 4096 || (!expandFlag && address >= 2048)) {
            printLog("Error: Load Memory Address Out of Range: " + address);
            return Integer.MIN_VALUE;
        } else {
            CacheLine cacheLine;
            // check if the cache has the address
            for (CacheLine line : cache) {
                cacheLine = line;
                if (cacheLine.getAddress() == address)    // hit
                    return cacheLine.getValue();
            }
            // not hit, load from memory, create a new cache line and add it into cache
            char value = load(address);
            cacheLine = new CacheLine(address, value);
            if (cache.size() == 16)
                cache.removeLast();
            cache.addFirst(cacheLine);
            return cacheLine.getValue();
        }
    }

    // store data into cache, also into memory synchronously
    public int storeCache(int address, char value) {
        // check is the address is valid
        if (address >= 4096 || (!expandFlag && address >= 2048)) {
            printLog("Error: Store Memory Address Out of Range: " + address);
            return Integer.MIN_VALUE;
        } else {
            // store into memory
            store(address, value);
            CacheLine cacheLine;
            // check if the cache has the address
            for (CacheLine line : cache) {
                cacheLine = line;
                if (cacheLine.getAddress() == address) // hit
                {
                    cacheLine.setValue(value);
                    return 0;
                }
            }
            // not hit, create a new cache line and add it into cache
            cacheLine = new CacheLine(address, value);
            if (cache.size() == 16)
                cache.removeLast();
            cache.addFirst(cacheLine);
            return 0;
        }
    }

    //expand memory size from 2048 to 4096
    public void expand() {
        if (expandFlag)
            printLog("Error: Memory has been expanded");
        else {
            char[] tmp = memory;
            memory = new char[4096];
            System.arraycopy(tmp, 0, memory, 0, 2048);
            expandFlag = true;
        }
    }

    // clear the memory, reset all values to initial state
    public void clear() {
        memory = new char[2048];
        expandFlag = false;
        cache = new LinkedList<>();
    }

    // clear the memory but keep the ROM code
    public void clearWithROM() {
        char[] tmp = new char[2048];
        System.arraycopy(memory, 0, tmp, 0, 32);
        memory = tmp;
        expandFlag = false;
        cache = new LinkedList<>();
    }

    // set the log console reference
    public void setTextPane(JTextPane log) {
        logTextPane = log;
    }

    // print log message of memory
    public void printLog(String s) {
        Document doc = logTextPane.getDocument();
        s = "\n" + s;
        SimpleAttributeSet attrSet = null;
        if (s.contains("Error")) {
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

    // load IPL program into memory
    public void loadROM() {
        printLog("Load ROM");
        clear();
        store(0, (char) 7); // PC for a Trap
        store(1, (char) 6); // PC for a machine fault
        store(6, (char) 0); // HLT for machine fault
        // Trap instruction entries
        // We use just 8 entries and all jump to same instructions
        store(7, (char) 0b0010110000001111); // JMA jump to 15
        store(8, (char) 0b0010110000001111); // JMA jump to 15
        store(9, (char) 0b0010110000001111); // JMA jump to 15
        store(10, (char) 0b0010110000001111); // JMA jump to 15
        store(11, (char) 0b0010110000001111); // JMA jump to 15
        store(12, (char) 0b0010110000001111); // JMA jump to 15
        store(13, (char) 0b0010110000001111); // JMA jump to 15
        store(14, (char) 0b0010110000001111); // JMA jump to 15
        // Trap instructions
        store(15, (char) 0b0000110000010101); // LDA 0, 0, 21
        store(16, (char) 0b0110010011000010); // SRC 0, 2, 1, 1
        store(17, (char) 0b1100100000000001); // OUT 0, 1 -- 'T'
        store(18, (char) 0b0001110000000010); // SIR 0, 2
        store(19, (char) 0b1100100000000001); // OUT 0, 1 -- 'R'
        store(20, (char) 0b0001110000010001); // SIR 0, 17
        store(21, (char) 0b1100100000000001); // OUT 0, 1 -- 'A'
        store(22, (char) 0b0001100000001111); // AIR 0, 1111
        store(23, (char) 0b1100100000000001); // OUT 0, 1 -- 'P'
        store(24, (char) 0b0000110000001010); // LDA 0, 0, 10
        store(25, (char) 0b1100100000000001); // OUT 0, 1 -- '\n'
        store(26, (char) 0b0000011100000010); // LDR 3, 0, 2
        store(27, (char) 0b0011010000000000); // RFS

        store(28, (char) 100); // address of memory for program to use
    }

    // load Test Program 1 into memory
    public void load1() {
        clearWithROM();
        printLog("Load Test Program 1");
        //data
        store(29, (char) 50);
        store(30, (char) 60);
        store(53, (char) 32767);
        store(54, (char) 20);
        // instruction
        store(61, (char) 0b1000010010011101);
        store(62, (char) 0b1000010011011110);
        store(63, (char) 0b0000011110000100);
        store(64, (char) 0b0001101000000001);
        store(65, (char) 0b0000101000011110);
        store(66, (char) 0b1000010001011110);
        store(67, (char) 0b1100010000000000);
        store(68, (char) 0b0000100001011110);
        store(69, (char) 0b0011101110001110);
        store(70, (char) 0b0001111000010101);
        store(71, (char) 0b0001101100000001);
        store(72, (char) 0b0000101100011110);
        store(73, (char) 0b1000010001011110);
        store(74, (char) 0b1100010000000000);
        store(75, (char) 0b0000100010000001);
        store(76, (char) 0b0000010010000001);
        store(77, (char) 0b0001010001011110);
        store(78, (char) 0b0011110011010110);
        store(79, (char) 0b0100000010000000);
        store(80, (char) 0b0000100100011110);
        store(81, (char) 0b0000010000011110);
        store(82, (char) 0b0000100000011110);
        store(83, (char) 0b0001010010000011);
        store(84, (char) 0b0011110011011100);
        store(85, (char) 0b0000010000011110);
        store(86, (char) 0b0000100010000011);
        store(87, (char) 0b0000101110000010);
        store(88, (char) 0b0000010010000100);
        store(89, (char) 0b0001101100000001);
        store(90, (char) 0b0000101100011110);
        store(91, (char) 0b1000010001011110);
        store(92, (char) 0b0000101100011110);
        store(93, (char) 0b0001010000011110);
        store(94, (char) 0b0011110011010000);
        store(95, (char) 0b0000010010000010);
        store(96, (char) 0b0000100000011110);
        store(97, (char) 0b1000010001011110);
        store(98, (char) 0b0000010001011110);
        store(99, (char) 0b1100100000000010);
    }

    // load Test Program 2 into memory
    public void load2() {
        clearWithROM();
        printLog("Load Test Program 2");
        store(29, (char) 1000); // address of the code segment
        store(30, (char) 500); // address of the data segment
        store(1000, (char) 0b1000010001011100);
        store(1001, (char) 0b1000010010011101);
        store(1002, (char) 0b1000010011011110);
        store(1003, (char) 0b0000111100000000);
        store(1004, (char) 0b0000101101000000);
        store(1005, (char) 0b0000010100011110);
        store(1006, (char) 0b0001000101000000);
        store(1007, (char) 0b0000100101000001);
        store(1008, (char) 0b0000010001100001);
        store(1009, (char) 0b0000111000000100);
        store(1010, (char) 0b0100100010000000);
        store(1011, (char) 0b0010101110001111);
        store(1012, (char) 0b1100100000000001);
        store(1013, (char) 0b0001101100000001);
        store(1014, (char) 0b0010110010000100);
        store(1015, (char) 0b0000110000001010);
        store(1016, (char) 0b1100100000000001);
        store(1017, (char) 0b0000110000011111);
        store(1018, (char) 0b0110010011000001);
        store(1019, (char) 0b0001100000010101);
        store(1020, (char) 0b1100100000000001);
        store(1021, (char) 0b0001110000001110);
        store(1022, (char) 0b1100100000000001);
        store(1023, (char) 0b0001110000000100);
        store(1024, (char) 0b1100100000000001);
        store(1025, (char) 0b0001100000010001);
        store(1026, (char) 0b1100100000000001);
        store(1027, (char) 0b0001110000001111);
        store(1028, (char) 0b1100100000000001);
        store(1029, (char) 0b0001100000000101);
        store(1030, (char) 0b1100100000000001);
        store(1031, (char) 0b0001110000001110);
        store(1032, (char) 0b1100100000000001);
        store(1033, (char) 0b0000110000011111);
        store(1034, (char) 0b0001100000000001);
        store(1035, (char) 0b1100100000000001);
        store(1036, (char) 0b0000010000011101);
        store(1037, (char) 0b0001100000011111);
        store(1038, (char) 0b0001100000010000);
        store(1039, (char) 0b0000100000011101);
        store(1040, (char) 0b1000010010011101);
        store(1041, (char) 0b0000110011000000);
        store(1042, (char) 0b0001110000010100);
        store(1043, (char) 0b0000100001000000);
        store(1044, (char) 0b0000010101000000);
        store(1045, (char) 0b0001100100000000);
        store(1046, (char) 0b0000100101000001);
        store(1047, (char) 0b1100010000000000);
        store(1048, (char) 0b0000100001100001);
        store(1049, (char) 0b0000111000000100);
        store(1050, (char) 0b0100100010000000);
        store(1051, (char) 0b0010101110001000);
        store(1052, (char) 0b0001100100000001);
        store(1053, (char) 0b0000100101000001);
        store(1054, (char) 0b0010110010000000);
        store(1055, (char) 0b0000010001000001);
        store(1056, (char) 0b0001010001000000);
        store(1057, (char) 0b0001100000000000);
        store(1058, (char) 0b0000100001000001);
        store(1059, (char) 0b0000010000011101);
        store(1060, (char) 0b0001100000011111);
        store(1061, (char) 0b0001100000000100);
        store(1062, (char) 0b0000100001000111);
        store(1063, (char) 0b0001100000010010);
        store(1064, (char) 0b0000100001001000);
        store(1065, (char) 0b0001100000000110);
        store(1066, (char) 0b0000100001001001);
        store(1067, (char) 0b0001100000001000);
        store(1068, (char) 0b0000100001001010);
        store(1069, (char) 0b0001100000011110);
        store(1070, (char) 0b0000100001001011);
        store(1071, (char) 0b0001100000000100);
        store(1072, (char) 0b0000100001001100);
        store(1073, (char) 0b0001100000000110);
        store(1074, (char) 0b0000100000011101);
        store(1075, (char) 0b1000010010011101);
        store(1076, (char) 0b0000010001000000);
        store(1077, (char) 0b0000100001000010);
        store(1078, (char) 0b0000110000000001);
        store(1079, (char) 0b0000100001000100);
        store(1080, (char) 0b0000100001000101);
        store(1081, (char) 0b0000010001000001);
        store(1082, (char) 0b0000100001000011);
        store(1083, (char) 0b0000010111000000);
        store(1084, (char) 0b0000111000000100);
        store(1085, (char) 0b0100100110000000);
        store(1086, (char) 0b0010101110000000);
        store(1087, (char) 0b0001101000011100);
        store(1088, (char) 0b0100100110000000);
        store(1089, (char) 0b0010101101101011);
        store(1090, (char) 0b0001101000001110);
        store(1091, (char) 0b0100100110000000);
        store(1092, (char) 0b0010101101101100);
        store(1093, (char) 0b0000011001000001);
        store(1094, (char) 0b0001011001000011);
        store(1095, (char) 0b0001001001000010);
        store(1096, (char) 0b0000101001000110);
        store(1097, (char) 0b0000011001100110);
        store(1098, (char) 0b0100100110000000);
        store(1099, (char) 0b0010101101101001);
        store(1100, (char) 0b0000010001000001);
        store(1101, (char) 0b0000010100011110);
        store(1102, (char) 0b0001100100000001);
        store(1103, (char) 0b0000100100011110);
        store(1104, (char) 0b1000010011011110);
        store(1105, (char) 0b0010110001100111);
        store(1106, (char) 0b0000010001000011);
        store(1107, (char) 0b0000010100011110);
        store(1108, (char) 0b0001100100000001);
        store(1109, (char) 0b0000100100011110);
        store(1110, (char) 0b1000010011011110);
        store(1111, (char) 0b0011100001100111);
        store(1112, (char) 0b0000110000001010);
        store(1113, (char) 0b1100100000000001);
        store(1114, (char) 0b0000110000011111);
        store(1115, (char) 0b0001100000011111);
        store(1116, (char) 0b0001100000001000);
        store(1117, (char) 0b1100100000000001);
        store(1118, (char) 0b0001100000001001);
        store(1119, (char) 0b1100100000000001);
        store(1120, (char) 0b0001100000000110);
        store(1121, (char) 0b1100100000000001);
        store(1122, (char) 0b0001110000000111);
        store(1123, (char) 0b1100100000000001);
        store(1124, (char) 0b0001110000001010);
        store(1125, (char) 0b1100100000000001);
        store(1126, (char) 0b0000110100011111);
        store(1127, (char) 0b0001100100000001);
        store(1128, (char) 0b1100100100000001);
        store(1129, (char) 0b0001100000001111);
        store(1130, (char) 0b1100100000000001);
        store(1131, (char) 0b0000111000011111);
        store(1132, (char) 0b0001101000010001);
        store(1133, (char) 0b0001001001000101);
        store(1134, (char) 0b1100101000000001);
        store(1135, (char) 0b1100100100000001);
        store(1136, (char) 0b0001100000000100);
        store(1137, (char) 0b1100100000000001);
        store(1138, (char) 0b0001011001000101);
        store(1139, (char) 0b0001001001000100);
        store(1140, (char) 0b1100101000000001);
        store(1141, (char) 0b0000110000001010);
        store(1142, (char) 0b1100100000000001);
        store(1143, (char) 0b0000000000000000);
        store(1144, (char) 0b0000010001000100);
        store(1145, (char) 0b0001100000000001);
        store(1146, (char) 0b0000100001000100);
        store(1147, (char) 0b0010110001101000);
        store(1148, (char) 0b0000010001000101);
        store(1149, (char) 0b0001100000000001);
        store(1150, (char) 0b0000100001000101);
        store(1151, (char) 0b0000110000000000);
        store(1152, (char) 0b0000100001000100);
        store(1153, (char) 0b0010110001101000);
        store(1154, (char) 0b0000110000001010);
        store(1155, (char) 0b1100100000000001);
        store(1156, (char) 0b0001100000011111);
        store(1157, (char) 0b0001100000011000);
        store(1158, (char) 0b0001100000001101);
        store(1159, (char) 0b1100100000000001);
        store(1160, (char) 0b0001100000000001);
        store(1161, (char) 0b1100100000000001);
        store(1162, (char) 0b0001100000000101);
        store(1163, (char) 0b1100100000000001);
        store(1164, (char) 0b0000110100011111);
        store(1165, (char) 0b0001100100000001);
        store(1166, (char) 0b1100100100000001);
        store(1167, (char) 0b0001110000001110);
        store(1168, (char) 0b1100100000000001);
        store(1169, (char) 0b0001100000001001);
        store(1170, (char) 0b1100100000000001);
        store(1171, (char) 0b0001100000000110);
        store(1172, (char) 0b1100100000000001);
        store(1173, (char) 0b0001110000000111);
        store(1174, (char) 0b1100100000000001);
        store(1175, (char) 0b0001110000001010);
        store(1176, (char) 0b1100100000000001);
        store(1177, (char) 0b0000110000001010);
        store(1178, (char) 0b1100100000000001);
        store(1179, (char) 0b0000000000000000);

        //paragraph data
        store(500, (char) 84);
        store(501, (char) 104);
        store(502, (char) 105);
        store(503, (char) 115);
        store(504, (char) 32);
        store(505, (char) 105);
        store(506, (char) 115);
        store(507, (char) 32);
        store(508, (char) 97);
        store(509, (char) 32);
        store(510, (char) 116);
        store(511, (char) 101);
        store(512, (char) 115);
        store(513, (char) 116);
        store(514, (char) 46);
        store(515, (char) 32);
        store(516, (char) 73);
        store(517, (char) 116);
        store(518, (char) 32);
        store(519, (char) 105);
        store(520, (char) 115);
        store(521, (char) 32);
        store(522, (char) 97);
        store(523, (char) 32);
        store(524, (char) 115);
        store(525, (char) 97);
        store(526, (char) 109);
        store(527, (char) 112);
        store(528, (char) 108);
        store(529, (char) 101);
        store(530, (char) 32);
        store(531, (char) 116);
        store(532, (char) 101);
        store(533, (char) 120);
        store(534, (char) 116);
        store(535, (char) 46);
        store(536, (char) 32);
        store(537, (char) 84);
        store(538, (char) 104);
        store(539, (char) 101);
        store(540, (char) 32);
        store(541, (char) 116);
        store(542, (char) 101);
        store(543, (char) 120);
        store(544, (char) 116);
        store(545, (char) 32);
        store(546, (char) 104);
        store(547, (char) 97);
        store(548, (char) 115);
        store(549, (char) 32);
        store(550, (char) 115);
        store(551, (char) 105);
        store(552, (char) 120);
        store(553, (char) 32);
        store(554, (char) 115);
        store(555, (char) 101);
        store(556, (char) 110);
        store(557, (char) 116);
        store(558, (char) 101);
        store(559, (char) 110);
        store(560, (char) 99);
        store(561, (char) 101);
        store(562, (char) 46);
        store(563, (char) 32);
        store(564, (char) 65);
        store(565, (char) 108);
        store(566, (char) 108);
        store(567, (char) 32);
        store(568, (char) 115);
        store(569, (char) 101);
        store(570, (char) 110);
        store(571, (char) 116);
        store(572, (char) 101);
        store(573, (char) 110);
        store(574, (char) 99);
        store(575, (char) 101);
        store(576, (char) 115);
        store(577, (char) 32);
        store(578, (char) 97);
        store(579, (char) 114);
        store(580, (char) 101);
        store(581, (char) 32);
        store(582, (char) 115);
        store(583, (char) 105);
        store(584, (char) 109);
        store(585, (char) 112);
        store(586, (char) 108);
        store(587, (char) 101);
        store(588, (char) 46);
        store(589, (char) 32);
        store(590, (char) 84);
        store(591, (char) 104);
        store(592, (char) 101);
        store(593, (char) 32);
        store(594, (char) 116);
        store(595, (char) 101);
        store(596, (char) 120);
        store(597, (char) 116);
        store(598, (char) 32);
        store(599, (char) 105);
        store(600, (char) 115);
        store(601, (char) 32);
        store(602, (char) 102);
        store(603, (char) 111);
        store(604, (char) 114);
        store(605, (char) 32);
        store(606, (char) 111);
        store(607, (char) 117);
        store(608, (char) 114);
        store(609, (char) 32);
        store(610, (char) 112);
        store(611, (char) 114);
        store(612, (char) 111);
        store(613, (char) 106);
        store(614, (char) 101);
        store(615, (char) 99);
        store(616, (char) 116);
        store(617, (char) 46);
        store(618, (char) 32);
        store(619, (char) 84);
        store(620, (char) 104);
        store(621, (char) 105);
        store(622, (char) 115);
        store(623, (char) 32);
        store(624, (char) 105);
        store(625, (char) 115);
        store(626, (char) 32);
        store(627, (char) 116);
        store(628, (char) 104);
        store(629, (char) 101);
        store(630, (char) 32);
        store(631, (char) 102);
        store(632, (char) 110);
        store(633, (char) 105);
        store(634, (char) 97);
        store(635, (char) 108);
        store(636, (char) 32);
        store(637, (char) 115);
        store(638, (char) 101);
        store(639, (char) 110);
        store(640, (char) 116);
        store(641, (char) 101);
        store(642, (char) 110);
        store(643, (char) 99);
        store(644, (char) 101);
        store(645, (char) 4);
    }
}