(ns cutils.digits.digits_num
  (:use midje.sweet)
  (:require [cutils.digits :refer :all]))

[[{:tag "digits-num-usage" :title "Usage of <code>digits->num</code>"}]]
^{:refer digits->num :added "1.0.0"}
(fact

  (digits->num         123)  => 123
  (digits->num       "123")  => 123
  (digits->num       [123])  => 123
  (digits->num     [1 2 3])  => 123
  (digits->num      [+123])  => 123
  (digits->num      [-123])  => -123
  (digits->num   [- 1 2 3])  => -123
  (digits->num   [+ 1 2 3])  => 123
  (digits->num [5 nil 5 5])  => 555
  (digits->num         "+")  => nil
  (digits->num          [])  => nil
  (digits->num         nil)  => nil
  (digits->num          "")  => nil)

[[{:tag "digits->num-usage-ex" :title "Handling invalid values by <code>digits->num</code>"}]]
^{:refer digits->num :added "1.0.0"}
(fact

  (digits->num         +)  => (throws java.lang.IllegalArgumentException)
  (digits->num        :a)  => (throws java.lang.IllegalArgumentException)
  (digits->num "1 a 2 3")  => (throws java.lang.IllegalArgumentException)


  (with-numeric-mode!
    (digits->num [5 -5]))  => (throws java.lang.IllegalArgumentException))
