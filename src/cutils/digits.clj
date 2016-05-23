(ns

    ^{:doc    "cutils library, collections of digits."
      :author "Paweł Wilk"}

    (:require [cutils.strings :refer :all
               cutils.ranges  :refer :all])

  cutils.digits)

(cutils.core/init)

(defn- add-minus
  "Adds minus sign to a collection coll if a number given as n is negative."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  [^java.lang.Number n
   ^clojure.lang.ISeq coll]
  (if (neg? n) (cons \- coll) coll))

(defn- pad-digits-with-zeros
  "Helper that pads sequence of digits with zeros."
  [^java.lang.Number min-digits
   [^java.lang.Number num-items
    ^clojure.lang.ISeq coll]]
  (pad coll (- min-digits num-items) 0 true))

(defn num->digits
  "Changes a number given as n into a sequence of numbers representing decimal
  digits. If min-digits argument is given then it pads the returned sequence
  with leading zeros to satisfy the number of elements."
  ([^java.lang.Number n]
   (loop [current n
          result ()]
     (if (zero? current)
       (add-minus n result)
       (recur (quot current 10)
              (cons (mod current 10) result)))))
  ([^java.lang.Number min-digits
    ^java.lang.Number n]
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
  ([^java.lang.Number min-digits
    ^java.lang.Number max-digits
    ^java.lang.Number n]
   (add-minus
    n (pad-digits-with-zeros
       min-digits
       (loop [current n
              result ()
              processed 0]
         (if (or (zero? current) (>= c max-digits))
           (cons processed result)
           (recur (quot current 10)
                  (cons (mod current 10) result)
                  (inc processed))))))))

(defn str->digits
  "Changes string of digits into a sequence of digits."
  [^java.lang.String s]
  (digitalize-seq (split s))) ;; FIXME

;; Digitalizing protocol

(defprotocol Digitalizing
  "States that collection is able to store digits that could be then used to
  produce valid numeric values."

  (digitalize
   [coll]
   "Ensures that coll is digital by cleaning it and performing basic
  validation. If the process succeeded it returns cleaned version of coll,
  otherwise it returns nil. Digital means that the collection consist of
  numbers from 0 to 9 and optional + or - sign in front.")

  (digits->num
   [coll] [coll num-take] [coll num-drop num-take]
   "Changes a collection of digits given as coll into integer number. An
  optional argument num-take controls how many digits to use (from left to
  right) and num-drop tells how many digits to drop before collecting
  number. The last one (num-drop) is applied before num-take when both are
  given. The collection is NOT validated NOR coerced before but there
  is cutils.digits/digitalize function that can do that.

  The function returns an integer or nil if something went wrong (e.g. empty
  collection was given or ranges were mismatched).")

  (digits->str
   [coll] [coll num-take] [coll num-drop num-take]
   "Changes a collection of digits given as coll into string containing
  integer number. An optional argument num-take controls how many digits to
  use (from left to right) and num-drop tells how many digits to drop before
  collecting number. The last one (num-drop) is applied before num-take when
  both are given. The collection is NOT validated NOR coerced before but there
  is cutils.digits/digitalize function that can do that.

  The function returns a string or nil if something went wrong (e.g. empty
  collection was given or ranges were mismatched)."))

(def whitechars
  "Blank characters to remove from digitalized sequences."
  ^{:added "1.0.0"
    :private true
    :tag clojure.lang.IPersistentSet
    :const true}
  #{nil \space \newline \tab \formfeed \return (char 0x0B)})

(def digitchars
  ^{:added "1.0.0"
    :private true
    :tag clojure.lang.IPersistentSet
    :const true}
  (set (range 0 10)))

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

(def s-to-chars
  ^{:added "1.0.0"
    :private true
    :tag clojure.lang.IPersistentMap
    :const true}
  {\- \-, - \-, :- \-, '- \-, "-" \-
   \+ \+, + \+, :+ \+, \+ \+, "+" \+})

(def sign-chars
  ^{:added "1.0.0"
    :private true
    :tag clojure.lang.IPersistentSet
    :const true}
  (set (keys s-to-chars)))

(defn subs-signed
  {:added "1.0.0"
   :tag java.lang.String}
  ([^java.lang.String            s
    ^java.lang.Number        start]
   (subs-preserve s sign-chars start))
  ([^java.lang.String            s
    ^java.lang.Number        start
    ^java.lang.Number          end]
   (subs-preserve s sign-chars start end)))

(defn subvec-signed
  {:added "1.0.0"
   :tag clojure.lang.IPersistentVector}
  ([^clojure.lang.IPersistentVector v
    ^java.lang.Number           start]
   (subvec-preserve s sign-chars start))
  ([^clojure.lang.IPersistentVector v
    ^java.lang.Number           start
    ^java.lang.Number             end]
   (subs-preserve s sign-chars start end)))

(defn- fix-sign-seq
  [^clojure.lang.ISeq coll]
  (let [first-char  (first  coll)
        second-char (second coll)]
    (not-empty
     (cond
       (number? first-char) coll
       (= \+    first-char) (if (number? second-char) (next coll) nil)
       (= \-    first-char) (if (number? second-char) coll nil)
       :default nil))))

(defn digitalize-seq
  [^clojure.lang.ISeq coll]
  (fix-sign-seq
   (loop [acc () src coll]
     (let [e (first src), n (next src)]
       (if (whitechars e)
         (recur acc n)
         (if-let [c (s-to-chars e)]
           (if (empty? acc) (recur (cons c acc) n) nil)
           (when-let [c (chars-to-digits e)]
             (recur (cons c acc) n))))))))

(defn digitalize-vec
  [^clojure.lang.IPersistentVector coll]
  (fix-sign-seq
   (loop [acc () src coll]
     (let [e (peek src), n (pop src)]
       (if (whitechars e)
         (recur acc n)
         (if-let [c (s-to-chars e)]
           (when (zero? (dec (count n))) (recur (cons c acc) n))
           (when-let [c (chars-to-digits e)]
             (recur (cons c acc) n))))))))

(defn- digitalize-str
  [^java.lang.String s]
  (when-some [s (remove-white-chars s)]
    (when (re-find #"^[+-]?\d+$" s)
      (not-empty (if (= \+ (get s 0)) (subs s 1) s)))))

(defn- seq-digits->num
  "Warning: nil or false element ends iteration."
  {:added "1.0.0"
   :tag java.lang.Number}
  ([^clojure.lang.ISeq d]
   (when-not (empty? d)
     (loop [x (reverse d) r 0 i 1]
       (if (nil? x)
         r
         (recur (next x) (+' r (*' (first x) i)) (*' 10 i))))))
  ([^clojure.lang.ISeq d
    ^java.lang.Number num-take]
   (seq-digits->num (take num-take d)))
  ([^clojure.lang.ISeq d
    ^java.lang.Number num-drop
    ^java.lang.Number num-take]
   (seq-digits->num (drop-take num-drop num-take d))))

(defn- vec-digits->num
  "Warning: nil or false element ends iteration."
  {:added "1.0.0"
   :tag java.lang.Number}
  ([^clojure.lang.IPersistentVector d]
   (when (contains? d 0)
     (loop [x d r 0 i 1]
       (if-let [n (peek x)]
         (recur (pop x) (+' r (*' n i)) (*' 10 i))
         r))))
  ([^clojure.lang.IPersistentVector d
    ^java.lang.Number num-take]
   (vec-digits->num (subvec-signed d 0 num-take)))
  ([^clojure.lang.IPersistentVector d
    ^java.lang.Number num-drop
    ^java.lang.Number num-take]
   (vec-digits->num (subvec-signed d num-drop (+ num-drop num-take)))))

(extend-protocol Digitalizing
  clojure.lang.IPersistentVector
  (digitalize  [d]         (not-empty (vec (digitalize-vec d))))
  (digits->num
      ([d]                 (seq-digits->num (digitalize-vec d)))
    ([d num-take]          (seq-digits->num (digitalize-vec d) num-take)) ;; bezpieczne dzielenie?
    ([d num-drop num-take] (seq-digits->num (digitalize-vec d) num-drop num-take)))
  (digits->str
      ([d]                 (not-empty (reduce str (digitalize-vec d))))
    ([d num-take]          (not-empty (reduce str (subvec-signed (digitalize-vec d) num-take))))
    ([d num-drop num-take] (not-empty (reduce str (subvec-signed (digitalize-vec d) num-drop (+ num-drop num-take))))))

  clojure.lang.ISeq
  (digitalize [d]          (digitalize-seq d))
  (digits->num
      ([d]                 (seq-digits->num (digitalize-seq d)))
    ([d num-take]          (seq-digits->num (digitalize-seq d) num-take))
    ([d num-drop num-take] (seq-digits->num (digitalize-seq d) num-drop num-take)))
  (digits->str
      ([d]                 (not-empty (reduce str (digitalize-seq d))))
    ([d num-take]          (not-empty (reduce str (take num-take (digitalize-seq d)))))
    ([d num-drop num-take] (not-empty (reduce str (drop-take num-drop num-take (digitalize-seq d))))))

  java.lang.String
  (digitalize [d]          (digitalize-str d))
  (digits->num
      ([d]                 (Integer/parseInt (digitalize-str d)))
    ([d num-take]          (Integer/parseInt (subs-signed (digitalize-str d) 0 num-take)))
    ([d num-drop num-take] (Integer/parseInt (subs-signed (digitalize-str d) num-drop (+ num-drop num-take)))))
  (digits->str
      ([d]                 (digitalize-str d))
    ([d num-take]          (subs-signed (digitalize-str d) num-take))
    ([d num-drop num-take] (subs-signed (digitalize-str d) num-drop (+ num-drop num-take))))

  java.lang.Number
  (digitalize [d]          d)
  (digits->num
      ([d]                 d)
    ([num-take d]          (digits->num (num->digits d) num-take)) ;; bezpieczne dzielenie?
    ([num-drop num-take d] (digits->num (num->digits d) num-drop num-take)))
  (digits->str
      ([d]                 (str d))
    ([d num-take]          (subs-signed (str d) num-take))
    ([d num-drop num-take] (subs-signed (str d) num-drop (+ num-drop num-take))))

  nil
  (digitalize [d]          nil)
  (digits->num
      ([d]                 nil)
    ([d num-take]          nil)
    ([d num-drop num-take] nil))
  (digits->str
      ([d]                 nil)
    ([d num-take]          nil)
    ([d num-drop num-take] nil)))

(defn digital?
  "Checks if a given collection is digital. Returns true if it is, false
  otherwise. Digital means that the collection consist of numbers from 0 to 9
  and optional + or - sign in front."
  [d]
  (boolean (digitalize d)))
