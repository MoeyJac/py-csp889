/***********************************************************************
 * RotorCage - contains the workings of the ECM rotors.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * This object contains the guts of the ECM algorithm.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
class RotorCage {
    static final boolean ENCRYPT = false, DECRYPT = true;
    static final int CSP889 = 0, CSP2900 = 1, CSPNONE = 2;
    
    // This table has the wiring between the left side of the control rotor 
    // bank to the left side of the index rotor.  This table is for a CSP-889.
    static final int CONTROL_INDEX_889[] =
    {9,1,2,3,3,4,4,4,5,5,5,6,6,6,6,7,7,7,7,7,8,8,8,8,8,8}; // index
    //  a b c d e f g h i j k l m n o p q r s t u v w x y z      control
    //  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5

    // This table has the wiring between the left side of the control rotor
    // bank to the left side of the index rotor.  This table is for a CSP-2900.
    // On the CSP-2900, P, Q and R are not connected. They are 9 in the table, but 
    // the code below handles the exception.
    static final int CONTROL_INDEX_2900[] =
        {9,1,2,3,3,4,4,4,5,5,5,6,6,6,6,9,9,9,7,7,0,0,8,8,8,8};  // index
    //  a b c d e f g h i j k l m n o p q r s t u v w x y z       control

    // This table has the wiring between the right side of the index rotor bank 
    // to the magnets that rotate the cipher rotors.
    static final int INDEX_MAG[] =
        {1,5,5,4,4,3,3,2,2,1};  // rotor stepping magnet
    //  0 1 2 3 4 5 6 7 8 9 index contacts minus one to match array

    public CipherRotor cipherBank[] = new CipherRotor[5];
    public ControlRotor controlBank[] = new ControlRotor[5];
    public IndexRotor indexBank[] = new IndexRotor[5];
    
    public int cipherCount = 0; // counter used to detect improperly installed index rotors.


/***********************************************************************
 * RotorCage - Constructor of the RotorCage object
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * This object contains the guts of the ECM algorithm.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    RotorCage(String cipherSet, String controlSet, String indexSet) {
        int i;
        int cipherNum, controlNum, indexNum;

        // The passed strings contain the order and orientation of the rotors.
        for (i = 0; i < 5; i++) {
            //Example cipherNum => cipherOrder = "0N1N2N3N4N"
            cipherNum = cipherSet.charAt(i * 2) - '0';  // zero
            //Example controlNum => controlOrder = "5N6N7N8N9N";
            controlNum = controlSet.charAt(i * 2) - '0';
            //Example indexNum => indexOrder = "0N1N2N3N4N";
            indexNum = indexSet.charAt(i * 2) - '0';
            
            // Check for out of bounds rotor numbers
            if ((cipherNum < 0) || (cipherNum > (Rotor.WIRING.length - 1)) ) {
                cipherNum = 0;
                }
            if ((controlNum < 0) || (controlNum > (Rotor.WIRING.length - 1)) ) {
                controlNum = 0;
                }
            if ((indexNum < 0) || (indexNum > (Rotor.INDEXWIRING.length -1)) ) {
                indexNum = 0;
                }
            
            // Create the rotor.
            cipherBank[i] = new CipherRotor(cipherNum); 
            if(cipherSet.charAt(i * 2 + 1) == 'R')
                cipherBank[i].reverse();    
            controlBank[i] = new ControlRotor(controlNum);
            if(controlSet.charAt(i * 2 + 1) == 'R')
                controlBank[i].reverse();   
            indexBank[i] = new IndexRotor(indexNum);
            if(indexSet.charAt(i * 2 + 1) == 'R')
                indexBank[i].reverse(); 
            }
        return;
        }

/***********************************************************************
 * zeroize - Positions the cipher and control rotors with letter 'O' on top.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public void zeroize() {
        int i;

        setCipherBankPos("OOOOO");
        setControlBankPos("OOOOO");
        return;
        }

/***********************************************************************
 * setCipherBankPos - Sets the cipher rotors position.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public void setCipherBankPos(String posString) {
        int i;
    
        for(i = 0; i < 5; i++) {
        
            // if the first or last rotor changes clear the cipherCount
            if ( (i == 0) || (i == 4) ) {
                if (cipherBank[i].pos != (int) posString.charAt(i) - 'A') {
                    cipherCount = 0;
                    }
                }
                
            // now update the cipher bank
            cipherBank[i].pos = (int) posString.charAt(i) - 'A';
            
            }
        return;
        }
        
/***********************************************************************
 * setControlBankPos - Sets the control rotors position.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public void setControlBankPos(String posString) {
        int i;
    
        for(i = 0; i < 5; i++) {
            controlBank[i].pos = (int) posString.charAt(i) - 'A';
            }
        return;
        }
        
/***********************************************************************
 * setIndexBankPos - Sets the index rotors position.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public void setIndexBankPos(String posString) {
        int i;
    
        for(i = 0; i < 5; i++) {
            indexBank[i].pos = (int) posString.charAt(i) - '0';
            }
        return;
        }

/***********************************************************************
 * controlBankUpdate - Updates the Control rotor positions between cycles.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * Control rotors are updated in a "water meter" movement,
 * control rotor 3 first rotates once per cycle, then rotor 4 rotates each time rotor 3
 * moves from the O position, and 2 move least often, rotating once each time rotor 4 
 * moves from the O position.
 *
 * Note that the array indexes are numbered one less than the rotor position names.
 * i.e. [2] is rotor 3 and always rotates, [3] is the 4th rotor, and the 2nd rotor,
 * [1] rotates last.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 * 12 Feb 98    1.01    RSP Slow rotor now only moves when the medium rotor moves.
 *                          It was incorrectly moving each time medium rotor was O.
 *      
 ***********************************************************************/
    public void controlBankUpdate() {

        if (controlBank[2].pos == (int) 'O' - 'A') {    // medium rotor moves
            if (controlBank[3].pos == (int) 'O' - 'A') {// slow rotor moves
                controlBank[1].rotCW();
                }
            controlBank[3].rotCW();
            }
        controlBank[2].rotCW();                         // fast rotor always moves
        return;
        }

/***********************************************************************
 * CipherBankUpdate - Updates the cipher rotor positions between cycles.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * cipherBankUpdate passes 4 (or 6 for CSP 2900) currents through the control bank using
 * controlBankPath(),
 * then through the wiring from the control bank to the index bank using the CONTROL_INDEX[]
 * table,
 * then through the index bank using indexBankPath(),
 * and finally through the wiring between the index bank and the and the magnets that rotate
 * the cipher rotors using the INDEX_MAG[] table.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public void cipherBankUpdate(int machine) {
        boolean move[] = new boolean[5];
        int i, j, k;

        for (i = 0 ; i < 5 ; i++)
            move[i] = false;

        // The movements are stored in move[] because more than one of the paths through
        // the control and index banks can connect with a single cipher rotor magnet at the
        // same time.  Using the move[] array allows the program to be sequential even though
        // the machine is concurrent and thereby avoid extra motions of the rotor.
        if (machine == CSP889) {
            for (j = (int) 'F' - 'A' ; j <= (int) 'I' - 'A' ; j++) {
                move[INDEX_MAG[indexBankPath(CONTROL_INDEX_889[controlBankPath(j)])]-1] = true;
                }
                
            // Between 1 and 4 cipher rotors will rotate.
            for (i = 0 ; i < 5 ; i++) {
                if (move[i]) {
                    cipherBank[i].rotCW();
                    // clear the cipher rotor movement counter if the first or last rotor turn.
                    if (i == 0 || i == 4) {
                        cipherCount = 0;
                        }
                    }
                }
            }
        else {  // This is a CSP-2900, there are three changes.
            //1 Six contacts are on instead of four.
            loop: for (j = (int) 'D' - 'A' ; j <= (int) 'I' - 'A' ; j++) {  
                //2 The control/index wiring is changed and contacts P, Q and R are not connected.
                k = controlBankPath(j); 
                if ( (k == (int) 'P' - 'A') || (k == (int) 'Q' - 'A') || (k == (int) 'R' - 'A') ) {
                    continue loop;  // Skip contacts P, Q and R since they are not connected.
                    }
                move[INDEX_MAG[indexBankPath(CONTROL_INDEX_2900[k])]-1] = true;
                }
            // Between 1 and 4 cipher rotors will rotate.
            //3 In a 2900 rotors 2 and 4 ( array index 1 and 3) rotate backwards.
            if (move[0]) {
                cipherBank[0].rotCW();
                cipherCount = 0;
                }
            if (move[1]) {
                cipherBank[1].rotCCW();
                }
            if (move[2]) {
        cipherBank[2].rotCW();
        }
            if (move[3]) {
                cipherBank[3].rotCCW();
                }
            if (move[4]) {
                cipherBank[4].rotCW();
                cipherCount = 0;
                }
            }
        return;
        }

/***********************************************************************
 * cipherBankPath - Passes a current through 5 cipher rotors.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 * 3 Sep 98     1.01    RSP Path reversed between encrypt and decrypt
 *      
 ***********************************************************************/
    public int cipherBankPath(boolean direction , int pos) {
        int c;
        int rotNum;

        c = pos;
        if (direction == ENCRYPT) {
            // encrypt from left to right
            for (rotNum = 0 ; rotNum <= 4 ; rotNum++)
                c = cipherBank[rotNum].cipherEncPath(c);
            }
        else {
            // decrypt from right to left
            for (rotNum = 4 ; rotNum >= 0 ; rotNum--)
                c = cipherBank[rotNum].cipherDecPath(c);
            }
        return(c);
        }

/***********************************************************************
 * controlBankPath - Passes a current through 5 control rotors.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public int controlBankPath(int pos) {
        int c;
        int rotNum;

        // Control rotor bank is always read right to left
        c = pos;
        for (rotNum = 4 ; rotNum >= 0 ; rotNum--)
            c = controlBank[rotNum].controlPath(c);
        return(c);
        }

/***********************************************************************
 * indexBankPath - Passes a current through 5 index rotors.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public int indexBankPath(int pos) {
        int c;
        int rotNum;

        // Index rotor bank is always read left to right
        c = pos;
        for (rotNum = 0 ; rotNum <= 4 ; rotNum++)
            c = indexBank[rotNum].indexPath(c);
        return(c);
        }

/***********************************************************************
 * cipherBankPosToString - Creates a string of cipher rotor positions for display.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public String cipherBankPosToString() {
        char c[] = new char[5];
        int rotNum;
        
        for (rotNum = 0 ; rotNum < 5 ; rotNum++) {
            c[rotNum] = (char) (cipherBank[rotNum].pos + (int) 'A');
            }
        return(String.valueOf(c));
        }
        
/***********************************************************************
 * controlBankPosToString - Creates a string of control rotor positions for display.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public String controlBankPosToString() {
        char c[] = new char[5];
        int rotNum;
        
        for (rotNum = 0 ; rotNum < 5 ; rotNum++) {
            c[rotNum] = (char) (controlBank[rotNum].pos + (int) 'A');
            }
        return(String.valueOf(c));
        }

/***********************************************************************
 * indexBankPosToString - Creates a string of index rotor positions for display.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * Creates a string of rotor positions, note the real rotors are numbered 10-19,
 * 20-29, 30-39, 40-49, 50-59, but for now this program uses a single digit index position.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public String indexBankPosToString() {
        char c[] = new char[5];
        int rotNum;
        
        for (rotNum = 0 ; rotNum < 5 ; rotNum++) {
            c[rotNum] = (char) (indexBank[rotNum].pos + (int) '0');
            }
        return(String.valueOf(c));
        }
        
    }   // end of RotorCage
        

/***********************************************************************
 * Rotor object emulates an ECM rotor.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
class Rotor {
    static final boolean ENCRYPT = false, DECRYPT = true;
    static final int RIGHT = 1; // Rotor wiring table from right to left
    static final int LEFT = 0;  // Rotor wiring table from left to right
    
    // This table represents the physical rotor wiring for large rotors.
    // Each machine was supplied with 10 large, 26 contact rotors that could be used as
    // either control or cipher rotors.  I do not have any authentic rotor wirings, these 
    // were created created with a shift register type generator. This table should be 10
    // items long. I.e. the two test rotors are normally commented out.  The table's index
    // is the left side of the rotor, the entry in the table the right. 
    // I.e. right side = WIRING[][left side]. Real rotors are labeled on the outside
    // circumference increasing in the clockwise direction.
    static final int WIRING[][] = {
        {'A','B','C','D','E','F','G','H','I','J','K','L','M',   //[0][] Straight through
        'N','O','P','Q','R','S','T','U','V','W','X','Y','Z'},   //      test rotor.
//d2        {'B','C','D','E','F','G','H','I','J','K','L','M','N',   //[0][] Shift by one
//d2        'O','P','Q','R','S','T','U','V','W','X','Y','Z','A'},   //      test rotor.
// Simulated random rotors are below:
//d1        {'Y','C','H','L','Q','S','U','G','B','D','I','X','N',   //[0][] 
//d1        'Z','K','E','R','P','V','J','T','A','W','F','O','M'},
        {'I','N','P','X','B','W','E','T','G','U','Y','S','A',   //[1][] Rotor wired Oct 96 in
        'O','C','H','V','L','D','M','Q','K','Z','J','F','R'},   // the real machine.
        {'W','N','D','R','I','O','Z','P','T','A','X','H','F',   //[2][] Rotor wired Sep 97 in
        'J','Y','Q','B','M','S','V','E','K','U','C','G','L'},   // the real machine.
        {'T','Z','G','H','O','B','K','R','V','U','X','L','Q',   //[3][]
        'D','M','P','N','F','W','C','J','Y','E','I','A','S'},
        {'Y','W','T','A','H','R','Q','J','V','L','C','E','X',   //[4][]
        'U','N','G','B','I','P','Z','M','S','D','F','O','K'},
        {'Q','S','L','R','B','T','E','K','O','G','A','I','C',   //[5][]
        'F','W','Y','V','M','H','J','N','X','Z','U','D','P'},
        {'C','H','J','D','Q','I','G','N','B','S','A','K','V',   //[6][]
        'T','U','O','X','F','W','L','E','P','R','M','Z','Y'},
        {'C','D','F','A','J','X','T','I','M','N','B','E','Q',   //[7][]
        'H','S','U','G','R','Y','L','W','Z','K','V','P','O'},
        {'X','H','F','E','S','Z','D','N','R','B','C','G','K',   //[8][]
        'Q','I','J','L','T','V','M','U','O','Y','A','P','W'},
        {'E','Z','J','Q','X','M','O','G','Y','T','C','S','F',   //[9][] 
        'R','I','U','P','V','N','A','D','L','H','W','B','K'}        
    };

        //  Just for quick reference.
    //  ABCDEFGHIJKLMNOPQRSTUVWXYZ  // letter
        //  01234567890123456789012345  // internal representation result
    
    // This table represents the physical rotor wiring for small, index rotors.
    // This table should be 5 items long.  I.e. the two test rotors are normally
    // commented out.  These are real rotor wirings as found in the CSP-889/2900
    // now aboard USS Pampanito.  We do not know when they served.  They are all
    // embossed with "CTT 68AAC" on the edge.  Note that index rotors labeling
    // on their circumference increases in the counter clockwise direction.
    static final int INDEXWIRING[][] = {
    //  0 1 2 3 4 5 6 7 8 9 Left side of rotor, below is right side.
//d3    {0,1,2,3,4,5,6,7,8,9},  // [0][] Straight through test rotor.
//d3    {1,2,3,4,5,6,7,8,9,0},  // [0][] A shift by one test rotor
    // The real rotors are below:
        {7,5,9,1,4,8,2,6,3,0},  // [0][]    Rotor # 10
        {3,8,1,0,5,9,2,7,6,4},  // [1][]            20
        {4,0,8,6,1,5,3,2,9,7},  // [2][]            30
        {3,9,8,0,5,2,6,1,7,4},  // [3][]            40
        {6,4,9,7,1,3,5,2,8,0}   // [4][]            50
        };

    // Position of a cipher or control rotor is its clockwise displacement from having
    // 'A' on top.  For an index rotor this is its counter clockwise displacement from 
    // zero on top.
    public int pos;         // Position of the rotor.
    public boolean reversed;    // Is this rotor reversed?


/***********************************************************************
 * rotCW() - Rotate a cipher or control rotor clockwise.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 *  rotate clockwise one position of a cipher or control rotor.  Note
 *  that these rotors are labeled clockwise increasing, except when
 *  reversed.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public int rotCW() {
                    
        if (reversed) {             // Reversed rotors increase counter clockwise.
            pos = (pos + 1) % 26;
            }
        else {
            pos = (pos - 1 + 26) % 26;  // Adding 26 guarantees a positive value.
            }
            
        return(pos);
        }

/***********************************************************************
 * rotCCW() - Rotate a cipher or control rotor counter-clockwise.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 *  rotate counter clockwise one position of a cipher or control rotor. Note
 *  that these rotors are labeled clockwise increasing, except when reversed.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public int rotCCW() {

        if (reversed) {                 // Reversed rotors increase counter clockwise.
            pos = (pos - 1 + 26) % 26;  // Adding 26 guarantees a positive value.
            }
        else {                          // Normal rotor increase clockwise.
            pos = (pos + 1) % 26;
            }
            
        return(pos);
        }

/***********************************************************************
 * reverse() - Reverse a rotor.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * Reversed rotors can be thought of as upside and backwards rotors.  This routine
 * just provides an interface to the reversed variable.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public void reverse() {

        reversed = true;
        return;
        } 
        
    } // end of Rotor


/***********************************************************************
 * CipherRotor object extends Rotor
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * This object contains a cipher rotor.  Cipher rotors are read from left to
 * right during encrypt and right to left during decrypt.  Since rotor wirings
 * supplied in a table of left to right wirings.  The object keeps a left to right
 * and a right to left version of the table.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
class CipherRotor extends Rotor {
    int cipherRotor[][] = new int[2][26];

    CipherRotor(int wiringNum) {    // Constructor for Cipher Rotors.
   
        int i;

        for(i = 0 ; i < 26 ; i++) {
            cipherRotor[LEFT][i] = WIRING[wiringNum][i] - (int) 'A';
            cipherRotor[RIGHT][cipherRotor[LEFT][i]] = i;
            }
            
        reversed = false;
        return;
    }
        

/***********************************************************************
 * cipherEncPath() - Encrypt path through a rotor.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * Encrypt path through a cipher rotor is from left to right, except for reversed 
 * rotors.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public int cipherEncPath(int in) {
        int out;

        if (reversed) {
            out = (pos - cipherRotor[RIGHT][(pos - in + 26) % 26] + 26) % 26;
            }
        else {
            out = (cipherRotor[LEFT][(in + pos) % 26] - pos + 26) % 26;
            }
            
        return(out);
        }

/***********************************************************************
 * cipherDecPath() - Decrypt path through a rotor.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * Decrypt path through a cipher rotor is from right to left, except for reversed 
 * rotors.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public int cipherDecPath(int in) {
        int out;

        if (reversed) {
            out = (pos - cipherRotor[LEFT][(pos - in + 26) % 26] + 26) % 26;
            }
        else {
            out = (cipherRotor[RIGHT][(in + pos) % 26] - pos + 26) % 26;
            }
            
        return(out);
        }

    } // end of CipherRotor


/***********************************************************************
 * ControlRotor object extends Rotor
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * This object contains a control rotor.  Control rotors are always read from right
 * to left, except when reversed.  The object keeps a left to right version of the
 * wiring table to handle reversed rotors.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
class ControlRotor extends Rotor {
    int controlRotor[][] = new int[2][26];

    ControlRotor(int wiringNum) {   // Constructor for Control Rotors.
        int i;

        for(i = 0 ; i < 26 ; i++) {
            controlRotor[LEFT][i] = WIRING[wiringNum][i] - (int) 'A';
            controlRotor[RIGHT][controlRotor[LEFT][i]] = i;
            }
            
        reversed = false;
        return;
        }

/***********************************************************************
 * controlPath() passes a current though a control rotor.
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * controlPath() passes a current though a rotor. Control rotor pos is the clockwise
 * rotation from zero on top.  A reversed rotor can be thought of as an upside down,
 * (i.e. counter clockwise) and backwards (i.e. left to right) normal rotor.
 * The control rotors are always read from right to left, except for reversed rotors.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public int controlPath(int in){
        int out;
 
        // Adding 26 to any value that might go negative prevents a negative value that might
        // cause a divide error during the mod (%) 26 operation.
        
        if (reversed) {
            out = (pos - controlRotor[LEFT][(pos - in + 26) % 26] + 26) % 26;
            }
        else {
            out = (controlRotor[RIGHT][(in + pos) % 26] - pos + 26) % 26;
            }
        
        return(out);
        }
        
    } // end of ControlRotor class


/***********************************************************************
 * IndexRotor object extends Rotor
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * This object contains an index rotor.  Index rotors are always read from left
 * to right, except when reversed.  The object keeps a right to left version of the
 * wiring table to handle reversed rotors.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
class IndexRotor extends Rotor {
    int indexRotor[][] = new int[2][10];

    IndexRotor(int wiringNum) { // Constructor for Index Rotors.
        int i;

        for(i = 0 ; i < 10 ; i++) {
            indexRotor[LEFT][i] = INDEXWIRING[wiringNum][i];
            indexRotor[RIGHT][indexRotor[LEFT][i]] = i;
            }
            
        reversed = false;
        return;
        }  

/***********************************************************************
 * indexPath() passes a current though an index rotor..
 * Copyright (C) 1996, by Richard Pekelney
 * All Rights Reserved
 *
 * SUMMARY:
 * indexPath() passes a current though an index rotor.  Index rotor pos is the counter
 * clockwise rotation from zero on top.  A reversed rotor can be thought of as
 * an upside down, (i.e. clockwise) and backwards (i.e. right to left) normal rotor.
 * The index rotors are always read from left to right, except for reversed rotors.
 *
 * REVISION HISTORY:
 *
 *   Date   Version By  Purpose of Revision
 * -------- ------- --- --------------------------------------
 * 8 Oct 96     1.00    RSP First release.
 *      
 ***********************************************************************/
    public int indexPath(int in){
        int out;

        // Adding 10 to any value that might go negative prevents a negative value that might
        // cause a divide error during the mod (%) 10 operation.
        
        if (reversed) {     // This is a reversed rotor.
            out = (pos - indexRotor[RIGHT][(pos - in + 10) % 10] + 10) % 10;
            }
        else {              // This is a normal rotor.
            out = (indexRotor[LEFT][(in + pos) % 10] - pos + 10) % 10;
            }
        
        return(out);    
        }
        
    } // end of IndexRotor class