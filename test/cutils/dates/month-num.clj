(ns cutils.dates.month-num
  (:use midje.sweet)
  (:require [cutils.dates :as dates]))

[[{:tag "month-num-usage" :title "Usage of <code>month->num</code>"}]]
^{:refer dates/month->num :added "1.0.0"}
(fact

  (dates/month->num       3)  => 3
  (dates/month->num      12)  => 12
  (dates/month->num       0)  => nil
  (dates/month->num      13)  => nil
  (dates/month->num       +)  => nil
  (dates/month->num     nil)  => nil
  (dates/month->num    "kw")  => 4
  (dates/month->num   "mar")  => 3
  (dates/month->num "marze")  => 3
  (dates/month->num   "may")  => 5
  (dates/month->num    :jan)  => 1
  (dates/month->num    'feb)  => 2
  (dates/month->num   [1 2])  => 12
  (dates/month->num [1 2 3])  => nil)

[[{:tag "month->num-usage-notfun" :title "Handling invalid values by <code>month->num</code>"}]]
^{:refer dates/month->num :added "1.0.0"}
(fact

  (dates/month->num        )  => (throws clojure.lang.ArityException)
  (dates/month->num nil nil)  => (throws clojure.lang.ArityException)
  (dates/month->num   1   1)  => (throws clojure.lang.ArityException))
