(ns

    ^{:doc    "cutils library, collections of digits."
      :author "Paweł Wilk"}

    cutils.digits

  (:require [cutils.core    :refer :all]
            [cutils.strings :refer :all]
            [cutils.ranges  :refer :all]
            [cutils.padding :refer :all]
            [clojure.edn       :as  edn]))

(cutils.core/init)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers

(defn- add-minus
  "Helper that adds minus sign to a collection coll if a number given as n is
  negative."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  [^Number n
   ^clojure.lang.ISeq coll]
  (if (neg? n) (cons \- coll) coll))

(defn- pad-digits-with-zeros
  "Helper that pads a sequence of digits with zeros."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  [^Number min-digits
   [^Number num-items
    ^clojure.lang.ISeq coll]]
  (pad coll (- min-digits num-items) 0 true))

(def digital-numbers
  ^{:added "1.0.0"
    :private true
    :const true
    :tag clojure.lang.IPersistentSet}
  #{java.lang.Byte
    java.lang.Short
    java.lang.Integer
    java.lang.Long
    clojure.lang.BigInt
    java.math.BigInteger
    java.math.BigDecimal})

(defn subs-signed
  "Safely creates a substring preserving its first character when it is a plus
  or a minus sign. Preservation means that that sign (if present in front of
  the given string) is memorized and prepended to the resulting substring
  unless that substring is empty."
  {:added "1.0.0"
   :tag String}
  ([^String     s
    ^Number start]
   (subs-preserve s sign-chars start))
  ([^String     s
    ^Number start
    ^Number   num]
   (subs-preserve s sign-chars start (+ start num))))

(defn subseq-signed
  "Safely creates a sequence preserving its first character when it is a plus
  or a minus sign. Preservation means that that sign (if present in front of
  the given string) is memorized and prepended to the resulting sequence
  unless that sequence is empty."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([^clojure.lang.ISeq    s
    ^Number start]
   (subseq-preserve s sign-chars start))
  ([^clojure.lang.ISeq    s
    ^Number start
    ^Number   num]
   (subseq-preserve s sign-chars start (+ start num))))

(defn subvec-signed
  "Safely creates subvector preserving its first element when it is a plus or
  a minus sign. Preservation means that the sign (if present in front of the
  given vector) is always memorized and prepended to the resulting vector
  unless that vector is empty."
  {:added "1.0.0"
   :tag clojure.lang.IPersistentVector}
  ([^clojure.lang.IPersistentVector v
    ^Number start]
   (subvec-preserve v sign-chars start))
  ([^clojure.lang.IPersistentVector v
    ^Number start
    ^Number   num]
   (subvec-preserve v sign-chars start (+ start num))))

(defn- fix-sign-seq
  "Removes plus character from the head of a sequential collection."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  [^clojure.lang.ISeq coll]
  (if (= \+ (first coll)) (next coll) coll))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Conversions

(def chars-to-digits
  ^{:added "1.0.0"
    :private true
    :tag clojure.lang.IPersistentMap
    :const true}
  (let [snum (range 0 10)
        nums (mapcat (partial repeat 2)  snum)
        strs (mapcat (juxt str identity) snum)
        syms (mapcat (juxt (comp symbol  str) identity) snum)
        kwds (mapcat (juxt (comp keyword str) identity) snum)
        chrc (mapcat (juxt (comp first   str) identity) snum)]
    (apply hash-map (concat nums strs syms kwds chrc))))

(defn num->digits
  "Changes a number given as n into a sequence of numbers representing decimal
  digits. If min-digits argument is given then it pads the returned sequence
  with leading zeros to satisfy the number of elements."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([^Number n]
   (loop [current n
          result ()]
     (if (zero? current)
       (add-minus n result)
       (recur (quot current 10)
              (cons (mod current 10) result)))))
  ([^Number min-digits
    ^Number n]
   (add-minus
    n (pad-digits-with-zeros
       min-digits
       (loop [current n
              result ()
              processed 0]
         (if (zero? current)
           (cons processed result)
           (recur (quot current 10)
                  (cons (mod current 10) result)
                  (inc processed)))))))
  ([^Number min-digits
    ^Number max-digits
    ^Number n]
   (add-minus
    n (pad-digits-with-zeros
       min-digits
       (loop [current n
              result ()
              processed 0]
         (if (or (zero? current) (>= processed max-digits))
           (cons processed result)
           (recur (quot current 10)
                  (cons (mod current 10) result)
                  (inc processed))))))))

(def big-seq-digits
  ^{:added "1.0.0"
    :const true
    :tag Long}
  (num->digits (Long/MAX_VALUE)))

(def big-seq-digits-count
  ^{:added "1.0.0"
    :const true
    :tag Long}
  (count big-seq-digits))

(defn seq-digits-big?
  {:added "1.0.0"
   :tag Boolean}
  [^clojure.lang.ISeq d]
  (>= (count d) big-seq-digits-count))



(defn- big-seq-digits->num
  "Warning: nil or false element ends iteration but that shouldn't be a
  problem in case of validated sequence with digits and optional sign."
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq d]
  (when-not (empty? d)
    (loop [x (reverse d) r 0N i 1N]
      (if (nil? x)
        r
        (recur (next x) (+' r (*' (first x) i)) (*' 10 i))))))

(defn- long-seq-digits->num
  "Warning: nil or false element ends iteration but that shouldn't be a
  problem in case of validated sequence with digits and optional sign."
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq d]
  (when-not (empty? d)
    (loop [x (reverse d) r (long 0) i (long 1)]
      (if (nil? x)
        r
        (recur (next x) (+ r (* (long (first x)) i)) (* 10 i))))))

(defn- seq-digits->num
  "Warning: nil or false element ends iteration but that shouldn't be a
  problem in case of validated sequence with digits and optional sign."
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq d]
  (if (seq-digits-big? d)
    (big-seq-digits->num d)
    (long-seq-digits->num d)))

(defn- vec-digits->num
  "Warning: nil or false element ends iteration but that shouldn't be a
  problem in case of validated vector with digits and optional sign."
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.IPersistentVector d]
  (when (contains? d 0)
    (loop [x d r 0N i 1N]
      (if-let [n (peek x)]
        (recur (pop x) (+' r (*' n i)) (*' 10 i))
        r))))

(defn- seq-digits->str
  {:added "1.0.0"
   :tag String}
  [^clojure.lang.ISeq s]
  (not-empty (reduce str s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Normalization and validation

(defn digital-number?
  "Returns true if the given number n can be used to express a collection of
  digits with optional sign. Returns false otherwise."
  {:added "1.0.0"
   :const true
   :tag java.lang.Boolean}
  [^Number n]
  (contains? digital-numbers (class n)))

(defn- digitize-seq
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  ([^clojure.lang.ISeq src]
   (fix-sign-seq (digitize-seq src false)))
  ([^clojure.lang.ISeq src
    ^Boolean had-number?]
   (when-not (empty? src)
     (let [e (first src)
           e (if (string? e) (pstr e) e)
           n (next src)]
       (if (whitechar? e)
         (recur n had-number?)
         (if-let [c (chars-to-digits e)]
           (cons c (lazy-seq (digitize-seq n true)))
           (if-let [c (s-to-chars e)]
             (if-not had-number?
               (cons c (lazy-seq (digitize-seq n true)))
               (throw-arg "The sign of a number should occur once and precede any digit"))
             (throw-arg "Sequence element is not a digit nor a sign: " e))))))))

(defn- digitize-vec
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  ([^clojure.lang.ISeq src]
   (fix-sign-seq (digitize-seq src false)))
  ([^clojure.lang.ISeq src
    ^Boolean had-number?]
   (when (contains? src 0)
     (let [e (get src 0)
           e (if (string? e) (pstr e) e)
           n (subvec src 1)]
       (if (whitechar? e)
         (recur n had-number?)
         (if-let [c (chars-to-digits e)]
           (cons c (lazy-seq (digitize-seq n true)))
           (if-let [c (s-to-chars e)]
             (if-not had-number?
               (cons c (lazy-seq (digitize-seq n true)))
               (throw-arg "The sign of a number should occur once and precede any digit"))
             (throw-arg "Sequence element is not a digit nor a sign: " e))))))))

(defn- digitize-str
  {:added "1.0.0"
   :tag String}
  [^String s]
  (when-some [s (remove-white-chars s)]
    (if (re-find #"^[+-]?\d+$" s)
      (not-empty (if (= \+ (get s 0)) (subs s 1) s))
      nil ;add some error handling
      )))

(defn- digitize-num
  "Changes number into normalized representation. Returns number or nil."
  {:added "1.0.0"
   :tag Number}
  [^Number n]
  (when (digital-number? n) n))

(def char-num-base
  "ASCII code of 0 character."
  ^{:added "1.0.0"
    :const true
    :tag java.lang.Integer}
  (int \0))

(defn- digitize-char
  "Changes numeric character into normalized representation. Returns character
  or nil."
  {:added "1.0.0"
   :tag Character}
  [^Character c]
  (when-some [c (chars-to-digits c)]
    (char (+ char-num-base c))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Digitizing protocol

(defprotocol Digitizing
  "States that collection is able to store digits that could be then used to
  produce valid numeric values."

  (digitize
   [coll]
   "Ensures that coll is digital by cleaning it and performing basic
  validation. If the process succeeded it returns cleaned version of coll,
  otherwise it returns nil. Digital means that the collection consist of
  numbers from 0 to 9 (as numbers) and optional + or - sign (as a character)
  in front.

  Normalization means that white characters are removed, digits (that might be
  characters, numbers, strings, symbols or keys) are changed to their
  numerical representations and first plus and minus signs (encoded as
  characters, strings, keywords, symbols or built-in function objects) are
  changed into characters.")

  (digits->num
   [coll] [coll num-take] [coll num-drop num-take]
   "Changes a collection of digits given as coll into integer number. An
  optional argument num-take controls how many digits to use (from left to
  right) and num-drop tells how many digits to drop before collecting
  number. The last one (num-drop) is applied before num-take when both are
  given.

  Before slicing the collection is normalized (white characters are removed
  and digits are changed into numerical representation) and validated (if the
  collection contains other characters operation is stopped and nil is
  returned). The first plus or minus character will not be taken into account
  during slicing.

  The function returns an integer or nil if something went wrong (e.g. empty
  collection was given or ranges were mismatched).")

  (digits->str
   [coll] [coll num-take] [coll num-drop num-take]
   "Changes a collection of digits given as coll into string containing
  integer number. An optional argument num-take controls how many digits to
  use (from left to right) and num-drop tells how many digits to drop before
  collecting number. The last one (num-drop) is applied before num-take when
  both are given.

  Before slicing the collection is normalized (white characters are removed
  and digits are changed into numerical representation) and validated (if the
  collection contains other characters operation is stopped and nil is
  returned). The first plus or minus character will not be taken into account
  during slicing.

  The function returns a string or nil if something went wrong (e.g. empty
  collection was given or ranges were mismatched).")

  (digits->seq
   [coll] [coll num-take] [coll num-drop num-take]
   "Changes a collection of digits given as coll into a sequence. An optional
  argument num-take controls how many digits to use (from left to right) and
  num-drop tells how many digits to drop before collecting number. The last
  one (num-drop) is applied before num-take when both are given.

  Before slicing the collection is normalized (white characters are removed
  and digits are changed into numerical representation) and validated (if the
  collection contains other characters operation is stopped and nil is
  returned). The first plus or minus character will not be taken into account
  during slicing.

  The function returns a sequence or nil if something went wrong (e.g. empty
  collection was given or ranges were mismatched)."))

(extend-protocol Digitizing

  clojure.lang.IPersistentVector

  (digitize
      [^clojure.lang.IPersistentVector  v]                         (not-empty (vec (digitize-vec v))))
  (digits->seq
      ([^clojure.lang.IPersistentVector v]                         (digitize-vec v))
    ([^clojure.lang.IPersistentVector   v, ^Number nt]             (subseq-signed (digitize-vec v) 0 nt))
    ([^clojure.lang.IPersistentVector   v, ^Number nd, ^Number nt] (subseq-signed (digitize-vec v) nd nt)))
  (digits->num
      ([^clojure.lang.IPersistentVector v]                         (seq-digits->num (digits->seq v)))
    ([^clojure.lang.IPersistentVector   v, ^Number nt]             (seq-digits->num (digits->seq v nt)))
    ([^clojure.lang.IPersistentVector   v, ^Number nd, ^Number nt] (seq-digits->num (digits->seq v nd nt))))
  (digits->str
      ([^clojure.lang.IPersistentVector v]                         (seq-digits->str (digits->seq v)))
    ([^clojure.lang.IPersistentVector   v, ^Number nt]             (seq-digits->str (digits->seq nt)))
    ([^clojure.lang.IPersistentVector   v, ^Number nd, ^Number nt] (seq-digits->str (digits->seq v nd nt))))

  clojure.lang.ISeq

  (digitize
      [^clojure.lang.ISeq s]                                       (digitize-seq s))
  (digits->seq
      ([^clojure.lang.ISeq s]                                      (digitize-seq s))
    ([^clojure.lang.ISeq   s, ^Number nt]                          (subseq-signed (digitize-seq s) 0 nt))
    ([^clojure.lang.ISeq   s, ^Number nd, ^Number nt]              (subseq-signed (digitize-seq s) nd nt)))
  (digits->num
      ([^clojure.lang.ISeq s]                                      (seq-digits->num (digits->seq s)))
    ([^clojure.lang.ISeq   s, ^Number nt]                          (seq-digits->num (digits->seq s nt)))
    ([^clojure.lang.ISeq   s, ^Number nd, ^Number nt]              (seq-digits->num (digits->seq s nd nt))))
  (digits->str
      ([^clojure.lang.ISeq s]                                      (seq-digits->str (digits->seq s)))
    ([^clojure.lang.ISeq   s, ^Number nt]                          (seq-digits->str (digits->seq s nt)))
    ([^clojure.lang.ISeq   s, ^Number nd, ^Number nt]              (seq-digits->str (digits->seq s nd nt))))

  java.lang.String

  (digitize
      [^String  s]                            (digitize-str s))
  (digits->seq
      ([^String s]                            (seq (digitize-str s)))
    ([^String   s, ^Number nt]                (subseq-signed (digitize-str s) 0 nt))
    ([^String   s, ^Number nd, ^Number nt]    (subseq-signed (digitize-str s) nd nt)))
  (digits->str
      ([^String s]                            (digitize-str s))
    ([^String   s, ^Number nt]                (subs-signed (digitize-str s) 0 nt))
    ([^String   s, ^Number nd, ^Number nt]    (subs-signed (digitize-str s) nd nt)))
  (digits->num
      ([^String s]                            (edn/read-string (digits->str s)))
    ([^String   s, ^Number nt]                (edn/read-string (digits->str s nt)))
    ([^String   s, ^Number nd, ^Number nt]    (edn/read-string (digits->str s nd nt))))

  java.lang.Character

  (digitize
      [^Character  c]                         (digitize-char c))
  (digits->seq
      ([^Character c]                         (seq (str (digitize-char c))))
    ([^Character   c, ^Number nt]             (subseq-signed (str (digitize-char c)) 0 nt))
    ([^Character   c, ^Number nd, ^Number nt] (subseq-signed (str (digitize-char c)) nd nt)))
  (digits->str
      ([^Character c]                         (str (digitize-char c)))
    ([^Character   c, ^Number nt]             (subs-signed (str (digitize-char c)) 0 nt))
    ([^Character   c, ^Number nd, ^Number nt] (subs-signed (str (digitize-char c)) nd nt)))
  (digits->num
      ([^Character c]                         (Integer/parseInt (digitize-char c)))
    ([^Character   c, ^Number nt]             (Integer/parseInt (digits->str c nt)))
    ([^Character   c, ^Number nd, ^Number nt] (Integer/parseInt (digits->str c nd nt))))

  java.lang.Number

  (digitize
      [^Number  n]                            (digitize-num n))
  (digits->seq
      ([^Number n]                            (num->digits (digitize-num n)))
    ([^Number   n, ^Number nt]                (subseq-signed (num->digits (digitize-num n)) 0 nt))
    ([^Number   n, ^Number nd, ^Number nt]    (subseq-signed (num->digits (digitize-num n)) nd nt)))
  (digits->num
      ([^Number n]                            (digitize-num n))
    ([^Number   n, ^Number nt]                (digits->num (digits->seq n nt)))
    ([^Number   n, ^Number nd, ^Number nt]    (digits->num (digits->seq n nd nt))))
  (digits->str
      ([^Number n]                            (digitize-str (str n)))
    ([^Number n, ^Number nt]                  (subs-signed (digitize-str (str n)) nt))
    ([^Number n, ^Number nd, ^Number nt]      (subs-signed (digitize-str (str n)) nd nt)))

  nil

  (digitize [d] nil)

  (digits->seq
      ([d]     nil)
    ([d nt]    nil)
    ([d nd nt] nil))

  (digits->num
      ([d]     nil)
    ([d nt]    nil)
    ([d nd nt] nil))

  (digits->str
      ([d]     nil)
    ([d nt]    nil)
    ([d nd nt] nil)))

(defn digital?
  "Checks if a given object is digital. Returns true if it is, false
  otherwise. Digital means that the collection, string or a numeric type
  object consist of numbers from 0 to 9 and optional + or - sign in front."
  [obj]
  (some? (digitize obj)))
