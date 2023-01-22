/***********************************************************************
 * ECMApp - Emulation of ECM Mark II (aka SIGABA)
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 *
 * This program emulates an ECM Mark II (aka SIGABA).  It emulates the CSP-889-2900 version
 * that is now on USS Pampanito SS-383 in San Francisco.  The emulation is based on the
 * artifact (the machine) not engineering drawings or other documentation.  This leaves
 * the possibility that some of the behavior is a bug in this particular machine rather
 * than the generic design.  All the documents I have are consistent with the machine's 
 * functioning, but the documents are not a complete machine definition. 
 *
 * A description of the machine on Pampanito may be found at:
 * https://maritime.org/tech/ecm2.htm
 * I would suggest reading the web page before the code.
 *
 * This program was written using Metrowerks Codewarrier version 1.6, the source
 * assumes 4 character tabs and fixed pitch fonts for printing. I have avoided too
 * many objects to improve loading over the net. Only standard AWT components where
 * used to keep the code size down and improve portability.
 *
 * This code has been written by a programmer that has not written
 * a line of code in 14 years after reading a Java book. Your comments
 * on style, legibility and Java culture would be appreciated in making
 * this code more comprehensible. Boy would this have been easier in Fortran.
 *
 * This is a first version. The user interface may improve in the future.  Anyone
 * that is up to speed on AWT and custom components(controls) is invited to help.
 * Please contact us at https://maritime.org/mail.htm.
 *
 * Algorithmic details I need to check in the doc and/or the machine:
 *  Should the cipher and control sample rotors be wired by the interval method?
 *
 * Things that should be done:
 *  Implement RPT functionality.  Note that repeat key must be pressed first, and it works
 *      on all keys.  Probably do this with the physical keyboard.  Unfortunately this will
 *      require figuring out why the up key events are not working as the documentation
 *      describes.  Then use events.
 *  Implement a counter display update method that preserves 4 chars, i.e. add the leading
 *      zeros to the string before updating the display.
 *  Figure out how to determine if we are rotor 0 or 4 in rotCW or rotCCW and add clear 
 *      cipherCount there instead of where it is called.
 *  The use of ECMApp instance variables as globals might be reduced. 
 *  Add Java style headers on the classes.
 *  See if it is possible to change displayed fonts in the provided components
 *      to a mono-space font.
 *  Add checking for uniqueness of each rotor with a warning when initializing rotor order.
 *  Add a warning when attempting to operate with zeroize or machine switch in middle.
 *  Check for changes in rotor positions on keystrokes instead of waiting for the return.
 *  Add spaces between rotors in the bank displays.  Add the tens digits in
 *      the index rotors.  Maybe flip the text on reversed rotors.
 *  Make another attempt at a better component layout.  Check out what happens with
 *      a wide machine.
 *  Find out what brace block convention is most used by Java type, K&R (as is), or at the 
 *      same paragraphing.
 *
 *  Future - Add webpage settable starting position of the index, cipher, and control rotors.
 *  Future - Add sounds.
 *  Future - Create custom controls that look like the real machine controls.
 *      Masterswitch should enforce order of its operation.
 *      Add second CSP-889 / 2900 switch check for matching.
 *  Future - Add graphics.
 *  Future - Allow keyboard data entry in addition to the on screen keyboard.  Maybe sooner
 *      as a way to deal with repeat key.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 30 Aug 96    1.00a1  RSP First alpha.
 * 8 Oct 96     1.00    RSP First release.
 * 12 Feb 98    1.01    RSP Fixed control rotor update problem so that slow control rotor
 *                          now only moves when the medium rotor moves.
 * 3 Sep 98     1.02    RSP Fixed cipher bank path was moving in the wrong direction, i.e.
 *                          from right to left during encrypt instead of the correct
 *                          left to right.  Decrypt was backwards as well.
 * 29 Jun 99    1.02    RSP Relinked into a .zip file, but no other change.
 *
 **********************************************************************/

import java.awt.*;
import java.applet.Applet;

public class ECMApp extends Applet {
    static final boolean DECRYPT = true, ENCRYPT = false;
    static final int CSP889 = 0, CSP2900 = 1, CSPNONE = 2;
    
    // KEYS[][] is the virtual keyboard. 
    static final String KEYS[][] = {
        {"1","2","3","4","5","6","7","8","9","0"},
        {"Q","W","E","R","T","Y","U","I","O","P"},
        {"A","S","D","F","G","H","J","K","L","RPT"},
        {" ","Z","X","C","V","B","N","M","-","Blank"},
        {"Space Bar"}
        };
    
    Choice zeroSwitch;      // zeroize switch
    String zs;              // position of zeroize switch
    
    Choice machSwitch;      // type of machine mode switch
    int machType = CSP889;  // position of machine switch
    
    Choice masterSwitch;    // master switch
    String ms;              // position of the master switch
    String oldms = "O";     // the last position of the master switch.
    
    TextField countDisp;    // counter display
    Button countBut;        // clear the counter button
    
    TextArea paperTape;     // paper tape output
    Button tearTape;        // clear paper tape button
    int count = 0;          // key counter
    int encPaperCount = 0;  // encrypt printing counter, used to create 5 character groups
    
    TextField cipherDisp;   // display of cipher rotor positions
    TextField controlDisp;  // display of control rotor positions
    TextField indexDisp ;   // display of index rotor positions
    
    RotorCage cage;         // the rotor cage contains the rotor banks and in
                            // this program, the wiring between rotors.  On the
                            // real machine the wiring is not in the rotor cage.
                            // This object encapsulates most of the algorithm.


/***********************************************************************
 * init() - Initializes ECMApp
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * init() runs each time the web page that contains it loads ECMApp, it then:
 * 
 *  1- Sets the order of the rotors
 *  2- Creates the RotorCage
 *  3- Initializes the positions of the rotors
 *  4- Creates and lays out the controls
 *  5- Displays the controls.
 *
 *  The controls cause events that are handled in action().
 *  The RotorCage object has the guts of the ECM algorithm.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public void init() {
        int i, j;
        
        resize(450,250);
        
        // Get rotor order parameters. In a real machine each rotor can be used only once,
        // this version allows using the same rotor number.
        // There are five rotors in each bank. The string contains an index to the rotor
        // wiring table followed by either R (reverse) or N (normal) for each of the rotors
        // that make up a bank.  If the parameters are not provided, or are not the correct
        // length, defaults are provided.  If an index is not in range, the RotorCage 
        // constructor will substitute the first rotor wiring.
        
        String cipherOrder = getParameter("CipherOrder");
        if ((cipherOrder == null) || (cipherOrder.length() != 10)) {
            cipherOrder = "0N1N2N3N4N";
            }
        String controlOrder = getParameter("ControlOrder");
        if ((controlOrder == null) || (controlOrder.length() != 10)) {
            controlOrder = "5N6N7N8N9N";
            }
        String indexOrder = getParameter("IndexOrder");
        if ((indexOrder == null) || (indexOrder.length() != 10)) {
            indexOrder = "0N1N2N3N4N";
            }

        // The parameters select the order of the rotors and their orientation (normal or
        // reversed. Near the end of the war, this was done once each day.
        //                  Cipher      Control     Index
    cage = new RotorCage(cipherOrder, controlOrder, indexOrder);
    
    // Zeroize all the control and cipher rotors. Zeroizing a rotor is 
    // setting its position to letter 'O'.
    
    cage.zeroize(); 

        // By the end of the war the Index rotor order is always set 10,20,30,40,50.
        // The starting positions of index rotors comes from the daily key list and 
        // different positions are used for each classification of message.
        
        cage.setIndexBankPos("00000");

        /* The rest of this method just lays out and displays the components i.e. switches in
        * the user interface. */

        // Layout for the default panel that will contain the "direction" layouts:
        
        GridBagLayout gblAll = new GridBagLayout(); 
        
        Panel pNE = new Panel();        //  NorthEast Panel- Zeroize and Machine Type Controls
        GridBagLayout gblNE = new GridBagLayout();
        Panel pN = new Panel();         //  North- Three Rotor bank displays
        GridBagLayout gblN = new GridBagLayout();
        Panel pNW = new Panel();        //  NorthWest- Master Switch and Counter w/ clear button.
        GridBagLayout gblNW = new GridBagLayout();
        Panel pC = new Panel();         //  Center- PaperTape with its tear off button 
        GridBagLayout gblC = new GridBagLayout();
        Panel pS = new Panel();         //  South- Keyboard
        GridBagLayout gblS = new GridBagLayout();
        
        GridBagConstraints gbc = new GridBagConstraints(); // We will keep reusing this.
        gbc.weightx = 0.0;              // Spread extra space around the controls
        gbc.weighty = 0.0;
        
        // Define the NorthEast Panel Components
        
        zeroSwitch = new Choice();
        zeroSwitch.addItem("Zeroize");  // Zeroize
        zeroSwitch.addItem(" ");        // In the middle position do nothing.
        zeroSwitch.addItem("Operate");  // Operate
        zs = new String("Zeroize");     // Starting position must match the display

        machSwitch = new Choice();
        machSwitch.addItem("CSP 889");  // Normal CSP 889 mode of operation.
        machSwitch.addItem(" ");        // In the middle position do nothing
        machSwitch.addItem("CSP 2900"); // CSP 2900 mode
    
        // Add the NorthEast panel layout to the overall layout
        
        gblNE = new GridBagLayout();    // Create the layout for NorthEast panel
        pNE.setLayout(gblNE);           // Set the layout in the panel
        gbc.gridwidth = GridBagConstraints.REMAINDER; // The last item on the line.
        gblNE.setConstraints(zeroSwitch, gbc);
        pNE.add(zeroSwitch);            // Display the Zeroize Switch
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gblNE.setConstraints(machSwitch, gbc);
        pNE.add(machSwitch);            // Display the Machine Switch
        
        gbc.gridwidth = 1;              // This is one item wide, and not the last
        gblAll.setConstraints(pNE, gbc);
        add(pNE);                       // Add the NorthEast panel to the overall panel.
                
        // Define the North Panel Components
        
        cipherDisp = new TextField(5);  // create the cipher rotor bank display
        controlDisp = new TextField(5); // create the control rotor bank display
        indexDisp = new TextField(5);   // create the index rotor bank display

        // Add the North panel layout to the overall layout
        
        gblN = new GridBagLayout();     // Create the layout for North panel
        pN.setLayout(gblN);             // Set the layout in the panel
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gblN.setConstraints(cipherDisp, gbc);
        pN.add(cipherDisp);             // Display the Cipher Rotor Bank
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gblN.setConstraints(controlDisp, gbc);
        pN.add(controlDisp);            // Display the Control Rotor Bank
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gblN.setConstraints(indexDisp, gbc);
        pN.add(indexDisp);              // Display the Index Rotor Bank
        
        gbc.gridwidth = 1;              // This is one item wide, and not the last
        gblAll.setConstraints(pN, gbc);
        add(pN);                        // Display the North panel
        
        // Define the NorthWest Panel Components
        
        masterSwitch = new Choice();
        masterSwitch.addItem("O");      // Off
        masterSwitch.addItem("P");      // Plaintext
        masterSwitch.addItem("R");      // Reset
        masterSwitch.addItem("E");      // Encrypt
        masterSwitch.addItem("D");      // Decrypt
        ms = new String("O");           // Machine starts off.
        
        countDisp = new TextField(4);   // create the counter display
        countDisp.setEditable(false);
        countDisp.setText("0000");
        countBut = new Button("Clear Counter"); // create the clear button
        
        // Add the NorthWest panel layout to the overall layout
        
        gblNW = new GridBagLayout();    // Create the layout for NorthWest panel
        pNW.setLayout(gblNW);           // Set the layout in the panel
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gblNW.setConstraints(masterSwitch, gbc);
        pNW.add(masterSwitch);          // Display the master swtich
        gbc.gridwidth = 1;              // Counter is on the same line as clear button
        gblNW.setConstraints(countDisp, gbc);
        pNW.add(countDisp);             // Display the counter
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gblNW.setConstraints(countBut, gbc);
        pNW.add(countBut);              // Display the counter clear button
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;   // Last panel on top (north)
        gblAll.setConstraints(pNW, gbc);
        add(pNW);                       // Display the NorthWest panel

        // Define the Center Panel Components
        
        tearTape = new Button("Tear off tape"); // create the clear paper tape button
        paperTape = new TextArea(1,25);         // create the paper tape display
        paperTape.setEditable(false);

        // Add the Center panel layout to the overall layout
        
        gblC = new GridBagLayout();     // Create the layout for Center panel
        pC.setLayout(gblC);             // Set the layout in the panel
        gbc.gridwidth = 1;              // Tear tape is on the same line as paper tape
        gblC.setConstraints(tearTape, gbc);
        pC.add(tearTape);                   // Display the tear tape Button
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gblC.setConstraints(paperTape, gbc);
        pC.add(paperTape);                  // Display the Paper Tape
        
        gbc.gridwidth = GridBagConstraints.REMAINDER; // One panel wide in the center
        gblAll.setConstraints(pC, gbc);
        add(pC);                        // Display the Center panel

        // Create and display the keyboard in the South panel.
        // Note that the event handler relies on each key label being different.
        
        gblS = new GridBagLayout(); // Create the layout for the South panel
        pS.setLayout(gblS);         // Set the layout in the panel
        for (i = 0; i < KEYS.length; i++) {     
            gbc.gridwidth = KEYS[i].length; // The width is the number of columns.
            for (j = 0; j < (KEYS[i].length - 1); j++) {
                Button button = new Button(KEYS[i][j]);
                gblS.setConstraints(button, gbc);
                pS.add(button);
                }
            gbc.gridwidth = GridBagConstraints.REMAINDER;   // next key is end of line
            Button button = new Button(KEYS[i][j]);
            gblS.setConstraints(button, gbc);
            pS.add(button);
            }

        gbc.gridwidth = GridBagConstraints.REMAINDER;;  // One keyboard in the center
        gblAll.setConstraints(pS, gbc);
        add(pS);                        // Display the South (keyboard) panel
                    
        validate(); // make sure all the controls that were added are displayed.
        
        // Make sure the rotor position displays are updated after initialization.
        
        cipherDisp.setText(cage.cipherBankPosToString());   
        controlDisp.setText(cage.controlBankPosToString());
        indexDisp.setText(cage.indexBankPosToString());

        }   // end of init  


/* keyDown - just a test for now.
 */
//d4    public boolean keyDown(Event e, int key) {
//d4        System.out.println("key down event="+e);
//d4        return false;
//d4        }

/* keyUp - just a test for now. I do not know why this does not ever get called.
 */
//d4    public boolean keyUp(Event e, int key) {
//d4        System.out.println("key up event="+e);
//d4        return false;
//d4        }

    
/***********************************************************************
/* action() handles component events. 
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * action() is called for any component event.  Since all activity of the machine is driven
 * by the switches this method emulates all of the wiring that is not in RotorCage.  Most of
 * the detail here is less interesting than looking at ECMCycle and the RotorCage.
 *
 * The switch action table:
 *
 *          P                       R                       E
 *  Zeroize Blank key only          Blank key only          Blank key only
 *          Counter updates         Counter updates         Counter updates
 *          No Rotor Moves          Zeroize rotors          Zeroize rotors
 *          Prints Blank            No printing             Prints Blanks
 *
 *  Operate All printable keys      1-5 and Blank keys      26 alpha keys
 *          Counter updates         Counter updates         Counter updates
 *          No Rotor Moves          1-5 move cipher&control Control & cipher move
 *          Blank is a space?       Blank moves cipher only X, Z, space & Blank handling
 *          All printable keys      No printing             Printing
 *                                                          
 *  When switching from E to R, or P to R, one to four spaces are output.
 *  Cipher text is printed in 5 character groups separated by spaces.
 *  
 *  Decipher is same as Encipher except Z, X and Space Bar and direction.
 *  During encipher, Z is converted to X, and Space Bar is converted to Z, and
 *          rotors are read from left to right.
 *      During decipher, Z is converted to space, Space Bar is ignored, and
 *          rotors are read from right to left.
 *  During both Decipher and Encipher, Blank moves rotors, prints a space.
 *
 *  If 889/2900 is in the middle, no keys work.
 *  If 889/2900 knob and switch do not match, no keys work
 *  If Zeroize/Operate switch is in the middle, no keys work
 *
 *  cipherCount implements a counter that increments on each encipher or deciphered 
 *      character.  It is cleared whenever the 1st or 5th rotor turns or the master switch
 *      is not E or D.  If it reaches 21, the machine is locked up until the master switch is
 *      rotated out of E. This was added postwar, probably to detect improperly positioned
 *      index rotors.  If the index rotors are not positioned properly there will be no cipher
 *      rotor movement.  In the program this is possible in a practicle sense only if straight
 *      through test control rotors are used.
 *
 *  Also see the curatorial report, SIGKKK and the Army history of converter.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public boolean action(Event e, Object arg) {
        int i, j;
                
        // Handle TextFields
        
        if (e.target instanceof TextField) {
            TextField tf = (TextField) e.target;
            String saNew = (String) e.arg;
            
            // Cipher and Control rotor displays.                       
            if ((tf == cipherDisp) || (tf == controlDisp)) {
            
                // Make sure the string is 5 characters long, i.e. 5 rotors. Fill
                // with letter "O" if too short, truncate if too long.              
                for (; saNew.length() < 5; saNew = saNew + "O") {
                    }
                if (saNew.length() > 5) {
                    saNew = saNew.substring(0, 5);
                    }
                    
                // check to makes sure each of the positions is alpha.              
                saNew = saNew.toUpperCase();
                for (i = 0; i < 5; i++) {
                    if (('A' > saNew.charAt(i)) ||
                    ('Z' < saNew.charAt(i)) ) {
                    
                        // Replace non-alpha positions with letter "O".
                        saNew = saNew.replace(saNew.charAt(i), 'O');  
                        }
                    }
                if (tf == cipherDisp) {
                    cage.setCipherBankPos(saNew);   // Update the cipher rotor positions.
                    cipherDisp.setText(saNew);      // Update the display
                    }
                else {  // tf == controlDisp
                    cage.setControlBankPos(saNew);  // Update the control rotor positions.
                    controlDisp.setText(saNew);     // Update the display
                    }
                return true;
                }   // end of Cipher and Control Display handling

            // Index rotor display.         
            if (tf == indexDisp) {
            
                // Make sure the string is 5 characters long, i.e. 5 rotors. Fill
                // with zero if too short, truncate if too long.
                for (; saNew.length() < 5; saNew = saNew + "0") {
                    }
                if (saNew.length() > 5) {
                    saNew = saNew.substring(0, 5);
                    }
                    
                // check to makes sure each of the positions is between 0 and 9.
                for (i = 0; i < 5; i++) {
                    if ("0123456789".indexOf(saNew.charAt(i)) < 0) {    // Not 0-9?
                        // Replace non-numeric positions with zero.
                        saNew = saNew.replace(saNew.charAt(i), '0');  
                        }
                    }
                cage.setIndexBankPos(saNew);    // Update the index rotor positions.
                indexDisp.setText(saNew);       // Update the display
                return true;
                }   // end of indexDisp handling
                
            return false;   // we did not handle this TextField
            }   // End of text field handler
                    

        // Handle choices, i.e. the switches.
        if (e.target instanceof Choice) {
            Choice ch = (Choice) e.target;
            
            // Handle the masterSwitch
            if (ch == masterSwitch) {
                ms = ch.getSelectedItem();
                
                // Clear the cipher movement counter if masterSwitch is not E or D.
                if ("OPR".indexOf(ms) >= 0) {
                    cage.cipherCount = 0;   // This is used to detect lack of cipher movement.
                    }

                if (ms.compareTo("O") == 0) {   // Turn the machine off.                    
                    oldms = "O";    // oldms is used to detect switch transitions used below
                    encPaperCount = 0;  // This is used to put cipher text in 5 char groups.
                    return true;
                    }
                    
                // Zero the encrypt paper count when not in encrypt.
                // When you change from P to R, or E to R, between 1 and 4 
                // spaces are output on the real machine while resetting the cam
                // that spaces 5 character groups during encrypt. To simulate this
                // behavior this program looks at count, this is not quite the same as
                // keeping track of the real state of the cam, but close enough. This
                // spacing is an artifact, not an essential element of the machine.
                if ( (ms.compareTo("R") == 0) && 
                    ( (oldms.compareTo("P") == 0) || (oldms.compareTo("E") == 0) ) ) {
                    encPaperCount = count % 5;
                    while ((encPaperCount++ % 5) != 0) {
                        paperTape.appendText(" ");
                        }
                    encPaperCount = 0;
                    oldms = ms;
                    return true;
                    }
                                        
                oldms = ms; // oldms is used to detect switch transitions above.
                }  // end of masterSwitch
                
            // Handle Machine Type switch. Some future version of the program
            // might have both the knob and the switch.  When this happens, both
            // knob and switch must agree for anything to happen.
            if (ch == machSwitch) {
                String s = ch.getSelectedItem();
                if (s.compareTo("CSP 889") == 0) {
                    machType = CSP889;
                    }
                else if (s.compareTo("CSP 2900") == 0) {
                    machType = CSP2900;
                    }
                else {
                    machType = CSPNONE;
                    }
                return true;
                }   // end of machSwitch
                
            // Zeroize switch.  
            if (ch == zeroSwitch) {
                zs = ch.getSelectedItem();
                return true;
                }   // end of zeroSwitch
                
            }   // end of choice handling block
        
        // Handle buttons
        if (e.target instanceof Button) {
            Button b = (Button) e.target;
            String bs = b.getLabel();
            
            /* First Handle all the Non-Keyboard Buttons that exist: */
            
            // Clear counter button - this is mechanical and works even if the machine is off.
            if (b == countBut) {
                count = 0;
                countDisp.setText(String.valueOf(count));
                return true;
                }
            
            // Clear paper tape button - this does not exist as a button on the machine
            // but is added as a convenience to the computer program's operators.
            if (b == tearTape) {
                paperTape.setText(" ");
                return true;
                }

            // If the unlabeled key on the left is pressed, do nothing.  This key does not
            // move on the real machine.
            if (bs.compareTo(" ") == 0) {
                return true;
                }
                
            // If the master switch is off OR the machine switch is in the middle,
            // or zeroize switch is in the middle, do nothing by returning.
            if ((ms.compareTo("O") == 0) || (machType == CSPNONE) || (zs.compareTo(" ") == 0)) {
                return true;
                }
            
            // Handle the cipherCounter. If the cipher counter is 21, return with no action.
            if (cage.cipherCount >= 21) {
                return true;
                }
                                        
            // RPT Button - For now, do nothing if the repeat key. A future feature.
            if (bs.compareTo("RPT") == 0) {
                return true;
                }
                
            /* From here down, action should only be a non-repeat keyboard button */
                        
            // First handle Zeroize switch in R, P, E and D positions of master switch.
            if (zs.compareTo("Zeroize") == 0) {     // When Zeroize is selected,
                if (bs.compareTo("Blank") == 0) {   // only respond to the Blank key and,
                    // only move the rotors if Reset, Encrypt or Decrypt. Note that you
                    // should really only use Zeroize with Reset, the rest of this behavior
                    // is a quirk.
                    if ("RED".indexOf(ms) >= 0) {
                    
                        // The Blank key will advance each of the rotors position until
                        // they are all on the "O" position.
                        for (i = 0; i < 5; i++) {   // There are 5 rotors in a bank.
                            // CSP 889 cipher rotors all move in the normal clockwise rotation
                            if (machType == CSP889) {   
                                if (cage.cipherBank[i].pos != (int) 'O' - 'A') {
                                    cage.cipherBank[i].rotCW();
                                    // update the cipher bank display.
                                    cipherDisp.setText(cage.cipherBankPosToString()); 
                                    // clear the cipher rotor movement counter if the first or
                                    // last rotor turn.
                                    if (i == 0 || i == 4) {
                                        cage.cipherCount = 0;
                                        }
                                    }
                                }
                            // CSP 2900's 2nd and 4th cipher rotors turn counter clockwise
                            if (machType == CSP2900) {  
                                if (cage.cipherBank[i].pos != (int) 'O' - 'A') {
                                    if ( (i == 0) || (i == 2) || (i ==4) )
                                        cage.cipherBank[i].rotCW();
                                    else 
                                        cage.cipherBank[i].rotCCW();
                                    // update the cipher bank display.
                                    cipherDisp.setText(cage.cipherBankPosToString());
                                    // clear the cipher rotor movement counter if the first or
                                    // last rotor turn.
                                    if (i == 0 || i == 4) {
                                        cage.cipherCount = 0;
                                        }
                                    }
                                }
                            // Both machines rotate the control bank in the same direction
                            if (cage.controlBank[i].pos != (int) 'O' - 'A') {
                                cage.controlBank[i].rotCW();
                                // update the control bank display.
                                controlDisp.setText(cage.controlBankPosToString()); 
                                }
                            }   // end of the 5 rotor for loop
                            
                        // Increment the counter and display it.
                        countDisp.setText(String.valueOf(++count));
                        
                        // Another quirk, not only does Blank work in zeroize in E and D,
                        // but it also prints a space.                      
                        if ("ED".indexOf(ms) >= 0){
                            // Also add a space to the paper tape.
                            paperTape.appendText(" ");
                            }
                        return true;
                        }   // end of a zeroize with the Blank key in R, E or D
                        
                    // The doc indicates that with zeroize and plaintext the machine should 
                    // do nothing, but on the real machine a Blank key spaces the paper tape
                    // and updates the counter.
                    if (ms.compareTo("P") == 0){
                        // Increment the counter and display it.
                        countDisp.setText(String.valueOf(++count)); 
                        paperTape.appendText(" ");      // Add a space to the paper tape.
                        return true;
                        }   // end of zeroize and plaintext with Blank key.
                        
                    }   // end of Zeroize and Blank key block.
                    
                // not a Blank key in zeroize, then do nothing.
                return true;
                }   // end of zeroize switch
            
            /* From here down we are in Operate (not Zeroize),
            handle R, P, E and D master switch positions. */
            
            // In the Reset position of master switch, pressing number keys 1 through 5
            // advances the corresponding control and cipher rotors. It is used to set the
            // rotor position to a key.  Another quirk is that the Blank key moves the cipher
            // rotors.
            if (ms.compareTo("R") == 0) {
                // The 1-5 keys work in R position
                j = "12345".indexOf(bs);
                if ( j >= 0) {          // indexOf is positive if bs is found
                    // Rotate 1 to 4 cipher rotors.  
                    cage.cipherBankUpdate(machType);
                    // update the cipher bank display.  
                    cipherDisp.setText(cage.cipherBankPosToString()); 
                    // change the position of a control rotor
                    cage.controlBank[j].rotCW();
                    // update its display   
                    controlDisp.setText(cage.controlBankPosToString()); 
                    // Increment the counter and display it.
                    countDisp.setText(String.valueOf(++count));
                    return true;
                    }
                    
                // The doc indicates that nothing should happen here, but Blank moves the
                // cipher rotors, this is a quirk, not a feature of the real hardware.
                if (bs.compareTo("Blank") == 0){
                    // Rotate 1 to 4 cipher rotors. 
                    cage.cipherBankUpdate(machType);
                    // update the cipher bank display.              
                    cipherDisp.setText(cage.cipherBankPosToString()); 
                    // Increment the counter and display it.
                    countDisp.setText(String.valueOf(++count));
                    return true;
                    }
                // Keys other than 1-5 and Blank do nothing
                return true;
                }
                                                        
            // Plaintext switch position
            if ( ms.compareTo("P") == 0) {
                // Convert Space Bar to a single space.
                if ((bs.compareTo("Blank") == 0) || (bs.compareTo("Space Bar") == 0)) {
                    bs = " ";
                    }
                countDisp.setText(String.valueOf(++count)); // Update the counter and display it.
                paperTape.appendText(bs);                   // Update the paper tape
                return true;
                }
                
            /* Now encipher or decipher.  Only character keys, number keys, dash,
            and Blank and Space Bar are left. */
            
            // Number keys and the dash key are ignored during encipher or decipher.
            if ("1234567890-".indexOf(bs) >= 0) {
                return true;    // ignore the key.
            }
                
            // Blank in E or D has the odd behavior of updating the cipher and control rotor
            // banks and printing a space.
            if (bs.compareTo("Blank") == 0) {
                // ECM cycle will update both cipher and control rotor banks
                bs = ECMcycle(" ", ENCRYPT, machType); 
                countDisp.setText(String.valueOf(++count)); // Increment displayed counter
                paperTape.appendText(" ");                  // Print a space.
                encPaperCount++;
                return true;
                }
                        
            // encipher
            if ( ms.compareTo("E") == 0 ) {
                // Convert Z to X. There are only 26 cipher text characters.
                if (bs.compareTo("Z") == 0) {
                    bs = "X";
                    }
                // Convert Space Bar to Z. Spaces are more important than Z. Note that the
                // deciphered plaintext can never have a Z.
                if ( bs.compareTo("Space Bar") == 0 ) {
                    bs = "Z";
                    }
                bs = ECMcycle(bs, ENCRYPT, machType);
                countDisp.setText(String.valueOf(++count)); // Increment displayed counter
                // Add a space to the paper tape if needed to generate 5 character groups. 
                // Do not add space the first time through, i.e. encPaperCount = 0.
                if ((encPaperCount != 0) && ((encPaperCount % 5) == 0)) {   
                    paperTape.appendText(" "+bs);   // Print the space and ciphertext.
                    }                       
                else {
                    paperTape.appendText(bs);       // Print the cipher text.
                    }
                encPaperCount++;
                cage.cipherCount++;
                return true;
                }   // end of encipher
                
            // decipher
            if ( ms.compareTo("D") == 0 ) {
                if (bs.compareTo("Space Bar") == 0) { // Ignore Space Bar
                    return true;
                    }
                bs = ECMcycle(bs, DECRYPT, machType);
                // Convert Z to Space.
                if (bs.compareTo("Z") == 0) {
                    bs = " ";
                    }
                countDisp.setText(String.valueOf(++count)); // Increment display counter.
                paperTape.appendText(bs);                   // Print the plaintext.
                cage.cipherCount++;
                return true;
                }   // end of decipher
                                        
        }   // end of button handling block
        
    // the action was something we did not handle.
    return false;  
    }   // end of action


/***********************************************************************
 * ECMcycle enciphers or deciphers a single character.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * Enciphers or deciphers a single character through the RotorCage object.
 *
 * Control and cipher rotor bank contacts are labeled as if a rotor were positioned
 * with letter "A" on top, not reversed, as printed (i.e. increasing clockwise.) 
 * Internally these positions are represented by ints from 0-25. The pos of a rotor 
 * is its displacement from A on top.  For later reference, index rotor contacts are
 * similarly labeled to match an index rotor with zero on top (10,20,30,40,50 on top),
 * not reversed, as printed (i.e. increasing counter clockwise.)
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public String ECMcycle(String s, boolean direction, int machine) {
        int in, out;
        String sout;
                
        // convert from keyboard string to internal integer representation.
    in = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(s);   // keyboard/printer contact
        //  01234567890123456789012345              // internal representation result
    
    out = cage.cipherBankPath(direction,in);        // encipher or decipher the character
    
    // Convert from internal integer representation to string.  This is the inverse of
    // the lookup done before the encipher or decipher.  Note that substring goes from
    // the first parameter to the second parameter - 1.
    sout = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".substring(out, out+1);
        //      01234567890123456789012345          // internal representation result

    cage.cipherBankUpdate(machine);         // Rotate 1 to 4 cipher rotors. 
        cipherDisp.setText(cage.cipherBankPosToString()); // update the cipher bank display.
        cage.controlBankUpdate();               // Rotate the control rotors in meter fashion.
        controlDisp.setText(cage.controlBankPosToString()); // update the control bank display
 
        return(sout);
        }   //end of ECMcycle


/***********************************************************************
 * getAppletInfo - Identifies the applet if asked.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * One of the standard applet routines.  I have never seen this information used so I have 
 * no idea if this is correct.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public String getAppletInfo() {

        return("ECMApp\nEmulates an ECM Mark II (SIGABA)\nCopyright (C) 1996, Richard Pekelney\nAll Rights Reserved");
        }   // end of getAppletInfo
        
        
/***********************************************************************
 * getParameterInfo - Supplies information on the HTML parameters.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * One of the standard applet routines.  I have never seen this information used so I have 
 * no idea if this is correct.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public String[][] getParameterInfo() {
        String[][] info = {
            // Parameter Name   Kind of Value   Description
                {"CipherOrder", "String",       "Cipher rotor bank order."},
                {"ControlOrder","String",       "Control rotor bank order"},
                {"IndexOrder",  "String",       "Index rotor bank order"}
                };  
            return info;
            }   // end of getParametersInfo
            
/* There is no need for a start(), stop(), paint(), or destroy() method. */ 

    }   // end of ECMApp