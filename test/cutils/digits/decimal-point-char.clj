(ns cutils.digits.decimal-point-char
  (:use midje.sweet)
  (:require [cutils.digits :as digits]))

[[{:tag "decimal-point-char-usage" :title "Usage of <code>decimal-point-char?</code>"}]]
^{:refer digits/decimal-point-char? :added "1.0.0"}
(fact

  (digits/decimal-point-char?  \.)  => true
  (digits/decimal-point-char? ".")  => true
  (digits/decimal-point-char?  :.)  => true
  (digits/decimal-point-char?  '.)  => true
  (digits/decimal-point-char?  \,)  => true
  (digits/decimal-point-char?   +)  => false
  (digits/decimal-point-char?   -)  => false
  (digits/decimal-point-char? nil)  => false
  (digits/decimal-point-char? 123)  => false)

[[{:tag "decimal-point-char-usage-notfun" :title "Handling invalid values by <code>decimal-point-char?</code>"}]]
^{:refer digits/decimal-point-char? :added "1.0.0"}
(fact

  (digits/decimal-point-char?        )  => (throws clojure.lang.ArityException)
  (digits/decimal-point-char? nil nil)  => (throws clojure.lang.ArityException)
  (digits/decimal-point-char?   \. \.)  => (throws clojure.lang.ArityException))
