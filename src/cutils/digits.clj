(ns

    ^{:doc    "cutils library, collections of digits."
      :author "Paweł Wilk"}

    cutils.digits

  (:require [cutils.core    :refer :all]
            [cutils.strings :refer :all]
            [cutils.ranges  :refer :all]))

(cutils.core/init)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Defaults

(def ^{:added "1.0.0"
       :dynamic true
       :tag Boolean}
  *spread-numbers*
  "If set to true causes conversion functions to split numbers that are
  not single digits."
  true)

(def ^{:added "1.0.0"
       :dynamic true
       :tag Boolean}
  *numeric-mode*
  "If set to true enables numeric mode. In this mode only digits, optional
  decimal point characters and single sign characters are allowed when
  normalizing and validating digital values (besides white characters
  that are ignored).

  If numeric mode is disabled (default) then both numbers and separators
  are allowed during validation and normalization but there is no guarantee
  that produced sequences, strings or other sequential collections will be
  valid expressions of numeric values.

  There are exceptions: cutils.digits/negative?, cutils.digits/numeric?
  and cutils.digits/digits->num are always forcing numeric mode (even if
  it was set to false in calling context). That's because for the correct
  behavior of that functions collections of digits must be treated as if
  they were expressing numbers.

  Another exception is cutils.digits/digital? that is always forcing
  generic mode (sets *numeric-mode* to false). It is so because the behavior
  of that function requires checking whether the given collection is
  a collection of digits but not necessarily a valid number."
  false)

(def ^{:added "1.0.0"
       :dynamic true
       :tag Boolean}
  *decimal-point-mode*
  "If set to true allows decimal dot to appear in a sequence when
  processing it in numeric mode."
  true)

(def ^{:added "1.0.0"
       :dynamic true
       :tag java.lang.Character}
  *dot-char*
  \.)

(def ^{:added "1.0.0"
       :dynamic true
       :tag clojure.lang.IPersistentSet}
  *decimal-point-chars*
  (let [ds (str *dot-char*)]
    (conj #{'. \. :. \, ', (keyword \,) \· \· \՝ \։ \، \؍
            \٫
            \٬
            \۔
            \܂
            \࠰
            \࡞
            \። \᙮ \᛫ \᠃ \᠉ \⳹ \⳾ \᠂ \᠈ \߸ \፣ \․ }
          ds
          (symbol  ds)
          (keyword ds))))

(def ^{:added "1.0.0"
       :dynamic true
       :tag clojure.lang.IPersistentSet}
  *digital-numbers*
  #{java.lang.Byte
    java.lang.Short
    java.lang.Integer
    java.lang.Long
    java.lang.Double
    clojure.lang.BigInt
    java.math.BigInteger
    java.math.BigDecimal})

(def ^{:added "1.0.0"
       :dynamic true
       :tag clojure.lang.IPersistentMap}
  *vals-to-digits*
  (let [snum (map byte (range 0 10))
        nums (mapcat (partial repeat 2)     snum)               ;; bytes 0-9
        bigs (mapcat (juxt bigdec identity) snum)               ;; big decimals 0-9
        strs (mapcat (juxt str    identity) snum)               ;; strings "0"-"9"
        syms (mapcat (juxt (comp symbol  str) identity) snum)   ;; symbols '0-'9
        kwds (mapcat (juxt (comp keyword str) identity) snum)   ;; keywords :0-:9
        chrc (mapcat (juxt (comp first   str) identity) snum)]  ;; characters \0-\9
    (apply hash-map (concat nums strs syms kwds chrc))))        ;; mapping all of the above to bytes

(def ^{:added "1.0.0"
       :const true
       :tag Integer}
  char-num-base
  "ASCII code of 0 character."
  (int \0))

(def ^{:added "1.0.0"
       :dynamic true
       :tag clojure.lang.IPersistentSet}
  *minus-signs*
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
  *signs*
  "Set of plus and minus signs."
  (set (keys *sign-to-char*)))

(def ^{:added "1.0.0"
       :tag clojure.lang.IPersistentMap
       :dynamic true}
  *separators-translate*
  "Separators translation map used when numeric mode is disabled."
  {- \-, + \+})

(def ^{:added "1.0.0"
       :tag clojure.lang.IPersistentSet
       :dynamic true}
  *separator-chars*
  "Separator characters."
  *signs*)

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
;; Switches

(defmacro with-numeric-mode!
  "Enables numeric mode processing for the given S-expression."
  {:added "1.0.0"}
  [& body]
  `(binding [cutils.digits/*numeric-mode* true] ~@body))

(defmacro with-generic-mode!
  "Disables numeric mode processing for the given S-expression."
  {:added "1.0.0"}
  [& body]
  `(binding [cutils.digits/*numeric-mode* false] ~@body))

(defmacro with-decimal-point-mode!
  "Enables numeric mode processing and decimal-point detection
  for the given S-expression."
  {:added "1.0.0"}
  [& body]
  `(binding [cutils.digits/*numeric-mode*       true
             cutils.digits/*decimal-point-mode* true] ~@body))

(defmacro with-spread-numbers!
  "Enables numbers spreading when processing digital sequences
  for the given S-expression."
  {:added "1.0.0"}
  [& body]
  `(binding [cutils.digits/*spread-numbers* true] ~@body))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers

(defn dot?
  "True if the given argument is a dot character."
  {:added "1.0.0"
   :tag Boolean}
  [^Character c]
  (= *dot-char* c))

(defn decimal-point-char?
  "True if the given argument is decimal point character."
  {:added "1.0.0"
   :tag Boolean}
  [^Character c]
  (contains? *decimal-point-chars* c))

(defn- pow10
  "Calculates 10 to the power of n."
  {:added "1.0.0"
   :tag clojure.lang.BigInt}
  [^Number n]
  (try
    (.pow (BigInteger/valueOf 10) n)
    (catch Exception e
      (try
        (.pow (BigInteger/valueOf 10) (bigint n))
        (catch Exception e
          (loop [r (bigint 1)
                 n (bigint n)]
            (if (zero? n) r (recur (*' 10N r) (dec' n)))))))))

(defn- dig-throw-arg
  "Throws argument exception."
  {:added "1.0.0"
   :tag nil}
  [& more]
  (apply throw-arg more))

(defn digital-number?
  "Returns true if the given number n can be used to express a collection of
  digits with optional sign. Returns false otherwise."
  {:added "1.0.0"
   :tag java.lang.Boolean}
  [^Number n]
  (contains? *digital-numbers* (class n)))

(defn digit?
  "Returns true if the given object is a digit."
  {:added "1.0.0"
   :tag java.lang.Boolean}
  [^Number n]
  (contains? *vals-to-digits* n))

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
  separator-class? function object that checks characters against a list of
  character classes. If *separator-chars* is not empty it returns a function
  that checks characters against a set bound to *separator-chars*."
  {:added "1.0.0"
   :tag clojure.lang.Fn}
  []
  (if (empty? *separator-chars*)
    separator-class?
    (fn [o]
      (or
       (separator-class? o)
       (separator-chars? o)))))

(defn- dfl-translator
  "Returns a function that translates certain separator objects."
  {:added "1.0.0"
   :tag clojure.lang.Fn}
  []
  (if-let [t (not-empty *separators-translate*)]
    (fn [o] (get *separators-translate* o o))
    identity))

(defn- subs-signed
  "Safely creates a substring preserving its first character when it is a plus
  or a minus sign. Preservation means that a mathematical sign (if present
  in front of the given string) is memorized and prepended to the resulting
  substring unless that substring is empty."
  {:added "1.0.0"
   :tag String}
  ([^String s
    ^Number start]
   (not-empty
    (subs-preserve s *signs* start)))
  ([^String s
    ^Number start
    ^Number num]
   (not-empty
    (subs-preserve s *signs* start (+' start num)))))

(defn- subseq-signed
  "Safely creates a subsequence preserving its first character when it is
  a plus or a minus sign. Preservation means that a mathematical sign
  (if present in front of the given collection) is memorized and prepended
  to the resulting sequence unless that sequence is empty."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([^clojure.lang.ISeq s
    ^Number num-drop]
   (not-empty
    (subseq-preserve s *signs* num-drop)))
  ([^clojure.lang.ISeq s
    ^Number num-drop
    ^Number num-take]
   (not-empty
    (subseq-preserve s *signs* num-drop (+' num-take num-drop)))))

(defn- subvec-signed
  "Safely creates a subvector preserving its first element when it is a plus
  or a minus sign. Preservation means that a mathematical sign (if present
  in front of the given vector) is always memorized and prepended to the resulting
  vector unless that vector is empty."
  {:added "1.0.0"
   :tag clojure.lang.IPersistentVector}
  ([^clojure.lang.IPersistentVector v
    ^Number                     start]
   (not-empty
    (subvec-preserve v *signs* start)))
  ([^clojure.lang.IPersistentVector v
    ^Number                     start
    ^Number                       num]
   (not-empty
    (subvec-preserve v *signs* start (+' start num)))))

(defn- fix-sign-seq
  "Removes plus character from the head of a sequential collection."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  [^clojure.lang.ISeq coll]
  (lazy-seq
   (if (= \+ (first coll)) (next coll) coll)))

(defn- seq-negative?
  "Returns true if the digital sequence is valid and
  expresses a negative number."
  {:added "1.0.0"
   :tag Boolean}
  [^clojure.lang.ISeq coll]
  (contains? *minus-signs* (first coll)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Counting

(defn- count-digits-dec
  "Counts decimal digits of the given number.
  Returns the number of digits."
  {:added "1.0.0"
   :tag Number}
  [^BigDecimal n]
  (if (zero? n) 0
      (let [d (.scale (bigdec n))]
        (if (> d Long/MAX_VALUE) d (long d)))))

(defn- count-digits-int
  "Counts base digits of the given number. Returns the number of digits."
  {:added "1.0.0"
   :tag Number}
  [^BigDecimal n]
  (if (zero? n) 1
      (let [n (bigdec n)
            t (.precision n)
            i (-' t (.scale n))]
        (if (> i Long/MAX_VALUE) i (long i)))))

(defn- count-digits-total
  "Counts digits of the given number. Returns the number of digits."
  {:added "1.0.0"
   :tag Number}
  [^BigDecimal n]
  (if (zero? n) 1
      (let [n (bigdec n)
            t (.precision n)]
        (if (> t Long/MAX_VALUE) t (long t)))))

(defn- num-count-digits
  "Counts digits of the given number. Returns the total number of digits. If
  *decimal-point-mode* is enabled then the result includes also the count of
  decimal digits. If *decimal-point-mode* is disabled then the result
  is just the number of integer digits."
  {:added "1.0.0"
   :tag Number}
  [^BigDecimal n]
  (if (zero? n) 1
      (let [n (bigdec n)
            t (.precision n)]
        (if *decimal-point-mode* t
            (let [i (-' t (.scale n))]
              (if (> i Long/MAX_VALUE) i (long i)))))))

(defn- seq-count-digits
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq s]
  (count (filter digit? s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Conversions

(defn- num->digits-core
  "Changes a number given as n into a lazy sequence of numbers representing
  decimal digits. Returns a sequence of two elements: first is a number of
  digits and second is a sequence."
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  ([^BigDecimal n]
   (when n
     (if (zero? n)
       (lazy-seq (cons 0 nil))
       (let [n (bigdec n)
             digits-dec (count-digits-dec n)
             digits-int (-' (count-digits-total n) digits-dec)
             res-int    (num->digits-core
                         (bigint n)
                         (pow10 (if (<= digits-int 1) 0 (dec' digits-int))))]
         (if (or (not *decimal-point-mode*) (zero? digits-dec))
           res-int
           (concat
            res-int
            (cons *dot-char*
                  (num->digits-core
                   (bigint (.movePointRight (.remainder n 1M) digits-dec))
                   (pow10 (dec' digits-dec))))))))))
  ([^clojure.lang.BigInt n
    ^clojure.lang.BigInt div-by]
   (if (zero? n)
     (if (> div-by 0)
       (lazy-seq
        (cons (byte 0)
              (num->digits-core 0 (quot div-by 10))))
       nil)
     (lazy-seq
      (cons (byte (quot n div-by))
            (num->digits-core (mod n div-by) (quot div-by 10)))))))

(defn- num->digits
  "Changes a number given as n into a sequence of numbers representing decimal
  digits."
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  ([^Number n]
   (lazy-seq
    (if (neg? n)
      (cons \- (num->digits-core (-' n)))
      (if (zero? n)
        (cons 0 nil)
        (num->digits-core n))))))

(def ^{:added "1.0.0"
       :const true
       :private true
       :tag Long}
  big-seq-digits
  (count-digits-int Long/MAX_VALUE))

(def ^{:added "1.0.0"
       :const true
       :private true
       :tag Long}
  long-before
  (long (/ (Long/MAX_VALUE) 10)))

(defn- seq-big-dec->num
  "Called by seq-dec->num2 if the value is greater than Long type. Operates on
  BigInt and optionally switches to BigDec value (calling seq-big-dec->num if
  it detects decimal dot)."
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq x
   ^clojure.lang.BigInt r
   ^clojure.lang.BigInt i]
  (if (nil? x) r
      (let [v (first x)]
        (if (dot? v)
          (seq-big-dec->num (next x) (/ r i) (bigint 1))
          (recur (next x)
                 (+' r (*' v i))
                 (*' 10 i))))))

(defn- seq-dec->num2
  "Called by seq-dec->num if the count of elements of a sequence is large
  enough to exceed Long data type. Operates on BigInt and switches to
  BigDec value (calling seq-big-dec->num if it detects decimal dot)."
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq x
   ^long r
   ^clojure.lang.BigInt i]
  (if (nil? x) r
      (let [v (first x)]
        (if (dot? v)
          (seq-big-dec->num (next x) (bigdec (/ r i)) (bigint 1))
          (let [o (+' r (* v i))]
            (if (> o Long/MAX_VALUE)
              (seq-big-dec->num (next x) (bigint o) (*' 10 i))
              (recur (next x)
                     (long o)
                     (*' 10 i))))))))

(defn- seq-dec->num
  "Changes a sequence of decimal digits into BigDecimal value. Uses
  Long primitives to calculate results. If there are more elements
  that would fit into data type it passes control to seq-dec->num2
  for further calculation. Returns a number.

  Warning: nil or false element ends iteration but that shouldn't be a
  problem in case of validated sequence with digits and optional sign."
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq d]
  (when-not (empty? d)
    (loop [x (reverse d) r (long 0) i (long 1)]
      (if (nil? x) r
          (let [v (first x)]
            (if (dot? v)
              (seq-big-dec->num (next x) (bigdec (/ r i)) (bigint 1))
              (if (>= i long-before)
                (seq-dec->num2 x r (bigint i))
                (recur (next x)
                       (+ r (* ^long v i))
                       (* 10 i)))))))))

(defn- seq-big->num
  "Changes a sequence of decimal digits into BigInt value. Operates on
  BigInt values."
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq x
   ^clojure.lang.BigInt r
   ^clojure.lang.BigInt i]
  (if (nil? x) r
      (recur (next x)
             (+' r (*' (first x) i))
             (*' 10 i))))

(defn- seq-long->num2
  "Continues looping through a sequence of digits to convert them into
  a number but uses BigInt value as a counter. If the result cannot be
  accumulated in Long primitive type it passes control to seq-big->num
  function for further processing."
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq x
   ^long r
   ^clojure.lang.BigInt i]
  (if (nil? x)
    r
    (let [o (+' r (* (first x) i))]
      (if (> o Long/MAX_VALUE)
        (seq-big->num (next x) (bigint o) (*' 10 i))
        (recur (next x)
               (long o)
               (*' 10 i))))))

(defn- seq-long->num
  "Loops through the given sequence d and changes its digits into a number.
  Operates on primitive data types (long) until it's possible. If the long
  range is almost exceeded (by counting the number of already processed
  elements) it passes control to seq-long->num2 to continue calculation.
  Returns a number.

  Warning: nil or false element ends iteration but that shouldn't be a
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
          (recur
           (next x)
           (+ r (* ^long (first x) i))
           (* 10 i)))))))

(defn- seq-digits->num
  "Warning: nil or false element ends iteration but that shouldn't be a
  problem in case of validated sequence with digits and optional sign."
  {:added "1.0.0"
   :tag Number}
  [^clojure.lang.ISeq coll]
  (let [is-minus? (contains? *minus-signs* (first coll))
        coll      (if is-minus? (next coll) coll)
        r         ((if *decimal-point-mode* seq-dec->num seq-long->num) coll)]
    (if is-minus? (-' r) r)))

(defn- seq-digits->str
  {:added "1.0.0"
   :tag String}
  [^clojure.lang.ISeq s]
  (not-empty (reduce str "" s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Normalization and validation

(defn- digitize-core-num
  [^clojure.lang.ISeq src
   ^Boolean had-number?
   ^Boolean had-point?]
  (when (seq src)
    (let [e (first src)
          e (if (string? e) (str-trim e) e)
          n (next src)]
      (if (dfl-whitechar? e)
        (recur n had-number? had-point?)
        (lazy-seq
         (if-let [c (*vals-to-digits* e)]

           ;; adding another number
           (cons c (digitize-core-num n true had-point?))
           (if-let [c (*sign-to-char* e)]

             ;; adding positive or negative sign
             (if-not had-number?
               (cons c (digitize-core-num n true had-point?))
               (dig-throw-arg "The sign of a number should occur once and precede first digit"))

             ;; spreading numbers (if spread numbers is enabled)
             (if (and *spread-numbers* (digital-number? e))
               (digitize-core-num (concat (num->digits e) n) had-number? had-point?)

               ;; handling decimal point mode (if enabled)
               (if *decimal-point-mode*
                 (if (decimal-point-char? e)
                   (if had-point?
                     (dig-throw-arg "The decimal point character should occur just once")
                     (cons *dot-char* (digitize-core-num n had-number? true)))
                   (dig-throw-arg "Sequence element is not a single digit, not a sign nor a decimal point separator: " e))

                 (if (decimal-point-char? e)
                   (dig-throw-arg "Sequence element is a decimal point separator but decimal-point-mode is disabled: " e)
                   (dig-throw-arg "Sequence element is not a single digit nor a sign: " e)))))))))))

(defn- digitize-core-gen
  [^clojure.lang.ISeq src
   ^clojure.lang.Fn sep-pred
   ^clojure.lang.Fn sep-trans]
  (when (seq src)
    (let [e (first src)
          n (next src)]

      ;; skipping white characters immediately
      (if (dfl-whitechar? e)
        (recur n sep-pred sep-trans)

        (lazy-seq
         (if-let [c (*vals-to-digits* e)]

           ;; adding another number
           (cons c (digitize-core-gen n sep-pred sep-trans))

           ;; adding separator (if detected)
           (if (sep-pred e)
             (cons (sep-trans e) (digitize-core-gen n sep-pred sep-trans))

             ;; spreading numbers (if enabled)
             (if (and *spread-numbers* (digital-number? e))
               (digitize-core-gen (concat (num->digits e) n) sep-pred sep-trans)
               (dig-throw-arg "Sequence element is not a single digit nor a separator: " e)))))))))

(defn- digitize-seq
  "Normalizes collection of digits by calling the digitize-core-gen or
  digitize-core-num and optionally slices the resulting collection,
  preserving minus sign and optional separators."
  {:added "1.0.0"
   :tag clojure.lang.Fn}
  ([^clojure.lang.ISeq src
    ^Number       num-drop
    ^Number       num-take]
   (when-some [r (digitize-seq src)]
     (if *numeric-mode*
       (subseq-signed r num-drop num-take)
       (safe-subseq   r num-drop (+' num-take num-drop)))))
  ([^clojure.lang.ISeq src
    ^Number       num-take]
   (digitize-seq src 0 num-take))
  ([^clojure.lang.ISeq src]
   (not-empty
    (if *numeric-mode*
      (fix-sign-seq (digitize-core-num src false false))
      (digitize-core-gen src (dfl-separator) (dfl-translator))))))

(defn- digitize-num
  "Changes number into normalized representation. Returns number or nil."
  {:added "1.0.0"
   :tag Number}
  [^Number n]
  (if (digital-number? n) n
      (dig-throw-arg "Given number cannot be expressed as a sequence of digits.")))

(defn- digitize-char
  "Changes numeric character into normalized representation. Returns
  a digit or nil."
  {:added "1.0.0"
   :tag Character}
  [^Character c]
  (when-some [x c]
    (if (digit? x)
      (get *vals-to-digits* x)
      (dig-throw-arg "Given character does not express a digit: " x))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Decimal dot fixing

(defn- fix-dot-seq-last
  [^clojure.lang.ISeq coll]
  (lazy-seq
   (when coll
     (if-some [n (next coll)]
       (cons (first coll) (fix-dot-seq-last n))
       (if (empty? coll) coll
           (let [f (first coll)]
             (when-not (dot? f)
               (cons f nil))))))))

(defn- fix-dot-seq
  [^clojure.lang.ISeq coll]
  (fix-dot-seq-last
   (lazy-seq
    (if (dot? (first coll))
      (if (nil? (next coll))
        (rest coll)
        (cons (byte 0) coll))
      coll))))

(defn- fix-dot-vec
  [^clojure.lang.IPersistentVector v]
  (when v
    (if-not (contains? v 0) v
            (let [c (dec (count v))]
              (if (zero? c)
                (if (dot? (get v 0)) (empty v) v)
                (let [v (if (dot? (get v c)) (subvec v 0 c) v)]
                  (if (dot? (get v 0))
                    (into [(byte 0)] v)
                    v)))))))

(defn- fix-dot-str
  [^String s]
  (when s
    (if (empty? s) s
        (let [c (dec (count s))]
          (if (zero? c)
            (if (dot? (get s 0)) (empty s) s)
            (let [s (if (dot? (get s c)) (subs s 0 c) s)]
              (if (dot? (get s 0))
                (str (byte 0) s)
                s)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Digitizing protocol

(defprotocol Digitizing
  "States that a collection is able to store digits that in some circumstances
  could be used to produce valid numeric values."

  (digitize
   [coll]
   "Ensures that coll is digital by sanitizing it and performing basic
  validation. If the process succeeds it returns a sequence which elements
  are normalized versions of elements from coll. Normalized collection
  consist of numbers from 0 to 9 (as Byte objects after normalization) and
  optional + or - sign (as Character objects after normalization) in front.

  During normalization white characters are removed, digits (that might be
  characters, numbers, strings, symbols or keys) are changed into their
  numerical representations and first plus and minus signs (encoded as
  characters, strings, keywords, symbols or built-in function objects) are
  changed into + or - characters.

  When numeric mode is enabled (by setting cutils.digits/*numeric-mode* to true)
  the validation is more strict. It means that the + or - sign must appear
  just once, before any digit and the only valid separator (besides one of white
  characters and nil) is decimal dot (but only when decimal-point mode
  is enabled by setting cutils.digits/*decimal-point-mode* to true). By default
  generic mode is enabled (*numeric-mode* is set to false) so the input does
  not need to express correct number, just a bunch of digits and separators.")

  (digital?
   [coll]
   "Checks if the given object is digital. Returns true if it is, false otherwise.
  Digital means that the collection, string or a numeric type object consist
  of numbers from 0 to 9 and optional separators.

  This function normalizes the given object by calling cutils.digits/digitize
  and forces generic mode by setting cutils.digits/*numeric-mode* to false.")

  (numeric?
   [coll]
   "Checks if the given object is digital and expresses a valid, decimal number.
  Returns true if it is, false otherwise.

  Numeric means that the collection, string or a numeric type object consist
  of numbers from 0 to 9, optional + or - sign in front and a decimal dot
  character (only if decimal mode is enabled  by setting
  cutils.digits/*decimal-point-mode* to true).

  This function normalizes the given object by calling cutils.digits/digitize
  and forces numeric mode by setting cutils.digits/*numeric-mode* to true.")

  (digits->num
   [coll] [coll num-take] [coll num-drop num-take]
   "Changes a collection of digits given as coll into a number consisting
  of decimal digits. An optional argument num-take controls how many
  digits to use (from left to right) and num-drop tells how many digits
  to drop. The last one (num-drop) is applied before num-take when both are
  given.

  Each element of the collection is normalized before performing further
  operations on it (white characters are removed, digits are changed into
  their numerical representations - Byte objects) and validated (if
  the collection contains other characters the processing is stopped and
  exception is generated).

  This function forces numeric mode by setting cutils.digits/*numeric-mode*
  to true which causes validation to be more strict. It means that
  the + or - sign must appear just once, before any digit and the only
  valid separator (besides one of white characters and nil) is decimal dot
  (but only when decimal-point mode is enabled by setting
  cutils.digits/*decimal-point-mode* to true).

  If two or three arguments are given (the resulting number is going to be
  sliced) then the first plus or minus character of the given collection will
  not be taken into account during that operation (won't count when slicing).

  The function returns a number or raises an exception if something went
  wrong (e.g. input sequence was not valid). It returns nil if there was
  an empty input or mismatched ranges.")

  (digits->str
   [coll] [coll num-take] [coll num-drop num-take]
   "Changes a collection of digits given as coll into a string expressing
  series of digits. An optional argument num-take controls how many digits to
  use (from left to right) and num-drop tells how many digits to drop.
  The last one (num-drop) is applied before num-take when both are given.

  Each element of the collection is normalized before performing further
  operations on it (white characters are removed, digits are changed into
  their numerical representations - Byte objects) and validated (if
  the collection contains other characters the processing is stopped and
  exception is generated).

  When numeric mode is enabled (by setting cutils.digits/*numeric-mode* to true)
  the validation is more strict. It means that the + or - sign must appear
  just once, before any digit and the only valid separator (besides one of white
  characters and nil) is decimal dot (but only when decimal-point mode
  is enabled by setting cutils.digits/*decimal-point-mode* to true). By default
  generic mode is enabled (*numeric-mode* is set to false) so the input does
  not need to express correct number, just a bunch of digits and separators.

  If two or three arguments are given (the resulting string is going to be
  sliced) then the first plus or minus character of the given collection will
  not be taken into account during that operation (won't be counted when
  slicing).

  The function returns a string or raises an exception if something went
  wrong (e.g. input sequence was not valid). It returns nil if there was
  an empty input or mismatched ranges.")

  (digits->seq
   [coll] [coll num-take] [coll num-drop num-take]
   "Changes a collection of digits given as coll into a lazy sequence
  expressing series of digits. An optional argument num-take controls
  how many digits to use (from left to right) and num-drop tells how many
  digits to drop. The last one (num-drop) is applied before num-take
  when both are given.

  Each element of the collection is lazily normalized (white characters
  are removed, digits are changed into their numerical representations
  - Byte objects) and validated (if the collection contains other
  characters exception is generated).

  When numeric mode is enabled (by setting cutils.digits/*numeric-mode* to true)
  the validation is more strict. It means that the + or - sign must appear
  just once, before any digit and the only valid separator (besides one of white
  characters and nil) is decimal dot (but only when decimal-point mode
  is enabled by setting cutils.digits/*decimal-point-mode* to true). By default
  generic mode is enabled (*numeric-mode* is set to false) so the input does
  not need to express correct number, just a bunch of digits and separators.

  If two or three arguments are given (the resulting sequence is going to be
  sliced) then the first plus or minus character of the given collection will
  not be taken into account during that operation (won't be counted when
  slicing).

  The function returns a lazy sequence or nil if there was an empty input
  or mismatched ranges. During evaluation of sequence elements exceptions
  might raise if the input value is not valid.")

  (digits-fix-dot
   [coll]
   "Removes last dot from the given digital collection and puts 0 in front
  of first dot if detected. Does not normalize nor validate the given
  collection.")

  (negative?
   [coll]
   "Checks if a collection of digits expresses a negative number.
  Returns true or false.

  Required amount of elements (until a sign appears) of the collection
  is normalized (white characters are removed, plus and minus signs
  expressed as operators, keywords or symbols are changed into characters)
  and validated (if the collection contains other characters the processing
  is stopped and false value is returned).

  This function forces numeric mode to be enabled (by setting
  cutils.digits/*numeric-mode* to true).

  If there is a need to check whether the given, presumably digital value
  is negative and completely expresses a number or valid collection of digits
  then cutils.digits/numeric? or cutils.digits/digital? should be used to
  execute additional checks.")

  (count-digits
   [coll]
   "Counts the number of digits in a digital collection, normalizing and
  validating the collection. Returns a number.

  When numeric mode is enabled (by setting cutils.digits/*numeric-mode* to true)
  the validation is more strict. It means that the + or - sign must appear
  just once, before any digit and the only valid separator (besides one of white
  characters and nil) is a decimal dot (but only when decimal-point mode
  is enabled by setting cutils.digits/*decimal-point-mode* to true). By default
  generic mode is enabled (*numeric-mode* is set to false) so the input does
  not need to express correct number, just a bunch of digits and separators."))

(extend-protocol Digitizing

  clojure.lang.IPersistentVector

  (count-digits
      [^clojure.lang.IPersistentVector  v]                         (seq-count-digits (digitize-seq v)))
  (digitize
      [^clojure.lang.IPersistentVector  v]                         (not-empty (vec (digitize-seq v))))
  (digital?
      [^clojure.lang.IPersistentVector  v]                         (try-arg-false (every? some? (with-generic-mode! (digitize-seq v)))))
  (digits->seq
      ([^clojure.lang.IPersistentVector v]                         (digitize-seq v))
    ([^clojure.lang.IPersistentVector   v, ^Number nt]             (digitize-seq v nt))
    ([^clojure.lang.IPersistentVector   v, ^Number nd, ^Number nt] (digitize-seq v nd nt)))
  (digits->num
      ([^clojure.lang.IPersistentVector v]                         (seq-digits->num (with-numeric-mode! (digits->seq v))))
    ([^clojure.lang.IPersistentVector   v, ^Number nt]             (seq-digits->num (with-numeric-mode! (digits->seq v nt))))
    ([^clojure.lang.IPersistentVector   v, ^Number nd, ^Number nt] (seq-digits->num (with-numeric-mode! (digits->seq v nd nt)))))
  (digits->str
      ([^clojure.lang.IPersistentVector v]                         (seq-digits->str (digits->seq v)))
    ([^clojure.lang.IPersistentVector   v, ^Number nt]             (seq-digits->str (digits->seq v nt)))
    ([^clojure.lang.IPersistentVector   v, ^Number nd, ^Number nt] (seq-digits->str (digits->seq v nd nt))))
  (digits-fix-dot
      [^clojure.lang.IPersistentVector  v]                         (not-empty (fix-dot-vec v)))
  (negative?
      [^clojure.lang.IPersistentVector  v]                         (try-arg-false (seq-negative? (with-numeric-mode! (digitize-seq v)))))
  (numeric?
      [^clojure.lang.IPersistentVector  v]                         (try-arg-false (every? some? (with-numeric-mode! (digitize-seq v)))))

  clojure.lang.ISeq

  (count-digits
      [^clojure.lang.ISeq s]                                       (seq-count-digits (digitize-seq s)))
  (digitize
      [^clojure.lang.ISeq s]                                       (digitize-seq s))
  (digital?
      [^clojure.lang.ISeq s]                                       (try-arg-false (every? some? (with-generic-mode! (digitize-seq s)))))
  (digits->seq
      ([^clojure.lang.ISeq s]                                      (digitize-seq s))
    ([^clojure.lang.ISeq   s, ^Number nt]                          (digitize-seq s nt))
    ([^clojure.lang.ISeq   s, ^Number nd, ^Number nt]              (digitize-seq s nd nt)))
  (digits->num
      ([^clojure.lang.ISeq s]                                      (seq-digits->num (with-numeric-mode! (digits->seq s))))
    ([^clojure.lang.ISeq   s, ^Number nt]                          (seq-digits->num (with-numeric-mode! (digits->seq s nt))))
    ([^clojure.lang.ISeq   s, ^Number nd, ^Number nt]              (seq-digits->num (with-numeric-mode! (digits->seq s nd nt)))))
  (digits->str
      ([^clojure.lang.ISeq s]                                      (seq-digits->str (digits->seq s)))
    ([^clojure.lang.ISeq   s, ^Number nt]                          (seq-digits->str (digits->seq s nt)))
    ([^clojure.lang.ISeq   s, ^Number nd, ^Number nt]              (seq-digits->str (digits->seq s nd nt))))
  (digits-fix-dot
      [^clojure.lang.ISeq  s]                                      (not-empty (fix-dot-seq s)))
  (negative?
      [^clojure.lang.ISeq  s]                                      (try-arg-false (seq-negative? (with-numeric-mode! (digitize-seq s)))))
  (numeric?
      [^clojure.lang.ISeq s]                                       (try-arg-false (every? some? (with-numeric-mode! (digitize-seq s)))))

  java.lang.String

  (count-digits
      [^String  s]                                                 (seq-count-digits (digitize-seq s)))
  (digitize
      [^String  s]                                                 (not-empty (seq-digits->str (digitize-seq s))))
  (digital?
      [^String  s]                                                 (try-arg-false (every? some? (with-generic-mode! (digitize-seq s)))))
  (digits->seq
      ([^String s]                                                 (digitize-seq s))
    ([^String   s, ^Number nt]                                     (digitize-seq s nt))
    ([^String   s, ^Number nd, ^Number nt]                         (digitize-seq s nd nt)))
  (digits->str
      ([^String s]                                                 (seq-digits->str (digits->seq s)))
    ([^String   s, ^Number nt]                                     (seq-digits->str (digits->seq s nt)))
    ([^String   s, ^Number nd, ^Number nt]                         (seq-digits->str (digits->seq s nd nt))))
  (digits->num
      ([^String s]                                                 (seq-digits->num (with-numeric-mode! (digits->seq s))))
    ([^String   s, ^Number nt]                                     (seq-digits->num (with-numeric-mode! (digits->seq s nt))))
    ([^String   s, ^Number nd, ^Number nt]                         (seq-digits->num (with-numeric-mode! (digits->seq s nd nt)))))
  (digits-fix-dot
      [^String  s]                                                 (not-empty (fix-dot-str s)))
  (negative?
      [^String  s]                                                 (try-arg-false (seq-negative? (with-numeric-mode! (digitize-seq s)))))
  (numeric?
      [^String  s]                                                 (try-arg-false (every? some? (with-numeric-mode! (digitize-seq s)))))

  clojure.lang.Symbol

  (count-digits
      [^clojure.lang.Symbol  s]                                    (seq-count-digits (digitize-seq (name s))))
  (digitize
      [^clojure.lang.Symbol  s]                                    (when-let [x (digitize (name s))] (symbol x)))
  (digital?
      [^clojure.lang.Symbol  s]                                    (digital? (name s)))
  (digits->seq
      ([^clojure.lang.Symbol s]                                    (digits->seq (name s)))
    ([^clojure.lang.Symbol   s, ^Number nt]                        (digits->seq (name s) nt))
    ([^clojure.lang.Symbol   s, ^Number nd, ^Number nt]            (digits->seq (name s) nd nt)))
  (digits->str
      ([^clojure.lang.Symbol s]                                    (digits->str (name s)))
    ([^clojure.lang.Symbol   s, ^Number nt]                        (digits->str (name s) nt))
    ([^clojure.lang.Symbol   s, ^Number nd, ^Number nt]            (digits->str (name s) nd nt)))
  (digits->num
      ([^clojure.lang.Symbol s]                                    (digits->num (name s)))
    ([^clojure.lang.Symbol   s, ^Number nt]                        (digits->num (name s) nt))
    ([^clojure.lang.Symbol   s, ^Number nd, ^Number nt]            (digits->num (name s) nd nt)))
  (digits-fix-dot
      [^clojure.lang.Symbol  s]                                    (when-let [x (fix-dot-str (name s))] (symbol x)))
  (negative?
      [^clojure.lang.Symbol  s]                                    (negative? (name s)))
  (numeric?
      [^clojure.lang.Symbol  s]                                    (numeric? (name s)))

  clojure.lang.Keyword

  (count-digits
      [^clojure.lang.Keyword  s]                                   (seq-count-digits (digitize-seq (name s))))
  (digitize
      [^clojure.lang.Keyword  s]                                   (when-let [x (digitize (name s))] (keyword x)))
  (digital?
      [^clojure.lang.Keyword  s]                                   (digital? (name s)))
  (digits->seq
      ([^clojure.lang.Keyword s]                                   (digits->seq (name s)))
    ([^clojure.lang.Keyword   s, ^Number nt]                       (digits->seq (name s) nt))
    ([^clojure.lang.Keyword   s, ^Number nd, ^Number nt]           (digits->seq (name s) nd nt)))
  (digits->str
      ([^clojure.lang.Keyword s]                                   (digits->str (name s)))
    ([^clojure.lang.Keyword   s, ^Number nt]                       (digits->str (name s) nt))
    ([^clojure.lang.Keyword   s, ^Number nd, ^Number nt]           (digits->str (name s) nd nt)))
  (digits->num
      ([^clojure.lang.Keyword s]                                   (digits->num (name s)))
    ([^clojure.lang.Keyword   s, ^Number nt]                       (digits->num (name s) nt))
    ([^clojure.lang.Keyword   s, ^Number nd, ^Number nt]           (digits->num (name s) nd nt)))
  (digits-fix-dot
      [^clojure.lang.Keyword  s]                                   (when-let [x (fix-dot-str (name s))] (keyword x)))
  (negative?
      [^clojure.lang.Keyword  s]                                   (negative? (name s)))
  (numeric?
      [^clojure.lang.Keyword  s]                                   (numeric? (name s)))

  java.lang.Character

  (count-digits
      [^Character  c]                                              (if (digit? (digitize-char c)) 1 0))
  (digitize
      [^Character  c]                                              (digitize-char c))
  (digital?
      [^Character  c]                                              (try-arg-false (some? (with-generic-mode! (digitize-char c)))))
  (digits->seq
      ([^Character c]                                              (not-empty (lazy-seq (cons (digitize-char c) nil))))
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
  (digits-fix-dot
      [^Character  c]                                              (when-not (dot? c) c))
  (negative?
      [^Character  c]                                              (try-arg-false (seq-negative? (with-numeric-mode! (digitize-char c)))))
  (numeric?
      [^Character  c]                                              (try-arg-false (some? (with-numeric-mode! (digitize-char c)))))

  java.lang.Number

  (count-digits
      [^Number  n]                                                 (num-count-digits (digitize-num n)))
  (digitize
      [^Number  n]                                                 (digitize-num n))
  (digital?
      [^Number n]                                                  (try-arg-false (some? (digitize-num n))))
  (digits->seq
      ([^Number n]                                                 (num->digits (digitize-num n)))
    ([^Number   n, ^Number nt]                                     (subseq-signed (digits->seq n) 0 nt))
    ([^Number   n, ^Number nd, ^Number nt]                         (subseq-signed (digits->seq n) nd nt)))
  (digits->num
      ([^Number n]                                                 (digitize-num n))
    ([^Number   n, ^Number nt]                                     (digits->num (digits->seq n nt)))
    ([^Number   n, ^Number nd, ^Number nt]                         (digits->num (digits->seq n nd nt))))
  (digits->str
      ([^Number n]                                                 (not-empty (str (digitize-num n))))
    ([^Number n, ^Number nt]                                       (seq-digits->str (digits->seq n nt)))
    ([^Number n, ^Number nd, ^Number nt]                           (seq-digits->str (digits->seq n nd nt))))
  (digits-fix-dot
      [^Number n]                                                  n)
  (negative?
      [^Number  n]                                                 (neg? n))
  (numeric?
      [^Number n]                                                  (digital? n))

  Object

  (count-digits   [o] (count-digits   (->str o)))
  (digitize       [o] (digitize       (->str o)))
  (digital?       [o] (digital?       (->str o)))
  (negative?      [o] (negative?      (->str o)))
  (numeric?       [o] (numeric?       (->str o)))
  (digits-fix-dot [o] (digits-fix-dot (->str o)))
  (digits->seq
      ([o]     (digits->seq (->str o)))
    ([o nt]    (digits->seq (->str o) nt))
    ([o nd nt] (digits->seq (->str o) nd nt)))
  (digits->num
      ([o]     (digits->num (->str o)))
    ([o nt]    (digits->num (->str o) nt))
    ([o nd nt] (digits->num (->str o) nd nt)))
  (digits->str
      ([o]     (digits->str (->str o)))
    ([o nt]    (digits->str (->str o) nt))
    ([o nd nt] (digits->str (->str o) nd nt)))

  nil

  (count-digits   [o]     0)
  (digitize       [o]   nil)
  (digital?       [o] false)
  (negative?      [o] false)
  (numeric?       [o] false)
  (digits-fix-dot [o]   nil)
  (digits->seq
      ([o]     nil)
    ([o nt]    nil)
    ([o nd nt] nil))
  (digits->num
      ([o]     nil)
    ([o nt]    nil)
    ([o nd nt] nil))
  (digits->str
      ([o]     nil)
    ([o nt]    nil)
    ([o nd nt] nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Marking and checking

(defn digitizing?
  "Returns true if coll satisfies the Digitizing protocol."
  {:added "1.0.0"
   :tag Boolean}
  [coll]
  (satisfies? Digitizing coll))

