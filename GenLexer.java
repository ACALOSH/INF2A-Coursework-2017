
// File:   GenLexer.java
// Date:   October 2013

// Java source file provided for Informatics 2A Assignment 1 (2013).
// Contains general infrastructure relating to DFAs and longest-match lexing,
// along with some trivial examples.


import java.io.* ;

// Some useful sets of characters.

class CharTypes {

    static boolean isLetter (char c) {
    return (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) ;
    }

    static boolean isSmall (char c) {
    return (('a' <= c && c <= 'z') || c == '_') ;
    }

    static boolean isLarge (char c) {
    return ('A' <= c && c <= 'Z') ;
    }

    static boolean isDigit (char c) {
    return ('0' <= c && c <= '9') ;
    }

    static boolean isSymbolic (char c) {
    return (c == '!' || c == '#' || c == '$' || c == '%' || c == '&' || 
        c == '*' || c == '+' || c == '.' || c == '/' || c == '<' || 
        c == '=' || c == '>' || c == '?' || c == '@' || c == '\\' ||
        c == '^' || c == '|' || c == '-' || c == '~' || c == ':') ;
    // inefficient but clear
    }

    static boolean isWhitespace (char c) {
    return (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\f') ;
    }

    static boolean isNewline (char c) {
    return (c == '\r' || c == '\n' || c == '\f' ) ;
    }

}

// Generic implementation of DFAs with explicit "dead" states

interface DFA {
    String lexClass() ;
    int numberOfStates() ;
    void reset() ;
    void processChar (char c) throws StateOutOfRange ;
    boolean isAccepting () ;
    boolean isDead () ;
}

abstract class Acceptor {

    // Stubs for methods specific to a particular DFA
    abstract String lexClass() ;
    abstract int numberOfStates() ;
    abstract int next (int state, char input) ;
    abstract boolean accepting (int state) ;
    abstract int dead () ;

    // General DFA machinery
    private int currState = 0 ;         // the initial state is always 0
    public void reset () {currState = 0 ;}  

    public void processChar (char c) throws StateOutOfRange {
    // performs the state transition determined by c
    currState = next (currState,c) ;
    if (currState >= numberOfStates() || currState < 0) {
        throw new StateOutOfRange (lexClass(), currState) ;
    }
    }

    public boolean isAccepting () {return accepting (currState) ;}
    public boolean isDead () {return (currState == dead()) ;}
}

class StateOutOfRange extends Exception {
    public StateOutOfRange (String lexClassName, int state) {
    super ("Illegal state " + Integer.toString(state) + 
               " in acceptor for " + lexClassName) ;
    }
}


// Examples of DFAs for some example weird lexical classes.
// These show how particular DFAs are to be constructed.
 
// Example 1: Tokens consisting of an even number of letters (and nothing else)

class EvenLetterAcceptor extends Acceptor implements DFA {

    public String lexClass() {return "EVEN" ;} ;
    public int numberOfStates() {return 3 ;} ;

    int next (int state, char c) {
    switch (state) {
    case 0: if (CharTypes.isLetter(c)) return 1 ; else return 2 ;
    case 1: if (CharTypes.isLetter(c)) return 0 ; else return 2 ;
        default: return 2 ; // garbage state, declared "dead" below
    }
    }

    boolean accepting (int state) {return (state == 0) ;}
    int dead () {return 2 ;}
}

// To create an instance of the above class, use `new EvenLetterAcceptor()'


// Example 2: Acceptor for just the token "&&"
 
class AndAcceptor extends Acceptor implements DFA {

    public String lexClass() {return "&&" ;} ;
    public int numberOfStates() {return 4 ;} ;

    int next (int state, char c) {
    switch (state) {
    case 0: if (c=='&') return 1 ; else return 3 ;
    case 1: if (c=='&') return 2 ; else return 3 ;
    case 2: return 3 ;
    default: return 3 ;
    }
    }
    boolean accepting (int state) {return (state == 2) ;}
    int dead () {return 3 ;}
}

// Example 3: Acceptor for just a space or linebreak character.
// Setting the lexical class as "" means these tokens will be discarded
// by the lexer.
   
class SpaceAcceptor extends Acceptor implements DFA {

    public String lexClass() {return "" ;} ; 
    public int numberOfStates() {return 3 ;} ;

    int next (int state, char c) {
    switch (state) {
    case 0: if (c == ' ' || c=='\n' || c=='\r') return 1 ; 
        else return 2 ;
    default: return 2 ;
    }
    }
    boolean accepting (int state) {return (state == 1) ;}
    int dead () {return 2 ;}
}


// Generic lexical analyser. 
// Uses principle of longest match, a.k.a. "maximal munch".


// A *lexical token* is simply a string tagged with the name of its
// lexical class.

class LexToken {
    private final String value, lexClass ;
    LexToken (String value, String lexClass) {
    this.value = value ; this.lexClass = lexClass ;
    }
    public String value () {return this.value ;} ;
    public String lexClass () {return this.lexClass ;} ;
}

// Typical example: new LexToken ("5", "num")



// The output of the lexing phase, and the input to the parsing phase,
// will be a LEX_TOKEN_STREAM object from which tokens may be drawn at will 
// by calling `nextToken'.


interface LEX_TOKEN_STREAM {
    LexToken pullToken () throws Exception ;
    // pulls next token from stream, regardless of class
    LexToken pullProperToken () throws Exception ;
    // pulls next token not of class "" (e.g. skip whitespace and comments)
    LexToken peekToken () throws Exception ;
    // returns next token without removing it from stream
    LexToken peekProperToken () throws Exception ;
    // similarly for non-"" tokens
    // All these methods return null once end of input is reached
}


// The following allows a LEX_TOKEN_STREAM object to be created for
// a given input file and a language-specific repertoire of lexical classes.

public class GenLexer implements LEX_TOKEN_STREAM {

    Reader reader ;       
    // for reading characters from input
    DFA[] acceptors ;  
    // array of acceptors for the lexical classes, in order of priority

    GenLexer (Reader reader, DFA[] acceptors) {
    this.reader = reader ;
    this.acceptors = acceptors ;
    }

    LexToken bufferToken ;       // buffer to allow 1-token lookahead
    boolean bufferInUse = false ;

    static final char EOF = (char)65535 ;

    // Implementation of longest-match lexer as described in lectures.
    // We go for simplicity and clarity rather than maximum efficiency.

    LexToken nextToken () 
    throws LexError, StateOutOfRange, IOException {
        char c ;                 // current input character
    String definite = "" ;   // characters up to last acceptance point
    String maybe = "" ;      // characters since last acceptance point
        int acceptorIndex = -1 ; // array index of highest priority acceptor
    boolean liveFound = false ;      // flags for use in 
    boolean acceptorFound = false ;  // iteration over acceptors

    for (int i=0; i<acceptors.length; i++) {
        acceptors[i].reset() ;
    } ;
    do {
        c = (char)(reader.read()) ;
        acceptorFound = false ;
        liveFound = false ;
        if (c != EOF) {
        maybe += c ;    
        for (int i=0; i<acceptors.length; i++) {
            acceptors[i].processChar(c) ;
            if (!acceptors[i].isDead()) {
            liveFound = true ;
            }
            if (!acceptorFound && acceptors[i].isAccepting()) {
            acceptorFound = true ;
            acceptorIndex = i ;
            definite += maybe ;
            maybe = "" ;
            reader.mark(10) ; // register backup point in input
            } ;
        }
        }
    } while (liveFound && c != EOF) ;
    if (acceptorIndex >= 0) { // lex token has been found
        // backup to last acceptance point and output token
        reader.reset() ;
        String theLexClass = acceptors[acceptorIndex].lexClass() ;
        return new LexToken (definite, theLexClass) ;
    } else if (c == EOF && maybe.equals("")) {
        // end of input already reached before nextToken was called
        reader.close() ;
        return null ;    // by convention, signifies end of input
    } else {
        reader.close() ;
        throw new LexError(maybe) ;
    }
    }

    public LexToken peekToken () 
    throws LexError, StateOutOfRange, IOException {
    if (bufferInUse) {
        return bufferToken ;
    } else {
        bufferToken = nextToken() ;
        bufferInUse = true ;
        return bufferToken ;
    }
    }

    public LexToken pullToken () 
    throws LexError, StateOutOfRange, IOException {
    peekToken () ;
    bufferInUse = false ;
    return bufferToken ;
    }

    public LexToken peekProperToken () 
    throws LexError, StateOutOfRange, IOException {
    LexToken tok = peekToken () ;
    while (tok != null && tok.lexClass().equals("")) {
        pullToken () ;
        tok = peekToken () ;
    }
    bufferToken = tok ;
    bufferInUse = true ;
    return tok ;
    }

    public LexToken pullProperToken () 
    throws LexError, StateOutOfRange, IOException {
    peekProperToken () ;
    bufferInUse = false ;
    return bufferToken ;
    }
}

// A simple example of a lexer using the DFAs constructed earlier

class DemoLexer extends GenLexer implements LEX_TOKEN_STREAM {

    static DFA evenLetterAcc = new EvenLetterAcceptor() ;
    static DFA andAcc = new AndAcceptor() ;
    static DFA spaceAcc = new SpaceAcceptor() ;
    static DFA[] acceptors = 
    new DFA[] {evenLetterAcc, andAcc, spaceAcc} ;
    DemoLexer (Reader reader) {
    super(reader,acceptors) ;
    }
}

// Interactive session for testing the above lexer.
// To try it out, compile this file, type 
//    java LexerDemo
// and then type a line of input such as
//    abcd &&&&
// You can also experiment with erroneous inputs.
// To quit the lexer, hit Ctrl-C.

class LexerDemo {
    
    public static void main (String[] args) 
    throws StateOutOfRange, IOException {
    BufferedReader consoleReader = new BufferedReader (new InputStreamReader (System.in)) ;
        while (0==0) {
        System.out.print ("Lexer> ") ;
            String inputLine = consoleReader.readLine() ;
            Reader lineReader = new BufferedReader (new StringReader (inputLine)) ;
            GenLexer demoLexer = new DemoLexer (lineReader) ;
            try {
            LexToken currTok = demoLexer.pullProperToken() ;
            while (currTok != null) {
                System.out.println (currTok.value() + " \t" + 
                             currTok.lexClass()) ;
                currTok = demoLexer.pullProperToken() ;
                }
            } catch (LexError x) {
        System.out.println ("Error: " + x.getMessage()) ;
            }
    } 
    }
}

class LexError extends Exception {
    public LexError (String nonToken) {
    super ("Can't make lexical token from input \"" + 
           nonToken + "\"") ;
    }
}
