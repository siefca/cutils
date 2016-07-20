(ns

    ^{:doc    "cutils, safe ranges."
      :author "Paweł Wilk"}

    cutils.ranges

  (:require [cutils.core]
            [clojure.string :as s]))

(cutils.core/init)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Range helpers

(defn safe-range
  "Range-safe wrapper for functions that require ranges. It fixes the given
  start and end values if they are lower or higher than a size of the given
  object. After that it calls the function f with proper values passed as
  arguments.

  The function f must be able to take 2 or 3 arguments: object, start of
  a range and (optionally) end of a range. The given object must be
  countable. Function returns the result of calling f.

  Warning: Some data structures (e.g. lazy sequences) are countable but it
  requires to pass through (and often calculate values of) all their elements
  in order to do that. This is not a problem in case of small structures and
  sequences that will all be traversed eventually; but be warned about
  possible performance impact."
  {:added "1.0.0"}
  ([^clojure.lang.Fn f
    obj
    ^java.lang.Number start]
   (let [co (count obj)
         st (if (> start co) co (if (< start 0) 0 start))]
     (f obj st)))
  ([^clojure.lang.Fn f
    obj
    ^java.lang.Number start
    ^java.lang.Number end]
   (if (>= start end)
     (f obj 0 0)
     (let [co (count obj)
           st (if (> start co) co (if (< start 0) 0 start))
           en (if (> end   co) co end)]
       (f obj st en)))))

(defn safe-range-fn
  "Transforms the given function f in a way that it uses safe-range
  wrapper. Returns a function."
  {:added "1.0.0"
   :tag clojure.lang.Fn}
  [^clojure.lang.Fn f]
  (fn
    ([obj, ^java.lang.Number start]
     (safe-range f obj start))
    ([obj, ^java.lang.Number start, ^java.lang.Number end]
     (safe-range f obj start end))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sequential operations

(defn drop-take
  "Calls (drop d coll) and then (take t …) on a result of that call."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  [^clojure.lang.ISeq coll
   ^java.lang.Number     d
   ^java.lang.Number     t]
  (take t (drop d coll)))

(defn safe-subseq
  "Returns an empty sequence if positions given as start and end are
  cancelling each other out. It fixes the positions if they are lower or
  higher than the size of the given sequence.

  Be aware that if end is not given it will be counted using the count
  function which might be inefficient for some data structures."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([^clojure.lang.ISeq s
    ^Number start]
   (safe-subseq s start (count s)))
  ([^clojure.lang.ISeq s
    ^Number start
    ^Number end]
   (take (- end start) (drop start s))))

(defn subseq-preserve
  "Takes a sequence s, a set of objects p and a range of elements expressed
  with start and end. Returns an empty sequence if positions given as start
  and end are cancelling each other out.

  Before returning new sequence it memorizes first element of the given
  sequence if it matches one of the elements from p. If there is no match the
  function works the same way as cutils.ranges/safe-subseq. If there is a
  match the function creates a sequence using ranges but without the first
  element. Then it returns the result of slicing with the memorized character
  added to the beginning of it. It doesn't add the element if the result is
  empty."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([^clojure.lang.ISeq           s
    ^clojure.lang.IPersistentSet p
    ^java.lang.Number        start]
   (subseq-preserve s p start (dec (count s))))
  ([^clojure.lang.ISeq           s
    ^clojure.lang.IPersistentSet p
    ^java.lang.Number        start
    ^java.lang.Number          end]
   (if (empty? s)
     s
     (let [f (first s)]
       (if (contains? p f)
         (let [r (safe-subseq s (inc start) (inc end))]
           (if (empty? r) r (cons f r)))
         (safe-subseq s start end))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; String operations

(def ^{:added "1.0.0"
       :tag String}
  safe-subs
  "Range-safe version of clojure.core/subs. It returns an empty string if
  positions given as start and end are cancelling each other out. It fixes the
  positions if they are lower or higher than the size of the given string."
  (safe-range-fn subs))

(defn subs-preserve
  "Range-safe version of clojure.core/subs with first character
  preservation. It returns an empty string if positions given as start and end
  are cancelling each other out. It fixes the positions if they are lower or
  higher than a size of the given string.

  Before generating substring it memorizes original string's first character
  if it matches one of the characters from a given set p. If there is no match
  the function works the same way as cutils.ranges/safe-subs. If there is a
  match the function creates substring from all characters except first and
  then returns the result with the memorized character added to the beginning
  of a string. It doesn't add the character if the resulting substring is
  empty."
  {:added "1.0.0"
   :tag java.lang.String}
  ([^java.lang.String            s
    ^clojure.lang.IPersistentSet p
    ^java.lang.Number        start]
   (subs-preserve s p start (dec (count s))))
  ([^java.lang.String            s
    ^clojure.lang.IPersistentSet p
    ^java.lang.Number        start
    ^java.lang.Number          end]
   (if (empty? s)
     s
     (let [f (first s)]
       (if (contains? p f)
         (let [r (safe-subs s (inc start) (inc end))]
           (if (empty? r) r (str f r)))
         (safe-subs s start end))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Vector operations

(defn vec-first
  "Gets first element of a vector. Returns the element or nil."
  {:added "1.0.0"}
  [^clojure.lang.IPersistentVector v]
  (get v 0))

(defn vec-find-idx
  "Returns index of first element of a vector v for which pred returns
  truth (not nil and not false). Returns nil if no matching element was
  found. If the start argument is given then it starts from that position."
  {:added "1.0.0"
   :tag java.lang.Number}
  ([^clojure.lang.Fn             pred
    ^clojure.lang.IPersistentVector v
    ^java.lang.Number           start]
   (when-let [n (vec-find-idx pred (subvec v start))]
     (+ start n)))
  ([^clojure.lang.Fn             pred
    ^clojure.lang.IPersistentVector v]
   (loop [r v n 0]
     (if-let [el (find r 0)]
       (if (pred (peek el)) n (recur (subvec r 1) (inc n)))
       nil))))

(def ^{:added "1.0.0"
       :tag clojure.lang.IPersistentVector}
  safe-subvec
  "Range-safe version of clojure.core/subvec. It returns an empty object of
  the same type as v if positions given as start and end are cancelling each
  other out. It fixes the positions if they are lower or higher than a size of
  the given vector."
  (safe-range-fn subvec))

(defn subvec-preserve
  "Range-safe version of clojure.core/subvec with first element
  preservation. It returns an empty vector if positions given as start and end
  are cancelling each other out. It fixes the positions if they are lower or
  higher than a size of the given vector.

  Before generating new vector it memorizes original vector's first element if
  it matches one of the elements from a given set p. If there is no match the
  function works the same way as cutils.ranges/safe-subvec. If there is a match
  the function creates a subvector from all elements except first and then
  returns the result with the memorized element added to the beginning of a
  vector. It doesn't add the element if the resulting vector is empty.

  The function does not create empty vectors to collect parsed
  data. Internally it uses subvec and assoc on original structure."
  {:added "1.0.0"
   :tag clojure.lang.IPersistentVector}
  ([^clojure.lang.IPersistentVector v
    ^clojure.lang.IPersistentSet    p
    ^java.lang.Number           start]
   (subvec-preserve v p start (count v)))
  ([^clojure.lang.IPersistentVector v
    ^clojure.lang.IPersistentSet    p
    ^java.lang.Number           start
    ^java.lang.Number             end]
   (if (or (>= start end) (not (contains? v 0)))
     (empty v)
     (let [f (get v 0)]
       (if (contains? p f)
         (let [last-idx (dec (count v))
               start (if (< start 1) 0 start)]
           (if (or (> start last-idx) (and (= start last-idx) (> end last-idx)))
             (empty v)
             (safe-subvec (assoc v start f) start (inc end))))
         (safe-subvec v start end))))))
