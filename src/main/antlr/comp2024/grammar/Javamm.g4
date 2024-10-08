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
    : (importDeclaration)* classDecl EOF #ProgramDeclaration
    ;

importDeclaration
    : 'import' importValue+=ID ('.' importValue+=ID )* ';' #ImportDecl
    ;

classDecl
    : 'class' name=(ID | 'main' | 'length')  ( 'extends' sname=ID )? LCURLY ( varDeclaration )* ( methodDecl )* RCURLY #ClassDeclaration
    ;

varDeclaration
    : type name = (ID|'main'|'length') ';' #VarDecl
    ;

returnStatement
    : 'return' expression ';' #ReturnStmt
    ;

methodDecl
    : (isPublic='public')? (isStatic='static')? type name=(ID | 'main' | 'length')  LPAREN ( param ( ',' param )* )? RPAREN LCURLY ( varDeclaration )* ( statement )* returnStatement RCURLY #MethodDeclaration
    | ('public')? (isStatic='static')?  type name='main' LPAREN mainParam aname=ID RPAREN LCURLY ( varDeclaration )* ( statement )* RCURLY #MethodDeclaration
    ;


param:
    type name=(ID | 'main' | 'length') #ParamDeclaration
    ;

mainParam:
    'String' LRECT RRECT #MainParamDeclaration
    ;

type locals [boolean isArray = false]
    : value='int' ('['{$isArray = true;}']')? #VARIABLE_INT   // variable number of integers
    | value='int' '...'{$isArray=true;} #VARARG //??
    | value='boolean'      #BOOL                        // Boolean
    | value='double'       #DOUBLE                     // Double
    | value='float'        #FLOAT             // Float
    | value='String'      #STRING                      // string
    | value='char'        #CHAR               // char
    | value='byte'        #BYTE                 // byte
    | value='short'       #SHORT                 // short
    | value='long'        #LONG                      // Long
    | value='void'        #VOID                 // Void
    | value=(ID | 'main' | 'length')            #ID                      // Id
    | value = 'int' #INT
    ;

statement
    : LCURLY ( statement )* RCURLY  #BRACKETS
    | 'if' LPAREN expression RPAREN statement 'else' statement #IfStm
    | 'while' LPAREN expression RPAREN statement #WhileStm
    | 'for' '(' statement expression ';' expression ')' statement #FOR_STM
    | expression ';' #exprStmt
    | var=ID LRECT expression RRECT '=' expression ';' #arrayAssign
    | expression '=' expression ';' #assignStmt
    ;

expression
    : LPAREN expression RPAREN  #Parentesis
    | 'new' 'int' LRECT expression RRECT #ArrayDeclaration
    | 'new' value=(ID | 'main' | 'length') LPAREN (expression (',' expression) *)? RPAREN  #NewClass
    | expression LRECT expression RRECT #arrayAccess
    | expression '.' value=(ID | 'main' | 'length')  LPAREN (expression (',' expression)*)? RPAREN #MethodCall
    | expression '.' 'length' #Length
    | 'this' #This
    | value = '!' expression #Negation
    | expression op=('*' | '/') expression #BinaryExpr
    | expression op=('+' | '-') expression #BinaryExpr
    | expression op=('<' | '>') expression #BinaryExpr
    | expression op=('==' | '!=' | '<=' | '>=' | '+=' | '-=' | '*=' | '/=') expression #BinaryExpr
    | expression op='&&' expression #BinaryExprAnd
    | LRECT ( expression ( ',' expression )* )? RRECT # ArrayLiteral
    | value=INT #IntegerLiteral
    | value='true' #Identifier
    | value='false' #Identifier
    | value=ID op=('++' | '--') #Increment
    | name = (ID | 'main' | 'length') #VarRefExpr
    ;
