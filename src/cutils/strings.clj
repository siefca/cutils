(ns

    ^{:doc    "cutils, string handling functions."
      :author "Paweł Wilk"}

    cutils.strings

  (:require [cutils.core]
            [clojure.string :as   s]
            [clojure.edn    :as edn]))

(cutils.core/init)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Character sets definitions

(def ^{:added "1.0.0"
       :private true
       :tag clojure.lang.IPersistentSet
       :const true}
  whitechars
  "Common blank characters."
  #{nil \space \newline \tab \formfeed \return (char 0x0B)})

(defn whitechar?
  {:added "1.0.0"
   :tag Boolean}
  [c]
  (contains? whitechars c))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cleaning strings

(defn str-trim
  "Cleans up string given as its first argument by removing preceding and following white characters,
  including tabs, new lines and carriage returns. Returns the string or nil (if a resulting string
  is empty)."
  [^String s]
  (some-> s str (s/replace #"(^\s+)|(\s+$)" "") not-empty))

(defn remove-white-chars
  "Removes all white characters from the given string."
  {:added "1.0.0"
   :tag String}
  [^String s]
  (s/replace s #"\s" ""))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; String conversion

(defn str->num
  "Converts a string to a number. Returns nil if something went wrong."
  {:added "1.0.0"
   :tag Number}
  [^String s]
  (if (number? s)
    s
    (some->> s str-trim
             (re-find #"[+-]?(\d|\d\.\d)+[MN]?")
             not-empty
             edn/read-string)))

(defn str->int
  "Converts a string to an integer. Returns nil if something went wrong."
  {:added "1.0.0"
   :tag Integer}
  [^String s]
  (some-> str->num int))
