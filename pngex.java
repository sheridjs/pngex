/* Author: Jay Sheridan
 * Application to extract png image files embedded in other files
 */

// jar cvfm pngex.jar manifest.txt pngex.class pngex$1.class

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.LinkedList;
import java.lang.Integer;
import java.lang.String;

public class pngex implements ActionListener
{
    static final byte[] pngSignature = new byte[] 
	{(byte)137, (byte)80, (byte)78, (byte)71, 
	 (byte)13, (byte)10, (byte)26, (byte)10};
    JFrame frm_MainWindow;
    JPanel pnl_Main;
    JScrollPane spn_Output;
    JTextArea txa_Output;
    JButton btn_Load, btn_Extract;
    GridBagConstraints gbc_Main;
    File inFile, outFile;
    String currentDir;
    LinkedList pngsWritten;
    byte[] currentEight = new byte[8];
    boolean validPNG = true;
    int pngCount = 0;

    public static void main(String[] args)
    {
	new pngex();
    }

    public pngex()
    {
	frm_MainWindow = new JFrame("PNG Extractor");
	frm_MainWindow.setDefaultCloseOperation(2); //dispose on exit
	frm_MainWindow.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    System.exit(0);
		}
	    });

	makeObjects();
	doLayout();

	frm_MainWindow.getContentPane().add(pnl_Main);
	frm_MainWindow.pack();
	frm_MainWindow.show();
    }

    public void makeObjects()
	// setup of GUI objects
    {
	pnl_Main = new JPanel(new GridBagLayout());
	gbc_Main = new GridBagConstraints();

	btn_Load = new JButton("Load");
	btn_Load.addActionListener(this);
	btn_Extract = new JButton("Extract");
	btn_Extract.addActionListener(this);

	spn_Output = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	txa_Output = new JTextArea(6,40);
	txa_Output.setLineWrap(true);
	txa_Output.setWrapStyleWord(true);

	currentDir = null;
	inFile = null;
	outFile = null;
	Arrays.fill(currentEight,(byte)0);
    }

    public void doLayout()
	// layout of GUI objects
    {
	gbc_Main.ipadx = 1;
	gbc_Main.ipady = 1;
	gbc_Main.anchor = GridBagConstraints.CENTER;
	gbc_Main.gridx = 1;
	gbc_Main.gridy = 1;
	gbc_Main.gridwidth = 1;
	gbc_Main.gridheight = 1;
	pnl_Main.add(btn_Load, gbc_Main);

	gbc_Main.gridx = 2;
	pnl_Main.add(btn_Extract, gbc_Main);

	gbc_Main.gridx = 1;
	gbc_Main.gridy = 2;
	gbc_Main.gridwidth = 2;
	spn_Output.setViewportView(txa_Output);
	pnl_Main.add(spn_Output, gbc_Main);
    }

    public void actionPerformed(ActionEvent e)
    {
	Object evtObj;
	evtObj = e.getSource();

	if (evtObj.equals(btn_Load)) {
	    onLoadBtn();
	}
	else if (evtObj.equals(btn_Extract)) {
	    onExtractBtn();
	}
    }

    public void onLoadBtn()
    {
	JFileChooser jfc_OpenFile;
	int jfcValue;

	//txa_Output.setText("Load Button pressed.\n"); //+
	if (currentDir == null) {
	    jfc_OpenFile = new JFileChooser();
	}
	else {
	    jfc_OpenFile = new JFileChooser(currentDir);
	}
	jfcValue = jfc_OpenFile.showOpenDialog(frm_MainWindow);

	if (jfcValue == JFileChooser.APPROVE_OPTION) {
	    inFile = jfc_OpenFile.getSelectedFile();
	    currentDir = inFile.getParent();
	    //txa_Output.append(currentDir + "\n"); //+
	    txa_Output.append("File loaded: \n" + inFile.getPath() + "\n"
			      + "Note: To extract PNG file(s), press the Extract button. Extraction may take some time.\n\n" );
	}
    }

    public void onExtractBtn()
    {
	DataInputStream inFileDS;
	DataOutputStream outBytesDS;
	ByteArrayOutputStream pngByteStream;
	byte[] testByteArray = new byte[8];
	int byteCount=0, addlCount=0;
	boolean fileEnd = false;

	pngCount = 0;
	pngsWritten = new LinkedList();
	//txa_Output.append("Extract Button pressed.\n"); //+
	//if (inFile != null) 
	try {
	    inFileDS = new DataInputStream(new FileInputStream(inFile));
	    fileEnd = false;

	    while ( !fileEnd ) {
		try { // look for png signatures until end of file
		    testByteArray[7]=inFileDS.readByte();
		    byteCount++;
		    if (Arrays.equals(testByteArray, pngSignature)) {
			txa_Output.append("** PNG found at byte " + 
					  (byteCount-7) + "\n");
			// setup byte array stream
			pngByteStream = new ByteArrayOutputStream();
			outBytesDS = new DataOutputStream(pngByteStream);
			outBytesDS.write(pngSignature);
			// read data from PNG file
			addlCount = readPNG(inFileDS, outBytesDS);
			if (addlCount < 0) {
			    txa_Output.append("End of file reached in middle of PNG file");
			    return;
			}
			byteCount = byteCount + addlCount;
			if (validPNG) {
			    // write PNG file(?)
			    pngCount++;
			    writePNG(pngByteStream.toByteArray());
			    // ** Next line makes it find only the first png **
			    //fileEnd = true;
			}
			
		    }
		    System.arraycopy(testByteArray, 1, testByteArray, 0, 7);
		}
		catch (IOException e) {
		    //txa_Output.append("Error in read loop: \n\t" + e.getMessage() + "\n");
		    fileEnd = true;
		}
	    }
	    //txa_Output.append("Total byte count: " + byteCount + "\n"); //+
	    inFileDS.close();
	    if (pngCount > 0) {
		txa_Output.append("File(s) written:\n");
		for (int i=0; i<pngCount; i++) {
		    txa_Output.append((String)pngsWritten.get(i) + "\n");
		}
	    }
	}
	catch (Exception e) {
	    txa_Output.append("Error reading file: \n\t" +
			      e.getMessage() + "\n");
	}

    }

    public int readPNG(DataInputStream inDS, DataOutputStream outDS) {
	// inDS would read from a file typically while outDS
	// would write to a byte array for later use.
	boolean endChunkFound = false;
	int byteCount = 0, addlCount = 0;
	byte[] chunkType, chunkIHDR, chunkIEND;

	chunkType = new byte[4];
	chunkIHDR = new byte[] {(byte)'I',(byte)'H',(byte)'D',(byte)'R'};
	chunkIEND = new byte[] {(byte)'I',(byte)'E',(byte)'N',(byte)'D'};
	Arrays.fill(chunkType,(byte)0);

	addlCount = readChunk(inDS, outDS, chunkType);
	if (addlCount < 0)
	    return addlCount;
	byteCount = byteCount + addlCount;
	if (Arrays.equals(chunkType, chunkIHDR)) {
	    while (!endChunkFound) {
		addlCount = readChunk(inDS, outDS, chunkType);
		if (addlCount < 0)
		    return addlCount;
		byteCount = byteCount + addlCount;
		if (Arrays.equals(chunkType, chunkIEND))
		    endChunkFound = true;
	    }
	}
	else {
	    txa_Output.append("Invalid PNG, no IHDR chunk.\n");
	    validPNG = false;
	}

	return byteCount;
    }

    public int readChunk(DataInputStream inDS, DataOutputStream outDS,
			 byte[] chunkType) {
	int byteCount = 0, chunkLength = 0, chunkCRC = 0;
	byte[] chunkData;

	try {
	    // Length
	    chunkLength = inDS.readInt();
	    outDS.writeInt(chunkLength);
	    byteCount = byteCount+4;

	    // Type
	    for (int i=0; i<4; i++) {
		chunkType[i] = inDS.readByte();
		txa_Output.append("" + (char)chunkType[i]);
		byteCount++;
	    }
	    outDS.write(chunkType);
	    txa_Output.append(" chunk found\nLength: " + chunkLength);

	    // Data
	    chunkData = new byte[chunkLength];
	    //inDS.skipBytes(chunkLength);
	    inDS.read(chunkData);
	    outDS.write(chunkData);

	    // CRC
	    chunkCRC = inDS.readInt();
	    outDS.writeInt(chunkCRC);
	    byteCount = byteCount + 4 + chunkLength;
	    txa_Output.append("\nCRC: " + chunkCRC + "\n\n");
	}
	catch (IOException e) {
	    byteCount = -1;
	}
	return byteCount;
    }

    public void writePNG(byte[] pngData) {
	BufferedOutputStream outFileStrm;
	StringTokenizer fileNameParser;
	String outFileName = "";
	String[] tempStrArray;
	int tokenCount, i=0;

	// make file name
	fileNameParser = new StringTokenizer(inFile.getName(), ".");
	tokenCount = fileNameParser.countTokens();
	
	if (tokenCount == 1) {
	    outFileName = inFile.getName();
	}
	else {
	    for (i=1; i < tokenCount; i++) {
		outFileName = outFileName + fileNameParser.nextToken();
		if (i < tokenCount - 1)
		    outFileName = outFileName + ".";
	    }
	}
	if (pngCount < 10)
	    outFileName = outFileName + "00" + Integer.toString(pngCount);
	else if (pngCount < 100)
	    outFileName = outFileName + "0" + Integer.toString(pngCount);
	else
	    outFileName = outFileName + Integer.toString(pngCount);
	outFileName = outFileName + ".png";

	// write file
	outFile = new File(inFile.getParentFile(), outFileName);
	//txa_Output.append("Filename to write: " + outFile.getPath() + "\n");
	try {
	    outFileStrm = 
		new BufferedOutputStream(new FileOutputStream(outFile));
	    outFileStrm.write(pngData);
	    // save filename to array
	    /*
	    tempStrArray = new String[pngCount];
	    System.arraycopy(pngsWritten, 0, tempStrArray, 0, pngCount-1);
	    tempStrArray[pngCount-1] = outFile.getPath();
	    pngsWritten = new String[pngCount];
	    System.arraycopy(tempStrArray, 0, pngsWritten, 0, pngCount); 
	    */
	    pngsWritten.add(outFile.getPath());
	    outFileStrm.close();
	}
	catch (Exception e) {
	    txa_Output.append("Error writing file: \n" + e.toString()
			      + "\n");
	}
    }

} // End program

