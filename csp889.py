class Rotor:

    def __init__(self):
        self.ENCRYPT, self.DECRYPT = False, True
        self.RIGHT, self.LEFT = 1, 0
        
        self.WIRING = [
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

        self.INDEX_WIRING = [
            [7,5,9,1,4,8,2,6,3,0],
            [3,8,1,0,5,9,2,7,6,4],
            [4,0,8,6,1,5,3,2,9,7],
            [3,9,8,0,5,2,6,1,7,4],
            [6,4,9,7,1,3,5,2,8,0]
        ]

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

    def __init__(self, wiringNum):
        #Create a 2x26 array 
        # array in 0th index stores Left to Right/Encrypt version of Rotor
        # array in 1st index store Right to Left/Decrypt version of Rotor
        self.cipherRotor = [[[]]*26]*2

        for i in range(26):
            self.cipherRotor[self.LEFT][i] = self.WIRING[wiringNum][i] - int('A')
            self.cipherRotor[self.RIGHT][self.cipherRotor[self.LEFT][i]] = i

        self.reversed = False
        
    def cipherEncPath(self, inVal):
        out = 0

        if self.reversed:
            out = (self.pos - self.cipherRotor[self.RIGHT][(self.pos - inVal + 26) % 26] + 26) % 26
        else:
            out = (self.cipherRotor[self.LEFT][(inVal + self.pos) % 26] - self.pos + 26) % 26

        return out

    def cipherDecPath(self, inVal):
        out = 0

        if self.reversed:
            out = (self.pos - self.cipherRotor[self.LEFT][(self.pos - inVal + 26) % 26] + 26) % 26
        else:
            out = (self.cipherRotor[self.RIGHT][(inVal + self.pos) % 26] - self.pos + 26) % 26

        return out
    
class ControlRotor(Rotor):

    def __init__(self, wiringNum):
        #Create a 2x26 array 
        # array in 0th index stores Left to Right/Encrypt version of Rotor
        # array in 1st index store Right to Left/Decrypt version of Rotor
        self.controlRotor = [[[]]*26]*2

        for i in range(26):
            self.controlRotor[self.LEFT][i] = self.WIRING[wiringNum][i] - int('A')
            self.controlRotor[self.RIGHT][self.controlRotor[self.LEFT][i]] = i

        self.reversed = False

    def controlPath(self, inVal):
        out = 0

        # Adding 26 to any value that might go negative prevents a negative value that might
        # cause a divide error during the mod (%) 26 operation.

        if self.reversed:
            out = (self.pos - self.controlRotor[self.LEFT][(self.pos - inVal + 26) % 26] + 26) % 26
        else:
            out = (self.controlRotor[self.RIGHT][(inVal + self.pos) % 26] - self.pos + 26) % 26

        return out

class IndexRotor(Rotor):

    def __init__(self, wiringNum):
        #Create a 2x10 array 
        # array in 0th index stores Left to Right/Encrypt version of Rotor
        # array in 1st index store Right to Left/Decrypt version of Rotor
        self.indexRotor = [[[]]*10]*2

        for i in range(10):
            self.indexRotor[self.LEFT][i] = self.INDEX_WIRING[wiringNum][i]
            self.indexRotor[self.RIGHT][self.indexRotor[self.LEFT][i]] = i

        self.reversed = False

    def indexPath(self, inVal):
        out = 0

        if self.reversed:
            out = (self.pos - self.indexRotor[self.RIGHT][(self.pos - inVal + 10) % 10] + 10) % 10
        else:
            out = (self.indexRotor[self.LEFT][(inVal + self.pos) % 10] - self.pos + 10) % 10

        return out

def main():
    pass

main()
