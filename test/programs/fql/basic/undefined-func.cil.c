/* Generated by CIL v. 1.3.7 */
/* print_CIL_Input is true */

#line 1 "undefined-func.c"
extern void *malloc(unsigned int  ) ;
#line 3
extern int f(int x , int y ) ;
#line 13 "undefined-func.c"
int bla(void) 
{ int x ;

  {
  {
#line 15
  x = f(x, x);
  }
#line 16
  return (x);
}
}
#line 19 "undefined-func.c"
int main(int argc , char **argv ) 
{ int x ;
  int y ;
  char *a ;
  void *tmp ;
  char *__cil_tmp7 ;
  char *__cil_tmp8 ;
  char __cil_tmp9 ;
  int __cil_tmp10 ;

  {
  {
#line 23
  tmp = malloc(10U);
#line 23
  a = (char *)tmp;
#line 24
  __cil_tmp7 = a + 0;
#line 24
  *__cil_tmp7 = (char )'b';
  }
  {
#line 27
  __cil_tmp8 = *argv;
#line 27
  __cil_tmp9 = *__cil_tmp8;
#line 27
  __cil_tmp10 = (int )__cil_tmp9;
#line 27
  if (__cil_tmp10 == 98) {
#line 27
    x = 5;
  } else {

  }
  }
#line 28
  if (argc > 5) {
#line 28
    x = 2;
  } else {
#line 28
    x = 1;
  }
#line 29
  if (argc > 27) {
#line 29
    x = x + 1;
  } else {
#line 29
    x = x - 1;
  }
  {
#line 30
  bla();
  }
#line 31
  return (y);
}
}