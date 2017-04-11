(ns cutils.digits.digital
  (:use midje.sweet)
  (:require [cutils.digits :refer :all]))

[[{:tag "digital-usage" :title "Usage of <code>digital?</code>"}]]
^{:refer digital? :added "1.0.0"}
(fact

  (digital?           0)  => true
  (digital?         123)  => true
  (digital?        -123)  => true
  (digital?       "123")  => true
  (digital?        :123)  => true
  (digital?       [123])  => true
  (digital?     [1 2 3])  => true
  (digital?      [-123])  => true
  (digital?    [5 5 -5])  => true
  (digital? [5 - 5 - 5])  => true
  (digital? [5 nil 5 5])  => true
  (digital?          :a)  => false
  (digital?         "-")  => false
  (digital?           +)  => false
  (digital?          [])  => false
  (digital?         nil)  => false
  (digital?          "")  => false)
