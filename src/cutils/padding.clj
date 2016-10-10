(ns

    ^{:doc    "cutils, safe padding."
      :author "Paweł Wilk"}

    cutils.padding

  (:require [cutils.core]))

(cutils.core/init)

(defn- pad-with-fn-seq
  "Pads sequential collection with results of the given function f called upto
  times without invoking count on all its elements (and realizing their
  values). Returns lazy sequence."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  [^clojure.lang.Fn      f
   ^clojure.lang.ISeq coll
   ^long              upto]
  ((fn fun
     [^clojure.lang.ISeq s, ^long i]
     (lazy-seq
      (if s
        (cons (first s) (fun (next s) (inc i)))
        (when (< i upto) (map f (range i upto))))))
   coll 0))

(defn pad-with-fn
  "Pads a collection given as coll with results of calling f. The number of
  elements added to a resulting collection will be calculated using the value
  of upto argument which specifies how large the collection should ultimately
  be.

  The given function f should take one argument which will contain integer
  index of currently generated element, starting from the index of first
  padded element. Example:

  (pad-with-fn [0 1 2] 5) ;=> [0 1 2 3 4]

  If no f is given then the padding is done with identity function which
  effectively puts index numbers of elements as elements.

  If the optional, fourth argument front? is true then padding values will be
  added to the beginning of a resulting collection. That also changes values
  of an argument passed during successive f calls as they will start with
  negative number and grow to -1. Example:

  (pad-with-fn identity [0 1 2] 5 true) ;=> [-2 -1 0 1 2]

  If the value of coll is a vector then a proper functions will be used and
  the result will also be a vector. Be aware that prepending to vectors (when
  front? is set to true) will internally create temporary vector.

  When appending to the end of a sequential collection its elements are not
  realized since no count is invoked. When appending to a vector or when
  prepending the count function is called (in case of a lazy sequence it
  means that values of its elements are realized)."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([^clojure.lang.ISeq coll
    ^long              upto]
   (pad-with-fn identity coll upto false))
  ([^clojure.lang.Fn   f
    ^clojure.lang.ISeq coll
    ^long              upto]
   (pad-with-fn f coll upto false))
  ([^clojure.lang.Fn   f
    ^clojure.lang.ISeq coll
    ^long              upto
    ^Boolean           front?]
   (let [ve? (vector? coll)]
     (if-not (or ve? front?)
       (pad-with-fn-seq f coll upto)
       (let [iop (if ve? into concat)
             cnt (count coll)
             rng (if front?
                   (range (- cnt upto) 0)
                   (range cnt upto))
             pdv (not-empty (map f rng))
             src (if front?
                   (list (if ve? (vec pdv) pdv) coll)
                   (list coll pdv))]
         (apply iop src))))))

(defn pad
  "Pads a collection given as coll with a value given as val. The number of
  elements added to a resulting collection will be calculated using the value
  of upto argument which specifies how large the collection should ultimately
  be. Examples:

  (pad [0 1 2] 5)     ;=> [0 1 2 nil nil]
  (pad [0 1 2] 5 :pa) ;=> [0 1 2 :pa :pa]

  If no val is given then the padding is done using nil values.

  If the optional, fourth argument front? is true then padding values will be
  added to the beginning of a resulting collection. Example:

  (pad [0 1 2] 5 :pa true) ;=> [:pa :pa 0 1 2]

  If the value of coll is a vector then a proper functions will be used and
  the result will also be a vector. Be aware that prepending to vectors (when
  front? is set to true) will internally create temporary vector.

  When appending to the end of a sequential collection its elements are not
  realized since no count is invoked. When appending to a vector or when
  prepending the count function is called (in case of a lazy sequence it
  means that values of its elements are realized)."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([^clojure.lang.ISeq coll, ^long upto]
   (pad-with-fn (constantly nil) coll upto))
  ([^clojure.lang.ISeq coll, ^long upto, val]
   (pad-with-fn (constantly val) coll upto))
  ([^clojure.lang.ISeq coll, ^long upto, val, ^Boolean front?]
   (pad-with-fn (constantly val) coll upto front?)))

(defn pad-front
  "Pads a collection given as coll with a value given as val by filling first
  elements with the given value. The number of elements added to a resulting
  collection will be calculated using the value of upto argument which
  specifies how large the collection should ultimately be. Examples:

  (pad-front [0 1 2] 5)     ;=> [nil nil 0 1 2]
  (pad-front [0 1 2] 5 :pa) ;=> [:pa :pa 0 1 2]

  If no val is given then the padding is done using nil values.

  If the value of coll is a vector then a proper functions will be used and
  the result will also be a vector. Be aware that prepending to vectors (when
  front? is set to true) will internally create temporary vector."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([^clojure.lang.ISeq coll, ^long upto]
   (pad coll upto nil true))
  ([^clojure.lang.ISeq coll, ^long upto, val]
   (pad coll upto val true)))
