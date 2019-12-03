import java.io.* ;

class MH_Lexer extends GenLexer implements LEX_TOKEN_STREAM {

 static class VarAcceptor extends Acceptor implements DFA {
    public String lexClass() {return "VAR" ;} ;
    public int numberOfStates() {return 3 ;} ;

    int next (int state, char c) {
    switch (state) {
    //case 0 checks whether char is small if not, goes to dead state
    case 0: if (CharTypes.isSmall(c)) return 1 ; else return 2 ;
    // case 1 checks if char is a letter or digit if not, goes to dead state
    case 1: if (CharTypes.isLetter(c)|| CharTypes.isDigit(c) || c == '\'') return 1 ; else return 2 ;
        default: return 2 ; // garbage state, declared "dead" below
    }
    }

    boolean accepting (int state) {return (state == 1) ;}
    int dead () {return 2 ;}
}


 static class NumAcceptor extends Acceptor implements DFA {
    public String lexClass() {return "NUM" ;} ;
    public int numberOfStates() {return 4 ;} ;

    int next (int state, char c) {
    switch (state) {
    //case 0 checks whether char is not a digit, if its not a digit, it goes to dead state, if it is, it checks weather its a 0, if it is, then goes to case 1, if not 0, goes to 2.
    case 0: if (!CharTypes.isDigit(c)) return 3 ; else if (c== '0') return 1; else return 2;
    //if theres any input after the 0, case 1 returns to the dead state.
    case 1: return 3;
    //case 2 makes sure that if anything other than a digit is inputed, it returns to dead state
    case 2: if (!(CharTypes.isDigit(c))) return 3 ; else return 2 ;
    default: return 3 ;
    }
    }
    boolean accepting (int state) {return (state == 2 || state == 1) ;}
    int dead () {return 3 ;}
}


 static class BooleanAcceptor extends Acceptor implements DFA {
     public String lexClass() {return "BOOLEAN" ;} ;
        public int numberOfStates() {return 9 ;} ;

        int next (int state, char c) {
        switch (state) {
        //checks char by char weather the string spells true or false
        case 0: if (c == 'T') return 1 ; else if ( c == 'F') return 2 ; else return 8;
        case 1: if (c == 'r') return 7 ; else return 8 ;
        case 2: if (c == 'a') return 3 ; else return 8 ;
        case 3: if (c == 'l') return 4 ; else return 8 ;
        case 4: if (c == 's') return 5; else return 8 ;
        case 5: if (c == 'e') return 6 ; else return 8 ;
        case 6: return 8 ;
        case 7: if (c == 'u') return 5 ; else return 8 ;
            default: return 8; // garbage state, declared "dead" below
        }
        }

        boolean accepting (int state) {return (state == 6) ;}
        int dead () {return 8;}
}

 static class SymAcceptor extends Acceptor implements DFA {
    public String lexClass() {return "SYM" ;} ;
    public int numberOfStates() {return 3 ;} ;

    int next (int state, char c) {
    switch (state) {
    // both cases check whether char is a symbol
    case 0: if (CharTypes.isSymbolic(c)) return 1 ; else return 2 ;
    case 1: if (CharTypes.isSymbolic(c))return 1 ; else return 2 ;
        default: return 2 ; // garbage state, declared "dead" below
    }
    }

    boolean accepting (int state) {return (state == 1) ;}
    int dead () {return 2 ;}
}

 static class WhitespaceAcceptor extends Acceptor implements DFA {
    public String lexClass() {return "" ;} ;
    public int numberOfStates() {return 3 ;} ;

    int next (int state, char c) {
    switch (state) {
    //both cases check whether char is a whitespace
    case 0: if (CharTypes.isWhitespace(c)) return 1 ; else return 2 ;
    case 1: if (CharTypes.isWhitespace(c))return 1 ; else return 2 ;
        default: return 2 ; // garbage state, declared "dead" below
    }
    }

    boolean accepting (int state) {return (state == 1) ;}
    int dead () {return 2 ;}
}

 static class CommentAcceptor extends Acceptor implements DFA {
    public String lexClass() {return "" ;} ;
    public int numberOfStates() {return 5 ;} ;

    int next (int state, char c) {
    switch (state) {
    // first 2 cases make sure that there is a -- for every comment
    case 0: if (c == '-') return 1 ; else return 4 ;
    case 1: if (c == '-') return 2 ; else return 4 ;
    //case 3 makes sure that if there are more -, that they are accepted as a comment and if not -, it makes sure that char isnt a newline symbol or a symbol
    case 2: if (c == '-') return 2 ; else if (!CharTypes.isNewline(c) && !CharTypes.isSymbolic(c)) return 3; else return 4;
    //case 4 makes sure the char isnt a new line symbol
    case 3: if (!CharTypes.isNewline(c)) return 3; else return 4;
        default: return 4 ; // garbage state, declared "dead" below
    }
    }
    boolean accepting (int state) {return (state == 2 || state == 3) ;}
    int dead () {return 4;}
}

 static class TokAcceptor extends Acceptor implements DFA {

    String tok ;
    int tokLen ;
    TokAcceptor (String tok) {this.tok = tok ; tokLen = tok.length() ;}

    //sets lex class as tok
    public String lexClass() {return tok ;} ;
    public int numberOfStates() {return (tokLen +2) ;} ;

    int next (int state, char c) {
        // makes sure if any char is added to the accepting state or dead state, it goes to dead state
        if(state>tokLen-1) {return tokLen+1;}
        //using indexing to check whether c is the same as the char at state/index
        else if (tok.charAt(state) == c )return state+1;
        else return (tokLen +1);
    }

    boolean accepting (int state) {return (state == tokLen) ;}
    int dead () {return tokLen +1;}

}


    static DFA VarAcceptor = new VarAcceptor() ;
    static DFA NumAcceptor = new NumAcceptor() ;
    static DFA BooleanAcceptor = new BooleanAcceptor() ;
    static DFA SymAcceptor = new SymAcceptor();
    static DFA WhitespaceAcceptor = new WhitespaceAcceptor();
    static DFA CommentAcceptor = new CommentAcceptor();
    static DFA IntAcceptor = new TokAcceptor("Integer");
    static DFA BoolAcceptor = new TokAcceptor("Bool");
    static DFA IfAcceptor = new TokAcceptor("if");
    static DFA ThenAcceptor = new TokAcceptor("then");
    static DFA ElseAcceptor = new TokAcceptor("else");
    static DFA ClosedBracAcc = new TokAcceptor(")");
    static DFA OpenBracAcc = new TokAcceptor("(");
    static DFA SemicolAcc = new TokAcceptor(";");
    static DFA [] MHacceptors = new DFA[] {CommentAcceptor,WhitespaceAcceptor, IfAcceptor,OpenBracAcc,
            ClosedBracAcc,ThenAcceptor,SemicolAcc,ElseAcceptor,BoolAcceptor,IntAcceptor,BooleanAcceptor,SymAcceptor, VarAcceptor,NumAcceptor} ;

            // add definitions of MH_acceptors here

    MH_Lexer (Reader reader) {
    super(reader,MHacceptors) ;
    }
}
    class MHLexerDemo {

        public static void main (String[] args)
        throws StateOutOfRange, IOException {
        BufferedReader consoleReader = new BufferedReader (new InputStreamReader (System.in)) ;
            while (0==0) {
            System.out.print ("Lexer> ") ;
                String inputLine = consoleReader.readLine() ;
                Reader lineReader = new BufferedReader (new StringReader (inputLine)) ;
                GenLexer demoLexer = new MH_Lexer (lineReader) ;
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
