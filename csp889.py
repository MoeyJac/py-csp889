class Rotor:
    ENCRYPT, DECRYPT = False, True
    RIGHT, LEFT = 1, 0
    
    WIRING = [
        ['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'],
        ['I','N','P','X','B','W','E','T','G','U','Y','S','A','O','C','H','V','L','D','M','Q','K','Z','J','F','R'],
        ['W','N','D','R','I','O','Z','P','T','A','X','H','F','J','Y','Q','B','M','S','V','E','K','U','C','G','L'],
        ['T','Z','G','H','O','B','K','R','V','U','X','L','Q','D','M','P','N','F','W','C','J','Y','E','I','A','S'],
        ['Y','W','T','A','H','R','Q','J','V','L','C','E','X','U','N','G','B','I','P','Z','M','S','D','F','O','K'],
        ['Q','S','L','R','B','T','E','K','O','G','A','I','C','F','W','Y','V','M','H','J','N','X','Z','U','D','P'],
        ['C','H','J','D','Q','I','G','N','B','S','A','K','V','T','U','O','X','F','W','L','E','P','R','M','Z','Y'],
        ['C','D','F','A','J','X','T','I','M','N','B','E','Q','H','S','U','G','R','Y','L','W','Z','K','V','P','O'],
        ['X','H','F','E','S','Z','D','N','R','B','C','G','K','Q','I','J','L','T','V','M','U','O','Y','A','P','W'],
        ['E','Z','J','Q','X','M','O','G','Y','T','C','S','F','R','I','U','P','V','N','A','D','L','H','W','B','K']
    ]

    #  Just for quick reference.
    #  ABCDEFGHIJKLMNOPQRSTUVWXYZ  // letter
    #  01234567890123456789012345  // internal representation result
    INDEX_WIRING = [
        [7,5,9,1,4,8,2,6,3,0],
        [3,8,1,0,5,9,2,7,6,4],
        [4,0,8,6,1,5,3,2,9,7],
        [3,9,8,0,5,2,6,1,7,4],
        [6,4,9,7,1,3,5,2,8,0]
    ]

    def __init__(self):
        self.pos
        self.reversed = False

    def rotCW(self):
        pos = (pos + 1) % 26 if self.reversed else (pos - 1 + 26) % 26
        return pos

    def rotCCW(self):
        pos = (pos - 1 + 26) % 26 if self.reversed else (pos + 1) % 26
        return pos

    def reverse(self):
        self.reverse = not self.reverse

class CipherRotor(Rotor):

    def __init__(self, wiringNum, reversed=False):
        #Create a 2x26 array 
        # array in 0th index stores Left to Right/Encrypt version of Rotor
        # array in 1st index store Right to Left/Decrypt version of Rotor
        self.cipherRotor = [[], []]

        for i in range(26):
            self.cipherRotor[Rotor.LEFT][i] = Rotor.WIRING[wiringNum][i] - ord('A')
            self.cipherRotor[Rotor.RIGHT][self.cipherRotor[Rotor.LEFT][i]] = i

        self.reversed = reversed
        
    def cipherEncPath(self, inVal):
        out = 0

        if self.reversed:
            out = (self.pos - self.cipherRotor[Rotor.RIGHT][(self.pos - inVal + 26) % 26] + 26) % 26
        else:
            out = (self.cipherRotor[Rotor.LEFT][(inVal + self.pos) % 26] - self.pos + 26) % 26

        return out

    def cipherDecPath(self, inVal):
        out = 0

        if self.reversed:
            out = (self.pos - self.cipherRotor[Rotor.LEFT][(self.pos - inVal + 26) % 26] + 26) % 26
        else:
            out = (self.cipherRotor[Rotor.RIGHT][(inVal + self.pos) % 26] - self.pos + 26) % 26

        return out
    
class ControlRotor(Rotor):

    def __init__(self, wiringNum, reversed=False):
        #Create a 2x26 array 
        # array in 0th index stores Left to Right/Encrypt version of Rotor
        # array in 1st index store Right to Left/Decrypt version of Rotor
        self.controlRotor = [[], []]

        for i in range(26):
            self.controlRotor[Rotor.LEFT][i] = Rotor.WIRING[wiringNum][i] - ord('A')
            self.controlRotor[Rotor.RIGHT][self.controlRotor[Rotor.LEFT][i]] = i

        self.reversed = reversed

    def controlPath(self, inVal):
        out = 0

        # Adding 26 to any value that might go negative prevents a negative value that might
        # cause a divide error during the mod (%) 26 operation.

        if self.reversed:
            out = (self.pos - self.controlRotor[Rotor.LEFT][(self.pos - inVal + 26) % 26] + 26) % 26
        else:
            out = (self.controlRotor[Rotor.RIGHT][(inVal + self.pos) % 26] - self.pos + 26) % 26

        return out

class IndexRotor(Rotor):

    def __init__(self, wiringNum, reversed=False):
        #Create a 2x10 array 
        # array in 0th index stores Left to Right/Encrypt version of Rotor
        # array in 1st index store Right to Left/Decrypt version of Rotor
        self.indexRotor = [[[]]*10]*2

        for i in range(10):
            self.indexRotor[Rotor.LEFT][i] = Rotor.INDEX_WIRING[wiringNum][i]
            self.indexRotor[Rotor.RIGHT][self.indexRotor[Rotor.LEFT][i]] = i

        self.reversed = reversed

    def indexPath(self, inVal):
        out = 0

        if self.reversed:
            out = (self.pos - self.indexRotor[Rotor.RIGHT][(self.pos - inVal + 10) % 10] + 10) % 10
        else:
            out = (self.indexRotor[Rotor.LEFT][(inVal + self.pos) % 10] - self.pos + 10) % 10

        return out

class RotorCage:
    ENCRYPT, DECRYPT = False, True
    CSP889, CSP2900, CSPNONE = 0, 1, 2

    # This table has the wiring between the left side of the control rotor 
    # bank to the left side of the index rotor.  This table is for a CSP-889.
    CONTROL_INDEX_889 = [9,1,2,3,3,4,4,4,5,5,5,6,6,6,6,7,7,7,7,7,8,8,8,8,8,8]
                            # a b c d e f g h i j k l m n o p q r s t u v w x y z
                            # 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5

    # This table has the wiring between the left side of the control rotor
    # bank to the left side of the index rotor.  This table is for a CSP-2900.
    # On the CSP-2900, P, Q and R are not connected. They are 9 in the table, but 
    # the code below handles the exception.
    CONTROL_INDEX_2900 = [9,1,2,3,3,4,4,4,5,5,5,6,6,6,6,9,9,9,7,7,0,0,8,8,8,8]
                            #  a b c d e f g h i j k l m n o p q r s t u v w x y z

    # This table has the wiring between the right side of the index rotor bank 
    # to the magnets that rotate the cipher rotors.
    INDEX_MAG = [1,5,5,4,4,3,3,2,2,1]
                #0 1 2 3 4 5 6 7 8 9 index contacts minus one to match array
    
    # Rotor Banks
    cipherBank  = []
    controlBank = []
    indexBank   = []

    # Counter used to detect improperly installed index rotors
    cipherCount = 0
    
    def __init__(self, cipherSet, controlSet, indexSet):
        # The passed strings contain the order and orientation of the rotors.
        for i in range(5):
            # Example cipherNum => cipherOrder = "0N1N2N3N4N"
            cipherNum = cipherSet.charAt(i * 2) - '0'
            # Example controlNum => controlOrder = "5N6N7N8N9N";
            controlNum = controlSet.charAt(i * 2) - '0'
            # Example indexNum => indexOrder = "0N1N2N3N4N";
            indexNum = indexSet.charAt(i * 2) - '0'

            # Check for out of bounds rotor numbers
            if (cipherNum < 0) or (cipherNum > (Rotor.WIRING.length - 1)):
                cipherNum = 0
            if (controlNum < 0) or (controlNum > (Rotor.WIRING.length - 1)):
                controlNum = 0
            if (indexNum < 0) or (indexNum > (Rotor.WIRING.length - 1)):
                indexNum = 0

            RotorCage.cipherBank[i] = CipherRotor(cipherNum, cipherSet.charAt(i * 2 + 1) == 'R')
            RotorCage.controlBank[i] = ControlRotor(controlNum, controlSet.charAt(i * 2 + 1) == 'R')
            RotorCage.indexBank[i] = IndexRotor(indexNum, indexSet.charAt(i * 2 + 1) == 'R')

    def zeroize(self):
        self.setCipherBankPos('OOOOO') # Note these are the Letter O NOT a zero
        self.setControlBankPos('OOOOO') # Note these are the Letter O NOT a zero

    def setCipherBankPos(self, posString):
        for i in range(5):

            # if the first or last rotor changes clear the cipherCount
            if (i is 0) or (i is 4):
                if RotorCage.cipherBank[i].pos != ord(posString.charAt(i)) - ord('A'):
                    RotorCage.cipherCount = 0
            # now update the cipher bank
            RotorCage.cipherBank[i].pos = ord(posString.charAt(i)) - ord('A')

    def setControlBankPos(self, posString):
        for i in range(5):
            RotorCage.controlBank[i].pos = ord(posString.charAt(i)) - ord('A')

    def setIndexBankPos(self, posString):
        for i in range(5):
            RotorCage.indexBank[i].pos = ord(posString.charAt(i)) - ord('0')

    def controlBankUpdate(self):
        if RotorCage.controlBank[2].pos == ord('O' - 'A'):      # medium/#4 control rotor moves
            if RotorCage.controlBank[3].pos == ord('O' - 'A'):  # slow/#2 control rotor moves
                RotorCage.controlBank[1].rotCW()

            RotorCage.controlBank[3].rotCW()

        RotorCage.controlBank[2].rotCW()                        # fast/#3 control rotor moves

    def cipherBankUpdate(self, machine:int):
        move = [False]*5

        # The movements are stored in move[] because more than one of the paths through
        # the control and index banks can connect with a single cipher rotor magnet at the
        # same time.  Using the move[] array allows the program to be sequential even though
        # the machine is concurrent and thereby avoid extra motions of the rotor.
        if machine == RotorCage.CSP889:

            # 5 = ord('F') - ord('A')
            # 8 = ord('I') - ord('A')
            # need i = 5 -> i = 8 [5,6,7,8]
            for i in range(5, 8 + 1):
                move[RotorCage.INDEX_MAG[self.indexBankPath(RotorCage.CONTROL_INDEX_889[self.controlBankPath(i)])] - 1] = True

            # Between 1 and 4 cipher rotors will rotate.
            for i in range(5):
                if move[i]:
                    RotorCage.cipherBank[i].rotCW()

                    # Clear the cipher rotor movement counter if the first or last rotor turn.
                    if i == 0 or i == 4:
                        RotorCage.cipherCount = 0

        else: # Need to implement CSP-2900 here
            pass

    def cipherBankPath(self, direction:bool, pos:int):
        c = self.pos
        if direction == RotorCage.ENCRYPT:
            for rotNum in range(5):
                c = RotorCage.cipherBank[rotNum].cipherEncPath(c)
        else:
            for rotNum in reversed(range(5)):
                c = RotorCage.cipherBank[rotNum].cipherDecPath(c)

        return c

    def controlBankPath(self, pos:int):
        c = pos
        for rotNum in reversed(range(5)):
            c = RotorCage.controlBank[rotNum].controlPath(c)
        return c

    def indexBankPath(self, pos:int):
        c = pos
        for rotNum in range(5):
            c = RotorCage.indexBank[rotNum].indexPath(c)

        return c

    def cipherBankPosToString(self):
        return ''.join([chr(cipherRotor.pos + ord('A')) for cipherRotor in RotorCage.cipherBank])

    def controlBankPosToString(self):
        return ''.join([chr(controlRotor.pos + ord('A')) for controlRotor in RotorCage.controlBank])

    def indexBankPosToString(self):
        return ''.join([chr(indexRotor.pos + ord('A')) for indexRotor in RotorCage.indexBank])

def main():

    cipherOrder = '0N1N2N3N4N'
    controlOrder = '5N6N7N8N9N'
    indexOrder = '0N1N2N3N4N'

    cage = RotorCage(cipherOrder, controlOrder, indexOrder)

    cage.zeroize()

    cage.setIndexBankPos('00000')

main()
