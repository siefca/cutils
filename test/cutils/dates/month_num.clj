(ns cutils.dates.month_num
  (:use midje.sweet)
  (:require [cutils.dates :refer :all]))

[[{:tag "month-num-usage" :title "Usage of <code>month->num</code>"}]]
^{:refer month->num :added "1.0.0"}
(fact

  (month->num       3)  => 3
  (month->num      12)  => 12
  (month->num       0)  => nil
  (month->num      13)  => nil
  (month->num       +)  => nil
  (month->num     nil)  => nil
  (month->num    "kw")  => 4
  (month->num   "mar")  => 3
  (month->num "marze")  => 3
  (month->num   "may")  => 5
  (month->num    :jan)  => 1
  (month->num    'feb)  => 2
  (month->num   [1 2])  => 12
  (month->num [1 2 3])  => nil)
