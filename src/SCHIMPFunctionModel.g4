grammar SCHIMPFunctionModel;

// function model
functionmodel : IDENTIFIER '/' NUMBER ':=' aconstmap ;

// mapping of lists of arithmetic constants to time/power probability mass functions
aconstmap : '{' aconstlist '->' (pmf | tptuple) (',' aconstlist '->' (pmf | tptuple))* '}' ;

// list of arithmetic constants
aconstlist : '(' aconst (',' aconst)* ')' ;

// time/power probability mass functions
pmf : '{' tptuple '->' NUMBER (',' tptuple '->' NUMBER)* '}' ;

// time/power consumption tuple
tptuple : '(' NUMBER ',' NUMBER ')' ;

// function names
IDENTIFIER : ([A-Za-z_])([A-Za-z0-9_])* ;

// floating-point numbers/integers
NUMBER : ('-')? [0-9]+ ('.' [0-9]+)? ;

// arithmetic constants (including '_' for "all arithmetic constants")
aconst : NUMBER
       | '_'
       ;

// skip comments
COMMENT :  '#' ~('\r' | '\n')*
        -> skip
        ;

// skip whitespace
WHITESPACE : [ \t\r\n]+
           -> skip
           ;