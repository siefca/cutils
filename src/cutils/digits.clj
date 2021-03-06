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
  decimal mark characters and single sign characters are allowed when
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
  *decimal-mark-mode*
  "If set to true allows decimal dot to appear in a sequence when
  processing it in numeric mode."
  true)

(def ^{:added "1.0.0"
       :const true
       :tag java.lang.Character}
  dot-char
  \.)

(def ^{:added "1.0.0"
       :const true
       :tag java.lang.Character}
  minus-char
  \-)

(def ^{:added "1.0.0"
       :const true
       :tag java.lang.Character}
  plus-char
  \+)

(def ^{:added "1.0.0"
       :const true
       :tag java.lang.Character}
  zero-char
  \0)

(def ^{:added "1.0.0"
       :const true
       :tag java.lang.Byte}
  zero-byte
  (byte 0))

(def ^{:added "1.0.0"
       :const true
       :tag java.lang.Byte}
  nine-byte
  (byte 9))

(def ^{:added "1.0.0"
       :dynamic true
       :tag clojure.lang.IPersistentSet}
  *decimal-mark-chars*
  (let [ds (str dot-char)]
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
          (keyword ds)
          (str ds)
          (str \,))))

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
       :const true
       :tag clojure.lang.ISeq}
  java-digital-bytes
  (map byte (range 0 10)))

(def ^{:added "1.0.0"
       :dynamic true
       :tag clojure.lang.IPersistentMap}
  *vals-to-digits*
  (let [snum java-digital-bytes
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
  (int zero-char))

(def ^{:added "1.0.0"
       :dynamic true
       :tag clojure.lang.IPersistentSet}
  *minus-signs*
  "Minus signs."
  (let [s (symbol (str minus-char))
        c #{minus-char
            (str          minus-char)
            (keyword (str minus-char))
            s}]
    (if-some [f (resolve s)] (conj c f (var-get f)) c)))

(def ^{:added "1.0.0"
       :dynamic true
       :tag clojure.lang.IPersistentSet}
  *plus-signs*
  "Plus signs."
  (let [s (symbol (str plus-char))
        c #{plus-char
            (str          plus-char)
            (keyword (str plus-char))
            s}]
    (if-some [f (resolve s)] (conj c f (var-get f)) c)))

(def ^{:added "1.0.0"
       :dynamic true
       :tag clojure.lang.IPersistentMap}
  *sign-to-char*
  "Map of plus and minus signs to characters."
  (apply hash-map (concat
                   (interleave *minus-signs* (repeat minus-char))
                   (interleave  *plus-signs* (repeat  plus-char)))))

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
  {- minus-char, + plus-char})

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

(defmacro without-numeric-mode!
  "Disables numeric mode processing for the given S-expression."
  {:added "1.0.0"}
  [& body]
  `(binding [cutils.digits/*numeric-mode* false] ~@body))

(defmacro with-generic-mode!
  "Disables numeric mode processing for the given S-expression."
  {:added "1.0.0"}
  [& body]
  `(binding [cutils.digits/*numeric-mode* false] ~@body))

(defmacro without-generic-mode!
  "Enables numeric mode processing for the given S-expression."
  {:added "1.0.0"}
  [& body]
  `(binding [cutils.digits/*numeric-mode* true] ~@body))

(defmacro with-decimal-mark-mode!
  "Enables decimal mark detection for the given S-expression."
  {:added "1.0.0"}
  [& body]
  `(binding [cutils.digits/*decimal-mark-mode* true] ~@body))

(defmacro without-decimal-mark-mode!
  "Disables decimal mark detection for the given S-expression."
  {:added "1.0.0"}
  [& body]
  `(binding [cutils.digits/*decimal-mark-mode* false] ~@body))

(defmacro with-spread-numbers!
  "Enables numbers spreading when processing digital sequences
  for the given S-expression."
  {:added "1.0.0"}
  [& body]
  `(binding [cutils.digits/*spread-numbers* true] ~@body))

(defmacro without-spread-numbers!
  "Enables numbers spreading when processing digital sequences
  for the given S-expression."
  {:added "1.0.0"}
  [& body]
  `(binding [cutils.digits/*spread-numbers* false] ~@body))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers

(defn dot?
  "True if the given argument is a dot character."
  {:added "1.0.0"
   :tag Boolean}
  [^Character c]
  (= dot-char c))

(defn zero-char?
  "True if the given argument is a zero character."
  {:added "1.0.0"
   :tag Boolean}
  [^Character c]
  (= zero-char c))

(defn zero-byte?
  "True if the given argument is a zero byte."
  {:added "1.0.0"
   :tag Boolean}
  [^Number c]
  (= zero-byte c))

(defn minus?
  "True if the given argument is a minus character."
  {:added "1.0.0"
   :tag Boolean}
  [^Character c]
  (= minus-char c))

(defn plus?
  "True if the given argument is a plus character."
  {:added "1.0.0"
   :tag Boolean}
  [^Character c]
  (= plus-char c))

(defn decimal-mark?
  "True if the given argument is decimal mark character."
  {:added "1.0.0"
   :tag Boolean}
  [^Character c]
  (contains? *decimal-mark-chars* c))

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

(defn byte-digit?
  "Returns true if the given object is a digit and is a kind of java.lang.Byte."
  {:added "1.0.0"
   :tag java.lang.Boolean}
  [^Number n]
  (and (instance? Byte n) (<= zero-byte n nine-byte)))

(defn- some-byte-digit?
  "Returns true if the given collection has at least one digit that is
  a kind of java.lang.Byte. Otherwise it returns nil.

  If the second argument is present it controls how many first elements
  to test."
  {:added "1.0.0"
   :tag java.lang.Boolean}
  ([^clojure.lang.ISeq coll]          (some byte-digit? coll))
  ([^long n, ^clojure.lang.ISeq coll] (some byte-digit? (take n coll))))

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

(defn- dfl-separator-fn
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

(defn- dfl-translator-fn
  "Returns a function that translates certain separator objects."
  {:added "1.0.0"
   :tag clojure.lang.Fn}
  []
  (if-let [t (not-empty *separators-translate*)]
    (fn [o] (get *separators-translate* o o))
    identity))

(defmacro try-or-false
  "Executes body catching IllegalArgumentException and if the result is
  false or nil it returns false."
  {:added "1.0.0"
   :tag clojure.lang.Fn}
  [& body]
  `(or (try-arg-false ~@body) false))

(defn- subs-signed
  "Safely creates a substring preserving its first character when it is a plus
  or a minus sign. Preservation means that a mathematical sign (if present
  in front of the given string) is memorized and prepended to the resulting
  substring unless that substring is empty."
  {:added "1.0.0"
   :tag String}
  ([^String s
    ^Number start]
   (not-empty (subs-preserve s *signs* start)))
  ([^String s
    ^Number start
    ^Number num]
   (not-empty (subs-preserve s *signs* start (+' start num)))))

(defn- subseq-signed
  "Safely creates a subsequence preserving its first character when it is
  a plus or a minus sign. Preservation means that a mathematical sign
  (if present in front of the given collection) is memorized and prepended
  to the resulting sequence unless that sequence is empty."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([^clojure.lang.ISeq s
    ^Number num-drop]
   (not-empty (subseq-preserve s *signs* num-drop)))
  ([^clojure.lang.ISeq s
    ^Number num-drop
    ^Number num-take]
   (not-empty (subseq-preserve s *signs* num-drop (+' num-take num-drop)))))

;; TODO: only java bytes? (always already normalized?)

(defn- subseq-num
  "Safely creates a subsequence preserving its first character when it is
  a plus or a minus sign. Preservation means that a mathematical sign
  (if present in front of the given collection) is memorized and prepended
  to the resulting sequence unless that sequence is empty. The drop and
  take arguments are numeric elements to be dropped and/or taken."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([^clojure.lang.ISeq s
    ^Number num-drop]
   (not-empty (subseq-preserve-select s ** *signs* num-drop)))
  ([^clojure.lang.ISeq s
    ^Number num-drop
    ^Number num-take]
   (not-empty (subseq-preserve-select s ** *signs* num-drop (+' num-take num-drop)))))


(defn- subvec-signed
  "Safely creates a subvector preserving its first element when it is a plus
  or a minus sign. Preservation means that a mathematical sign (if present
  in front of the given vector) is always memorized and prepended to the resulting
  vector unless that vector is empty."
  {:added "1.0.0"
   :tag clojure.lang.IPersistentVector}
  ([^clojure.lang.IPersistentVector v
    ^Number                     start]
   (not-empty (subvec-preserve v *signs* start)))
  ([^clojure.lang.IPersistentVector v
    ^Number                     start
    ^Number                       num]
   (not-empty (subvec-preserve v *signs* start (+' start num)))))

(defn- fix-sign-seq
  "Removes plus character from the head of a sequential collection."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  [^clojure.lang.ISeq coll]
  (lazy-seq
   (if (plus? (first coll)) (rest coll) coll)))

(defn- seq-negative?
  "Returns true if the digital sequence is valid and
  expresses a negative number."
  {:added "1.0.0"
   :tag Boolean}
  [^clojure.lang.ISeq coll]
  (contains? *minus-signs* (first coll)))

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
    (let [f (first coll)]
      (if (dot? f)
        (if (nil? (next coll))
          (rest coll)
          (cons zero-byte coll))
        (if (minus? f)
          (cons f (fix-dot-seq (next coll)))
          coll))))))

(defn- fix-dot-vec
  [^clojure.lang.IPersistentVector v]
  (when v
    (if-not (contains? v 0) v
            (let [c (dec (count v))]
              (if (zero? c)
                (if (dot? (get v 0)) (empty v) v)
                (let [v (if (dot? (get v c)) (subvec v 0 c) v)
                      f (get v 0)]
                  (if (dot? f)
                    (into [zero-byte] v)
                    (if (and (minus? f) (dot? (get v 1)))
                      (into [f zero-byte] (subvec v 1))
                      v))))))))

(defn- fix-dot-str
  [^String s]
  (when s
    (if (empty? s) s
        (let [c (dec (count s))]
          (if (zero? c)
            (if (dot? (get s 0)) (empty s) s)
            (let [s (if (dot? (get s c)) (subs s 0 c) s)
                  f (get s 0)]
              (if (dot? f)
                (str zero-byte s)
                (if (and (minus? f) (dot? (get s 1)))
                  (str f zero-byte (subs s 1))
                  s))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Removing minus zero

(defn- cleanmz-seq
  [^clojure.lang.ISeq coll]
  (lazy-seq
   (if (minus? (first coll))
     (if-some [n (next coll)]
       (if (and (nil? (next n)) (zero-byte? (first n))) n coll)
       coll)
     coll)))

(defn- cleanmz-str
  [^String s]
  (when s
    (if (and
         (= 2 (count s))
         (minus? (get s 0))
         (= zero-char (get s 1)))
      (subs s 1)
      s)))

(defn- cleanmz-vec
  [^clojure.lang.IPersistentVector v]
  (when v
    (if (and
         (= 2 (count v))
         (minus? (get v 0))
         (= zero-char (get v 1)))
      (subvec v 1)
      v)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Counting

(defn- count-digits-dec
  "Counts decimal digits of the given number.
  Returns the number of digits."
  {:added "1.0.0"
   :tag Number}
  [^BigDecimal n]
  (if (zero? n) 1
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
  *decimal-mark-mode* is enabled then the result includes also the count of
  decimal digits. If *decimal-mark-mode* is disabled then the result
  is just the number of integer digits."
  {:added "1.0.0"
   :tag Number}
  [^BigDecimal n]
  (if (zero? n) 1
      (let [n (bigdec n)
            t (.precision n)]
        (if *decimal-mark-mode* t
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
  decimal digits. Returns a sequence."
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  ([^BigDecimal n]
   (when n
     (if (zero? n)
       (lazy-seq (cons zero-byte nil))
       (let [n (bigdec n)
             digits-dec (count-digits-dec n)
             digits-int (-' (count-digits-total n) digits-dec)
             res-int    (num->digits-core
                         (bigint n)
                         (pow10 (if (<= digits-int 1) 0 (dec' digits-int))))]
         (if (or (not *decimal-mark-mode*) (zero? digits-dec))
           res-int
           (concat
            res-int
            (cons dot-char
                  (num->digits-core
                   (bigint (.movePointRight (.remainder n 1M) digits-dec))
                   (pow10 (dec' digits-dec))))))))))
  ([^clojure.lang.BigInt n
    ^clojure.lang.BigInt div-by]
   (if (zero? n)
     (if (> div-by 0)
       (lazy-seq
        (cons zero-byte
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
      (cons minus-char (num->digits-core (-' n)))
      (if (zero? n)
        (cons zero-byte nil)
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
        r         ((if *decimal-mark-mode* seq-dec->num seq-long->num) coll)]
    (if is-minus? (-' r) r)))

(defn- seq-digits->str
  {:added "1.0.0"
   :tag String}
  [^clojure.lang.ISeq s]
  (not-empty (reduce str "" s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Normalization and validation

(defn- digitize-core-num-fn
  {:added "1.0.0"
   :tag clojure.lang.Fn}
  []

  (let [had-numsig? (volatile! false)
        had-mark?   (volatile! false)]

    (fn ^clojure.lang.ISeq digitize-core-num [^clojure.lang.ISeq src]
      (lazy-seq
       (when (seq src)
         (let [e (first src)
               e (if (string? e) (str-trim e) e)
               n (next src)]

           (if (dfl-whitechar? e)
             (digitize-core-num n)
             (if-let [c (*vals-to-digits* e)]

               ;; adding another number
               (do (or @had-numsig? (vreset! had-numsig? true)) (cons c (digitize-core-num n)))
               (if-let [c (*sign-to-char* e)]

                 ;; adding positive or negative sign
                 (if @had-numsig?
                   (dig-throw-arg "The sign of a number should occur once and precede first digit")
                   (do
                     (vreset! had-numsig? true)
                     (cons c (digitize-core-num n))))

                 ;; spreading numbers (if spread numbers is enabled)
                 (if (and *spread-numbers* (digital-number? e))
                   (digitize-core-num (concat (num->digits e) n))

                   ;; handling decimal mark mode (if enabled)
                   (if *decimal-mark-mode*
                     (if (decimal-mark? e)
                       (if @had-mark?
                         (dig-throw-arg "The decimal mark should occur just once")
                         (do
                           (vreset! had-mark? true)
                           (cons dot-char (digitize-core-num n))))
                       (dig-throw-arg "Element is not a single digit, not a sign nor a decimal mark: " e))

                     (if (decimal-mark? e)
                       (dig-throw-arg "Element is a decimal mark but decimal-mark-mode is disabled: " e)
                       (dig-throw-arg "Element is not a single digit nor a sign: " e)))))))))))))

(defn- digitize-core-gen-fn
  {:added "1.0.0"
   :tag clojure.lang.Fn}
  [^clojure.lang.Fn sep-fn
   ^clojure.lang.Fn trn-fn]

  (fn ^clojure.lang.ISeq digitize-core-gen [^clojure.lang.ISeq src]
    (lazy-seq
     (when (seq src)
       (let [e (first src)
             n (next  src)]

         ;; skipping white characters
         (if (dfl-whitechar? e)
           (digitize-core-gen n)
           (if-let [c (*vals-to-digits* e)]

             ;; adding another number
             (cons c (digitize-core-gen n))

             ;; adding separator (if detected)
             (if (sep-fn e) (cons (trn-fn e) (digitize-core-gen n))

                 ;; spreading numbers (if enabled)
                 (if (and *spread-numbers* (digital-number? e))
                   (digitize-core-gen (concat (num->digits e) n))
                   (dig-throw-arg "Element is not a single digit nor a separator: " e))))))))))

(defn- seek-digits
  [^clojure.lang.ISeq src]
  (when (some? src)
    (let [had-number? (volatile! false)]
      ((fn ^clojure.lang.ISeq sd [^clojure.lang.ISeq o]
         (lazy-seq
          (if (seq o)
            (let [f (first o)]
              (and (byte-digit? (first o)) (not @had-number?) (vreset! had-number? true))
              (cons f (sd (next o))))
            (when-not @had-number? (dig-throw-arg "No digits found in a series"))))) src))))

(defn- postvalid-seq-gen
  [^clojure.lang.ISeq src]
  (not-empty (seek-digits src)))

(defn- postvalid-seq-num
  [^clojure.lang.ISeq src]
  (when-not (or (nil? src) (some-byte-digit? 3 src))
    (dig-throw-arg "No digits found in a numeric series"))
  (not-empty (seek-digits (cleanmz-seq (fix-dot-seq (fix-sign-seq src))))))

(defn- digitize-seq
  "Normalizes collection of digits by calling the digitize-core-gen or
  digitize-core-num and optionally slices the resulting collection,
  preserving minus sign and optional separators."
  {:added "1.0.0"
   :tag clojure.lang.Fn}
  ([^clojure.lang.ISeq src
    ^Number num-drop
    ^Number num-take]
   (when (seq src)
     (if *numeric-mode*
       (-> src
           ((digitize-core-num-fn))
           (subseq-signed num-drop num-take)
           postvalid-seq-num)
       (-> src
           ((digitize-core-gen-fn (dfl-separator-fn) (dfl-translator-fn)))
           (safe-subseq num-drop (+' num-take num-drop))
           postvalid-seq-gen))))
  ([^clojure.lang.ISeq src
    ^Number num-take]
   (digitize-seq src 0 num-take))
  ([^clojure.lang.ISeq src]
   (when (seq src)
     (if *numeric-mode*
       (-> src
           ((digitize-core-num-fn))
           postvalid-seq-num)
       (-> src
           ((digitize-core-gen-fn (dfl-separator-fn) (dfl-translator-fn)))
           postvalid-seq-gen)))))

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
      (dig-throw-arg "Given character does not express a digit: " (str x)))))

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

    When numeric mode is enabled (by setting cutils.digits/*numeric-mode* to
    true) the validation is more strict. It means that the + or - sign must
    appear just once, before any digit and the only valid separator (besides
    one of white characters and nil) is decimal dot (but only when decimal
    mark mode is enabled by setting cutils.digits/*decimal-mark-mode* to
    true). By default generic mode is enabled (*numeric-mode* is set to false)
    so the input does not need to express correct number, just a bunch of
    digits and separators.")

  (digital?
    [coll]
    "Checks if the given object is digital. Returns true if it is, false otherwise.
    Digital means that the collection, string or a numeric type object consist
    of numbers from 0 to 9 and optional separators.

    This function normalizes the given object by calling
    cutils.digits/digitize and forces generic mode by setting
    cutils.digits/*numeric-mode* to false.")

  (numeric?
    [coll]
    "Checks if the given object is digital and expresses a valid, decimal number.
    Returns true if it is, false otherwise.

    Numeric means that the collection, string or a numeric type object consist
    of numbers from 0 to 9, optional + or - sign in front and a decimal dot
    character (only if decimal mode is enabled by setting
    cutils.digits/*decimal-mark-mode* to true).

    This function normalizes the given object by calling cutils.digits/digitize
    and forces numeric mode by setting cutils.digits/*numeric-mode* to true.")

  (digits->num
    [coll] [coll num-take] [coll num-drop num-take]
    "Changes a collection of digits given as coll into a number consisting of
    decimal digits. An optional argument num-take controls how many digits to
    use (from left to right) and num-drop tells how many digits to drop. The
    last one (num-drop) is applied before num-take when both are given.

    Each element of the collection is normalized before performing further
    operations on it (white characters are removed, digits are changed into
    their numerical representations - Byte objects) and validated (if the
    collection contains other characters the processing is stopped and
    exception is generated).

    This function forces numeric mode by setting cutils.digits/*numeric-mode*
    to true which causes validation to be more strict. It means that the + or
    - sign must appear just once, before any digit and the only valid
    separator (besides one of white characters and nil) is decimal dot
    (but only when decimal-mark mode is enabled by setting
    cutils.digits/*decimal-mark-mode* to true).

    If two or three arguments are given (the resulting number is going to be
    sliced) then the first plus or minus character of the given collection
    will not be taken into account during that operation (won't count when
    slicing).

    The function returns a number or raises an exception if something went
    wrong (e.g. input sequence was not valid). It returns nil if there was an
    empty input or mismatched ranges.")

  (digits->str
    [coll] [coll num-take] [coll num-drop num-take]
    "Changes a collection of digits given as coll into a string expressing
    series of digits. An optional argument num-take controls how many digits
    to use (from left to right) and num-drop tells how many digits to drop.
    The last one (num-drop) is applied before num-take when both are given.

    Each element of the collection is normalized before performing further
    operations on it (white characters are removed, digits are changed into
    their numerical representations - Byte objects) and validated (if the
    collection contains other characters the processing is stopped and
    exception is generated).

    When numeric mode is enabled (by setting cutils.digits/*numeric-mode* to
    true) the validation is more strict. It means that the + or - sign must
    appear just once, before any digit and the only valid separator (besides
    one of white characters and nil) is decimal dot (but only when
    decimal-mark mode is enabled by setting cutils.digits/*decimal-mark-mode*
    to true). By default generic mode is enabled (*numeric-mode* is set to
    false) so the input does not need to express correct number, just a bunch
    of digits and separators.

    If two or three arguments are given (the resulting string is going to be
    sliced) then the first plus or minus character of the given collection
    will not be taken into account during that operation (won't be counted
    when slicing).

    The function returns a string or raises an exception if something went
    wrong (e.g. input sequence was not valid). It returns nil if there was an
    empty input or mismatched ranges.")

  (digits->seq
    [coll] [coll num-take] [coll num-drop num-take]
    "Changes a collection of digits given as coll into a lazy sequence
    expressing series of digits. An optional argument num-take controls how
    many digits to use (from left to right) and num-drop tells how many digits
    to drop. The last one (num-drop) is applied before num-take when both are
    given.

    Each element of the collection is lazily normalized (white characters are
    removed, digits are changed into their numerical representations - Byte
    objects) and validated (if the collection contains other characters
    exception is generated).

    When numeric mode is enabled (by setting cutils.digits/*numeric-mode* to
    true) the validation is more strict. It means that the + or - sign must
    appear just once, before any digit and the only valid separator (besides
    one of white characters and nil) is decimal dot (but only when decimal
    mark mode is enabled by setting cutils.digits/*decimal-mark-mode* to
    true). By default generic mode is enabled (*numeric-mode* is set to false)
    so the input does not need to express correct number, just a bunch of
    digits and separators.

    If two or three arguments are given (the resulting sequence is going to be
    sliced) then the first plus or minus character of the given collection
    will not be taken into account during that operation (won't be counted
    when slicing).

    The function returns a lazy sequence or nil if there was an empty input or
    mismatched ranges. During evaluation of sequence elements exceptions might
    raise if the input value is not valid.")

  (digits-fix-dot
    [coll]
    "Removes last dot from the given digital collection and puts 0 in front of
    first dot if detected. Does not normalize nor validate the given
    collection.")

  (negative?
    [coll]
    "Checks if a collection of digits expresses a negative number.
    Returns true or false.

    Required amount of elements (until a sign appears) of the collection is
    normalized (white characters are removed, plus and minus signs expressed
    as operators, keywords or symbols are changed into characters) and
    validated (if the collection contains other characters the processing is
    stopped and false value is returned).

    This function forces numeric mode to be enabled (by setting
    cutils.digits/*numeric-mode* to true).

    If there is a need to check whether the given, presumably digital value is
    negative and completely expresses a number or valid collection of digits
    then cutils.digits/numeric? or cutils.digits/digital? should be used to
    execute additional checks.")

  (count-digits
    [coll]
    "Counts the number of digits in a digital collection, normalizing and
    validating the collection. Returns a number.

    When numeric mode is enabled (by setting cutils.digits/*numeric-mode* to
    true) the validation is more strict. It means that the + or - sign must
    appear just once, before any digit and the only valid separator (besides
    one of white characters and nil) is a decimal dot (but only when decimal
    mark mode is enabled by setting cutils.digits/*decimal-mark-mode* to
    true). By default generic mode is enabled (*numeric-mode* is set to false)
    so the input does not need to express correct number, just a bunch of
    digits and separators."))

(extend-protocol Digitizing

  clojure.lang.IPersistentVector

  (count-digits [v]              (seq-count-digits (digitize-seq v)))
  (digitize     [v]              (not-empty (vec   (digitize-seq v))))
  (digital?     [v]              (try-or-false (some-byte-digit? (with-generic-mode! (digitize-seq v)))))
  (negative?    [v]              (try-or-false (seq-negative?    (with-numeric-mode! (digitize-seq v)))))
  (numeric?     [v]              (try-or-false (some-byte-digit? (with-numeric-mode! (digitize-seq v)))))
  (digits->seq
    ([v]                         (digitize-seq v))
    ([v, ^Number nt]             (digitize-seq v nt))
    ([v, ^Number nd, ^Number nt] (digitize-seq v nd nt)))
  (digits->num
    ([v]                         (seq-digits->num (with-numeric-mode! (digits->seq v))))
    ([v, ^Number nt]             (seq-digits->num (with-numeric-mode! (digits->seq v nt))))
    ([v, ^Number nd, ^Number nt] (seq-digits->num (with-numeric-mode! (digits->seq v nd nt)))))
  (digits->str
    ([v]                         (seq-digits->str (digits->seq v)))
    ([v, ^Number nt]             (seq-digits->str (digits->seq v nt)))
    ([v, ^Number nd, ^Number nt] (seq-digits->str (digits->seq v nd nt))))

  clojure.lang.ISeq

  (count-digits [s]              (seq-count-digits (digitize-seq s)))
  (digitize     [s]              (digitize-seq s))
  (digital?     [s]              (try-or-false (some-byte-digit? (with-generic-mode! (digitize-seq s)))))
  (negative?    [s]              (try-or-false (seq-negative?    (with-numeric-mode! (digitize-seq s)))))
  (numeric?     [s]              (try-or-false (some-byte-digit? (with-numeric-mode! (digitize-seq s)))))
  (digits->seq
    ([s]                         (digitize-seq s))
    ([s, ^Number nt]             (digitize-seq s nt))
    ([s, ^Number nd, ^Number nt] (digitize-seq s nd nt)))
  (digits->num
    ([s]                         (seq-digits->num (with-numeric-mode! (digits->seq s))))
    ([s, ^Number nt]             (seq-digits->num (with-numeric-mode! (digits->seq s nt))))
    ([s, ^Number nd, ^Number nt] (seq-digits->num (with-numeric-mode! (digits->seq s nd nt)))))
  (digits->str
    ([s]                         (seq-digits->str (digits->seq s)))
    ([s, ^Number nt]             (seq-digits->str (digits->seq s nt)))
    ([s, ^Number nd, ^Number nt] (seq-digits->str (digits->seq s nd nt))))

  java.lang.String

  (count-digits [s]              (seq-count-digits (digitize-seq s)))
  (digitize     [s]              (not-empty (seq-digits->str (digitize-seq s))))
  (digital?     [s]              (try-or-false (some-byte-digit? (with-generic-mode! (digitize-seq s)))))
  (negative?    [s]              (try-or-false (seq-negative?    (with-numeric-mode! (digitize-seq s)))))
  (numeric?     [s]              (try-or-false (some-byte-digit? (with-numeric-mode! (digitize-seq s)))))
  (digits->seq
    ([s]                         (digitize-seq s))
    ([s, ^Number nt]             (digitize-seq s nt))
    ([s, ^Number nd, ^Number nt] (digitize-seq s nd nt)))
  (digits->str
    ([s]                         (seq-digits->str (digits->seq s)))
    ([s, ^Number nt]             (seq-digits->str (digits->seq s nt)))
    ([s, ^Number nd, ^Number nt] (seq-digits->str (digits->seq s nd nt))))
  (digits->num
    ([s]                         (seq-digits->num (with-numeric-mode! (digits->seq s))))
    ([s, ^Number nt]             (seq-digits->num (with-numeric-mode! (digits->seq s nt))))
    ([s, ^Number nd, ^Number nt] (seq-digits->num (with-numeric-mode! (digits->seq s nd nt)))))

  clojure.lang.Symbol

  (count-digits [s]              (seq-count-digits (digitize-seq (name s))))
  (digitize     [s]              (when-let [x (digitize (name s))] (symbol x)))
  (digital?     [s]              (digital?    (name s)))
  (negative?    [s]              (negative?   (name s)))
  (numeric?     [s]              (numeric?    (name s)))
  (digits->seq
    ([s]                         (digits->seq (name s)))
    ([s, ^Number nt]             (digits->seq (name s) nt))
    ([s, ^Number nd, ^Number nt] (digits->seq (name s) nd nt)))
  (digits->str
    ([s]                         (digits->str (name s)))
    ([s, ^Number nt]             (digits->str (name s) nt))
    ([s, ^Number nd, ^Number nt] (digits->str (name s) nd nt)))
  (digits->num
    ([s]                         (digits->num (name s)))
    ([s, ^Number nt]             (digits->num (name s) nt))
    ([s, ^Number nd, ^Number nt] (digits->num (name s) nd nt)))

  clojure.lang.Keyword

  (count-digits [s]              (seq-count-digits (digitize-seq (name s))))
  (digitize     [s]              (when-let [x (digitize (name s))] (keyword x)))
  (digital?     [s]              (digital?    (name s)))
  (negative?    [s]              (negative?   (name s)))
  (numeric?     [s]              (numeric?    (name s)))
  (digits->seq
    ([s]                         (digits->seq (name s)))
    ([s, ^Number nt]             (digits->seq (name s) nt))
    ([s, ^Number nd, ^Number nt] (digits->seq (name s) nd nt)))
  (digits->str
    ([s]                         (digits->str (name s)))
    ([s, ^Number nt]             (digits->str (name s) nt))
    ([s, ^Number nd, ^Number nt] (digits->str (name s) nd nt)))
  (digits->num
    ([s]                         (digits->num (name s)))
    ([s, ^Number nt]             (digits->num (name s) nt))
    ([s, ^Number nd, ^Number nt] (digits->num (name s) nd nt)))

  java.lang.Character

  (count-digits [c]              (if (digit? (digitize-char c)) 1 0))
  (digitize     [c]              (digitize-char c))
  (digital?     [c]              (try-or-false (byte-digit?   (with-generic-mode! (digitize-char c)))))
  (negative?    [c]              (try-or-false (seq-negative? (with-numeric-mode! (digitize-char c)))))
  (numeric?     [c]              (try-or-false (byte-digit?   (with-numeric-mode! (digitize-char c)))))
  (digits->seq
    ([c]                         (not-empty (lazy-seq (cons (digitize-char c) nil))))
    ([c, ^Number nt]             (subseq-signed (digits->seq c) 0 nt))
    ([c, ^Number nd, ^Number nt] (subseq-signed (digits->seq c) nd nt)))
  (digits->str
    ([c]                         (not-empty (str (digitize-char c))))
    ([c, ^Number nt]             (subs-signed (digits->str c) 0 nt))
    ([c, ^Number nd, ^Number nt] (subs-signed (digits->str c) nd nt)))
  (digits->num
    ([c]                         (some-> (digitize-char c)     int))
    ([c, ^Number nt]             (some-> (digits->str c nt)    Integer/parseInt))
    ([c, ^Number nd, ^Number nt] (some-> (digits->str c nd nt) Integer/parseInt)))

  java.lang.Number

  (count-digits [n]              (num-count-digits (digitize-num n)))
  (digitize     [n]              (digitize-num n))
  (digital?     [n]              (try-or-false (some? (digitize-num n))))
  (negative?    [n]              (neg? n))
  (numeric?     [n]              (digital? n))
  (digits->seq
    ([n]                         (num->digits (digitize-num n)))
    ([n, ^Number nt]             (subseq-signed (digits->seq n) 0 nt))
    ([n, ^Number nd, ^Number nt] (subseq-signed (digits->seq n) nd nt)))
  (digits->num
    ([n]                         (digitize-num n))
    ([n, ^Number nt]             (digits->num (digits->seq n nt)))
    ([n, ^Number nd, ^Number nt] (digits->num (digits->seq n nd nt))))
  (digits->str
    ([n]                         (not-empty (str  (digitize-num n))))
    ([n, ^Number nt]             (seq-digits->str (digits->seq n nt)))
    ([n, ^Number nd, ^Number nt] (seq-digits->str (digits->seq n nd nt))))

  clojure.lang.Fn

  (count-digits [o] (count-digits   (cons o ())))
  (digitize     [o] (first (digitize(cons o ()))))
  (digital?     [o] (digital?       (cons o ())))
  (negative?    [o] (negative?      (cons o ())))
  (numeric?     [o] (numeric?       (cons o ())))
  (digits->seq
    ([o]              (digits->seq (cons o ())))
    ([o nt]           (digits->seq (cons o ()) nt))
    ([o nd nt]        (digits->seq (cons o ()) nd nt)))
  (digits->num
    ([o]             (digits->num (cons o ())))
    ([o nt]          (digits->num (cons o ()) nt))
    ([o nd nt]       (digits->num (cons o ()) nd nt)))
  (digits->str
    ([o]             (digits->str (cons o ())))
    ([o nt]          (digits->str (cons o ()) nt))
    ([o nd nt]       (digits->str (cons o ()) nd nt)))

  nil

  (count-digits   [o]     0)
  (digitize       [o]   nil)
  (digital?       [o] false)
  (negative?      [o] false)
  (numeric?       [o] false)
  (digits->seq
    ([o]       nil)
    ([o nt]    nil)
    ([o nd nt] nil))
  (digits->num
    ([o]       nil)
    ([o nt]    nil)
    ([o nd nt] nil))
  (digits->str
    ([o]       nil)
    ([o nt]    nil)
    ([o nd nt] nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Type checking

(defn digitizing?
  "Returns true if coll satisfies the Digitizing protocol."
  {:added "1.0.0"
   :tag Boolean}
  [coll]
  (satisfies? Digitizing coll))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Safe wrappers

(defn try-digitize
  {:added "1.0.0"}
  [o] (try-arg-nil (doall (digitize o))))

(defn try-count-digits
  {:added "1.0.0"
   :tag Number}
  [o] (if-some [o (try-arg-nil (count-digits o))] o 0))

(defn try-digits->num
  {:added "1.0.0"
   :tag Number}
  ([o    ] (try-arg-nil (digits->num o)))
  ([o m  ] (try-arg-nil (digits->num o m)))
  ([o m n] (try-arg-nil (digits->num o m n))))

(defn try-digits->seq
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([o    ] (try-arg-nil (doall (digits->seq o))))
  ([o m  ] (try-arg-nil (doall (digits->seq o m))))
  ([o m n] (try-arg-nil (doall (digits->seq o m n)))))

(defn try-digits->str
  {:added "1.0.0"
   :tag String}
  ([o    ] (try-arg-nil (digits->str o)))
  ([o m  ] (try-arg-nil (digits->str o m)))
  ([o m n] (try-arg-nil (digits->str o m n))))

;; TODO: subseq-preserve should count just numeric characters and stop after collecting the given number of digits
;;       maybe rename it to subseq-num
