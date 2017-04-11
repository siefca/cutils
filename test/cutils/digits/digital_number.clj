(ns cutils.digits.digital_number
  (:use midje.sweet)
  (:require [cutils.digits :refer :all]))

[[{:tag "digital-number-usage" :title "Usage of <code>digital-number?</code>"}]]
^{:refer digital-number? :added "1.0.0"}
(fact

  (digital-number?           0)  => true
  (digital-number?         123)  => true
  (digital-number?        -123)  => true
  (digital-number?       -123M)  => true
  (digital-number?       -123N)  => true
  (digital-number?    (byte 1))  => true
  (digital-number?     (int 1))  => true
  (digital-number?    (long 1))  => true
  (digital-number?    2.235555)  => true
  (digital-number? 3.14000000M)  => true
  (digital-number?         1/2)  => false
  (digital-number?          :a)  => false
  (digital-number?         "-")  => false
  (digital-number?           +)  => false
  (digital-number?          [])  => false
  (digital-number?         nil)  => false
  (digital-number?          "")  => false)
