grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';       // equal
SEMI : ';' ;        // semicolon
LCURLY : '{' ;      // left curly brackets
RCURLY : '}' ;      // rigth curly brackets
LPAREN : '(' ;      // left round brackets
RPAREN : ')' ;      // rigth round brackets
LRECT : '[' ;       // left square brackets
RRECT : ']' ;       // rigth square brackets
MUL : '*' ;         // multiplication
DIV : '/' ;         // division
ADD : '+' ;         // addition
SUB : '-' ;         // subtraction

IF : 'if' ;         // if
ELSE : 'else' ;     // else
WHILE : 'while' ;   // while

CLASS : 'class' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

INT : [0] | ([1-9][0-9]*) ; //integer
ID : [a-zA-Z_$][a-zA-Z$0-9_]*;   // id

COMMENT_SINGLE : '//' .*? '\n' -> skip ;    // single line comment
COMMENT_MULTI : '/*' .*? '*/' -> skip ;     // multi line comment

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDeclaration)* classDecl EOF
    ;

importDeclaration
    : 'import' importValue+=ID ('.' importValue+=ID )* ';'
    ;

classDecl
    : 'class' name=ID ( 'extends' sname=ID )? LCURLY ( varDeclaration )* ( methodDecl )* RCURLY
    ;

varDeclaration
    : type ('main' | name=ID) ';'
    ;

methodDecl
    : ('public')? type name=ID LPAREN ( param ( ',' param )* )? RPAREN LCURLY ( varDeclaration )* ( statement )* 'return' expression ';' RCURLY
    | ('public')? 'static'  type name='main' LPAREN 'String' LRECT RRECT aname=ID RPAREN LCURLY ( varDeclaration )* ( statement )* RCURLY
    ;

param:
    type name=ID
    ;

type locals [boolean isArray = false]
    : value='int' ('['{$isArray = true;}']')?   // variable number of integers
    | value='int' ('...')?
    | value='boolean'                           // Boolean
    | value='double'                            // Double
    | value='float'                             // Float
    | value='String'                            // string
    | value='char'                              // char
    | value='byte'                              // byte
    | value='short'                             // short
    | value='long'                              // Long
    | value='void'                              // Void
    | value=ID                                  // Id
    ;

statement
    : LCURLY ( statement )* RCURLY
    | 'if' LPAREN expression RPAREN statement 'else' statement
    | 'while' LPAREN expression RPAREN statement
    | 'for' '(' statement expression ';' expression ')' statement
    | expression ';'
    | var=ID '=' expression ';'
    | var=ID LRECT expression RRECT '=' expression ';'
    ;

expression
    : LPAREN expression RPAREN  #Parentesis
    | 'new' 'int' LRECT expression RRECT #ArrayDeclaration
    | 'new' classname=ID LPAREN (expression (',' expression) *)? RPAREN  #NewClass
    | expression LRECT expression RRECT #arrayAccess
    | expression '.' value=ID LPAREN (expression (',' expression) *)? RPAREN #FunctionCall
    | expression '.' 'length' #Length
    | value = 'this' #Object
    | value = '!' expression #Negation
    | expression op=('*' | '/') expression #binaryExpr
    | expression op=('+' | '-') expression #binaryExpr
    | expression op=('<' | '>') expression #binaryExpr
    | expression op=('==' | '!=' | '<=' | '>=' | '+=' | '-=' | '*=' | '/=') expression #binaryExpr
    | expression op=('&&' | '||') expression #binaryExpr
    | className=ID expression # Constructor
    | LRECT ( expression ( ',' expression )* )? RRECT # ArrayLiteral
    | value=INT #IntegerLiberal
    | value='true' #Identifier
    | value='false' #Identifier
    | value=ID #Identifier
    | value=ID op=('++' | '--') #Increment
    | name = ID #VarRefExpr
    ;


//grammar Javamm;
//
//@header {
//    package pt.up.fe.comp2024;
//}
//
//EQUALS : '=';
//SEMI : ';' ;
//LCURLY : '{' ;
//RCURLY : '}' ;
//LSQUARE : '[' ;
//RSQUARE : ']' ;
//LPAREN : '(' ;
//RPAREN : ')' ;
//MUL : '*' ;
//DIV : '/' ;
//SUB : '-' ;
//ADD : '+' ;
//LESS : '<' ;
//AND : '&&' ;
//
//CLASS : 'class' ;
//NEW : 'new' ;
//
//INT : 'int' ;
//BOOL : 'boolean' ;
//STRING : 'String' ;
//VOID : 'void' ;
//FLOAT : 'float' ;
//DOUBLE : 'double' ;
//LENGTH : 'length' ;
//
//MAIN : 'main' ;
//PUBLIC : 'public' ;
//STATIC : 'static' ;
//RETURN : 'return' ;
//
//INTEGER : [0-9] | [1-9][0-9]* ;
//ID : [a-zA-Z_$] [a-zA-Z_$0-9]* ;
//
//SINGLE_LINE_COMMENT : '//' .*? '\n' -> skip ;
//MULTI_LINE_COMMENT : '/*' .*? '*/' -> skip ;
//WS : [ \t\n\r\f]+ -> skip ;
//
//program
//    : (importDeclaration)* classDecl EOF
//    ;
//
//
//importDeclaration
//    : 'import' name=ID ('.'ID)* SEMI
//    ;
//
//classDecl
//    : CLASS name=ID ('extends' sup=ID)?
//        LCURLY
//        (varDecl)*
//        (methodDecl)*
//        RCURLY
//    ;
//
//
//varDecl
//    : type name=ID SEMI
//    | type name=MAIN SEMI
//    | type name=ID LSQUARE RSQUARE SEMI
//    ;
//
//methodDecl locals[boolean isPublic=false]
//    : (PUBLIC {$isPublic=true;})? type name=ID LPAREN (param (',' param)*)? RPAREN LCURLY varDecl* stmt* RCURLY #AuxMethod
//    | (PUBLIC {$isPublic=true;})? 'static' 'void' name='main' LPAREN 'String[]' name=ID RPAREN LCURLY varDecl* stmt* RCURLY #MainMethod
//    ;
//
//type locals[boolean isArray=false]
//    : name=INT
//    | name=INT'...'
//    | name=INT LSQUARE RSQUARE {$isArray=true;}
//    | name=ID
//    | name=BOOL
//    | name=FLOAT
//    | name=DOUBLE
//    | name=STRING
//    | name=VOID
//    ;
//
//param
//    : type name=ID
//    ;
//
//stmt
//    : expr SEMI #SingleExp
//    | LCURLY (stmt)* RCURLY #OneOrMoreStmt
//    | 'if' LPAREN expr RPAREN stmt 'else' stmt #IfStmt
//    | 'while' LPAREN expr RPAREN stmt #WhileStmt
//    | 'for' LPAREN expr SEMI expr SEMI expr RPAREN stmt #ForStmt
//    | ID EQUALS expr SEMI #AssignStmt //
//    | ID LSQUARE expr RSQUARE EQUALS expr SEMI #AssignArrayStmt
//    | RETURN expr SEMI #ReturnStmt
//    ;
//
//expr
//    : expr op= (MUL | DIV) expr #BinaryExpr
//    | expr op=(ADD | SUB) expr #BinaryExpr
//    | expr op=AND expr #BinaryExpr
//    | expr op= LESS expr #BinaryExpr
//    | expr LSQUARE expr RSQUARE #ArrayAccess
//    | expr '.' LENGTH #ArrayLength
//    | expr '.' ID LPAREN (expr (',' expr)*)? RPAREN #MethodCall
//    | NEW INT LSQUARE expr RSQUARE #NewArray
//    | NEW name=ID LPAREN RPAREN #NewFunction
//    | '!' expr #Not
//    | LPAREN expr RPAREN #ParenExpr
//    | LSQUARE (expr (',' expr)*)? RSQUARE #ArrayLiteral
//    | INTEGER #IntegerLiteral
//    | name=ID #VarRefExpr
//    | 'true' #True
//    | 'false' #False
//    | name='this' #This
//    ;