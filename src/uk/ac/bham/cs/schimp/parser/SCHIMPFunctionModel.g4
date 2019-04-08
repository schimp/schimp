grammar SCHIMPFunctionModel;

// list of function models
functionmodellist : (functionmodel (';' functionmodel)*)? ;

// function model
functionmodel : 'model' IDENTIFIER '/' INTEGER ':=' aconstlistmap ;

// mapping of lists of arithmetic constants to time/power probability mass functions
aconstlistmap : '{' aconstlist '->' tptupleexp (',' aconstlist '->' tptupleexp)* '}' ;

// list of arithmetic constants
aconstlist : '(' (aconst (',' aconst)*)? ')' ;

tptupleexp : pmf
           | tptuple
           ;

// time/power probability mass functions
pmf : '{' tptuple '->' rational (',' tptuple '->' rational)* '}' ;

// arithmetic constants (including '_' for "all arithmetic constants")
aconst : rational # AconstRational
       | '_'      # AconstAny
       ;

// time/power consumption tuple
tptuple : '(' INTEGER ',' INTEGER ')' ;

// function names
IDENTIFIER : ([A-Za-z_])([A-Za-z0-9_])* ;

// integers
INTEGER : ('-')? [0-9]+ ;

// rational number expressions
rational : '(' rational ')'      # RationalParens
         | rational '*' rational # RationalMultiply
         | rational '/' rational # RationalDivide
         | rational '+' rational # RationalAdd
         | rational '-' rational # RationalSubtract
         | INTEGER               # RationalInteger
         ;

// skip comments
COMMENT :  '#' ~('\r' | '\n')*
        -> skip
        ;

// skip whitespace
WHITESPACE : [ \t\r\n]+
           -> skip
           ;