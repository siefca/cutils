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
;; Defaults

(def ^{:added "1.0.0"
       :dynamic true
       :tag Boolean}
  *digitization-throws*
  true)

(def ^{:added "1.0.0"
       :dynamic true
       :tag clojure.lang.IPersistentSet}
  *digital-numbers*
  #{java.lang.Byte
    java.lang.Short
    java.lang.Integer
    java.lang.Long
    clojure.lang.BigInt
    java.math.BigInteger
    java.math.BigDecimal})

(def ^{:added "1.0.0"
       :dynamic true
       :tag clojure.lang.IPersistentMap}
  *chars-to-digits*
  (let [snum (range 0 10)
        nums (mapcat (partial repeat 2)  snum)
        strs (mapcat (juxt str identity) snum)
        syms (mapcat (juxt (comp symbol  str) identity) snum)
        kwds (mapcat (juxt (comp keyword str) identity) snum)
        chrc (mapcat (juxt (comp first   str) identity) snum)]
    (apply hash-map (concat nums strs syms kwds chrc))))

(def ^{:added "1.0.0"
       :const true
       :tag Integer}
  char-num-base
  "ASCII code of 0 character."
  (int \0))

(def ^{:added "1.0.0"
       :dynamic true
       :tag clojure.lang.IPersistentMap}
  *s-to-chars*
  "Map of plus and minus signs to characters."
  {- \-, :- \-, '- \-, "-" \-, \- \-
   + \+, :+ \+, '+ \+, "+" \+, \+ \+})

(def ^{:added "1.0.0"
       :tag clojure.lang.IPersistentSet
       :dynamic true}
  *sign-chars*
  "Set of plus and minus signs."
  (set (keys *s-to-chars*)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers

(defn- dig-throw-arg
  "Throws argument exception when *digitization-throws* is not false nor nil."
  {:added "1.0.0"
   :tag nil}
  [& more]
  (when *digitization-throws*
    (apply throw-arg more)))

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
   ^Number num-items
   ^clojure.lang.ISeq coll]
  (pad coll (- min-digits num-items) 0 true))

(defn subs-signed
  "Safely creates a substring preserving its first character when it is a plus
  or a minus sign. Preservation means that that sign (if present in front of
  the given string) is memorized and prepended to the resulting substring
  unless that substring is empty."
  {:added "1.0.0"
   :tag String}
  ([^String     s
    ^Number start]
   (subs-preserve s *sign-chars* start))
  ([^String     s
    ^Number start
    ^Number   num]
   (subs-preserve s *sign-chars* start (+ start num))))

(defn subseq-signed
  "Safely creates a sequence preserving its first character when it is a plus
  or a minus sign. Preservation means that that sign (if present in front of
  the given collection) is memorized and prepended to the resulting sequence
  unless that sequence is empty."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([^clojure.lang.ISeq s
    ^Number        start]
   (subseq-preserve s *sign-chars* start))
  ([^clojure.lang.ISeq s
    ^Number        start
    ^Number          num]
   (subseq-preserve s *sign-chars* start (+ start num))))

(defn subvec-signed
  "Safely creates subvector preserving its first element when it is a plus or
  a minus sign. Preservation means that the sign (if present in front of the
  given vector) is always memorized and prepended to the resulting vector
  unless that vector is empty."
  {:added "1.0.0"
   :tag clojure.lang.IPersistentVector}
  ([^clojure.lang.IPersistentVector v
    ^Number start]
   (subvec-preserve v *sign-chars* start))
  ([^clojure.lang.IPersistentVector v
    ^Number start
    ^Number   num]
   (subvec-preserve v *sign-chars* start (+ start num))))

(defn- fix-sign-seq
  "Removes plus character from the head of a sequential collection."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  [^clojure.lang.ISeq coll]
  (if (= \+ (first coll)) (next coll) coll))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Conversions

(defn- num->digits-core
  "Changes a number given as n into a sequence of numbers representing decimal
  digits (in reverse order for positive values). Returns a sequence of two
  elements: first is a number of digits and second is a lazy sequence."
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  [^Number n]
  (when-not (zero? n)
    (cons (byte (mod n 10))
          (lazy-seq (num->digits-core (quot n 10))))))

(defn num->digits
  "Changes a number given as n into a sequence of numbers representing decimal
  digits. If min-digits argument is given and it is larger than the number of
  digits then it pads the returned sequence with leading zeros to satisfy the
  number of elements with a first element preserved (and not counted) if it's
  not a digit.

  Be aware that in the second case the values of sequence
  elements will be evaluated in order to know their count."
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  ([^Number n]
   (lazy-seq
    (if (neg? n)
      (cons \- (lazy-seq (num->digits-core n)))
      (if (zero? n)
        (list 0)
        (num->digits-core (*' -1 n))))))
  ([^Number n
    ^Number min-digits]
   (let [d (num->digits n)
         f (first d)]
     (if (digital-number? f)
       (pad d min-digits 0 true)
       (lazy-seq (cons f (pad (next d) (dec min-digits) 0 true)))))))

(def ^{:added "1.0.0"
       :const true
       :private true
       :tag Long}
  big-seq-digits
  (num->digits (Long/MAX_VALUE)))

(def ^{:added "1.0.0"
       :const true
       :private true
       :tag Long}
  big-seq-digits-count
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
        (recur (next x)
               (+' r (*' (first x) i))
               (*' 10 i))))))

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
        (recur (next x)
               (+ r (* (long (first x)) i))
               (* 10 i))))))

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
  (contains? *digital-numbers* (class n)))

(defn- digitize-seq-core
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  [^clojure.lang.ISeq src
   ^Boolean had-number?]
  (when-not (empty? src)
    (let [e (first src)
          e (if (string? e) (pstr e) e)
          n (next src)]
      (if (whitechar? e)
        (recur n had-number?)
        (if-let [c (*chars-to-digits* e)]
          (cons c (lazy-seq (digitize-seq-core n true)))
          (if-let [c (*s-to-chars* e)]
            (if-not had-number?
              (cons c (lazy-seq (digitize-seq-core n true)))
              (dig-throw-arg "The sign of a number should occur once and precede any digit"))
            (dig-throw-arg "Sequence element is not a digit nor a sign: " e)))))))

(defn- digitize-seq
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  ([^clojure.lang.ISeq src
    ^Number       num-drop
    ^Number       num-take]
   (-> src
       (digitize-seq-core false)
       (fix-sign-seq)
       (subseq-signed num-drop num-take)))
  ([^clojure.lang.ISeq src
    ^Number       num-take]
   (digitize-seq src 0 num-take))
  ([^clojure.lang.ISeq src]
   (fix-sign-seq (digitize-seq-core src false))))

(defn- digitize-vec-core
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  [^clojure.lang.IPersistentVector src
   ^Boolean had-number?]
  (when (contains? src 0)
    (let [e (get src 0)
          e (if (string? e) (pstr e) e)
          n (subvec src 1)]
      (if (whitechar? e)
        (recur n had-number?)
        (if-let [c (*chars-to-digits* e)]
          (cons c (lazy-seq (digitize-vec-core n true)))
          (if-let [c (*s-to-chars* e)]
            (if-not had-number?
              (cons c (lazy-seq (digitize-vec-core n true)))
              (dig-throw-arg "The sign of a number should occur once and precede any digit"))
            (dig-throw-arg "Sequence element is not a digit nor a sign: " e)))))))

(defn- digitize-vec
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  ([^clojure.lang.IPersistentVector src
    ^Number                    num-drop
    ^Number                    num-take]
   (-> src
       (digitize-vec-core false)
       (fix-sign-seq)
       (subseq-signed num-drop num-take)))
  ([^clojure.lang.IPersistentVector src
    ^Number                    num-take]
   (digitize-vec src 0 num-take))
  ([^clojure.lang.IPersistentVector src]
   (fix-sign-seq (digitize-vec-core src false))))

(defn- digitize-num
  "Changes number into normalized representation. Returns number or nil."
  {:added "1.0.0"
   :tag Number}
  [^Number n]
  (when (digital-number? n) n))

(defn- digitize-char
  "Changes numeric character into normalized representation. Returns character
  or nil."
  {:added "1.0.0"
   :tag Character}
  [^Character c]
  (when-some [c (*chars-to-digits* c)]
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
    ([^clojure.lang.IPersistentVector   v, ^Number nt]             (digitize-vec v nt))
    ([^clojure.lang.IPersistentVector   v, ^Number nd, ^Number nt] (digitize-vec v nd nt)))
  (digits->num
      ([^clojure.lang.IPersistentVector v]                         (seq-digits->num (digitize-vec v)))
    ([^clojure.lang.IPersistentVector   v, ^Number nt]             (seq-digits->num (digitize-vec v nt)))
    ([^clojure.lang.IPersistentVector   v, ^Number nd, ^Number nt] (seq-digits->num (digitize-vec v nd nt))))
  (digits->str
      ([^clojure.lang.IPersistentVector v]                         (seq-digits->str (digitize-vec v)))
    ([^clojure.lang.IPersistentVector   v, ^Number nt]             (seq-digits->str (digitize-vec v nt)))
    ([^clojure.lang.IPersistentVector   v, ^Number nd, ^Number nt] (seq-digits->str (digitize-vec v nd nt))))

  clojure.lang.ISeq

  (digitize
      [^clojure.lang.ISeq s]                                       (not-empty (digitize-seq s)))
  (digits->seq
      ([^clojure.lang.ISeq s]                                      (digitize-seq s))
    ([^clojure.lang.ISeq   s, ^Number nt]                          (digitize-seq s nt))
    ([^clojure.lang.ISeq   s, ^Number nd, ^Number nt]              (digitize-seq s nd nt)))
  (digits->num
      ([^clojure.lang.ISeq s]                                      (seq-digits->num (digitize-seq s)))
    ([^clojure.lang.ISeq   s, ^Number nt]                          (seq-digits->num (digitize-seq s nt)))
    ([^clojure.lang.ISeq   s, ^Number nd, ^Number nt]              (seq-digits->num (digitize-seq s nd nt))))
  (digits->str
      ([^clojure.lang.ISeq s]                                      (seq-digits->str (digitize-seq s)))
    ([^clojure.lang.ISeq   s, ^Number nt]                          (seq-digits->str (digitize-seq s nt)))
    ([^clojure.lang.ISeq   s, ^Number nd, ^Number nt]              (seq-digits->str (digitize-seq s nd nt))))

  java.lang.String

  (digitize
      [^String  s]                                                 (not-empty (seq-digits->str (digitize-seq s))))
  (digits->seq
      ([^String s]                                                 (digitize-seq s))
    ([^String   s, ^Number nt]                                     (digitize-seq s nt))
    ([^String   s, ^Number nd, ^Number nt]                         (digitize-seq s nd nt)))
  (digits->str
      ([^String s]                                                 (seq-digits->str (digitize-seq s)))
    ([^String   s, ^Number nt]                                     (seq-digits->str (digitize-seq s nt)))
    ([^String   s, ^Number nd, ^Number nt]                         (seq-digits->str (digitize-seq s nd nt))))
  (digits->num
      ([^String s]                                                 (seq-digits->num (digitize-seq s)))
    ([^String   s, ^Number nt]                                     (seq-digits->num (digitize-seq s nt)))
    ([^String   s, ^Number nd, ^Number nt]                         (seq-digits->num (digitize-seq s nd nt))))

  java.lang.Character

  (digitize
      [^Character  c]                                              (digitize-char c))
  (digits->seq
      ([^Character c]                                              (seq (str (digitize-char c))))
    ([^Character   c, ^Number nt]                                  (subseq-signed (str (digitize-char c)) 0 nt))
    ([^Character   c, ^Number nd, ^Number nt]                      (subseq-signed (str (digitize-char c)) nd nt)))
  (digits->str
      ([^Character c]                                              (str (digitize-char c)))
    ([^Character   c, ^Number nt]                                  (subs-signed (str (digitize-char c)) 0 nt))
    ([^Character   c, ^Number nd, ^Number nt]                      (subs-signed (str (digitize-char c)) nd nt)))
  (digits->num
      ([^Character c]                                              (Integer/parseInt (digitize-char c)))
    ([^Character   c, ^Number nt]                                  (Integer/parseInt (digits->str c nt)))
    ([^Character   c, ^Number nd, ^Number nt]                      (Integer/parseInt (digits->str c nd nt))))

  java.lang.Number

  (digitize
      [^Number  n]                                                 (digitize-num n))
  (digits->seq
      ([^Number n]                                                 (num->digits (digitize-num n)))
    ([^Number   n, ^Number nt]                                     (subseq-signed (digits->seq n) 0 nt))
    ([^Number   n, ^Number nd, ^Number nt]                         (subseq-signed (digits->seq n) nd nt)))
  (digits->num
      ([^Number n]                                                 (digitize-num n))
    ([^Number   n, ^Number nt]                                     (digits->num (digits->seq n nt)))
    ([^Number   n, ^Number nd, ^Number nt]                         (digits->num (digits->seq n nd nt))))
  (digits->str
      ([^Number n]                                                 (digitize (str n)))
    ([^Number n, ^Number nt]                                       (subs-signed (digitize (str n)) nt))
    ([^Number n, ^Number nd, ^Number nt]                           (subs-signed (digitize (str n)) nd nt)))

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
