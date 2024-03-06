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
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

INTEGER : [0-9]+ ;      // integer
ID : [a-zA-Z0-9_$]+ ;   // id

COMMENT_SINGLE : ('//')+ -> skip ;          // single line comment
COMMENT_MULTI : ('/*' | '*/')+ -> skip ;    // multi line comment

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDeclaration)* classDeclaration EOF
    ;

importDeclaration
    : 'import' importValue+=ID ('.' importValue+=ID )* ';'
    ;

classDeclaration
    : 'class' ID ( 'extends' ID )? LCURLY ( varDeclaration )* ( methodDeclaration )* RCURLY
    ;

varDeclaration
    : type ID ';'
    ;

methodDeclaration
    : ('public')? type mname+=ID LPAREN ( type pname+=ID ( ',' type pname+=ID )* )? RPAREN LCURLY ( varDeclaration )* ( statement )* 'return' expression ';' RCURLY
    | ('public')? 'static' 'void' 'main' LPAREN 'String' LRECT RRECT name+=ID RPAREN LCURLY ( varDeclaration )* ( statement )* RCURLY
    ;

type
    : type LRECT RRECT          // array
    | value='int' '...'         // variable number of integers
    | value='boolean'           // boolean
    | value='int'               // integer
    | value='double'            // double
    | value='float'             // float
    | value='String'            // string
    | value='char'              // char
    | value='byte'              // byte
    | value='short'             // short
    | value='long'              // long
    | value=ID                  // id
    ;

statement
    : LCURLY ( statement )* RCURLY
    | 'if' LPAREN expression RPAREN statement 'else' statement
    | 'while' LPAREN expression RPAREN statement
    | expression ';'
    | var=ID '=' expression ';'
    | var=ID LRECT expression RRECT '=' expression ';'
    ;

expression
    : expression op=('*' | '/') expression
    | expression op=('+' | '-') expression
    | expression op=('<' | '>') expression
    | expression op=('==' | '!=' | '<=' | '>=' | '+=' | '-=' | '*=' | '/=') expression
    | expression op=('&&' | '||') expression
    | 'new' 'int' LRECT expression RRECT
    | 'new' classname=ID LPAREN (expression (',' expression) *)? RPAREN
    | expression LRECT expression RRECT
    | className=ID expression
    | expression '.' value=ID LPAREN (expression (',' expression) *)? RPAREN
    | expression '.' 'length'
    | value = '!' expression
    | LPAREN expression RPAREN
    | LRECT ( expression ( ',' expression )* )? RRECT
    | value=INTEGER
    | value='true'
    | value='false'
    | value=ID
    | value = 'this'
    | value=ID op=('++' | '--')
    ;
