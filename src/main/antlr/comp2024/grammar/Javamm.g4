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
    : LCURLY ( statement )* RCURLY #BRACKETS
    | 'if' LPAREN expression RPAREN statement 'else' statement #ConditionStm
    | 'while' LPAREN expression RPAREN statement #ConditionStm
    | 'for' '(' statement expression ';' expression ')' statement #FOR_STM
    | expression ';' #EXPR_STM
    | var=ID '=' expression ';' #ASSIGNMENT_STM
    | var=ID LRECT expression RRECT '=' expression ';' #ARRAY_ASSIGNMENT_STM
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
    | expression op=('*' | '/') expression #BinaryExpr
    | expression op=('+' | '-') expression #BinaryExpr
    | expression op=('<' | '>') expression #BinaryExpr
    | expression op=('==' | '!=' | '<=' | '>=' | '+=' | '-=' | '*=' | '/=') expression #BinaryExpr
    | expression op=('&&' | '||') expression #BinaryExpr
    | LRECT ( expression ( ',' expression )* )? RRECT #ArrayLiteral
    | value=INT #IntegerLiberal
    | name = ID #VarRefExpr
    | value='false' #Identifier
    | value='true' #Identifier
    | value=ID op=('++' | '--') #Increment
    ;
