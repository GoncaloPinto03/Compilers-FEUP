grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LRECT : '[' ;
RRECT : ']' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;

CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

INTEGER : [0-9]+ ;
ID : [a-zA-Z0-9_$]+ ;

COMMENT_SINGLE : '//' ;
COMMENT_MULTI : '/*' | '*/' ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDeclaration)* classDeclaration EOF
    ;

importDeclaration
    : 'import' ID ( '.' ID )* ';'
    ;

classDeclaration
    : 'class' ID ( 'extends' ID )? LCURLY ( varDeclaration )* ( methodDeclaration )* RCURLY
    ;

varDeclaration
    : type ID ';'
    ;

methodDeclaration
    : ('public')? type ID LPAREN ( type ID ( ',' type ID )* )? RPAREN LCURLY ( varDeclaration )* ( statement )* 'return' expression ';' RCURLY
    | ('public')? 'static' 'void' 'main' LPAREN 'String' LRECT RRECT ID RPAREN LCURLY ( varDeclaration )* ( statement )* RCURLY
    ;

type
    : 'int' LRECT RRECT
    | 'int' '...'
    | 'boolean'
    | 'int'
    | ID
    ;

statement
    : LCURLY ( statement )* RCURLY
    | 'if' LPAREN expression RPAREN statement 'else' statement
    | 'while' LPAREN expression RPAREN statement
    | expression ';'
    | ID '=' expression ';'
    | ID LRECT expression RRECT '=' expression ';'
    ;

expression
    : expression ('&&' | '<' | '+' | '-' | '*' | '/' ) expression
    | expression LRECT expression RRECT
    | expression '.' 'length'
    | expression '.' ID LPAREN ( expression ( ',' expression )* )? RPAREN
    | 'new' 'int' LRECT expression RRECT
    | 'new' ID LPAREN RPAREN
    | '!' expression
    | LPAREN expression RPAREN
    | LRECT ( expression ( ',' expression )* )? RRECT
    | INT
    | 'true'
    | 'false'
    | ID
    | 'this'
    ;


