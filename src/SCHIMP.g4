grammar SCHIMP;

// schimp program
program : (cmdinitial ';')* (cmdnew ';')* cmdfunction (';' cmdfunction)* ';' cmdinvoke ;

// commands
cmdinitial : 'initial' IDENTIFIER ':=' (pmf | aexp) ;
cmdfunction : 'function' IDENTIFIER '(' (varnamelist)? ')' '{' cmdlist (';' cmdoutput)? '}' ;
cmdoutput : 'output' aexplist ;
cmdnew : 'new' IDENTIFIER ':=' (pmf | aexp) ;
cmdassign : IDENTIFIER ':=' (pmf | aexp) ;
cmdinvoke : IDENTIFIER '(' (aexplist)? ')' ;
cmdskip : 'skip' ;
cmdif : 'if' '(' bexp ')' '{' cmdlist '}' ('else' '{' cmdlist '}')? ;
cmdwhile : 'while' '(' bexp ')' '{' cmdlist '}' ;

cmd : cmdnew    # New
    | cmdassign # Assign
    | cmdinvoke # Invoke
    | cmdskip   # Skip
    | cmdif     # If
    | cmdwhile  # While
    ;

// arithmetic expressions
aexp : '(' aexp ')'    # AexpParens
     | aexp '*' aexp   # AexpMultiply
     | aexp '/' aexp   # AexpDivide
     | aexp '+' aexp   # AexpAdd
     | aexp '-' aexp   # AexpSubtract
     | aexp 'mod' aexp # AexpModulo
     | aexp 'xor' aexp # AexpXor
     | IDENTIFIER      # AexpVarname
     | INTEGER         # AexpConst
     ;

// boolean expressions
bexp : '(' bexp ')'             # BexpParens
     | <assoc=right> 'not' bexp # BexpNot
     | bexp 'and' bexp          # BexpAnd
     | bexp 'or' bexp           # BexpOr
     | aexp '==' aexp           # BexpEquals
     | aexp '<' aexp            # BexpLess
     | aexp '>' aexp            # BexpGreater
     | BOOLEAN                  # BexpConst
     ;

// probability mass functions
pmf : '{' aexp '->' aexp (',' aexp '->' aexp)* '}' ;

// integers
INTEGER : ('-')? [0-9]+ ;

// booleans
BOOLEAN : 'true'
        | 'false'
        ;

// variable/function names
IDENTIFIER : ([A-Za-z_])([A-Za-z0-9_])* ;

// list of arithmetic expressions
aexplist : aexp (',' aexp)* ;

// list of variable names
varnamelist : IDENTIFIER (',' IDENTIFIER)* ;

// list of commands
cmdlist : cmd (';' cmd)* ;

// skip comments
COMMENT :  '#' ~('\r' | '\n')*
        -> skip
        ;

// skip whitespace
WHITESPACE : [ \t\r\n]+
           -> skip
           ;
