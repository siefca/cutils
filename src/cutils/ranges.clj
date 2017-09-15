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
  start and end values if they are lower or higher than the size of the given
  collection. After that it calls the function f with proper values passed as
  arguments. If three arguments are given the start argument is a number of
  collection's first elements that are going to be dropped.

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
         st (if (>= start co) co (if (< start 0) 0 start))]
     (f obj st)))
  ([^clojure.lang.Fn f
    obj
    ^java.lang.Number start
    ^java.lang.Number end]
   (if (>= start end)
     (f obj 0 0)
     (let [co (count obj)
           st (if (< start 0) 0 (if (>= start co) co start))
           en (if (>= end co) co (if (< end 0) 0 end))]
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
  cancelling each other out and fixes them if they're exceeding the
  boundaries. Positions are counted from 0 and are determining the range which
  is left-closed (first element pointed by start position is included) and
  right-open (last element pointed by end position is excluded) similarly to
  clojure.core/subs. If two arguments are given the start argument is a number
  of first elements to drop."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([^clojure.lang.ISeq s
    ^Number start]
   (drop start s))
  ([^clojure.lang.ISeq s
    ^Number start
    ^Number end]
   (if (>= start end)
     (take 0 s)
     (let [start (if (< start 0) 0 start)]
       (take (- end start) (drop start s))))))

(defn subseq-preserve
  "Takes a sequence s, a set of objects p and a range of elements expressed
  with start and (optional) end. Returns an empty sequence if positions given
  as start and end are cancelling each other out and fixes them if they're
  exceeding the boundaries. Positions are counted from 0 and are determining
  the range which is left-closed (first element pointed by start position is
  included) and right-open (last element pointed by end position is
  excluded), similarly to clojure.core/subs. If two arguments are given the
  start argument is a number of first elements to drop (excluding preserved
  element, if any).

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
   (if (empty? s)
     s
     (let [f (first s)]
       (if (contains? p f)
         (if-some [r (not-empty (safe-subseq (next s) start))]
           (cons f r) (take 0 s))
         (safe-subseq s start)))))
  ([^clojure.lang.ISeq           s
    ^clojure.lang.IPersistentSet p
    ^java.lang.Number        start
    ^java.lang.Number          end]
   (if (empty? s)
     s
     (let [f (first s)]
       (if (contains? p f)
         (if-some [r (not-empty (safe-subseq (next s) start end))]
           (cons f r) (take 0 s))
         (safe-subseq s start end))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; String operations

(def ^{:added "1.0.0"
       :arglists '([^String s, ^Number start]
                   [^String s, ^Number start, ^Number end])
       :tag String}
  safe-subs
  "Range-safe version of clojure.core/subs. Returns an empty string if positions
  given as start and end are cancelling each other out and fixes them if
  they're exceeding the boundaries. Positions are counted from 0 and are
  determining the range which is left-closed (first element pointed by start
  position is included) and right-open (last element pointed by end position
  is excluded) similarly to clojure.core/subs. If two arguments are given the
  start argument is a number of first elements to drop."
  (safe-range-fn subs))

(defn subs-preserve
  "Range-safe version of clojure.core/subs with first character
  preservation. Returns an empty string if positions given as start and end
  are cancelling each other out and fixes them if they're exceeding the
  boundaries. Positions are counted from 0 and are determining the range which
  is left-closed (first element pointed by start position is included) and
  right-open (last element pointed by end position is excluded) similarly to
  clojure.core/subs. If two arguments are given the start argument is a number
  of first elements to drop (excluding preserved character, if any).

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
   (if (empty? s)
     s
     (let [f (get s 0)]
       (if (contains? p f)
         (if-some [r (not-empty (safe-subs (subs s 1) start))]
           (str f r) (subs s 0 0))
         (safe-subs s start)))))
  ([^java.lang.String            s
    ^clojure.lang.IPersistentSet p
    ^java.lang.Number        start
    ^java.lang.Number          end]
   (if (empty? s)
     s
     (let [f (get s 0)]
       (if (contains? p f)
         (if-some [r (not-empty (safe-subs (subs s 1) start end))]
           (str f r) (subs s 0 0))
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
       :arglists '([^clojure.lang.IPersistentVector v, ^Number start]
                   [^clojure.lang.IPersistentVector v, ^Number start, ^Number end])
       :tag clojure.lang.IPersistentVector}
  safe-subvec
  "Range-safe version of clojure.core/subvec. Returns an empty vector if
  positions given as start and end are cancelling each other out and fixes
  them if they're exceeding the boundaries. Positions are counted from 0 and
  are determining the range which is left-closed (first element pointed by
  start position is included) and right-open (last element pointed by end
  position is excluded) similarly to clojure.core/subs. If two arguments are
  given the start argument is a number of first elements to drop."
  (safe-range-fn subvec))

(defn subvec-preserve
  "Range-safe version of clojure.core/subvec with first element
  preservation. Returns an empty vector if positions given as start and end
  are cancelling each other out and fixes them if they're exceeding the
  boundaries. Positions are counted from 0 and are determining the range which
  is left-closed (first element pointed by start position is included) and
  right-open (last element pointed by end position is excluded) similarly to
  clojure.core/subs. If two arguments are given the start argument is a number
  of first elements to drop (excluding preserved element, if any).

  Before generating new vector it memorizes original vector's first element if
  it matches one of the elements from a given set p. If there is no match the
  function works the same way as cutils.ranges/safe-subvec. If there is a
  match the function creates a subvector from all elements except first and
  then returns the result with the memorized element added to the beginning of
  a vector. It doesn't add the element if the resulting vector is empty.

  The function does not create new, empty vectors to collect parsed
  data. Internally it uses subvec and assoc on original structure."
  {:added "1.0.0"
   :tag clojure.lang.IPersistentVector}
  ([^clojure.lang.IPersistentVector v
    ^clojure.lang.IPersistentSet    p
    ^java.lang.Number           start]
   (if (empty? v)
     v
     (let [f (vec-first v)]
       (if (contains? p f)
         (if-some [r (not-empty (safe-subvec v start))]
           (if (contains? r 1) (assoc r 0 f) (empty v))
           (empty v))
         (safe-subvec v start)))))
  ([^clojure.lang.IPersistentVector v
    ^clojure.lang.IPersistentSet    p
    ^java.lang.Number           start
    ^java.lang.Number             end]
   (if (empty? v)
     v
     (let [f (vec-first v)]
       (if (contains? p f)
         (if-some [r (not-empty (safe-subvec v start (inc end)))]
           (if (contains? r 1) (assoc r 0 f) (empty v))
           (empty v))
         (safe-subvec v start end))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sliceable protocol

(defprotocol Sliceable
  "States that collection is sliceable (can be safely, without errors,
  sliced to sub-collecitons)."

  (sub
    [coll start] [coll start end]
    "Produces a subcollection of coll. If two arguments are given the
    resulting collection is a copy of the original collection with first
    elements removed (with their count specified by the start argument). If
    three arguments are given the last two specify a range which is
    left-closed (first element pointed by start position is included) and
    right-open (last element pointed by end position is excluded) similarly to
    clojure.core/subs.

    Returns an empty string if positions given as start and end are cancelling
    each other out. Increases or decreases the range if its out of boundaries.")

  (sub-preserve
    [coll to-keep start] [coll to-keep start end]
    "Produces a subcollection of coll preserving first element if its value
    belongs to a set passed as to-keep. If three arguments are given the
    resulting collection is a copy of the original collection with first
    elements removed (with their number specified by the start argument). If
    four arguments are given the last two specify a range which is
    left-closed (first element pointed by start position is included) and
    right-open (last element pointed by end position is excluded) similarly to
    clojure.core/subs.

    Returns an empty string if positions given as start and end are cancelling
    each other out. Increases or decreases the range if its out of boundaries."))

(extend-protocol Sliceable

  clojure.lang.IPersistentVector

  (sub
    ([^clojure.lang.IPersistentVector v, ^Number start]                                              (safe-subvec v start))
    ([^clojure.lang.IPersistentVector v, ^Number start, ^Number end]                                 (safe-subvec v start end)))
  (sub-preserve
    ([^clojure.lang.IPersistentVector v, ^clojure.lang.IPersistentSet p, ^Number start]              (subvec-preserve v p start))
    ([^clojure.lang.IPersistentVector v, ^clojure.lang.IPersistentSet p, ^Number start, ^Number end] (subvec-preserve v start end)))

  java.lang.String

  (sub
    ([^String s, ^Number start]                                                                     (safe-subs s start))
    ([^String s, ^Number start, ^Number end]                                                        (safe-subs s start end)))
  (sub-preserve
    ([^String s,  ^clojure.lang.IPersistentSet p, ^Number start]                                    (subs-preserve s start))
    ([^String s,  ^clojure.lang.IPersistentSet p, ^Number start, ^Number end]                       (subs-preserve s start end)))

  clojure.lang.ISeq

  (sub
    ([^clojure.lang.ISeq s, ^Number start]                                                           (safe-subseq s start))
    ([^clojure.lang.ISeq s, ^Number start, ^Number end]                                              (safe-subseq s start end)))
  (sub-preserve
    ([^clojure.lang.ISeq s, ^clojure.lang.IPersistentSet p, ^Number start]                           (subseq-preserve s start))
    ([^clojure.lang.ISeq s, ^clojure.lang.IPersistentSet p, ^Number start, ^Number end]              (subseq-preserve s start end)))

  nil

  (sub
    ([o start]       nil)
    ([o start end]   nil))
  (sub-preserve
    ([o p start]     nil)
    ([o p start end] nil)))

(defn sliceable? [coll]
  "Returns true if coll satisfies the Sliceable protocol."
  {:added "1.0.0"
   :tag Boolean}
  (satisfies? Sliceable coll))
