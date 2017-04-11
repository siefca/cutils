(ns cutils.digits.decimal_mark
  (:use midje.sweet)
  (:require [cutils.digits :refer :all]))

[[{:tag "decimal-mark-usage" :title "Usage of <code>decimal-mark?</code>"}]]
^{:refer decimal-mark? :added "1.0.0"}
(fact

  (decimal-mark?  \.)  => true
  (decimal-mark? ".")  => true
  (decimal-mark?  :.)  => true
  (decimal-mark?  '.)  => true
  (decimal-mark?  \,)  => true
  (decimal-mark?   +)  => false
  (decimal-mark?   -)  => false
  (decimal-mark? nil)  => false
  (decimal-mark? 123)  => false)
