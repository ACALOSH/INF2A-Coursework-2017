
// File:   GenParser.java
// Date:   October 2013

// Java source file provided for Informatics 2A Assignment 1 (2013).
// Contains general infrastructure relating to syntax trees and LL(1) parsing,
// along with some trivial examples.

import java.io.* ;
import java.util.* ;

// Recursive class for syntax tree nodes (any grammar).
// The same class serves for both terminal and non-terminal nodes.

interface TREE {
    String getLabel() ;    // nonterminal symbol or lexical class of terminal
    boolean isTerminal() ;
    String getValue() ;    // only relevant for terminal nodes
    void setValue(String value) ;
    String[] getRhs() ;    // only relevant for non-terminal nodes
    TREE[] getChildren() ; // ditto
    void setRhsChildren(String[] rhs, TREE[] children) ; 
}

class STree implements TREE {
    
    String label ;        // Convention: nonterminals begin with "#".
    String value ;
    String[] rhs ;
    TREE[] children ;

    public String getLabel() {return label ;}
    public boolean isTerminal() {return (label.charAt(0) != '#') ;}
    public String getValue() {return value ;}
    public void setValue(String value) {this.value = value ;}
    public String[] getRhs() {return rhs ;}
    public TREE[] getChildren() {return children ;}
    public void setRhsChildren(String[] rhs, TREE[] children) {
    this.rhs = rhs ; this.children = children ;
    }

    // Constructors
    STree (String label) {this.label = label ;}
}

interface PARSER {
    TREE parseTokenStream (LEX_TOKEN_STREAM tokStream) 
    throws Exception ;
    TREE parseTokenStreamAs (LEX_TOKEN_STREAM tokStream, String nonterm) 
    throws Exception ;
}

abstract class GenParser implements PARSER {

    // Stubs for methods specific to a particular grammar
    abstract String startSymbol() ;
    abstract String[] tableEntry (String nonterm, String tokenType) ;
    // LL(1) parse table - should return null for blank entries.
    // In the second argument, null serves as the end-of-input marker '$'.

    // The LL(1) parsing algorithm, as in lectures

    public TREE parseTokenStream (LEX_TOKEN_STREAM tokStream) 
    throws Exception {
    return parseTokenStreamAs (tokStream, this.startSymbol()) ;
    }

    public TREE parseTokenStreamAs 
    (LEX_TOKEN_STREAM tokStream, String nonterm) 
    throws Exception {
    Stack<TREE> theStack = new Stack<TREE>() ;
    STree rootNode = new STree (nonterm) ;
    theStack.push(rootNode) ;
    STree currNode ;
    String currLabel ;
    LexToken currToken ;
    String currLexClass ;
    do {
        currNode = (STree)(theStack.pop()) ;
        currLabel = currNode.getLabel() ;
        currToken = tokStream.peekProperToken() ;
        if (currToken == null) {
        currLexClass = null ;
        } else {
        currLexClass = currToken.lexClass() ;
        } ;
        if (currNode.isTerminal()) {
        // match expected terminal against input token
        if (currLexClass != null && 
            currLexClass.equals(currLabel)) {
            // all OK
            currNode.setValue (currToken.value()) ;
            tokStream.pullToken() ;
        } else { // report error: expected terminal not found
            if (currToken == null) {
            throw new UnexpectedInput 
                (currLabel, "end of input") ;
            } else throw new UnexpectedInput
              (currLabel, currLexClass) ;
        } 
        } else { 
        // lookup expected nonterminal vs input token in table
        // OK if currLexClass is null (end-of-input marker)
        String[] rhs = tableEntry (currLabel, currLexClass) ;
        if (rhs != null) {
            STree[] children = new STree[rhs.length] ;
            for (int i=0; i<rhs.length; i++) {
            children[i] = new STree(rhs[i]) ;
            } ;
            currNode.setRhsChildren(rhs,children) ;
            for (int i=rhs.length-1; i>=0; i--) {
            theStack.push(children[i]) ;
            }
                } else if (currToken == null) {
                    throw new UnexpectedInput (currLabel, "end of input") ;
        } else {
            // report error: blank entry in table
            throw new UnexpectedInput (currLabel, currLexClass) ;
        }
        }
    } while (!theStack.empty()) ;
        LexToken next = tokStream.pullProperToken() ;
    if (next != null) {
        // non-fatal warning: parse completed before end of input
        System.out.println ("Warning: " + next.value() +
                " found after parse completed.");
    } else {
        System.out.println ("Parse successful.") ;
    } ;
    return rootNode ;
    }

    // Perhaps add method for parsing as a specified nonterminal
}

class UnexpectedInput extends Exception {
    public UnexpectedInput (String expected, String found) {
    super ("Parse error: " + found + " encountered where " + 
           expected + " expected.") ;
    }
}


// Tiny example: Parser for grammar
// #S -> epsilon | EVEN #S && #S
// Hint: read EVEN as (, && as ). 

class EvenAndParser extends GenParser implements PARSER {

    String startSymbol() {return "#S" ;}

    String[] epsilon      = new String[] { } ;
    String[] EVEN_S_AND_S = new String[] {"EVEN", "#S", "&&", "#S"} ;

    String[] tableEntry (String nonterm, String tokClass) {
    if (nonterm.equals("#S")) {
        if (tokClass == null) return epsilon ;
        else if (tokClass.equals("&&")) return epsilon ;
        else if (tokClass.equals("EVEN")) return EVEN_S_AND_S ;
        else return null ;
    } 
    else return null ;
    }
    // N.B. All this use of strings isn't great for efficiency,
    // but at least it makes for relatively readable code.
}

// For testing

class ParserDemo {

    static PARSER evenAndParser = new EvenAndParser() ;

    public static void main (String[] args) throws Exception {
    Reader reader = new BufferedReader (new FileReader (args[0])) ;
    GenLexer demoLexer = new DemoLexer (reader) ;
    TREE theTree = evenAndParser.parseTokenStream (demoLexer) ;
    }
}
