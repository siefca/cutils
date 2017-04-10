(ns cutils.digits.count_digits
  (:use midje.sweet)
  (:require [cutils.digits :as digits]))

[[{:tag "count-digits-usage" :title "Usage of <code>count-digits</code>"}]]
^{:refer digits/count-digits :added "1.0.0"}
(fact

  (digits/count-digits         123)  => 3
  (digits/count-digits       "123")  => 3
  (digits/count-digits       [123])  => 3
  (digits/count-digits     [1 2 3])  => 3
  (digits/count-digits      [-123])  => 3
  (digits/count-digits    [5 5 -5])  => 3
  (digits/count-digits [5 - 5 - 5])  => 3
  (digits/count-digits [5 nil 5 5])  => 3
  (digits/count-digits         "-")  => 0
  (digits/count-digits          [])  => 0
  (digits/count-digits         nil)  => 0
  (digits/count-digits          "")  => 0)

[[{:tag "count-digits-usage-ex" :title "Handling invalid values by <code>count-digits</code>"}]]
^{:refer digits/count-digits :added "1.0.0"}
(fact

  (digits/count-digits         +)  => (throws java.lang.IllegalArgumentException)
  (digits/count-digits        :a)  => (throws java.lang.IllegalArgumentException)
  (digits/count-digits "1 a 2 3")  => (throws java.lang.IllegalArgumentException)
  (digits/with-numeric-mode!
    (digits/count-digits [5 -5]))  => (throws java.lang.IllegalArgumentException))
