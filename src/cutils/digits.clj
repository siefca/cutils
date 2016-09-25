(ns

    ^{:doc    "cutils library, collections of digits."
      :author "Paweł Wilk"}

    cutils.digits

  (:require [cutils.core    :refer :all]
            [cutils.strings :refer :all]
            [cutils.ranges  :refer :all]
            [cutils.padding :refer :all]))

(cutils.core/init)

;; TODO:
;;
;; - add keyword and symbol handling
;; - fix single number conv to seq (*spread-numbers*)
;; - countable lazy sequence

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Defaults

(def ^{:added "1.0.0"
       :dynamic true
       :tag Boolean}
  *digitization-throws*
  true)

(def ^{:added "1.0.0"
       :dynamic true
       :tag Boolean}
  *numeric-mode*
  "If set to true then enables numeric mode. In this mode only digits,
  optional decimal point character and a single preceding sign characters are
  allowed. If numeric mode is disabled then separators are allowed but you may
  get strange results when converting to numbers (all separators and white
  characters are omitted)."
  true)

(def ^{:added "1.0.0"
       :dynamic true
       :tag Boolean}
  *decimal-point-mode*
  "If set to true then allows decimal dot to appear in a sequence when
  processing in numeric mode."
  false)

(def ^{:added "1.0.0"
       :dynamic true
       :tag clojure.lang.IPersistentSet}
  *decimal-point-chars*
  #{'. \. :. \, ', (keyword \,)})

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
  (let [snum (map byte (range 0 10))
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
       :tag clojure.lang.IPersistentSet}
  *minus-chars*
  "Minus signs."
  #{- :- '- "-" \-})

(def ^{:added "1.0.0"
       :dynamic true
       :tag clojure.lang.IPersistentMap}
  *sign-to-char*
  "Map of plus and minus signs to characters."
  {- \-, :- \-, '- \-, "-" \-, \- \-
   + \+, :+ \+, '+ \+, "+" \+, \+ \+})

(def ^{:added "1.0.0"
       :tag clojure.lang.IPersistentSet
       :dynamic true}
  *sign-chars*
  "Set of plus and minus signs."
  (set (keys *sign-to-char*)))

(def ^{:added "1.0.0"
       :tag clojure.lang.IPersistentSet
       :dynamic true}
  *separator-chars*
  "Separator characters."
  nil)

(def ^{:added "1.0.0"
       :tag clojure.lang.IPersistentSet
       :dynamic true}
  *white-chars*
  "Common blank characters."
  #{nil \space \newline \tab \formfeed \return (char 0x0B) (char 0)})

(def ^{:added "1.0.0"
       :tag clojure.lang.IPersistentSet
       :dynamic true}
  *separator-classes*
  "Separator character classes (used when separator-chars is not in use)."
  #{Character/CONNECTOR_PUNCTUATION
    Character/DASH_PUNCTUATION
    Character/START_PUNCTUATION
    Character/END_PUNCTUATION
    Character/OTHER_PUNCTUATION
    Character/SPACE_SEPARATOR
    Character/LINE_SEPARATOR
    Character/PARAGRAPH_SEPARATOR
    Character/MATH_SYMBOL})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers

(defn pow
  "Calculates x to the power of n."
  {:added "1.0.0"
   :tag clojure.lang.BigInt}
  [x n]
  (try
    (.pow (BigInteger/valueOf x) n)
    (catch Exception e
      (try
        (.pow (BigInteger. (str x)) n)
        (catch Exception e
          (loop [r (bigint 1) n (bigint n)]
            (if (zero? n) r (recur (*' x r) (dec n)))))))))

(defn count-digits
  "Counts digits for the given number. Returns the number of digits."
  [^clojure.lang.BigInt n]
  {:added "1.0.0"}
  (if (zero? n)
    1
    (let [r (inc (bigint (Math/log10 (if (neg? n) (*' -1 n) n))))]
      (if (> r Long/MAX_VALUE) r (long r)))))

(defn- dig-throw-arg
  "Throws argument exception when *digitization-throws* is not false nor nil."
  {:added "1.0.0"
   :tag nil}
  [& more]
  (when *digitization-throws*
    (apply throw-arg more)))

(defn digital-number?
  "Returns true if the given number n can be used to express a collection of
  digits with optional sign. Returns false otherwise."
  {:added "1.0.0"
   :tag java.lang.Boolean}
  [^Number n]
  (contains? *digital-numbers* (class n)))

(def ^{:added "1.0.0"
       :tag Boolean
       :private true}
  dfl-whitechar?
  "Returns true if a character is a white character (space, tab, newline or
  carriage return)."
  (partial contains? *white-chars*))

(defn- separator-class?
  "Returns true if the given value is a character and is a separator."
  {:added "1.0.0"
   :tag Boolean}
  [^Character c]
  (and (char? c) (contains? *separator-classes* (Character/getType ^char c))))

(def ^{:added "1.0.0"
       :tag Boolean
       :private true}
  separator-chars?
  "Returns true if the given value is a character and is a separator."
  (partial contains? *separator-chars*))

(defn- dfl-separator
  "Returns separator predicate. If the *separator-chars* is empty it returns
  separator-class? function object that checks characters against list of
  character classes. If *separator-chars* is not empty it returns a function
  that checks characters against a set bind to *separator-chars*."
  {:added "1.0.0"
   :tag clojure.lang.Fn}
  []
  (if (empty? *separator-chars*) separator-class? separator-chars?))

(defn- subs-signed
  "Safely creates a substring preserving its first character when it is a plus
  or a minus sign. Preservation means that that sign (if present in front of
  the given string) is memorized and prepended to the resulting substring
  unless that substring is empty."
  {:added "1.0.0"
   :tag String}
  ([^String     s
    ^Number start]
   (not-empty (subs-preserve s *sign-chars* start)))
  ([^String     s
    ^Number start
    ^Number   num]
   (not-empty (subs-preserve s *sign-chars* start (+ start num)))))

(defn- subseq-signed
  "Safely creates a sequence preserving its first character when it is a plus
  or a minus sign. Preservation means that that sign (if present in front of
  the given collection) is memorized and prepended to the resulting sequence
  unless that sequence is empty."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([^clojure.lang.ISeq s
    ^Number     num-drop]
   (not-empty (subseq-preserve s *sign-chars* num-drop)))
  ([^clojure.lang.ISeq s
    ^Number     num-drop
    ^Number     num-take]
   (not-empty (subseq-preserve s *sign-chars* num-drop (+ num-take num-drop)))))

(defn- subvec-signed
  "Safely creates a subvector preserving its first element when it is a plus
  or a minus sign. Preservation means that the sign (if present in front of
  the given vector) is always memorized and prepended to the resulting vector
  unless that vector is empty."
  {:added "1.0.0"
   :tag clojure.lang.IPersistentVector}
  ([^clojure.lang.IPersistentVector v
    ^Number start]
   (not-empty (subvec-preserve v *sign-chars* start)))
  ([^clojure.lang.IPersistentVector v
    ^Number start
    ^Number   num]
   (not-empty (subvec-preserve v *sign-chars* start (+ start num)))))

(defn- fix-sign-seq
  "Removes plus character from the head of a sequential collection."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  [^clojure.lang.ISeq coll]
  (if (= \+ (first coll)) (next coll) coll))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Conversions

(defn- num->digits-core
  "Changes a number given as n into a lazy sequence of numbers representing
  decimal digits (in reverse order for positive values). Returns a sequence of
  two elements: first is a number of digits and second is a sequence."
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  ([^Number n]
   (when n
     (if (zero? n)
       (lazy-seq (cons 0 nil))
       (num->digits-core n (pow 10 (dec (count-digits n)))))))
  ([^Number n
    ^Number div-by]
   (if (zero? n)
     (if (> div-by 0)
       (lazy-seq
        (cons
         (byte 0)
         (num->digits-core 0 (quot div-by 10))))
       nil)
     (lazy-seq
      (cons
       (byte (quot n div-by))
       (num->digits-core (mod n div-by) (quot div-by 10)))))))

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
      (cons \- (num->digits-core (*' -1 n)))
      (if (zero? n)
        (cons 0 nil)
        (num->digits-core n))))))

(def ^{:added "1.0.0"
       :const true
       :private true
       :tag Long}
  big-seq-digits
  (count (num->digits (Long/MAX_VALUE))))

(def ^{:added "1.0.0"
       :const true
       :private true
       :tag Long}
  long-before
  (long (/ (Long/MAX_VALUE) 10)))

(defn- seq-big->num
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq x
   ^BigDecimal r
   ^clojure.lang.BigInt i]
  (if (nil? x) r (recur (next x) (+' r (*' (first x) i)) (*' 10 i))))

(defn- seq-big-dec->num
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq x
   ^clojure.lang.BigInt r
   ^clojure.lang.BigInt i]
  (if (nil? x)
    r
    (let [v (first x)]
      (if (= \. v)
        (seq-big-dec->num (next x) (/ r i) (bigint 1))
        (recur (next x) (+' r (*' v i)) (*' 10 i))))))

(defn- seq-dec->num2
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq x
   ^long r
   ^clojure.lang.BigInt i]
  (if (nil? x)
    r
    (let [v (first x)]
      (if (= \. v)
        (seq-big-dec->num (next x) (bigdec (/ r i)) (bigint 1))
        (let [o (+ r (* v i))]
          (if (> o Long/MAX_VALUE)
            (seq-big-dec->num (next x) (bigint o) (*' 10 i))
            (recur (next x) (long o) (*' 10 i))))))))

(defn- seq-dec->num
  "Warning: nil or false element ends iteration but that shouldn't be a
  problem in case of validated sequence with digits and optional sign."
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq d]
  (when-not (empty? d)
    (loop [x (reverse d) r (long 0) i (long 1)]
      (if (nil? x)
        r
        (let [v (first x)]
          (if (= \. v)
            (seq-big-dec->num (next x) (bigdec (/ r i)) (bigint 1))
            (if (>= i long-before)
              (seq-dec->num2 x r (bigint i))
              (recur (next x) (+ r (* ^long v i)) (* 10 i)))))))))

(defn- seq-big->num
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq x
   ^clojure.lang.BigInt r
   ^clojure.lang.BigInt i]
  (if (nil? x) r (recur (next x) (+' r (*' (first x) i)) (*' 10 i))))

(defn- seq-long->num2
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq x
   ^long r
   ^clojure.lang.BigInt i]
  (if (nil? x)
    r
    (let [o (+ r (* (first x) i))]
      (if (> o Long/MAX_VALUE)
        (seq-big->num (next x) (bigint o) (*' 10 i))
        (recur (next x) (long o) (*' 10 i))))))

(defn- seq-long->num
  "Warning: nil or false element ends iteration but that shouldn't be a
  problem in case of validated sequence with digits and optional sign."
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq d]
  (when-not (empty? d)
    (loop [x (reverse d) r (long 0) i (long 1)]
      (if (nil? x)
        r
        (if (>= i long-before)
          (seq-long->num2 x r (bigint i))
          (recur (next x) (+ r (* ^long (first x) i)) (* 10 i)))))))

(defn- seq-digits->num
  "Warning: nil or false element ends iteration but that shouldn't be a
  problem in case of validated sequence with digits and optional sign."
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq coll]
  (let [is-minus? (contains? *minus-chars* (first coll))
        coll      (if is-minus? (next coll) coll)
        r         ((if *decimal-point-mode* seq-dec->num seq-long->num) coll)]
    (if is-minus? (*' -1 r) r)))

(defn- seq-digits->str
  {:added "1.0.0"
   :tag String}
  [^clojure.lang.ISeq s]
  (not-empty (reduce str s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Normalization and validation

(defn- digitize-core-fn
  {:added "1.0.0"
   :tag clojure.lang.Fn}
  [^Boolean numeric-version
   ^clojure.lang.Fn f-notempty
   ^clojure.lang.Fn f-first
   ^clojure.lang.Fn f-next]
  (if numeric-version
    (fn digitize-core-num
      [^clojure.lang.ISeq src
       ^Boolean had-number?
       ^Boolean had-point?]
      (when (f-notempty src)
        (let [e (f-first src)
              e (if (string? e) (str-trim e) e)
              n (f-next src)]
          (if (dfl-whitechar? e)
            (recur n had-number? had-point?)
            (lazy-seq
             (if-let [c (*chars-to-digits* e)]
               (cons c (digitize-core-num n true had-point?))
               (if-let [c (*sign-to-char* e)]
                 (if-not had-number?
                   (cons c (digitize-core-num n true had-point?))
                   (dig-throw-arg "The sign of a number should occur once and precede first digit"))
                 (if *decimal-point-mode*
                   (if (contains? *decimal-point-chars* e)
                     (if had-point?
                       (dig-throw-arg "The decimal point character should occur just once")
                       (cons \. (digitize-core-num n had-number? true)))
                     (dig-throw-arg "Sequence element is not a single digit, not a sign nor a decimal point separator: " e))
                   (if (contains? *decimal-point-chars* e)
                     (dig-throw-arg "Sequence element is a decimal point separator but decimal-point-mode is disabled: " e) 
                     (dig-throw-arg "Sequence element is not a single digit nor a sign: " e))))))))))
    (fn digitize-core
      [^clojure.lang.ISeq src
       ^clojure.lang.Fn sep-pred]
      (when (f-notempty src)
        (let [e (f-first src)
              n (f-next src)]
          (if (dfl-whitechar? e)
            (recur n sep-pred)
            (lazy-seq
             (if-let [c (*chars-to-digits* e)]
               (cons c (digitize-core n))
               (if (sep-pred e)
                 (cons e (digitize-core n))
                 (dig-throw-arg "Sequence element is not a single digit nor a separator: " e))))))))))

(defn- digitize-fn
  {:added "1.0.0"
   :tag clojure.lang.Fn}
  [^clojure.lang.Fn f-notempty
   ^clojure.lang.Fn f-first
   ^clojure.lang.Fn f-next]
  (let [dcore     (digitize-core-fn false f-notempty f-first f-next)
        dcore-num (digitize-core-fn true  f-notempty f-first f-next)]
    (fn digitize-generic
      ([src
        ^Number num-drop
        ^Number num-take]
       (let [r (digitize-generic src)]
         (if *numeric-mode*
           (subseq-signed r num-drop num-take)
           (safe-subseq   r num-drop (+ num-take num-drop)))))
      ([src
        ^Number num-take]
       (digitize-generic src 0 num-take))
      ([src]
       (if *numeric-mode*
         (fix-sign-seq (dcore-num src false false))
         (dcore src dfl-separator))))))

(def ^{:added "1.0.0"
       :private true
       :tag clojure.lang.ISeq
       :arglists '([^clojure.lang.ISeq coll]
                   [^clojure.lang.ISeq coll, ^Number num-take]
                   [^clojure.lang.ISeq coll, ^Number num-drop, ^Number num-take])}
  digitize-seq
  (digitize-fn (comp not empty?) first next))

(def ^{:added "1.0.0"
       :private true
       :tag clojure.lang.ISeq
       :arglists '([^clojure.lang.IPersistentVector coll]
                   [^clojure.lang.IPersistentVector coll, ^Number num-take]
                   [^clojure.lang.IPersistentVector coll, ^Number num-drop, ^Number num-take])}
  digitize-vec
  (digitize-fn #(contains? % 0) #(get % 0) #(subvec % 1)))

(defn- digitize-num
  "Changes number into normalized representation. Returns number or nil."
  {:added "1.0.0"
   :tag Number}
  [^Number n]
  (when (digital-number? n) n))

(defn- digitize-char
  "Changes numeric character into normalized representation. Returns
  a digit or nil."
  {:added "1.0.0"
   :tag Character}
  [^Character c]
  (when-not (nil? c)
    (if (contains? *chars-to-digits* c)
      (get *chars-to-digits* c)
      (dig-throw-arg "Given character does not express a digit: " c))))

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

  (digital?
   [coll]
   "Checks if a given object is digital. Returns true if it is, false
  otherwise. Digital means that the collection, string or a numeric type
  object consist of numbers from 0 to 9 and optional + or - sign in front.")

  (digits->num
   [coll] [coll num-take] [coll num-drop num-take]
   "Changes a collection of digits given as coll into an integer number. An
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
  collection was given or ranges were mismatched).")

  (negative?
   [coll]
   "Checks if first element of a sequence is a minus sign. Does not normalizes
  the given sequence. Returns true or false.")

  (slice-digits
   [coll start] [coll start num-take]
   "Preserves first element of a series of digits when it is a plus
  or a minus sign. Preservation means that the sign (if present in front of
  the given collection) is always memorized and prepended to the resulting
  collection unless that collection is empty. Any white characters preceding
  and following the sign are removed."))

(extend-protocol Digitizing

  clojure.lang.IPersistentVector

  (digitize
      [^clojure.lang.IPersistentVector  v]                         (not-empty (vec (digitize-vec v))))
  (digital?
      [^clojure.lang.IPersistentVector  v]                         (some? (digitize-vec v)))
  (digits->seq
      ([^clojure.lang.IPersistentVector v]                         (digitize-vec v))
    ([^clojure.lang.IPersistentVector   v, ^Number nt]             (digitize-vec v nt))
    ([^clojure.lang.IPersistentVector   v, ^Number nd, ^Number nt] (digitize-vec v nd nt)))
  (digits->num
      ([^clojure.lang.IPersistentVector v]                         (seq-digits->num (digits->seq v)))
    ([^clojure.lang.IPersistentVector   v, ^Number nt]             (seq-digits->num (digits->seq v nt)))
    ([^clojure.lang.IPersistentVector   v, ^Number nd, ^Number nt] (seq-digits->num (digits->seq v nd nt))))
  (digits->str
      ([^clojure.lang.IPersistentVector v]                         (seq-digits->str (digits->seq v)))
    ([^clojure.lang.IPersistentVector   v, ^Number nt]             (seq-digits->str (digits->seq v nt)))
    ([^clojure.lang.IPersistentVector   v, ^Number nd, ^Number nt] (seq-digits->str (digits->seq v nd nt))))
  (slice-digits
      ([^clojure.lang.IPersistentVector v, ^Number st]             (subvec-signed v st))
    ([^clojure.lang.IPersistentVector   v, ^Number st, ^Number nt] (subvec-signed v st nt)))
  (negative?
      [^clojure.lang.IPersistentVector  v]                         (contains? *minus-chars* (get v 0)))

  clojure.lang.ISeq

  (digitize
      [^clojure.lang.ISeq s]                                       (not-empty (digitize-seq s)))
  (digital?
      [^clojure.lang.ISeq s]                                       (some? (digitize-seq s)))
  (digits->seq
      ([^clojure.lang.ISeq s]                                      (digitize-seq s))
    ([^clojure.lang.ISeq   s, ^Number nt]                          (digitize-seq s nt))
    ([^clojure.lang.ISeq   s, ^Number nd, ^Number nt]              (digitize-seq s nd nt)))
  (digits->num
      ([^clojure.lang.ISeq s]                                      (seq-digits->num (digits->seq s)))
    ([^clojure.lang.ISeq   s, ^Number nt]                          (seq-digits->num (digits->seq s nt)))
    ([^clojure.lang.ISeq   s, ^Number nd, ^Number nt]              (seq-digits->num (digits->seq s nd nt))))
  (digits->str
      ([^clojure.lang.ISeq s]                                      (seq-digits->str (digits->seq s)))
    ([^clojure.lang.ISeq   s, ^Number nt]                          (seq-digits->str (digits->seq s nt)))
    ([^clojure.lang.ISeq   s, ^Number nd, ^Number nt]              (seq-digits->str (digits->seq s nd nt))))
  (slice-digits
      ([^clojure.lang.ISeq s, ^Number st]                          (subseq-signed s st))
    ([^clojure.lang.ISeq   s, ^Number st, ^Number nt]              (subseq-signed s st nt)))
  (negative?
      [^clojure.lang.ISeq  s]                                      (contains? *minus-chars* (first s)))

  java.lang.String

  (digitize
      [^String  s]                                                 (not-empty (seq-digits->str (digitize-seq s))))
  (digital?
      [^String  s]                                                 (some? (digitize-seq s)))
  (digits->seq
      ([^String s]                                                 (digitize-seq s))
    ([^String   s, ^Number nt]                                     (digitize-seq s nt))
    ([^String   s, ^Number nd, ^Number nt]                         (digitize-seq s nd nt)))
  (digits->str
      ([^String s]                                                 (seq-digits->str (digits->seq s)))
    ([^String   s, ^Number nt]                                     (seq-digits->str (digits->seq s nt)))
    ([^String   s, ^Number nd, ^Number nt]                         (seq-digits->str (digits->seq s nd nt))))
  (digits->num
      ([^String s]                                                 (seq-digits->num (digits->seq s)))
    ([^String   s, ^Number nt]                                     (seq-digits->num (digits->seq s nt)))
    ([^String   s, ^Number nd, ^Number nt]                         (seq-digits->num (digits->seq s nd nt))))
  (slice-digits
      ([^String s, ^Number st]                                     (subs-signed s st))
    ([^String   s, ^Number st, ^Number nt]                         (subs-signed s st nt)))
  (negative?
      [^String  s]                                                 (contains? *minus-chars* (first s)))

  java.lang.Character

  (digitize
      [^Character  c]                                              (digitize-char c))
  (digital?
      [^Character  c]                                              (some? (digitize-char c)))
  (digits->seq
      ([^Character c]                                              (lazy-seq (cons (digitize-char c) nil)))
    ([^Character   c, ^Number nt]                                  (subseq-signed (digits->seq c) 0 nt))
    ([^Character   c, ^Number nd, ^Number nt]                      (subseq-signed (digits->seq c) nd nt)))
  (digits->str
      ([^Character c]                                              (not-empty (str (digitize-char c))))
    ([^Character   c, ^Number nt]                                  (subs-signed (digits->str c) 0 nt))
    ([^Character   c, ^Number nd, ^Number nt]                      (subs-signed (digits->str c) nd nt)))
  (digits->num
      ([^Character c]                                              (some-> (digitize-char c) int))
    ([^Character   c, ^Number nt]                                  (some-> (digits->str c nt) Integer/parseInt))
    ([^Character   c, ^Number nd, ^Number nt]                      (some-> (digits->str c nd nt) Integer/parseInt)))
  (slice-digits
      ([^Character c, ^Number st]                                  (subs-signed (str (digitize-char c)) st))
    ([^Character   c, ^Number st, ^Number nt]                      (subs-signed (str (digitize-char c)) st nt)))
  (negative?
      [^Character  c]                                              (contains? *minus-chars* c))

  java.lang.Number

  (digitize
      [^Number  n]                                                 (digitize-num n))
  (digital?
      [^Number n]                                                  (some? (digitize-num n)))
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
  (slice-digits
      ([^Number n, ^Number st]                                     (digits->num (subseq-signed (digits->seq n) st)))
    ([^Number   n, ^Number st, ^Number nt]                         (digits->num (subseq-signed (digits->seq n) st nt))))
  (negative?
      [^Number  n]                                                 (neg? n))

  nil

  (digitize  [d] nil)
  (digital?  [o] false)
  (negative? [o] false)
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
    ([d nd nt] nil))
  (slice-digits
      ([d st]  nil)
    ([d st nt] nil)))
