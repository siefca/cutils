(ns cutils.digits.decimal-mark
  (:use midje.sweet)
  (:require [cutils.digits :as digits]))

[[{:tag "decimal-mark-usage" :title "Usage of <code>decimal-mark?</code>"}]]
^{:refer digits/decimal-mark? :added "1.0.0"}
(fact

  (digits/decimal-mark?  \.)  => true
  (digits/decimal-mark? ".")  => true
  (digits/decimal-mark?  :.)  => true
  (digits/decimal-mark?  '.)  => true
  (digits/decimal-mark?  \,)  => true
  (digits/decimal-mark?   +)  => false
  (digits/decimal-mark?   -)  => false
  (digits/decimal-mark? nil)  => false
  (digits/decimal-mark? 123)  => false)
