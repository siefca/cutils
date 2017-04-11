(ns cutils.digits.digitize
  (:use midje.sweet)
  (:require [cutils.digits :refer :all]))

[[{:tag "digitize-usage" :title "Usage of <code>digitize</code>"}]]
^{:refer digitize :added "1.0.0"}
(fact

  (digitize           0)  => 0
  (digitize         123)  => 123
  (digitize        -123)  => -123
  (digitize       "123")  => "123"
  (digitize        :123)  => :123
  (digitize     [1 2 3])  => [1 2 3]


  ;; spreading numbers
  (digitize       [123])  => [1 2 3]

  ;; separators preservation (generic mode)
  (digitize   [+ 1 2 3])  => [\+ 1 2 3]
  (digitize      [-123])  => [\- 1 2 3]
  (digitize      [+123])  => [1 2 3]    ;; + here is a part of number literal!
  (digitize    [5 5 -5])  => [5 5 \- 5]
  (digitize   "+123.75")  => "+123.75"
  (digitize  "+1.23.75")  => "+1.23.75"

  ;; blank values removal
  (digitize   [5 nil 5])  => [5 5]

  ;; only separators
  (digitize         "-.") => "-."

  ;; nil punning
  (digitize         nil)  => nil
  (digitize          [])  => nil
  (digitize          "")  => nil

  ;; numeric mode behavior
  (with-numeric-mode!
    (digitize [+ 1 2 3])  => [1 2 3]
    (digitize    [-123])  => [\- 1 2 3]
    (digitize    [+123])  => [1 2 3]
    (digitize "+123.75")  => "123.75"))

[[{:tag "digitize-usage-ex" :title "Handling invalid input by <code>digitize</code>"}]]
^{:refer digitize :added "1.0.0"}

(fact
  (digitize          :a)  => (throws java.lang.IllegalArgumentException)
  (digitize           +)  => (throws java.lang.IllegalArgumentException)
  (digitize     "1 2 x")  => (throws java.lang.IllegalArgumentException)
  (digitize    [1 2 :x])  => (throws java.lang.IllegalArgumentException)


  (without-spread-numbers!
   (digitize      [123])  => (throws java.lang.IllegalArgumentException)
   (digitize     '(123))  => (throws java.lang.IllegalArgumentException))

  (with-numeric-mode!
    (digitize  [1 2 -3])  => (throws java.lang.IllegalArgumentException)
    (digitize   [1 - 2])  => (throws java.lang.IllegalArgumentException)
    (digitize [- - 1 2])  => (throws java.lang.IllegalArgumentException)
    (digitize "1.23.75")  => (throws java.lang.IllegalArgumentException)

    (without-decimal-mark-mode!
     (digitize "123.75")  => (throws java.lang.IllegalArgumentException))))
