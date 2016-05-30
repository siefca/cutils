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
  "Removes plus character from the head of a sequential collection. If the
  first element of that collection is plus or minus character and second is
  not a number then it returns nil."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  [^clojure.lang.ISeq coll]
  (let [first-char  (first  coll)
        second-char (second coll)]
    (not-empty
     (cond
       (number? first-char) coll
       (= \+    first-char) (if (number? second-char) (next coll) nil)
       (= \-    first-char) (if (number? second-char) coll nil)
       :default nil))))

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

(defn- digitalize-seq
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  ([^clojure.lang.ISeq src]
   (fix-sign-seq (digitalize-seq src false)))
  ([^clojure.lang.ISeq src
    ^Boolean had-number?]
   (when-not (empty? src)
     (let [e (first src)
           e (if (string? e) (pstr e) e)
           n (next src)]
       (if (whitechar? e)
         (recur n had-number?)
         (if-let [c (chars-to-digits e)]
           (cons c (lazy-seq (digitalize-seq n true)))
           (if-let [c (s-to-chars e)]
             (if-not had-number?
               (cons c (lazy-seq (digitalize-seq n true)))
               (throw-arg "The sign of a number should occur once and precede any digit"))
             (throw-arg "Sequence element is not a digit nor a sign: " e))))))))

(defn- digitalize-vec
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  ([^clojure.lang.ISeq src]
   (fix-sign-seq (digitalize-seq src false)))
  ([^clojure.lang.ISeq src
    ^Boolean had-number?]
   (when (contains? src 0)
     (let [e (get src 0)
           e (if (string? e) (pstr e) e)
           n (subvec src 1)]
       (if (whitechar? e)
         (recur n had-number?)
         (if-let [c (chars-to-digits e)]
           (cons c (lazy-seq (digitalize-seq n true)))
           (if-let [c (s-to-chars e)]
             (if-not had-number?
               (cons c (lazy-seq (digitalize-seq n true)))
               (throw-arg "The sign of a number should occur once and precede any digit"))
             (throw-arg "Sequence element is not a digit nor a sign: " e))))))))

(defn- digitalize-str
  {:added "1.0.0"
   :tag String}
  [^String s]
  (when-some [s (remove-white-chars s)]
    (if (re-find #"^[+-]?\d+$" s)
      (not-empty (if (= \+ (get s 0)) (subs s 1) s))
      nil ;add some error handling
      )))

(defn- digitalize-num
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

(defn- digitalize-char
  "Changes numeric character into normalized representation. Returns character
  or nil."
  {:added "1.0.0"
   :tag Character}
  [^Character c]
  (when-some [c (chars-to-digits c)]
    (char (+ char-num-base c))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Digitalizing protocol

(defprotocol Digitalizing
  "States that collection is able to store digits that could be then used to
  produce valid numeric values."

  (digitalize
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

(extend-protocol Digitalizing

  clojure.lang.IPersistentVector

  (digitalize
      [^clojure.lang.IPersistentVector  v] (not-empty (vec (digitalize-vec v))))

  (digits->seq
      ([^clojure.lang.IPersistentVector v] (digitalize-vec v))
    ([^clojure.lang.IPersistentVector v
      ^Number num-take]                    (subseq-signed (digitalize-vec v) 0 num-take))
    ([^clojure.lang.IPersistentVector v
      ^Number num-drop
      ^Number num-take]                    (subseq-signed (digitalize-vec v) num-drop num-take)))

  (digits->num
      ([^clojure.lang.IPersistentVector v] (seq-digits->num (digits->seq v)))
    ([^clojure.lang.IPersistentVector v
      ^Number num-take]                    (seq-digits->num (digits->seq v num-take)))
    ([^clojure.lang.IPersistentVector v
      ^Number num-drop
      ^Number num-take]                    (seq-digits->num (digits->seq v num-drop num-take))))

  (digits->str
      ([^clojure.lang.IPersistentVector v] (seq-digits->str (digits->seq v)))
    ([^clojure.lang.IPersistentVector v
      ^Number num-take]                    (seq-digits->str (digits->seq num-take)))
    ([^clojure.lang.IPersistentVector v
      ^Number num-drop
      ^Number num-take]                    (seq-digits->str (digits->seq v num-drop num-take))))

  clojure.lang.ISeq

  (digitalize
      [^clojure.lang.ISeq s]               (digitalize-seq s))

  (digits->seq
      ([^clojure.lang.ISeq s]              (digitalize-seq s))
    ([^clojure.lang.ISeq s
      ^Number num-take]                    (subseq-signed (digitalize-seq s) 0 num-take))
    ([^clojure.lang.ISeq s
      ^Number num-drop
      ^Number num-take]                    (subseq-signed (digitalize-seq s) num-drop num-take)))

  (digits->num
      ([^clojure.lang.ISeq s]              (seq-digits->num (digits->seq s)))
    ([^clojure.lang.ISeq s
      ^Number num-take]                    (seq-digits->num (digits->seq s num-take)))
    ([^clojure.lang.ISeq s
      ^Number num-drop
      ^Number num-take]                    (seq-digits->num (digits->seq s num-drop num-take))))

  (digits->str
      ([^clojure.lang.ISeq s]              (seq-digits->str (digits->seq s)))
    ([^clojure.lang.ISeq s
      ^Number num-take]                    (seq-digits->str (digits->seq s num-take)))
    ([^clojure.lang.ISeq s
      ^Number num-drop
      ^Number num-take]                    (seq-digits->str (digits->seq s num-drop num-take))))

  java.lang.String

  (digitalize
      [^String  s]                         (digitalize-str s))

  (digits->seq
      ([^String s]                         (seq (digitalize-str s)))
    ([^String s
      ^Number num-take]                    (subseq-signed (digitalize-str s) 0 num-take))
    ([^String s
      ^Number num-drop
      ^Number num-take]                    (subseq-signed (digitalize-str s) num-drop num-take)))

  (digits->str
      ([^String s]                         (digitalize-str s))
    ([^String s
      ^Number num-take]                    (subs-signed (digitalize-str s) 0 num-take))
    ([^String s
      ^Number num-drop
      ^Number num-take]                    (subs-signed (digitalize-str s) num-drop num-take)))

  (digits->num
      ([^String s]                         (edn/read-string (digits->str s)))
    ([^String s
      ^Number num-take]                    (edn/read-string (digits->str s num-take)))
    ([^String s
      ^Number num-drop
      ^Number num-take]                    (edn/read-string (digits->str s num-drop num-take))))

  java.lang.Character

  (digitalize
      [^Character  c]                      (digitalize-char c))

  (digits->seq
      ([^Character c]                      (seq (str (digitalize-char c))))
    ([^Character c
      ^Number num-take]                    (subseq-signed (str (digitalize-char c)) 0 num-take))
    ([^Character c
      ^Number num-drop
      ^Number num-take]                    (subseq-signed (str (digitalize-char c)) num-drop num-take)))

  (digits->str
      ([^Character c]                      (str (digitalize-char c)))
    ([^Character c
      ^Number num-take]                    (subs-signed (str (digitalize-char c)) 0 num-take))
    ([^Character c
      ^Number num-drop
      ^Number num-take]                    (subs-signed (str (digitalize-char c)) num-drop num-take)))

  (digits->num
      ([^Character c]                      (Integer/parseInt (digitalize-char c)))
    ([^Character c
      ^Number num-take]                    (Integer/parseInt (digits->str c num-take)))
    ([^Character c
      ^Number num-drop
      ^Number num-take]                    (Integer/parseInt (digits->str c num-drop num-take))))

  java.lang.Number

  (digitalize
      [^Number  n]                         (digitalize-num n))

  (digits->seq
      ([^Number n]                         (num->digits (digitalize-num n)))
    ([^Number n
      ^Number num-take]                    (subseq-signed (num->digits (digitalize-num n)) 0 num-take))
    ([^Number n
      ^Number num-drop
      ^Number num-take]                    (subseq-signed (num->digits (digitalize-num n)) num-drop num-take)))

  (digits->num
      ([^Number n]                         (digitalize-num n))
    ([^Number n
      ^Number num-take]                    (digits->num (digits->seq n num-take)))
    ([^Number n
      ^Number num-drop
      ^Number num-take]                    (digits->num (digits->seq n num-drop num-take))))

  (digits->str
      ([^Number n]                         (digitalize-str (str n)))
    ([^Number n
      ^Number num-take]                    (subs-signed (digitalize-str (str n)) num-take))
    ([^Number n
      ^Number num-drop
      ^Number num-take]                    (subs-signed (digitalize-str (str n)) num-drop num-take)))

  nil

  (digitalize [d]          nil)

  (digits->seq
      ([d]                 nil)
    ([d num-take]          nil)
    ([d num-drop num-take] nil))

  (digits->num
      ([d]                 nil)
    ([d num-take]          nil)
    ([d num-drop num-take] nil))

  (digits->str
      ([d]                 nil)
    ([d num-take]          nil)
    ([d num-drop num-take] nil)))

(defn digital?
  "Checks if a given object is digital. Returns true if it is, false
  otherwise. Digital means that the collection, string or a numeric type
  object consist of numbers from 0 to 9 and optional + or - sign in front."
  [obj]
  (some? (digitalize obj)))
