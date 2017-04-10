(ns cutils.digits.digital
  (:use midje.sweet)
  (:require [cutils.digits :as digits]))

[[{:tag "digital-usage" :title "Usage of <code>digital?</code>"}]]
^{:refer digits/digital? :added "1.0.0"}
(fact

  (digits/digital?           0)  => true
  (digits/digital?         123)  => true
  (digits/digital?        -123)  => true
  (digits/digital?       "123")  => true
  (digits/digital?        :123)  => true
  (digits/digital?       [123])  => true
  (digits/digital?     [1 2 3])  => true
  (digits/digital?      [-123])  => true
  (digits/digital?    [5 5 -5])  => true
  (digits/digital? [5 - 5 - 5])  => true
  (digits/digital? [5 nil 5 5])  => true
  (digits/digital?          :a)  => false
  (digits/digital?         "-")  => false
  (digits/digital?           +)  => false
  (digits/digital?          [])  => false
  (digits/digital?         nil)  => false
  (digits/digital?          "")  => false)
