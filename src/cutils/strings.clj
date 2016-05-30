(ns

    ^{:doc    "cutils, string handling functions."
      :author "Paweł Wilk"}

    cutils.strings

  (:require [cutils.core]
            [clojure.string :as s]))

(cutils.core/init)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Character sets definitions

(def whitechars
  "Common blank characters."
  ^{:added "1.0.0"
    :private true
    :tag clojure.lang.IPersistentSet
    :const true}
  #{nil \space \newline \tab \formfeed \return (char 0x0B)})

(defn whitechar?
  {:added "1.0.0"
   :tag Boolean}
  [c]
  (contains? whitechars c))

(def s-to-chars
  "Map of plus and minus signs to characters."
  ^{:added "1.0.0"
    :private true
    :tag clojure.lang.IPersistentMap
    :const true}
  {- \-, :- \-, '- \-, "-" \-, \- \-
   + \+, :+ \+, '+ \+, "+" \+, \+ \+})

(def sign-chars
  "Set of plus and minus signs."
  ^{:added "1.0.0"
    :private true
    :tag clojure.lang.IPersistentSet
    :const true}
  (set (keys s-to-chars)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cleaning strings

(defn pstr
  "Cleans up string given as its first argument by removing preceding and following white characters.
  Returns the string or nil."
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
    (some->> s pstr
             (re-find #"[+-]?(\d|\d\.\d)+[MN]?") not-empty
             clojure.edn/read-string)))

(defn str->int
  "Converts a string to an integer. Returns nil if something went wrong."
  {:added "1.0.0"
   :tag Integer}
  [^String s]
  (some-> str->num int))
