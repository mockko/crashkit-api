<?php
  function distance($vect1, $vect2) 
  {
      if (!is_array($vect1) || !is_array($vect2)) {
          trigger_error("Incorrect parameters, arrays expected", E_USER_ERROR);
          return NULL;
      }

      if (count($vect1) != count($vect2)) {
          trigger_error("Vectors need to be of the same size", E_USER_ERROR);
          return NULL;
      }

      for ($i=0; $i<count($vect1); $i++) {
          $c1 = $vect1[$i]; $c2 = $vect2[$i];
          $d = 0.0;
          if (!is_numeric($c1)) {
              trigger_error("Coordinate $i in vector 1 is not a number, using zero", 
                              E_USER_WARNING);
              $c1 = 0.0;
          }
          if (!is_numeric($c2)) {
              trigger_error("Coordinate $i in vector 2 is not a number, using zero", 
                              E_USER_WARNING);
              $c2 = 0.0;
          }
          $d += $c2*$c2 - $c1*$c1;
      }
      return sqrt($d);
  }

  // undefined constant, generates a warning
  $t = I_AM_NOT_DEFINED;

  // define some "vectors"
  $a = array(2, 3, "foo");
  $b = array(5.5, 4.3, -1.6);
  $c = array(1, -3);

  // generate a warning
  $t3 = distance($a, $b) . "\n";

  // generate a user error
  $t1 = distance($c, $b) . "\n";

  // generate another user error
  $t2 = distance($b, "i am not an array") . "\n";

?>